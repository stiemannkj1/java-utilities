package dev.stiemannkj1.bytecode;

import static dev.stiemannkj1.collection.arrays.GrowableArrays.GrowableByteArray.size;
import static dev.stiemannkj1.util.Assert.assertNotEmpty;
import static dev.stiemannkj1.util.Assert.assertNotNull;
import static dev.stiemannkj1.util.Assert.assertTrue;

import dev.stiemannkj1.allocator.Allocators.Allocator;
import dev.stiemannkj1.collection.arrays.GrowableArrays.GrowableByteArray;
import dev.stiemannkj1.util.Assert;
import dev.stiemannkj1.util.Pair;
import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.Map;

public final class Namespacer {

  private static final int DEFAULT_STRING_BUILDER_SIZE = 256;
  private static final int CONSTANT_UTF8_INFO_HEADER_SIZE = 3;
  private static final int U16_MAX_VALUE = (1 << 16) - 1;

  public static String namespace(
      final Allocator allocator,
      final String fileName,
      final GrowableByteArray classFileBefore,
      final Map<String, String> replacementsMap,
      final GrowableByteArray classFileAfter) {

    final List<Pair<byte[], byte[]>> replacements = allocator.allocateList(replacementsMap.size());
    // defer$(() -> allocator.deallocateObject(replacements));

    for (final Map.Entry<String, String> entry : replacementsMap.entrySet()) {
      replacements.add(
          Pair.of(
              entry.getKey().getBytes(StandardCharsets.UTF_8),
              entry.getValue().getBytes(StandardCharsets.UTF_8)));
    }

    final ByteParser parser = allocator.allocateObject(ByteParser::new);
    ByteParser.reset(parser, classFileBefore, fileName);

    ByteParser.consumeRequired(parser, 0xCAFEBABE);
    parser.currentIndex += 4; // Skip major and minor versions.
    final int constant_pool_count = (int) ByteParser.consumeUnsignedBytes(parser, 2) - 1;
    GrowableByteArray.copyBytes(classFileBefore, 0, classFileAfter, 0, parser.currentIndex);

    for (int i = 0; i < constant_pool_count; i++) {

      if (!ByteParser.shouldContinue(parser)) {
        break;
      }

      final int constantStartIndex = parser.currentIndex;

      final short cp_info_tag = (short) ByteParser.consumeUnsignedBytes(parser, 1);

      final ConstantPoolTag tag;

      if (cp_info_tag > ConstantPoolTag.VALUES.length) {
        tag = ConstantPoolTag.CONSTANT_Unused_2;
      } else {
        tag = ConstantPoolTag.VALUES[cp_info_tag];
      }

      final int length = consumeCpInfoLength(parser, tag, cp_info_tag);

      if (ConstantPoolTag.CONSTANT_Utf8 != tag) {
        parser.currentIndex += length;
        GrowableByteArray.appendBytes(
            classFileBefore, constantStartIndex, classFileAfter, length + 1);

        continue;
      }

      final String error =
          namespaceUtf8ConstantString(
              allocator,
              classFileBefore,
              classFileAfter,
              replacements,
              parser,
              constantStartIndex,
              length,
              i);

      if (error != null) {
        return error;
      }
    }

    if (!ByteParser.shouldContinue(parser)) {
      return parser.errorMessage.toString();
    }

    GrowableByteArray.appendBytes(
        classFileBefore,
        parser.currentIndex,
        classFileAfter,
        size(classFileBefore) - parser.currentIndex);

    return null;
  }

  private static String namespaceUtf8ConstantString(
      final Allocator allocator,
      final GrowableByteArray classFileBefore,
      final GrowableByteArray classFileAfter,
      final List<Pair<byte[], byte[]>> replacements,
      final ByteParser parser,
      final int constantStartIndexBefore,
      final int length,
      final int indexInConstantPool) {

    if (!ByteParser.shouldContinue(parser)) {
      return parser.errorMessage.toString();
    }

    final int constantStartIndexAfter = GrowableByteArray.size(classFileAfter);
    final byte currentChar = GrowableByteArray.get(classFileBefore, parser.currentIndex);
    final String error;

    if ('L' == currentChar || '[' == currentChar) {
      error =
          namespaceTypeSignature(
              allocator,
              classFileBefore,
              classFileAfter,
              replacements,
              parser,
              constantStartIndexBefore,
              length,
              indexInConstantPool);
    } else if ('(' == currentChar) {
      error = null; // namespaceMethodSignature(TODO);
    } else if ('<' == currentChar) {
      error = null; // namespaceGenericClassSignature(TODO);
    } else if (Character.isAlphabetic(currentChar)) {
      error =
          namespaceType(
              allocator,
              classFileBefore,
              classFileAfter,
              replacements,
              parser,
              constantStartIndexBefore,
              length,
              indexInConstantPool);
    } else {
      parser.currentIndex += length;
      GrowableByteArray.appendBytes(
          classFileBefore, constantStartIndexBefore, classFileAfter, length + 1);
      error = null;
    }

    if (error != null) {
      GrowableByteArray.copyBytes(
          classFileBefore,
          constantStartIndexBefore,
          classFileAfter,
          constantStartIndexAfter,
          length + 1);
    }

    return error;
  }

  private static String namespaceTypeSignature(
      final Allocator allocator,
      final GrowableByteArray classFileBefore,
      final GrowableByteArray classFileAfter,
      final List<Pair<byte[], byte[]>> replacements,
      final ByteParser parser,
      final int constantStartIndex,
      final int length,
      final int indexInConstantPool) {
    return null;
  }

  private static String namespaceType(
      final Allocator allocator,
      final GrowableByteArray classFileBefore,
      final GrowableByteArray classFileAfter,
      final List<Pair<byte[], byte[]>> replacements,
      final ByteParser parser,
      final int constantStartIndex,
      final int length,
      final int indexInConstantPool) {
    int remainingLength = length;

    replacing:
    for (final Pair<byte[], byte[]> replacement : replacements) {

      for (int i = 0; i < replacement.left().length; i++) {
        if (replacement.left()[i]
            != GrowableByteArray.get(classFileBefore, parser.currentIndex + i)) {
          continue replacing;
        }
      }

      remainingLength = length - replacement.left().length;
      final int finalLength = replacement.right().length + remainingLength;

      if (finalLength > U16_MAX_VALUE) {
        return allocator
            .allocateStringBuilder(DEFAULT_STRING_BUILDER_SIZE)
            .append("Constant String at index ")
            .append(indexInConstantPool + 1)
            .append(" is larger than max allowed ")
            .append(ConstantPoolTag.CONSTANT_Utf8.name())
            .append(" size. Expected less than or equal to ")
            .append(U16_MAX_VALUE)
            .append(" but was ")
            .append(finalLength)
            .toString();
      }

      parser.currentIndex += (length - remainingLength);

      GrowableByteArray.append(classFileAfter, ConstantPoolTag.CONSTANT_Utf8.ordinal);
      GrowableByteArray.appendBytes(classFileAfter, finalLength, 2);
      GrowableByteArray.appendBytes(
          replacement.right(), 0, classFileAfter, replacement.right().length);
      // TODO add simple test for feedback
      // TODO fix bugs
      // TODO add complex tests with field refs, method refs, lambdas, invokedynamic, method
      // descriptors etc.
      break;
    }

    if (remainingLength >= length) {
      GrowableByteArray.appendBytes(
          classFileBefore, constantStartIndex, classFileAfter, CONSTANT_UTF8_INFO_HEADER_SIZE);
    }

    final int start = parser.currentIndex;
    parser.currentIndex += remainingLength;
    GrowableByteArray.appendBytes(classFileBefore, start, classFileAfter, remainingLength);
    return null;
  }

  private static int consumeCpInfoLength(
      final ByteParser parser, final ConstantPoolTag tag, short cp_info_tag) {

    switch (tag) {
      case CONSTANT_Utf8:
        return (int) ByteParser.consumeUnsignedBytes(parser, 2);
      case CONSTANT_Integer:
        return 4;
      case CONSTANT_Float:
        return 4;
      case CONSTANT_Long:
        return 8;
      case CONSTANT_Double:
        return 8;
      case CONSTANT_Class:
        return 2;
      case CONSTANT_String:
        return 2;
      case CONSTANT_Fieldref:
        return 4;
      case CONSTANT_Methodref:
        return 4;
      case CONSTANT_InterfaceMethodref:
        return 4;
      case CONSTANT_NameAndType:
        return 4;
      case CONSTANT_MethodHandle:
        return 3;
      case CONSTANT_MethodType:
        return 2;
      case CONSTANT_Dynamic:
        return 4;
      case CONSTANT_InvokeDynamic:
        return 4;
      case CONSTANT_Module:
        return 2;
      case CONSTANT_Package:
        return 2;
      case CONSTANT_Unused_2:
        // fallthrough;
      case CONSTANT_Unused_13:
        // fallthrough;
      case CONSTANT_Unused_14:
        // fallthrough;
      default:
        parser
            .errorMessage
            .append("Unexpected constant tag found ")
            .append(cp_info_tag)
            .append(". Unable to namespace values in class file.");
        return -1;
    }
  }

  private enum ConstantPoolTag {
    CONSTANT_Unused_0,
    CONSTANT_Utf8,
    CONSTANT_Unused_2,
    CONSTANT_Integer,
    CONSTANT_Float,
    CONSTANT_Long,
    CONSTANT_Double,
    CONSTANT_Class,
    CONSTANT_String,
    CONSTANT_Fieldref,
    CONSTANT_Methodref,
    CONSTANT_InterfaceMethodref,
    CONSTANT_NameAndType,
    CONSTANT_Unused_13,
    CONSTANT_Unused_14,
    CONSTANT_MethodHandle,
    CONSTANT_MethodType,
    CONSTANT_Dynamic,
    CONSTANT_InvokeDynamic,
    CONSTANT_Module,
    CONSTANT_Package;

    private final byte ordinal;

    private ConstantPoolTag() {
      final int ordinal = ordinal();
      if (Assert.ASSERT_ENABLED) {
        Assert.assertFalse(
            ordinal < Byte.MIN_VALUE || Byte.MAX_VALUE < ordinal,
            () -> "ConstantPoolTag must fit into a byte.");
      }
      this.ordinal = (byte) ordinal;
    }

    private static final ConstantPoolTag[] VALUES = ConstantPoolTag.values();
  }

  private static final class ByteParser {
    private GrowableByteArray bytes;
    private int currentIndex;
    private String fileName;
    private StringBuilder errorMessage;

    private ByteParser(final Allocator allocator) {
      this.errorMessage =
          assertNotNull(allocator).allocateStringBuilder(DEFAULT_STRING_BUILDER_SIZE);
    }

    private static void reset(
        final ByteParser parser, final GrowableByteArray bytes, final String fileName) {
      parser.bytes = assertNotNull(bytes);
      parser.fileName = assertNotEmpty(fileName);
      parser.errorMessage.setLength(0);
    }

    private static boolean shouldContinue(final ByteParser parser) {
      return parser.errorMessage.length() == 0 && size(parser.bytes) > parser.currentIndex;
    }

    private static boolean consumeOptional(final ByteParser parser, final int i4) {

      for (int i = (Integer.BYTES - 1); i >= 0; i--) {

        if (!shouldContinue(parser)) {
          return false;
        }

        if (((byte) ((i4 >> (i * Byte.SIZE)) & 0xFF))
            != GrowableByteArray.get(parser.bytes, parser.currentIndex++)) {
          return false;
        }
      }

      return true;
    }

    private static void consumeRequired(final ByteParser parser, final int i4) {

      final int indexBefore = parser.currentIndex;

      if (!consumeOptional(parser, i4)) {
        parser
            .errorMessage
            .append("Failed to parse ")
            .append(parser.fileName)
            .append(" at offset ")
            .append(parser.currentIndex)
            .append(". Expecting 0x");
        appendHexString(parser.errorMessage, i4).append(" but found 0x");
        appendHexString(parser.errorMessage, parser.bytes, indexBefore, Integer.BYTES);
      }
    }

    private static long consumeUnsignedBytes(final ByteParser parser, final int sizeInBytes) {
      assertTrue(
          0 < sizeInBytes && sizeInBytes < 8,
          () -> "Unsigned size must fit into 7 bytes, but size was " + sizeInBytes);

      long u8 = 0;

      for (int i = (sizeInBytes - 1); i >= 0; i--) {

        if (!shouldContinue(parser)) {
          return 0;
        }

        u8 =
            (u8
                | (Byte.toUnsignedLong(GrowableByteArray.get(parser.bytes, parser.currentIndex++))
                    << (i * Byte.SIZE)));
      }

      return u8;
    }

    private static void consumeBytes(final ByteParser parser, final int sizeToConsume) {
      parser.currentIndex += sizeToConsume;

      if (!shouldContinue(parser)) {
        parser
            .errorMessage
            .append("Index out of bounds for array with size ")
            .append(size(parser.bytes))
            .append(": ")
            .append(parser.currentIndex);
      }
    }

    private static StringBuilder appendHexString(final StringBuilder stringBuilder, final int i4) {

      final int leadingZeros = Integer.numberOfLeadingZeros(i4);
      final int leadingBytes = leadingZeros << Byte.SIZE;
      stringBuilder.append(ZEROS[leadingBytes]);
      final int trailingBytes = Integer.BYTES - leadingBytes;

      for (int i = trailingBytes; i >= 0; i--) {
        appendHexString(stringBuilder, (byte) ((i4 >> i) & 0xFF));
      }

      return stringBuilder;
    }

    private static StringBuilder appendHexString(
        final StringBuilder stringBuilder, final GrowableByteArray bytes, int offset, int size) {

      for (int i = offset; i < size; i++) {
        appendHexString(stringBuilder, GrowableByteArray.get(bytes, i));
      }

      return stringBuilder;
    }

    // TODO this is broken
    private static StringBuilder appendHexString(final StringBuilder stringBuilder, final byte i1) {
      final int i4 = Byte.toUnsignedInt(i1);
      return stringBuilder.append(BYTES_AS_HEX[i4]).append(BYTES_AS_HEX[i4 + 1]);
    }

    private static final char[] HEX_CHARS =
        new char[] {'0', '1', '2', '3', '4', '5', '6', '7', '8', '9', 'A', 'B', 'C', 'D', 'E', 'F'};

    private static final char[] BYTES_AS_HEX = new char[HEX_CHARS.length << 1];

    static {
      for (int i = 0; i < HEX_CHARS.length; i++) {
        for (int j = 0; j < HEX_CHARS.length - 1; j++) {
          BYTES_AS_HEX[i + j] = HEX_CHARS[j];
          j++;
          BYTES_AS_HEX[i + j] = HEX_CHARS[i];
        }
      }
    }

    private static final String[] ZEROS = new String[Long.BYTES];

    static {
      final StringBuilder stringBuilder = new StringBuilder();
      for (int i = 0; i < ZEROS.length; i++) {
        ZEROS[i] = stringBuilder.append('0').append('0').toString();
      }
    }
  }
}
