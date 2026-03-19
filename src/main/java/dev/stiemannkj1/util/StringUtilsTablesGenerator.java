package dev.stiemannkj1.util;

import static dev.stiemannkj1.util.Assert.assertAsciiPrintable;
import static dev.stiemannkj1.util.Assert.assertNotNull;
import static dev.stiemannkj1.util.Assert.assertTrue;
import static dev.stiemannkj1.util.StringUtils.appendHexString;
import static dev.stiemannkj1.util.StringUtils.isAsciiPrintable;

import dev.stiemannkj1.util.Assert;
import java.io.File;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.StandardOpenOption;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

/**
 * To avoid generating tables at runtime, generate StringUtilsTablesGenerated.java with:
 *
 * <pre>{@code
 * mkdir -p build/generated
 * javac -sourcepath src/main/java -d build/generated src/main/java/dev/stiemannkj1/util/StringUtilsTablesGenerator.java
 * java -cp build/generated dev.stiemannkj1.util.StringUtilsTablesGenerator src/main/java
 * }</pre>
 */
final class StringUtilsTablesGenerator {

    public static void main(final String[] args) throws IOException {

        System.setProperty(Assert.ASSERT_ENABLED_PROPERTY_NAME, "true");

        if (args.length < 1 || args[0] == null) {
            throw new IllegalArgumentException("You must provide the Java source dir.");
        }

        final File parentDir = new File(args[0]);

        if (!(parentDir.exists()
                && parentDir.isDirectory()
                && parentDir.canRead()
                && parentDir.canWrite()
                && parentDir.canExecute())) {
            throw new IllegalArgumentException(
                    "The Java source dir was not a writeable directory: " + parentDir.getAbsolutePath());
        }

        final String packageName = StringUtilsTablesGenerator.class.getPackage().getName();
        final File packageDir = new File(parentDir, packageName.replace('.', File.separatorChar));
        final File currentFile =
                new File(packageDir, StringUtilsTablesGenerator.class.getSimpleName() + ".java");

        if (!currentFile.exists()) {
            throw new IllegalArgumentException(
                    "The Java source dir did not contain the Java source: " + parentDir.getAbsolutePath());
        }

        final StringBuilder stringBuilder =
                new StringBuilder()
                        .append("package ")
                        .append(packageName)
                        .append(";\n\n")
                        .append("import static dev.stiemannkj1.util.Assert.assertNotNull;\n")
                        .append("import dev.stiemannkj1.util.GeneratedBy;\n\n")
                        .append("@GeneratedBy(\"")
                        .append(StringUtilsTablesGenerator.class.getTypeName())
                        .append("\")\n")
                        .append("final class StringUtilsTablesGenerated {\n");

        stringBuilder.append("    static final String[] BYTE_TO_HEX_STRING = new String[] {\n");

        for (int i = 0; i < (1 << Byte.SIZE); i++) {
            stringBuilder.append("        \"");
            appendHexString(stringBuilder, i, 1);
            stringBuilder.append("\",\n");
        }

        stringBuilder.append("    };\n");

        stringBuilder.append("\n    private StringUtilsTablesGenerated() {}\n").append("}\n");

        final File generatedTablesFile = new File(packageDir, "StringUtilsTablesGenerated.java");

        Files.write(
                generatedTablesFile.toPath(),
                stringBuilder.toString().getBytes(StandardCharsets.UTF_8),
                StandardOpenOption.CREATE,
                StandardOpenOption.TRUNCATE_EXISTING);

        System.out.println("Generated: " + generatedTablesFile.getAbsolutePath());
    }

    private StringUtilsTablesGenerator() {}
}