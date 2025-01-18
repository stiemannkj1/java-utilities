package dev.stiemannkj1.bytecode.namespacer;

import static dev.stiemannkj1.bytecode.ClassGenerator.ClassUtil.readClassBytes;
import static dev.stiemannkj1.bytecode.ClassGenerator.ClassUtil.toEntryPath;
import static dev.stiemannkj1.bytecode.namespacer.Namespacer.ObjectPool.DEFAULT_MAX_CLASS_FILE_MAJOR_VERSION;
import static dev.stiemannkj1.util.Assert.assertNotEmpty;
import static dev.stiemannkj1.util.Assert.assertNotNull;
import static dev.stiemannkj1.util.Assert.assertPositive;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.BufferedInputStream;
import java.io.BufferedOutputStream;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.URL;
import java.net.URLClassLoader;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.time.Instant;
import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Deque;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.ServiceLoader;
import java.util.stream.Collectors;
import java.util.zip.ZipEntry;
import java.util.zip.ZipInputStream;
import java.util.zip.ZipOutputStream;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

public final class NamespacerMainTests {

  @Test
  void namespacesMultipleJars(final @TempDir File tempDir)
      throws IOException, ReflectiveOperationException {

    // Note: minimum timestamp precision is 2000ms.
    final long timestampMs = Instant.parse("2000-01-01T00:00:00.00Z").toEpochMilli();
    final File jar1File = new File(tempDir, "jar1.jar");

    // TODO maybe handle log4j, but maybe not

    try (OutputStream outputStream = Files.newOutputStream(jar1File.toPath());
        BufferedOutputStream bufferedOutputStream = new BufferedOutputStream(outputStream);
        ZipOutputStream jarOutputStream = new ZipOutputStream(bufferedOutputStream)) {
      writeEntryForClass(jarOutputStream, OverriddenClassInBoth.class, timestampMs + 2000);
      writeEntryForClass(jarOutputStream, Class1Jar1.class, timestampMs + 4000);
      writeEntryForClass(jarOutputStream, Class2Jar1.class, timestampMs + 6000);
      writeEntryForClass(jarOutputStream, KeepNamespaceTestService.class, timestampMs + 8000);
      writeEntryForClass(jarOutputStream, KeepNamespaceTestServiceImpl.class, timestampMs + 10_000);
      writeEntry(
          jarOutputStream,
          "META-INF/services/dev.stiemannkj1.bytecode.namespacer.NamespacerMainTests$MyService",
          (MyServiceImpl1.class.getTypeName()
                  + "\n"
                  + MyServiceImpl2.class.getTypeName()
                  + "\n# My comment\n"
                  + KeepNamespaceTestServiceImpl.class.getTypeName()
                  + "\n")
              .getBytes(StandardCharsets.UTF_8),
          timestampMs + 12_000);
      writeEntry(
          jarOutputStream,
          "META-INF/services/dev.stiemannkj1.bytecode.namespacer.KeepNamespaceTestService",
          (MyServiceImpl1.class.getTypeName()
                  + "\n"
                  + MyServiceImpl2.class.getTypeName()
                  + "\n# My comment\n"
                  + KeepNamespaceTestServiceImpl.class.getTypeName()
                  + "\n")
              .getBytes(StandardCharsets.UTF_8),
          timestampMs + 14_000);
    }

    final File jar2File = new File(tempDir, "jar2.jar");

    try (OutputStream outputStream = Files.newOutputStream(jar2File.toPath());
        BufferedOutputStream bufferedOutputStream = new BufferedOutputStream(outputStream);
        ZipOutputStream jarOutputStream = new ZipOutputStream(bufferedOutputStream)) {
      writeEntryForClass(jarOutputStream, OverriddenClassInBoth.class, timestampMs + 16_000);
      writeEntryForClass(jarOutputStream, Class1Jar2.class, timestampMs + 18_000);
      writeEntryForClass(jarOutputStream, Class2Jar2.class, timestampMs + 20_000);
      writeEntry(
          jarOutputStream,
          "META-INF/services/dev.stiemannkj1.bytecode.namespacer.NamespacerMainTests$MyService",
          "ShouldBeOverridden".getBytes(StandardCharsets.UTF_8),
          timestampMs + 22_000);
      writeEntryForClass(jarOutputStream, MyService.class, timestampMs + 24_000);
      writeEntryForClass(jarOutputStream, MyServiceImpl1.class, timestampMs + 26_000);
      writeEntryForClass(jarOutputStream, MyServiceImpl2.class, timestampMs + 28_000);
      writeEntry(
          jarOutputStream,
          "META-INF/groovy/dev.stiemannkj1.bytecode.namespacer.NamespacerMainTests$MyService",
          MyServiceImpl1.class.getTypeName().getBytes(StandardCharsets.UTF_8),
          timestampMs + 30_000);
    }

    final Map<String, String> replacements = new HashMap<>();
    replacements.put("dev.stiemannkj1.bytecode.namespacer.NamespacerMainTests$", "foo.bar.");

    final File namespacedJar = new File(tempDir, "namespaced.jar");
    NamespacerMain.namespaceJars(
        true,
        Arrays.asList(jar1File, jar2File),
        replacements,
        namespacedJar,
        1 << 10,
        DEFAULT_MAX_CLASS_FILE_MAJOR_VERSION);

    final class ExpectedEntry {
      private final String name;
      private final String className;
      private final String expectedPrefix;
      private final long timestampMs;

      private ExpectedEntry(
          final String className,
          final String name,
          final long timestampMs,
          final String expectedPrefix) {
        this.className = className != null ? assertNotEmpty(className) : null;
        this.name = assertNotEmpty(name);
        this.timestampMs = assertPositive(timestampMs, "timestampMs");
        this.expectedPrefix = className != null ? assertNotEmpty(expectedPrefix) : null;
      }
    }

    final Deque<ExpectedEntry> expectedEntries = new ArrayDeque<>();
    expectedEntries.addLast(
        new ExpectedEntry(
            "foo.bar.OverriddenClassInBoth",
            "foo/bar/OverriddenClassInBoth.class",
            timestampMs + 2000,
            "foo.bar."));
    expectedEntries.addLast(
        new ExpectedEntry(
            "foo.bar.Class1Jar1", "foo/bar/Class1Jar1.class", timestampMs + 4000, "foo.bar."));
    expectedEntries.addLast(
        new ExpectedEntry(
            "foo.bar.Class2Jar1", "foo/bar/Class2Jar1.class", timestampMs + 6000, "foo.bar."));
    expectedEntries.addLast(
        new ExpectedEntry(
            KeepNamespaceTestService.class.getTypeName(),
            toEntryPath(KeepNamespaceTestService.class),
            timestampMs + 8000,
            "dev.stiemannkj1.bytecode.namespacer."));
    expectedEntries.addLast(
        new ExpectedEntry(
            KeepNamespaceTestServiceImpl.class.getTypeName(),
            toEntryPath(KeepNamespaceTestServiceImpl.class),
            timestampMs + 10_000,
            "dev.stiemannkj1.bytecode.namespacer."));
    expectedEntries.addLast(
        new ExpectedEntry(null, "META-INF/services/foo.bar.MyService", timestampMs + 12_000, null));
    expectedEntries.addLast(
        new ExpectedEntry(
            null,
            "META-INF/services/dev.stiemannkj1.bytecode.namespacer.KeepNamespaceTestService",
            timestampMs + 14_000,
            null));
    expectedEntries.addLast(
        new ExpectedEntry(
            "foo.bar.Class1Jar2", "foo/bar/Class1Jar2.class", timestampMs + 18_000, "foo.bar."));
    expectedEntries.addLast(
        new ExpectedEntry(
            "foo.bar.Class2Jar2", "foo/bar/Class2Jar2.class", timestampMs + 20_000, "foo.bar."));
    expectedEntries.addLast(
        new ExpectedEntry(
            "foo.bar.MyService", "foo/bar/MyService.class", timestampMs + 24_000, "foo.bar."));
    expectedEntries.addLast(
        new ExpectedEntry(
            "foo.bar.MyServiceImpl1",
            "foo/bar/MyServiceImpl1.class",
            timestampMs + 26_000,
            "foo.bar."));
    expectedEntries.addLast(
        new ExpectedEntry(
            "foo.bar.MyServiceImpl2",
            "foo/bar/MyServiceImpl2.class",
            timestampMs + 28_000,
            "foo.bar."));
    expectedEntries.addLast(
        new ExpectedEntry(null, "META-INF/groovy/foo.bar.MyService", timestampMs + 30_000, null));

    try (InputStream inputStream = Files.newInputStream(namespacedJar.toPath());
        BufferedInputStream bufferedInputStream = new BufferedInputStream(inputStream);
        ZipInputStream jarInputStream = new ZipInputStream(bufferedInputStream);
        URLClassLoader classLoader =
            new URLClassLoader(new URL[] {namespacedJar.toURI().toURL()}, null)) {

      ZipEntry entry;

      while ((entry = jarInputStream.getNextEntry()) != null) {
        final ExpectedEntry expectedEntry = expectedEntries.pop();

        if (expectedEntry.className != null) {
          final Class<?> aClass = classLoader.loadClass(expectedEntry.className);

          final String className = aClass.getTypeName();

          assertTrue(className.startsWith(expectedEntry.expectedPrefix));

          if (!aClass.isInterface()) {
            assertEquals(
                expectedEntry.className,
                assertNotNull(assertNotEmpty(aClass.getConstructors())[0])
                    .newInstance()
                    .toString());
          }
        }

        assertEquals(expectedEntry.name, entry.getName());
        assertEquals(
            expectedEntry.timestampMs,
            entry.getTime(),
            "Wrong timestamp for: " + expectedEntry.name);
      }

      final Class<?> keepNamespaceTestServiceClass =
          classLoader.loadClass(KeepNamespaceTestService.class.getTypeName());

      final List<String> actualServiceStrings = new ArrayList<>();

      for (final Object service : ServiceLoader.load(keepNamespaceTestServiceClass, classLoader)) {
        actualServiceStrings.add(service.toString());
      }

      assertEquals(
          Arrays.asList(
              "foo.bar.MyServiceImpl1",
              "foo.bar.MyServiceImpl2",
              KeepNamespaceTestServiceImpl.class.getTypeName()),
          actualServiceStrings);

      actualServiceStrings.clear();

      for (final Object service :
          ServiceLoader.load(classLoader.loadClass("foo.bar.MyService"), classLoader)) {
        actualServiceStrings.add(service.toString());
      }

      assertEquals(
          Arrays.asList(
              "foo.bar.MyServiceImpl1",
              "foo.bar.MyServiceImpl2",
              KeepNamespaceTestServiceImpl.class.getTypeName()),
          actualServiceStrings);

      final String groovyServiceName;

      try (InputStream groovyServiceFile =
          classLoader.getResourceAsStream("META-INF/groovy/foo.bar.MyService")) {
        groovyServiceName =
            new String(assertNotNull(groovyServiceFile).readAllBytes(), StandardCharsets.UTF_8);
      }

      assertEquals(
          "foo.bar.MyServiceImpl1",
          assertNotNull(
                  assertNotEmpty(classLoader.loadClass(groovyServiceName).getConstructors())[0])
              .newInstance()
              .toString());
    }

    assertTrue(
        expectedEntries.isEmpty(),
        "Expected entries were missing from namespaced jar:\n"
            + expectedEntries.stream().map(entry -> entry.name).collect(Collectors.joining("\n")));
  }

  private static void writeEntry(
      final ZipOutputStream jarOutputStream,
      final String entryPath,
      final byte[] bytes,
      final long timestampMs)
      throws IOException {

    final ZipEntry zipEntry = new ZipEntry(entryPath);
    zipEntry.setTime(timestampMs);
    jarOutputStream.putNextEntry(zipEntry);
    jarOutputStream.write(bytes);
  }

  private static void writeEntryForClass(
      final ZipOutputStream jarOutputStream, final Class<?> aClass, final long timestampMs)
      throws IOException {
    writeEntry(jarOutputStream, toEntryPath(aClass), readClassBytes(aClass), timestampMs);
  }

  public static final class OverriddenClassInBoth {
    @Override
    public String toString() {
      return "dev.stiemannkj1.bytecode.namespacer.NamespacerMainTests$OverriddenClassInBoth";
    }
  }

  public static final class Class1Jar1 {
    @Override
    public String toString() {
      return "dev.stiemannkj1.bytecode.namespacer.NamespacerMainTests$Class1Jar1";
    }
  }

  public static final class Class2Jar1 {
    @Override
    public String toString() {
      return "dev.stiemannkj1.bytecode.namespacer.NamespacerMainTests$Class2Jar1";
    }
  }

  public static final class Class1Jar2 {
    @Override
    public String toString() {
      return "dev.stiemannkj1.bytecode.namespacer.NamespacerMainTests$Class1Jar2";
    }
  }

  public static final class Class2Jar2 {
    @Override
    public String toString() {
      return "dev.stiemannkj1.bytecode.namespacer.NamespacerMainTests$Class2Jar2";
    }
  }

  public interface MyService {
    String getString();
  }

  public static final class MyServiceImpl1 implements KeepNamespaceTestService, MyService {
    @Override
    public String getString() {
      return "dev.stiemannkj1.bytecode.namespacer.NamespacerMainTests$MyServiceImpl1";
    }

    @Override
    public String toString() {
      return getString();
    }
  }

  public static final class MyServiceImpl2 implements MyService, KeepNamespaceTestService {
    @Override
    public String getString() {
      return "dev.stiemannkj1.bytecode.namespacer.NamespacerMainTests$MyServiceImpl2";
    }

    @Override
    public String toString() {
      return getString();
    }
  }
}
