package dev.stiemannkj1.bytecode.namespacer;

import static dev.stiemannkj1.util.Assert.assertNotNull;

import dev.stiemannkj1.collection.arrays.GrowableArrays.GrowableByteArray;
import dev.stiemannkj1.util.WithReusableStringBuilder;
import java.io.BufferedInputStream;
import java.io.BufferedOutputStream;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.nio.file.Files;
import java.nio.file.StandardCopyOption;
import java.nio.file.StandardOpenOption;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.zip.ZipEntry;
import java.util.zip.ZipInputStream;
import java.util.zip.ZipOutputStream;

public final class NamespacerMain {

  private static final class ObjectPool extends WithReusableStringBuilder {
    private Boolean first = Boolean.TRUE;
    private byte[] buffer = new byte[1 << 14];
    private Map<String, Boolean> processedFiles = new HashMap<>(1 << 8);
    private Namespacer.ObjectPool namespacer;
    private GrowableByteArray classFileBefore;
    private GrowableByteArray classFileAfter;
    private List<String> errors = new ArrayList<String>();

    public ObjectPool(
        final Namespacer.ObjectPool namespacer,
        final GrowableByteArray classFileBefore,
        final GrowableByteArray classFileAfter) {
      this.namespacer = assertNotNull(namespacer);
      this.classFileBefore = assertNotNull(classFileBefore);
      this.classFileAfter = assertNotNull(classFileAfter);
    }
  }

  public static void namespaceJars(
      final List<File> jarsToNamespace,
      final Map<String, String> replacementsMap,
      final File outputJar,
      final int initialClassFileBufferCapacity)
      throws IOException {

    final File outputJarParent = outputJar.getParentFile();
    final File partialJar = new File(outputJarParent, outputJar.getName() + ".PART.zip");
    final ObjectPool objectPool =
        new ObjectPool(
            new Namespacer.ObjectPool(replacementsMap),
            new GrowableByteArray(initialClassFileBufferCapacity),
            new GrowableByteArray(initialClassFileBufferCapacity));
    IOException error = null;

    try (final OutputStream outputStream =
            Files.newOutputStream(
                partialJar.toPath(),
                StandardOpenOption.CREATE,
                // TODO replace with truncating stream.
                StandardOpenOption.TRUNCATE_EXISTING);
        final BufferedOutputStream bufferedOutputStream = new BufferedOutputStream(outputStream);
        final ZipOutputStream jarOutputStream = new ZipOutputStream(bufferedOutputStream)) {

      for (final File jarToNamespace : jarsToNamespace) {
        namespaceJar(objectPool, replacementsMap, jarToNamespace, jarOutputStream);
        objectPool.first = Boolean.FALSE;
      }
    } catch (final IOException e) {
      error = e;
    }

    for (final String errorMessage : objectPool.errors) {

      if (error == null) {
        error = new IOException("Failed to namespace files in JAR.");
      }

      error.addSuppressed(
          new IOException(errorMessage) {
            // We only add this exception for the message, so don't capture the stacktrace since
            // it's extremely slow.
            @Override
            public synchronized Throwable fillInStackTrace() {
              return this;
            }
          });
    }

    if (error != null) {
      final boolean ignored = outputJar.delete();
      throw error;
    }

    Files.move(partialJar.toPath(), outputJar.toPath(), StandardCopyOption.REPLACE_EXISTING);
  }

  private static void namespaceJar(
      final ObjectPool objectPool,
      final Map<String, String> replacementsMap,
      final File jarToNamespace,
      final ZipOutputStream jarOutputStream)
      throws IOException {

    try (final InputStream inputStream = Files.newInputStream(jarToNamespace.toPath());
        // TODO create reusable buffered input stream and pass in
        final BufferedInputStream bufferedInputStream = new BufferedInputStream(inputStream);
        // TODO create reusable hacked/reflective zip input stream and pass in
        final ZipInputStream jarInputStream = new ZipInputStream(bufferedInputStream)) {

      ZipEntry zipEntryToRead;

      while ((zipEntryToRead = jarInputStream.getNextEntry()) != null) {

        String name = zipEntryToRead.getName();

        // Ignore duplicates if they duplicate files in the first JAR so that callers can handle
        // duplicates by overriding them from their own JAR.
        final Boolean fileWasOverridden =
            objectPool.processedFiles.putIfAbsent(name, assertNotNull(objectPool.first));

        if (fileWasOverridden != null) {
          if (fileWasOverridden == Boolean.FALSE) {
            objectPool.errors.add(
                objectPool
                    .resetStringBuilder()
                    .append("Duplicate file ")
                    .append(name)
                    .append(" found in ")
                    .append(jarToNamespace.getAbsolutePath())
                    .append(". Exclude the file or override it in the current project.")
                    .toString());
          }
          continue;
        }

        boolean serviceFile = false;

        for (int i = 0; i < objectPool.namespacer.replacements.paths; i++) {

          // specify if service file here
          // serviceFile = true;
          if (name.startsWith(objectPool.namespacer.replacements.beforePath[i])) {
            name =
                objectPool
                    .resetStringBuilder()
                    .append(objectPool.namespacer.replacements.afterPath[i])
                    .append(
                        name,
                        objectPool.namespacer.replacements.beforePath[i].length(),
                        name.length())
                    .toString();
            break;
          }
        }

        final ZipEntry zipEntryToWrite = new ZipEntry(name);

        // TODO test and verify that this allows for reproducible JARs.
        zipEntryToWrite.setTime(zipEntryToRead.getTime());
        jarOutputStream.putNextEntry(zipEntryToWrite);

        if (zipEntryToWrite.isDirectory()) {
          continue;
        }

        if (serviceFile) {
          Namespacer.namespaceServiceFile(
              objectPool.namespacer,
              objectPool.classFileBefore,
              replacementsMap,
              objectPool.classFileAfter);
          continue;
        }

        if (!name.endsWith(".class")) {

          // TODO handle service files
          // TODO should we handle JSPs and .java,.groovy,.kts,.gradle,.kotlin,.scala sources?
          // TODO should we handle Log4j2's binary cache file?

          int read;

          while ((read = jarInputStream.read(objectPool.buffer, 0, objectPool.buffer.length))
              > -1) {
            jarOutputStream.write(objectPool.buffer, 0, read);
          }

          continue;
        }

        GrowableByteArray.clear(objectPool.classFileBefore);
        GrowableByteArray.clear(objectPool.classFileAfter);
        GrowableByteArray.readFully(objectPool.classFileBefore, jarInputStream);

        final String result =
            Namespacer.namespaceClassFile(
                objectPool.namespacer,
                zipEntryToRead.getName(),
                objectPool.classFileBefore,
                replacementsMap,
                objectPool.classFileAfter);

        if (result != null) {
          objectPool.errors.add(result);
          continue;
        }

        jarOutputStream.write(
            GrowableByteArray.bytes(objectPool.classFileAfter),
            0,
            GrowableByteArray.size(objectPool.classFileAfter));
      }
    }
  }

  //    public static void namespaceFiles(final File directory) {
  //       // TODO focus on JARs first
  //       // iterating the file tree in Java always allocates for each dir sadly. Either you're
  // allocating with File#list or Files#newDirectoryStream and the system doesn't allow array or
  // stream reuse. Writing our own filesystem stuff is out-of-scope
  //        Files.newDirectoryStream()
  //
  //        Files.list()
  //                dir.list()
  //        Files.walkFileTree()
  //    }

  private NamespacerMain() {}
}
