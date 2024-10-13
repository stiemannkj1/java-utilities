package dev.stiemannkj1.bytecode.namespacer;

import static dev.stiemannkj1.util.ProcessUtils.runWithTimeout;
import static org.junit.jupiter.api.Assertions.assertEquals;

import dev.stiemannkj1.util.ProcessUtils;
import dev.stiemannkj1.util.Require;
import java.io.File;
import java.io.IOException;
import java.net.URL;
import java.net.URLClassLoader;
import java.util.concurrent.TimeUnit;
import org.junit.jupiter.api.Test;

final class NamespacerGradlePluginTests {

  @Test
  void test_plugin() throws IOException, InterruptedException {

    ProcessUtils.Result result = null;

    try {
      final ProcessBuilder processBuilder =
          new ProcessBuilder()
              .command(
                  ".gradle/gradle/bin/gradle",
                  "--project-dir",
                  "src/test/gradle",
                  "clean",
                  "build",
                  "--no-daemon",
                  "--info",
                  "--stacktrace",
                  "--gradle-user-home",
                  ".gradle/.fake-user-home",
                  "--project-cache-dir",
                  ".gradle/.fake-cache");

      processBuilder
          .environment()
          .put(
              "JAVA_HOME", Require.notEmpty(System.getProperty("java.home"), "java.home property"));

      result =
          runWithTimeout(
              processBuilder, TimeUnit.MINUTES.toMillis(5), TimeUnit.SECONDS.toMillis(10));
      System.out.println(result.stdout());
      System.err.println(result.stderr());
      assertEquals(0, result.exitValue);

      try (final URLClassLoader testClassLoader =
          new URLClassLoader(
              new URL[] {new File("src/test/gradle/build/libs/namespaced.jar").toURI().toURL()},
              null)) {

        final Class<?> namespacedClass = testClassLoader.loadClass("im.namespaced.foo.Bar");
        assertEquals(
            "im.namespaced.dev.stiemannkj1.bytecode.namespacer.Namespacer",
            namespacedClass.getConstructor().newInstance().toString());

      } catch (final ReflectiveOperationException e) {
        throw new AssertionError("foo.Bar was not namespaced correctly.", e);
      }

    } catch (final Throwable t) {

      if (t instanceof AssertionError) {
        throw t;
      }

      if (result != null) {
        System.out.println(result.stdout());
        System.err.println(result.stderr());
      }

      throw t;
    }
  }
}
