package dev.stiemannkj1.bytecode;

import static dev.stiemannkj1.collection.arrays.GrowableArrays.GrowableByteArray.size;
import static dev.stiemannkj1.util.Assert.assertNotNull;

import dev.stiemannkj1.collection.arrays.GrowableArrays.GrowableByteArray;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;

public final class ClassGenerator extends ClassLoader {

  public Class<?> generateClass(
      final String className, final byte[] classBytes, final int offset, final int length) {
    return defineClass(className, classBytes, offset, length);
  }

  public static void writeClassOnError(
      final Throwable e, final String className, final GrowableByteArray classBytes) {

    try {
      final byte[] failedClass = new byte[size(classBytes)];
      System.arraycopy(GrowableByteArray.bytes(classBytes), 0, failedClass, 0, size(classBytes));
      final File failedClassFile =
          new File(
              System.getProperty("project.build.dir")
                  + File.separator
                  + className.replace(".", File.separator)
                  + ".class");
      final boolean ignored = failedClassFile.getParentFile().mkdirs();
      Files.write(failedClassFile.toPath(), failedClass);
    } catch (final IOException ioe) {
      e.addSuppressed(ioe);
    }

    if (e instanceof RuntimeException) {
      throw ((RuntimeException) e);
    } else if (e instanceof Error) {
      throw ((Error) e);
    } else {
      throw new AssertionError(
          "Expected unchecked exception, but found: " + e.getClass().getTypeName(), e);
    }
  }

  public ClassGenerator(final ClassLoader parent) {
    super(parent);
  }

  public static final class ClassUtil {

    public static String toEntryPath(final Class<?> aClass) {
      return aClass.getTypeName().replace(".", "/") + ".class";
    }

    public static InputStream classAsStream(final Class<?> aClass) {
      return assertNotNull(aClass.getClassLoader().getResourceAsStream(toEntryPath(aClass)));
    }

    public static byte[] readClassBytes(final Class<?> aClass) throws IOException {
      try (InputStream inputStream = classAsStream(aClass)) {
        return inputStream.readAllBytes();
      }
    }

    private ClassUtil() {}
  }
}
