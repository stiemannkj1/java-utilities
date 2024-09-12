package dev.stiemannkj1.bytecode.namespacer;

import static dev.stiemannkj1.bytecode.ClassGenerator.ClassUtil.readClassBytes;
import static dev.stiemannkj1.bytecode.ClassGenerator.ClassUtil.toEntryPath;
import static dev.stiemannkj1.util.Assert.assertNotEmpty;
import static dev.stiemannkj1.util.Assert.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertEquals;

import java.io.BufferedOutputStream;
import java.io.File;
import java.io.IOException;
import java.io.OutputStream;
import java.net.URL;
import java.net.URLClassLoader;
import java.nio.file.Files;
import java.time.Instant;
import java.util.Arrays;
import java.util.HashMap;
import java.util.Map;
import java.util.zip.ZipEntry;
import java.util.zip.ZipOutputStream;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

public final class NamespacerMainTests {

  @Test
  void namespacesMultipleJars(final @TempDir File tempDir)
      throws IOException, ReflectiveOperationException {

    final long timestampMs = Instant.parse("2000-01-01T00:00:00.00Z").toEpochMilli();
    final File jar1File = new File(tempDir, "jar1.jar");

    try (OutputStream outputStream = Files.newOutputStream(jar1File.toPath());
        BufferedOutputStream bufferedOutputStream = new BufferedOutputStream(outputStream);
        ZipOutputStream jarOutputStream = new ZipOutputStream(bufferedOutputStream)) {
      writeEntryForClass(jarOutputStream, OverriddenClassInBoth.class, timestampMs + 1);
      writeEntryForClass(jarOutputStream, Class1Jar1.class, timestampMs + 2);
      writeEntryForClass(jarOutputStream, Class2Jar1.class, timestampMs + 3);
    }

    final File jar2File = new File(tempDir, "jar2.jar");

    try (OutputStream outputStream = Files.newOutputStream(jar2File.toPath());
        BufferedOutputStream bufferedOutputStream = new BufferedOutputStream(outputStream);
        ZipOutputStream jarOutputStream = new ZipOutputStream(bufferedOutputStream)) {
      writeEntryForClass(jarOutputStream, OverriddenClassInBoth.class, timestampMs + 4);
      writeEntryForClass(jarOutputStream, Class1Jar2.class, timestampMs + 5);
      writeEntryForClass(jarOutputStream, Class2Jar2.class, timestampMs + 6);
    }

    final Map<String, String> replacements = new HashMap<>();
    replacements.put("dev.stiemannkj1.bytecode.namespacer.NamespacerMainTests$", "foo.bar.");

    final File namespacedJar = new File(tempDir, "namespaced.jar");
    NamespacerMain.namespaceJars(
        Arrays.asList(jar1File, jar2File), replacements, namespacedJar, 1 << 10);

    try (URLClassLoader classLoader =
        new URLClassLoader(new URL[] {namespacedJar.toURI().toURL()}, null)) {
      assertEquals(
          "foo.bar.OverriddenClassInBoth",
          newInstance(classLoader, "foo.bar.OverriddenClassInBoth").toString());
      assertEquals("foo.bar.Class1Jar1", newInstance(classLoader, "foo.bar.Class1Jar1").toString());
      assertEquals("foo.bar.Class2Jar1", newInstance(classLoader, "foo.bar.Class2Jar1").toString());
      assertEquals("foo.bar.Class1Jar2", newInstance(classLoader, "foo.bar.Class1Jar2").toString());
      assertEquals("foo.bar.Class2Jar2", newInstance(classLoader, "foo.bar.Class2Jar2").toString());
    }
  }

  private static void writeEntryForClass(
      final ZipOutputStream jarOutputStream, final Class<?> aClass, final long timestampMs)
      throws IOException {

    final ZipEntry zipEntry = new ZipEntry(toEntryPath(aClass));
    zipEntry.setTime(timestampMs);
    jarOutputStream.putNextEntry(zipEntry);
    jarOutputStream.write(readClassBytes(aClass));
  }

  private Object newInstance(final ClassLoader classLoader, final String className)
      throws ReflectiveOperationException {
    return assertNotNull(assertNotEmpty(classLoader.loadClass(className).getConstructors())[0])
        .newInstance();
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
      return "dev.stiemannkj1.bytecode.namespacer.NamespacerMainTests$Class1Jar1";
    }
  }

  public static final class Class2Jar2 {
    @Override
    public String toString() {
      return "dev.stiemannkj1.bytecode.namespacer.NamespacerMainTests$Class2Jar1";
    }
  }
}
