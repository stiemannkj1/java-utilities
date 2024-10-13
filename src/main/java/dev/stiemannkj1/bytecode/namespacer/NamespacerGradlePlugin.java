package dev.stiemannkj1.bytecode.namespacer;

import dev.stiemannkj1.util.Require;
import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Set;
import org.gradle.api.DefaultTask;
import org.gradle.api.Plugin;
import org.gradle.api.Project;
import org.gradle.api.artifacts.Configuration;
import org.gradle.api.plugins.JavaPlugin;
import org.gradle.api.tasks.Input;
import org.gradle.api.tasks.OutputFile;
import org.gradle.api.tasks.TaskAction;
import org.gradle.jvm.tasks.Jar;

@SuppressWarnings("UnstableApiUsage")
public class NamespacerGradlePlugin implements Plugin<Project> {

  private static final int DEFAULT_BUFFER_CAPACITY = 1 << 10;

  @Override
  public void apply(final Project project) {

    final Configuration namespace =
        project
            .getConfigurations()
            .create(
                "namespace",
                namespaceConfiguration -> {
                  namespaceConfiguration.setCanBeConsumed(true);
                  namespaceConfiguration.setCanBeResolved(true);
                  namespaceConfiguration.setTransitive(false);
                  namespaceConfiguration.setVisible(false);
                });

    project
        .getTasks()
        .register(
            "namespaceJars",
            NamespaceJars.class,
            task -> {
              task.getInputs().files(namespace);
              task.dependsOn(namespace);
              task.namespaceConfiguration = namespace;

              final Jar jarTask =
                  project
                      .getTasks()
                      .withType(Jar.class)
                      .named(JavaPlugin.JAR_TASK_NAME)
                      .getOrNull();

              if (jarTask != null) {
                task.dependsOn(jarTask);
              }
            });

    // TODO depend on the Java plugin
    // TODO add task
    // TODO get the runtime classpath of main
    // TODO iterate the classpath
    // TODO specify namespaced jar

    // TODO add tests

    // TODO add main class which does the following:
    // TODO iterate classes in jars
    // TODO iterate classes in the file system
    // TODO run namespacer on classes
    // TODO add classes to new jar

    // TODO add tests with JARs and class files on the file system.
    // TODO test with --no-daemon, test should install gradle and maven
    // TODO add option to prevent GrowableByteArray resizing to prevent allocations.
    // TODO add option for changing GrowableByteArray default size.
  }

  public static class NamespaceJars extends DefaultTask {

    private Configuration namespaceConfiguration;

    private Map<String, String> replacements;

    @Input
    public Map<String, String> getReplacements() {
      return replacements;
    }

    public void setReplacements(final Map<String, String> replacements) {
      this.replacements = Require.notNull(replacements, "replacements");
    }

    private File outputJar;

    @OutputFile
    public File getOutputJar() {
      return outputJar;
    }

    public void setOutputJar(final File outputJar) {
      this.outputJar = Require.notNull(outputJar, "outputJar");
    }

    @TaskAction
    public void namespaceJars() throws IOException {

      final Set<File> jarsFromNamespaceConfiguration = namespaceConfiguration.resolve();
      final List<File> jars = new ArrayList<>(jarsFromNamespaceConfiguration.size() + 1);

      final Project project = getProject();
      final Jar jarTask =
          project.getTasks().withType(Jar.class).named(JavaPlugin.JAR_TASK_NAME).getOrNull();

      if (jarTask != null) {
        // Add the project JAR first. Override files from other jars on conflicts.
        jars.add(jarTask.getArchiveFile().get().getAsFile());
      }

      jars.addAll(jarsFromNamespaceConfiguration);

      NamespacerMain.namespaceJars(
          jarTask != null,
          jars,
          Require.notEmpty(getReplacements(), "replacements"),
          Require.notNull(getOutputJar(), "outputJar"),
          DEFAULT_BUFFER_CAPACITY);
    }
  }
}
