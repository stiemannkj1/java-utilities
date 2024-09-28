package dev.stiemannkj1.bytecode.namespacer;

import static dev.stiemannkj1.bytecode.ClassGenerator.ClassUtil.readClassBytes;
import static dev.stiemannkj1.bytecode.ClassGenerator.ClassUtil.toEntryPath;
import static dev.stiemannkj1.util.Assert.assertNotEmpty;
import static dev.stiemannkj1.util.Assert.assertNotNull;
import static dev.stiemannkj1.util.Assert.assertPositive;
import static dev.stiemannkj1.util.Assert.assertTrue;
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
import java.nio.file.Files;
import java.time.Instant;
import java.util.ArrayDeque;
import java.util.Arrays;
import java.util.Deque;
import java.util.HashMap;
import java.util.Map;
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

    try (OutputStream outputStream = Files.newOutputStream(jar1File.toPath());
        BufferedOutputStream bufferedOutputStream = new BufferedOutputStream(outputStream);
        ZipOutputStream jarOutputStream = new ZipOutputStream(bufferedOutputStream)) {
      writeEntryForClass(jarOutputStream, OverriddenClassInBoth.class, timestampMs + 2000);
      writeEntryForClass(jarOutputStream, Class1Jar1.class, timestampMs + 4000);
      writeEntryForClass(jarOutputStream, Class2Jar1.class, timestampMs + 6000);
    }

    final File jar2File = new File(tempDir, "jar2.jar");

    try (OutputStream outputStream = Files.newOutputStream(jar2File.toPath());
        BufferedOutputStream bufferedOutputStream = new BufferedOutputStream(outputStream);
        ZipOutputStream jarOutputStream = new ZipOutputStream(bufferedOutputStream)) {
      writeEntryForClass(jarOutputStream, OverriddenClassInBoth.class, timestampMs + 8000);
      writeEntryForClass(jarOutputStream, Class1Jar2.class, timestampMs + 10000);
      writeEntryForClass(jarOutputStream, Class2Jar2.class, timestampMs + 12000);
    }

    final Map<String, String> replacements = new HashMap<>();
    replacements.put("dev.stiemannkj1.bytecode.namespacer.NamespacerMainTests$", "foo.bar.");

    final File namespacedJar = new File(tempDir, "namespaced.jar");
    NamespacerMain.namespaceJars(
        Arrays.asList(jar1File, jar2File), replacements, namespacedJar, 1 << 10);

    final class ExpectedEntry {
      private final String name;
      private final String className;
      private final long timestampMs;

      private ExpectedEntry(final String className, final String name, final long timestampMs) {
        this.className = assertNotEmpty(className);
        this.name = assertNotEmpty(name);
        this.timestampMs = assertPositive(timestampMs, "timestampMs");
      }
    }

    final Deque<ExpectedEntry> expectedEntries = new ArrayDeque<>();
    expectedEntries.addLast(
        new ExpectedEntry(
            "foo.bar.OverriddenClassInBoth",
            "foo/bar/OverriddenClassInBoth.class",
            timestampMs + 2000));
    expectedEntries.addLast(
        new ExpectedEntry("foo.bar.Class1Jar1", "foo/bar/Class1Jar1.class", timestampMs + 4000));
    expectedEntries.addLast(
        new ExpectedEntry("foo.bar.Class2Jar1", "foo/bar/Class2Jar1.class", timestampMs + 6000));
    expectedEntries.addLast(
        new ExpectedEntry("foo.bar.Class1Jar2", "foo/bar/Class1Jar2.class", timestampMs + 10000));
    expectedEntries.addLast(
        new ExpectedEntry("foo.bar.Class2Jar2", "foo/bar/Class2Jar2.class", timestampMs + 12000));

    try (InputStream inputStream = Files.newInputStream(namespacedJar.toPath());
        BufferedInputStream bufferedInputStream = new BufferedInputStream(inputStream);
        ZipInputStream jarInputStream = new ZipInputStream(bufferedInputStream);
        URLClassLoader classLoader =
            new URLClassLoader(new URL[] {namespacedJar.toURI().toURL()}, null)) {

      ZipEntry entry;

      while ((entry = jarInputStream.getNextEntry()) != null) {
        final ExpectedEntry expectedEntry = expectedEntries.pop();
        assertEquals(
            expectedEntry.className, newInstance(classLoader, expectedEntry.className).toString());
        assertEquals(expectedEntry.name, entry.getName());
        assertEquals(
            expectedEntry.timestampMs,
            entry.getTime(),
            "Wrong timestamp for: " + expectedEntry.name);
      }
    }

    assertTrue(
        expectedEntries.isEmpty(),
        "Expected entries were missing from namespaced jar:\n"
            + expectedEntries.stream().map(entry -> entry.name).collect(Collectors.joining("\n")));
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
      return "dev.stiemannkj1.bytecode.namespacer.NamespacerMainTests$Class1Jar2";
    }
  }

  public static final class Class2Jar2 {
    @Override
    public String toString() {
      return "dev.stiemannkj1.bytecode.namespacer.NamespacerMainTests$Class2Jar2";
    }
  }
}
