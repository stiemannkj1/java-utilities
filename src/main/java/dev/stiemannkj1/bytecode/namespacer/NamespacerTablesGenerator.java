package dev.stiemannkj1.bytecode.namespacer;

import static dev.stiemannkj1.util.Assert.assertAsciiPrintable;
import static dev.stiemannkj1.util.Assert.assertNotEmpty;
import static dev.stiemannkj1.util.Assert.assertNotNull;
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
 * To avoid generating tables at runtime, generate NamespacerTablesGenerated.java with:
 *
 * <pre>{@code
 * mkdir -p build/generated
 * javac -sourcepath src/main/java -d build/generated src/main/java/dev/stiemannkj1/bytecode/namespacer/NamespacerTablesGenerator.java
 * java -cp build/generated dev.stiemannkj1.bytecode.namespacer.NamespacerTablesGenerator src/main/java
 * }</pre>
 */
public final class NamespacerTablesGenerator {

  private static final int MAX_CHAR_LENGTH = "\\u0000".length();

  public static void main(final String[] args) throws IOException {

    System.setProperty(Assert.ASSERT_ENABLED_PROPERTY_NAME, "true");

    final String packageName = NamespacerTablesGenerator.class.getPackage().getName();

    final StringBuilder stringBuilder =
        new StringBuilder()
            .append("package ")
            .append(packageName)
            .append(";\n\n")
            .append("import static dev.stiemannkj1.util.Assert.assertNotNull;\n")
            .append("import dev.stiemannkj1.util.GeneratedBy;\n\n")
            .append("@GeneratedBy(\"")
            .append(NamespacerTablesGenerator.class.getTypeName())
            .append("\")\n")
            .append("final class NamespacerTablesGenerated {\n\n");

    stringBuilder
        .append("    /**\n")
        .append(
            "     * Lookup table to determine if an ASCII character is a valid Java ClassTypeSignature as described\n")
        .append("     * in the JVM spec section 4.7.9.1 Signatures\n")
        .append(
            "     * (https://docs.oracle.com/javase/specs/jvms/se22/html/jvms-4.html#jvms-ClassTypeSignature).\n")
        .append("     *\n")
        .append(
            "     * <p>NOTE: this table also allows '$' (which is not allowed in the spec) so that code can be\n")
        .append(
            "     * reused for class names, file names, internal class names, and class type signatures. The\n")
        .append(
            "     * namespacer avoids strictly validating strings and signatures for the sake of performance.\n")
        .append("     */\n")
        .append("     static boolean[] ASCII_CLASS_TYPE_SIGNATURE_CHARS = new boolean[] {\n");

    final int asciiClassTypeSignatureCharsLength = 128;

    for (int i = 0; i < asciiClassTypeSignatureCharsLength; i++) {

      if (i > 0) {
        stringBuilder.append(",\n");
      }

      stringBuilder.append("        /* '");

      final int start = stringBuilder.length();

      switch (i) {
        case '\b':
          stringBuilder.append("\\b");
          break;
        case '\f':
          stringBuilder.append("\\f");
          break;
        case '\n':
          stringBuilder.append("\\n");
          break;
        case '\r':
          stringBuilder.append("\\r");
          break;
        case '\t':
          stringBuilder.append("\\t");
          break;
        default:
          if (isAsciiPrintable((char) i)) {
            stringBuilder.append((char) i);
          } else {
            stringBuilder.append("\\u");
            appendHexString(stringBuilder, i, 2);
          }
      }

      final int length = stringBuilder.length() - start;

      stringBuilder.append("' ");

      for (int j = (MAX_CHAR_LENGTH - length); j >= 0; j--) {
        stringBuilder.append(' ');
      }

      stringBuilder
          .append("== */ ")
          .append(
              '$' == i
                  || ('.' <= i && i <= '9')
                  || ('A' <= i && i <= 'Z')
                  || ('a' <= i && i <= 'z')
                  || '_' == i);
    }

    stringBuilder.append("\n    };\n\n");

    // TODO replace with trie/prefix tree.
    final String[] standardConstants =
        new String[] {
          "ConstantValue",
          "Code",
          "StackMapTable",
          "Exceptions",
          "InnerClasses",
          "EnclosingMethod",
          "Synthetic",
          "Signature",
          "SourceFile",
          "SourceDebugExtension",
          "LineNumberTable",
          "LocalVariableTable",
          "LocalVariableTypeTable",
          "Deprecated",
          "RuntimeVisibleAnnotations",
          "RuntimeInvisibleAnnotations",
          "RuntimeVisibleParameterAnnotations",
          "RuntimeInvisibleParameterAnnotations",
          "RuntimeVisibleTypeAnnotations",
          "RuntimeInvisibleTypeAnnotations",
          "AnnotationDefault",
          "BootstrapMethods",
          "MethodParameters",
          "Module",
          "ModulePackages",
          "ModuleMainClass",
          "NestHost",
          "NestMembers",
          "Record",
          "PermittedSubclasses"
        };

    final String[] sorted = sort(Arrays.copyOf(standardConstants, standardConstants.length));

    if (Arrays.deepEquals(standardConstants, sorted)) {
      throw new AssertionError(
          "STANDARD_CONSTANTS was not in sorted order. Expected: <[\n"
              + String.join("\n", sorted)
              + "\n]> but found <[\n"
              + String.join("\n", standardConstants)
              + "\n]>");
    }

    stringBuilder.append("    static final byte[][] STANDARD_CONSTANTS = new byte[][] {\n");

    final List<Integer> notTypeIndexes = new ArrayList<>();

    for (int i = 0; i < standardConstants.length; i++) {

      if (i > 0) {
        stringBuilder.append(",\n");
      }

      final byte[] standardConstant = standardConstants[i].getBytes(StandardCharsets.UTF_8);

      stringBuilder.append("        /* \"").append(standardConstants[i]).append("\" */ ");
      appendAsciiArray(stringBuilder, standardConstant);

      if ('L' == standardConstant[0]) {
        notTypeIndexes.add(i);
      }
    }

    stringBuilder.append("\n    };\n\n");

    stringBuilder.append("    static final byte[][] NOT_TYPE = new byte[][] {\n");

    for (int i = 0; i < notTypeIndexes.size(); i++) {

      final String standardConstant = standardConstants[notTypeIndexes.get(i)];

      if ('L' != assertNotNull(standardConstant).charAt(0)) {
        throw new AssertionError(
            "NOT_TYPE values must start with 'L', but found \"" + standardConstant + "\".");
      }

      if (i > 0) {
        stringBuilder.append(",\n");
      }

      stringBuilder
          .append("        /* \"")
          .append(standardConstant)
          .append("\" */ assertNotNull(STANDARD_CONSTANTS[")
          .append(notTypeIndexes.get(i))
          .append("])");
    }

    stringBuilder.append("\n    };\n\n");

    final String[] notGeneric = new String[] {"<clinit>", "<init>"};
    stringBuilder.append("    static final byte[][] NOT_GENERIC = new byte[][] {\n");

    for (int i = 0; i < notGeneric.length; i++) {

      final byte[] notGenericConstant = notGeneric[i].getBytes(StandardCharsets.UTF_8);

      if ('<' != notGenericConstant[0]) {
        throw new AssertionError(
            "NOT_GENERIC values must start with '<', but found \"" + notGenericConstant[0] + "\".");
      }

      if (i > 0) {
        stringBuilder.append(",\n");
      }

      stringBuilder.append("        /* \"").append(notGeneric[i]).append("\" */ ");
      appendAsciiArray(stringBuilder, notGenericConstant);
    }

    stringBuilder.append("\n    };\n\n");

    stringBuilder.append("    private NamespacerTablesGenerated() {}\n").append("}\n");

    Files.write(
        new File(
                new File(
                    assertNotEmpty(assertNotEmpty(args)[0]),
                    packageName.replace('.', File.separatorChar)),
                "NamespacerTablesGenerated.java")
            .toPath(),
        stringBuilder.toString().getBytes(StandardCharsets.UTF_8),
        StandardOpenOption.CREATE,
        StandardOpenOption.TRUNCATE_EXISTING);
  }

  private static <T> T[] sort(final T[] array) {
    Arrays.sort(array);
    return array;
  }

  private static void appendAsciiArray(final StringBuilder stringBuilder, final byte[] ascii) {

    stringBuilder.append("new byte[] { ");

    for (int i = 0; i < ascii.length; i++) {

      if (i > 0) {
        stringBuilder.append(", ");
      }

      stringBuilder.append('\'').append(assertAsciiPrintable((char) ascii[i])).append('\'');
    }

    stringBuilder.append('}');
  }

  private NamespacerTablesGenerator() {}
}
