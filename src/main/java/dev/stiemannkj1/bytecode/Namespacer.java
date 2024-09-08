package dev.stiemannkj1.bytecode;

import static dev.stiemannkj1.collection.arrays.GrowableArrays.GrowableByteArray.size;
import static dev.stiemannkj1.util.Assert.*;

import dev.stiemannkj1.allocator.Allocators.Allocator;
import dev.stiemannkj1.collection.arrays.GrowableArrays.GrowableByteArray;
import dev.stiemannkj1.util.Assert;
import dev.stiemannkj1.util.Pair;
import dev.stiemannkj1.util.References.LongRef;
import java.nio.charset.StandardCharsets;
import java.util.Collections;
import java.util.List;
import java.util.Map;

public final class Namespacer {

  private static final int DEFAULT_STRING_BUILDER_SIZE = 256;
  private static final int CONSTANT_UTF8_INFO_HEADER_SIZE = 3;
  private static final int U2_MAX_VALUE = (1 << 16) - 1;
  private static final int BITS_24 = 24;
  private static final int BITS_16 = 16;
  private static final int BITS_8 = 8;
  private static final String NOT_SIGNATURE = Namespacer.class.getTypeName() + ".NOT_SIGNATURE";

  private static final byte[] CLASS_FILE_SUFFIX = ".class".getBytes(StandardCharsets.UTF_8);
  // TODO always skip these and other standard class file strings
  private static final byte[] LOCAL_VARIABLE_TABLE =
      "LocalVariableTable".getBytes(StandardCharsets.UTF_8);
  private static final byte[] LINE_NUMBER_TABLE =
      "LineNumberTable".getBytes(StandardCharsets.UTF_8);
  private static final byte[] Lookup = "Lookup".getBytes(StandardCharsets.UTF_8);
  private static final byte[] InnerClasses = "InnerClasses".getBytes(StandardCharsets.UTF_8);
  private static final byte[] NestMembers = "NestMembers".getBytes(StandardCharsets.UTF_8);
  private static final byte[] SourceFile = "SourceFile".getBytes(StandardCharsets.UTF_8);
  private static final byte[] Exceptions = "Exceptions".getBytes(StandardCharsets.UTF_8);
  private static final byte[] StackMapTable = "StackMapTable".getBytes(StandardCharsets.UTF_8);
  private static final byte[] RuntimeVisibleAnnotations =
      "RuntimeVisibleAnnotations".getBytes(StandardCharsets.UTF_8);
  private static final byte[] Code = "Code".getBytes(StandardCharsets.UTF_8);
  private static final byte[] Init = "<init>".getBytes(StandardCharsets.UTF_8);
  private static final byte[] Clinit = "<clinit>".getBytes(StandardCharsets.UTF_8);

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
      replacements.add(
          Pair.of(
              ("/META-INF/services/" + entry.getKey()).getBytes(StandardCharsets.UTF_8),
              ("/META-INF/services/" + entry.getValue()).getBytes(StandardCharsets.UTF_8)));
      replacements.add(
          Pair.of(
              ("META-INF/services/" + entry.getKey()).getBytes(StandardCharsets.UTF_8),
              ("META-INF/services/" + entry.getValue()).getBytes(StandardCharsets.UTF_8)));
      replacements.add(
          Pair.of(
              ("/WEB-INF/classes/META-INF/services/" + entry.getKey())
                  .getBytes(StandardCharsets.UTF_8),
              ("/WEB-INF/classes/META-INF/services/" + entry.getValue())
                  .getBytes(StandardCharsets.UTF_8)));
      replacements.add(
          Pair.of(
              ("WEB-INF/classes/META-INF/services/" + entry.getKey())
                  .getBytes(StandardCharsets.UTF_8),
              ("WEB-INF/classes/META-INF/services/" + entry.getValue())
                  .getBytes(StandardCharsets.UTF_8)));
      replacements.add(
          Pair.of(
              entry.getKey().replace('.', '/').getBytes(StandardCharsets.UTF_8),
              entry.getValue().replace('.', '/').getBytes(StandardCharsets.UTF_8)));
      replacements.add(
          Pair.of(
              ("/" + entry.getKey().replace('.', '/')).getBytes(StandardCharsets.UTF_8),
              ("/" + entry.getValue().replace('.', '/')).getBytes(StandardCharsets.UTF_8)));
      replacements.add(
          Pair.of(
              ("/WEB-INF/classes/" + entry.getKey().replace('.', '/'))
                  .getBytes(StandardCharsets.UTF_8),
              ("/WEB-INF/classes/" + entry.getValue().replace('.', '/'))
                  .getBytes(StandardCharsets.UTF_8)));
      replacements.add(
          Pair.of(
              ("WEB-INF/classes/" + entry.getKey().replace('.', '/'))
                  .getBytes(StandardCharsets.UTF_8),
              ("WEB-INF/classes/" + entry.getValue().replace('.', '/'))
                  .getBytes(StandardCharsets.UTF_8)));
    }

    final ByteParser parser = allocator.allocateObject(ByteParser::new);
    ByteParser.reset(parser, classFileBefore);

    if (!ByteParser.consumeOptional(parser, 0xCAFEBABE)) {
      final StringBuilder errorMessage =
          allocator
              .allocateStringBuilder(DEFAULT_STRING_BUILDER_SIZE)
              .append("Failed to parse ")
              .append(fileName)
              .append(" at offset ")
              .append(parser.currentIndex)
              .append(". Expecting 0x");
      appendHexString(errorMessage, 0xCAFEBABE).append(" but found 0x");
      appendHexString(errorMessage, parser.bytes, 0, Integer.BYTES);
      return errorMessage.toString();
    }

    parser.currentIndex += 4; // Skip major and minor versions.
    // TODO add strict and non-strict options for versioning
    final LongRef constant_pool_count_ref = ByteParser.consumeOptionalUnsignedBytes(parser, 2);

    if (constant_pool_count_ref == null) {
      return allocateTruncatedClassFileErrorMessage(allocator, parser, "constant_pool_count");
    }

    final int constant_pool_count = ((int) constant_pool_count_ref.value) - 1;
    GrowableByteArray.copyBytes(classFileBefore, 0, classFileAfter, 0, parser.currentIndex);
    Utf8ConstantInfo utf8ConstantInfo = null;

    for (int i = 0; i < constant_pool_count; i++) {

      if (size(parser.bytes) <= parser.currentIndex) {
        break;
      }

      final int constantStartIndex = parser.currentIndex;

      final LongRef cp_info_tag_ref = ByteParser.consumeOptionalUnsignedBytes(parser, 1);

      if (cp_info_tag_ref == null) {
        return allocateTruncatedClassFileErrorMessage(allocator, parser, "cp_info_tag");
      }

      final short cp_info_tag = (short) cp_info_tag_ref.value;

      final ConstantPoolTag tag;

      if (cp_info_tag > ConstantPoolTag.VALUES.length) {
        tag = ConstantPoolTag.CONSTANT_Unused_2;
      } else {
        tag = ConstantPoolTag.VALUES[cp_info_tag];
      }

      final int length = consumeCpInfoLength(parser, tag, cp_info_tag);

      if (length < 0) {
        return allocator
            .allocateStringBuilder(DEFAULT_STRING_BUILDER_SIZE)
            .append("Invalid class file for ")
            .append(fileName)
            .append(". Unexpected constant tag of ")
            .append(cp_info_tag)
            .append(" found at constant_pool[")
            .append(i)
            .append("], byte index: ")
            .append(constantStartIndex)
            .append(". Unable to namespace values in class file.")
            .toString();
      }

      if (ConstantPoolTag.CONSTANT_Utf8 != tag) {
        parser.currentIndex += length;

        if (parser.currentIndex >= GrowableByteArray.size(parser.bytes)) {
          return allocator
              .allocateStringBuilder(DEFAULT_STRING_BUILDER_SIZE)
              .append("Invalid class file for ")
              .append(fileName)
              .append(". No data left in constant pool at ")
              .append(GrowableByteArray.size(parser.bytes))
              .toString();
        }

        GrowableByteArray.appendBytes(
            classFileBefore, constantStartIndex, classFileAfter, length + 1);

        continue;
      }

      if (utf8ConstantInfo == null) {
        utf8ConstantInfo = allocator.allocateObject(Utf8ConstantInfo::new);
      }

      Utf8ConstantInfo.initialize(
          utf8ConstantInfo,
          i,
          constantStartIndex,
          length,
          GrowableByteArray.size(classFileAfter),
          GrowableByteArray.bytes(classFileBefore));

      parser.maxIndexExclusive =
          utf8ConstantInfo.utf8StartIndexBefore + utf8ConstantInfo.utf8LengthBefore;

      if (GrowableByteArray.size(classFileBefore) <= parser.maxIndexExclusive) {
        // TODO return allocateTruncatedClassFileErrorMessage(allocator, parser, "cp_info_tag");
        return allocator
            .allocateStringBuilder(DEFAULT_STRING_BUILDER_SIZE)
            .append("Invalid class file for ")
            .append(fileName)
            .append(". ")
            .append(ConstantPoolTag.CONSTANT_Utf8.name())
            .append("_info at constant_pool[")
            .append(i)
            .append("], byte index ")
            .append(utf8ConstantInfo.startIndexBefore)
            .append(" has length ")
            .append(utf8ConstantInfo.lengthBefore)
            .append(" extending past the end of the class file array with size: ")
            .append(GrowableByteArray.size(parser.bytes))
            .toString();
      }

      final String error =
          namespaceUtf8ConstantString(
              allocator, fileName, parser, utf8ConstantInfo, replacements, classFileAfter);

      parser.maxIndexExclusive = GrowableByteArray.size(parser.bytes);

      if (error != null) {
        return error;
      }
    }

    if (size(parser.bytes) <= parser.currentIndex) {
      return allocator
          .allocateStringBuilder(DEFAULT_STRING_BUILDER_SIZE)
          .append("Invalid class file for ")
          .append(fileName)
          .append(". No data after constant pool at ")
          .append(parser.currentIndex)
          .toString();
    }

    GrowableByteArray.appendBytes(
        classFileBefore,
        parser.currentIndex,
        classFileAfter,
        size(classFileBefore) - parser.currentIndex);

    return null;
  }

  private static String allocateTruncatedClassFileErrorMessage(
      final Allocator allocator, final ByteParser parser, final String missingValue) {
    // TODO add constant pool index and byte index of current constant that we're working on
    return allocator
        .allocateStringBuilder(DEFAULT_STRING_BUILDER_SIZE)
        .append("Class file missing ")
        .append(missingValue)
        .append(" truncated at: ")
        .append(parser.currentIndex)
        .toString();
  }

  private static final class Utf8ConstantInfo {

    private static final int LENGTH_BYTES = 2;
    private static final int HEADER_BYTES = /* cp_info.tag */ 1 + LENGTH_BYTES;

    private int constantPoolIndex;
    private int startIndexBefore;
    private int lengthBefore;
    private int utf8StartIndexBefore;
    private int utf8LengthBefore;
    private int startIndexAfter;

    @SuppressWarnings("unused")
    private String valueBeforeForDebugging;

    private Utf8ConstantInfo(final Allocator allocator) {}

    private static void initialize(
        final Utf8ConstantInfo constant,
        final int constantPoolIndex,
        final int startIndexBefore,
        final int lengthBefore,
        final int startIndexAfter,
        final byte[] classFile) {
      constant.constantPoolIndex = constantPoolIndex;
      constant.startIndexBefore = startIndexBefore;
      constant.utf8StartIndexBefore = HEADER_BYTES + constant.startIndexBefore;
      constant.utf8LengthBefore = lengthBefore;
      constant.lengthBefore = HEADER_BYTES + lengthBefore;
      constant.startIndexAfter = startIndexAfter;

      if (ASSERT_ENABLED) {
        constant.valueBeforeForDebugging =
            new String(
                classFile,
                constant.utf8StartIndexBefore,
                constant.utf8LengthBefore,
                StandardCharsets.UTF_8);
      }
    }

    @SuppressWarnings("unused")
    public static String valueAfterForDebugging(
        final Utf8ConstantInfo constant, final GrowableByteArray classFileAfter) {
      final int start = constant.startIndexAfter + HEADER_BYTES;
      final int length = GrowableByteArray.size(classFileAfter) - start;

      if (length <= 0) {
        return "";
      }

      return new String(
          GrowableByteArray.bytes(classFileAfter), start, length, StandardCharsets.UTF_8);
    }
  }

  private static String namespaceUtf8ConstantString(
      final Allocator allocator,
      final String fileName,
      final ByteParser parser,
      final Utf8ConstantInfo constant,
      final List<Pair<byte[], byte[]>> replacements,
      final GrowableByteArray classFileAfter) {

    if (constant.lengthBefore == 0) {
      GrowableByteArray.appendBytes(
          parser.bytes, constant.startIndexBefore, classFileAfter, constant.lengthBefore);
      parser.currentIndex = parser.maxIndexExclusive;
      return null;
    }

    GrowableByteArray.copyBytes(
        parser.bytes,
        constant.startIndexBefore,
        classFileAfter,
        constant.startIndexAfter,
        Utf8ConstantInfo.HEADER_BYTES);

    final byte currentChar = GrowableByteArray.get(parser.bytes, parser.currentIndex);
    String result;

    if ('L' == currentChar || '[' == currentChar) {
      result =
          namespaceTypeSignature(
              allocator, fileName, parser, constant, replacements, classFileAfter);
    } else if ('(' == currentChar) {
      result =
          namespaceMethodSignature(
              allocator, fileName, parser, constant, replacements, classFileAfter);
    } else if ('<' == currentChar) {
      result =
          namespaceGenericSignature(
              allocator, fileName, parser, constant, replacements, classFileAfter);
    } else if ('/' == currentChar || Character.isAlphabetic(currentChar)) {

      result = namespaceType(allocator, fileName, parser, constant, replacements, classFileAfter);

      final int remainingBytes = parser.maxIndexExclusive - parser.currentIndex;

      if (result == null && CLASS_FILE_SUFFIX.length == remainingBytes) {
        // TODO shim Arrays.compare with multi-release jar
        for (int i = 0; i < CLASS_FILE_SUFFIX.length; i++) {
          if (ByteParser.currentMatches(parser, CLASS_FILE_SUFFIX[i])) {
            continue;
          }

          result = NOT_SIGNATURE;
          break;
        }

        if (result == null) {
          GrowableByteArray.appendBytes(
              CLASS_FILE_SUFFIX, 0, classFileAfter, CLASS_FILE_SUFFIX.length);
          parser.currentIndex = parser.maxIndexExclusive;
        }
      } else if (remainingBytes > 0) {
        result = NOT_SIGNATURE;
      }
    } else {
      result = NOT_SIGNATURE;
    }

    if (result == NOT_SIGNATURE) {
      GrowableByteArray.copyBytes(
          parser.bytes,
          constant.startIndexBefore,
          classFileAfter,
          constant.startIndexAfter,
          constant.lengthBefore);
      parser.currentIndex = parser.maxIndexExclusive;
      GrowableByteArray.resize(classFileAfter, constant.startIndexAfter + constant.lengthBefore);
      return null;
    }

    final int lengthAfter =
        GrowableByteArray.size(classFileAfter)
            - constant.startIndexAfter
            - Utf8ConstantInfo.HEADER_BYTES;

    if (lengthAfter > U2_MAX_VALUE) {
      return allocator
          .allocateStringBuilder(DEFAULT_STRING_BUILDER_SIZE)
          .append("Invalid class file for ")
          .append(fileName)
          .append(". Constant String at index ")
          .append(constant.constantPoolIndex + 1)
          .append(" is larger than max allowed ")
          .append(ConstantPoolTag.CONSTANT_Utf8.name())
          .append(" size. Expected less than or equal to ")
          .append(U2_MAX_VALUE)
          .append(" but was ")
          .append(lengthAfter)
          .toString();
    }

    GrowableByteArray.copyBytes(
        classFileAfter, lengthAfter, constant.startIndexAfter + /* cp_info.tag size */ 1, 2);

    return result;
  }

  private static String namespaceTypeSignature(
      final Allocator allocator,
      final String fileName,
      final ByteParser parser,
      final Utf8ConstantInfo constant,
      final List<Pair<byte[], byte[]>> replacements,
      final GrowableByteArray classFileAfter) {

    while (ByteParser.consumeOptional(parser, '[')) {
      GrowableByteArray.append(classFileAfter, '[');
    }

    if (parser.currentIndex >= parser.maxIndexExclusive) {
      return NOT_SIGNATURE;
    }

    // See https://docs.oracle.com/javase/specs/jvms/se22/html/jvms-4.html#jvms-4.7.9.1
    final byte current = GrowableByteArray.get(parser.bytes, parser.currentIndex);

    switch (current) {
      case 'B':
        // fallthrough;
      case 'C':
        // fallthrough;
      case 'D':
        // fallthrough;
      case 'F':
        // fallthrough;
      case 'I':
        // fallthrough;
      case 'J':
        // fallthrough;
      case 'S':
        // fallthrough;
      case 'Z':
        GrowableByteArray.append(classFileAfter, current);
        parser.currentIndex++;
        return null;
      case 'V':
        // fallthrough;
      default:
        return NOT_SIGNATURE;
      case 'T':
        final int start = parser.currentIndex;
        final int end = ByteParser.consumeUntil(parser, ';');

        if (end < 0) {
          return NOT_SIGNATURE;
        }

        GrowableByteArray.appendBytes(parser.bytes, start, classFileAfter, end - start);
        return null;
      case 'L':
        GrowableByteArray.append(classFileAfter, 'L');
        parser.currentIndex++;
    }

    String result =
        namespaceType(allocator, fileName, parser, constant, replacements, classFileAfter);

    if (result == NOT_SIGNATURE) {
      return NOT_SIGNATURE;
    }

    if (result != null) {
      return result;
    }

    if (ByteParser.currentMatches(parser, '<')) {
      result =
          namespaceGenericClassSignature(
              allocator, fileName, parser, constant, replacements, classFileAfter);

      if (result == NOT_SIGNATURE) {
        return NOT_SIGNATURE;
      }

      if (result != null) {
        return result;
      }
    }

    if (ByteParser.consumeOptional(parser, '.')) {

      GrowableByteArray.append(classFileAfter, '.');

      result =
          namespaceType(
              allocator,
              fileName,
              parser,
              constant,
              // TODO handle namespacing nested types.
              Collections.emptyList(),
              classFileAfter);

      if (result == NOT_SIGNATURE) {
        return NOT_SIGNATURE;
      }

      result =
          namespaceGenericClassSignature(
              allocator, fileName, parser, constant, replacements, classFileAfter);

      if (result == NOT_SIGNATURE) {
        return NOT_SIGNATURE;
      }

      if (result != null) {
        return result;
      }
    }

    if (!ByteParser.consumeOptional(parser, ';')) {
      return NOT_SIGNATURE;
    }

    GrowableByteArray.append(classFileAfter, ';');
    return null;
  }

  private static String namespaceType(
      final Allocator allocator,
      final String fileName,
      final ByteParser parser,
      final Utf8ConstantInfo constant,
      final List<Pair<byte[], byte[]>> replacements,
      final GrowableByteArray classFileAfter) {

    int remainingLength = parser.maxIndexExclusive - parser.currentIndex;

    replacing:
    for (final Pair<byte[], byte[]> replacement : replacements) {

      if (replacement.left().length > remainingLength) {
        continue;
      }

      for (int i = 0; i < replacement.left().length; i++) {
        if (replacement.left()[i] != GrowableByteArray.get(parser.bytes, parser.currentIndex + i)) {
          continue replacing;
        }
      }

      remainingLength -= replacement.left().length;
      parser.currentIndex += replacement.left().length;

      GrowableByteArray.appendBytes(
          replacement.right(), 0, classFileAfter, replacement.right().length);
      break;
    }

    final int indexAfterReplacement = parser.currentIndex;

    for (; parser.currentIndex < (indexAfterReplacement + remainingLength); parser.currentIndex++) {

      final byte currentChar = GrowableByteArray.get(parser.bytes, parser.currentIndex);

      int size = 1;

      if ((0b11110000 & currentChar) == 0b11110000) {
        size = 4;
      } else if ((0b11100000 & currentChar) == 0b11100000) {
        size = 3;
      } else if ((0b11000000 & currentChar) == 0b11000000) {
        size = 2;
      }

      if (size > remainingLength) {
        //        TODO return allocateTruncatedClassFileErrorMessage(allocator,
        // constant.startIndexBefore)
        return allocator
            .allocateStringBuilder(DEFAULT_STRING_BUILDER_SIZE)
            .append("Invalid class file for ")
            .append(fileName)
            .append(". Expected multibyte UTF-8 character of size ")
            .append(size)
            .append(" at ")
            .append(parser.currentIndex)
            .append(" but UTF-8 string constant ")
            .append(constant.constantPoolIndex)
            .append(" was missing ")
            .append(size - remainingLength)
            .append(" bytes.")
            .toString();
      }

      if (size > 1) {
        final LongRef unicode = ByteParser.consumeOptionalUnsignedBytes(parser, size);

        if (unicode == null) {
          return NOT_SIGNATURE;
        }

        if (!Character.isJavaIdentifierPart((int) unicode.value)) {
          // Not a Java Class/Type.
          return NOT_SIGNATURE;
        }

        GrowableByteArray.appendBytes(parser.bytes, parser.currentIndex, classFileAfter, size);
        parser.currentIndex += (size - 1);
        continue;
      }

      if (!ASCII_CLASS_TYPE_SIGNATURE_CHARS[currentChar]) {
        return null;
      }

      GrowableByteArray.append(classFileAfter, currentChar);
    }

    return null;
  }

  private static String namespaceMethodSignature(
      final Allocator allocator,
      final String fileName,
      final ByteParser parser,
      final Utf8ConstantInfo constant,
      final List<Pair<byte[], byte[]>> replacements,
      final GrowableByteArray classFileAfter) {

    if (!ByteParser.consumeOptional(parser, '(')) {
      return NOT_SIGNATURE;
    }

    GrowableByteArray.append(classFileAfter, '(');

    boolean closedParens = false;

    namespaceArgsAndReturnValue:
    while (parser.currentIndex < parser.maxIndexExclusive) {
      final byte currentChar = GrowableByteArray.get(parser.bytes, parser.currentIndex);
      switch (currentChar) {
        case ')':
          closedParens = true;
          // fallthrough;
        case 'V':
          GrowableByteArray.append(classFileAfter, currentChar);
          parser.currentIndex++;
          break;
        case '^':
          break namespaceArgsAndReturnValue;
        default:
          final String result =
              namespaceTypeSignature(
                  allocator, fileName, parser, constant, replacements, classFileAfter);

          if (result != null) {
            return result;
          }

          break;
      }
    }

    if (!closedParens) {
      return NOT_SIGNATURE;
    }

    while (ByteParser.consumeOptional(parser, '^')) {

      GrowableByteArray.append(classFileAfter, '^');

      final String result =
          namespaceTypeSignature(
              allocator, fileName, parser, constant, replacements, classFileAfter);

      if (result != null) {
        return result;
      }
    }

    return null;
  }

  private static String namespaceGenericClassSignature(
      final Allocator allocator,
      final String fileName,
      final ByteParser parser,
      final Utf8ConstantInfo constant,
      final List<Pair<byte[], byte[]>> replacements,
      final GrowableByteArray classFileAfter) {

    if (!ByteParser.consumeOptional(parser, '<')) {
      return NOT_SIGNATURE;
    }

    GrowableByteArray.append(classFileAfter, '<');

    boolean expectsTypeNext = false;
    boolean emptyGenerics = true;
    byte current;

    while (parser.currentIndex < parser.maxIndexExclusive
        && (current = GrowableByteArray.get(parser.bytes, parser.currentIndex)) != '>') {

      emptyGenerics = false;

      switch (current) {
        case '*':
          // fallthrough;
        case '-':
          // fallthrough;
        case '+':
          if (expectsTypeNext) {
            return NOT_SIGNATURE;
          }

          expectsTypeNext = (current != '*');
          GrowableByteArray.append(classFileAfter, current);
          parser.currentIndex++;
          continue;
        case 'L':
          // fallthrough;
        case 'T':
          // fallthrough;
        case '[':
          // fallthrough;
          final String result =
              namespaceTypeSignature(
                  allocator, fileName, parser, constant, replacements, classFileAfter);

          if (result != null) {
            return result;
          }

          break;
        default:
          return NOT_SIGNATURE;
      }

      expectsTypeNext = false;
    }

    if (emptyGenerics) {
      return NOT_SIGNATURE;
    }

    if (ByteParser.consumeOptional(parser, '>')) {
      GrowableByteArray.append(classFileAfter, (byte) '>');
    } else {
      return NOT_SIGNATURE;
    }

    return null;
  }

  private static String namespaceGenericSignature(
      final Allocator allocator,
      final String fileName,
      final ByteParser parser,
      final Utf8ConstantInfo constant,
      final List<Pair<byte[], byte[]>> replacements,
      final GrowableByteArray classFileAfter) {

    if (!ByteParser.consumeOptional(parser, '<')) {
      return NOT_SIGNATURE;
    }

    boolean emptyGenerics = true;

    GrowableByteArray.append(classFileAfter, '<');

    while (parser.currentIndex < parser.maxIndexExclusive
        && GrowableByteArray.get(parser.bytes, parser.currentIndex) != '>') {

      emptyGenerics = false;

      // Primitive types aren't supported for Generics.

      // Consume until ':'.
      final int start = parser.currentIndex;
      final int end = ByteParser.consumeUntil(parser, ':');

      if (end < 0) {
        return NOT_SIGNATURE;
      }

      final int length = end - start;
      GrowableByteArray.appendBytes(parser.bytes, start, classFileAfter, length);

      final String result =
          namespaceTypeSignature(
              allocator, fileName, parser, constant, replacements, classFileAfter);

      if (result != null) {
        return result;
      }
    }

    if (emptyGenerics) {
      return NOT_SIGNATURE;
    }

    if (ByteParser.consumeOptional(parser, '>')) {
      GrowableByteArray.append(classFileAfter, (byte) '>');
    } else {
      return NOT_SIGNATURE;
    }

    if (ByteParser.reachedEnd(parser)) {
      return NOT_SIGNATURE;
    }

    final byte current = GrowableByteArray.get(parser.bytes, parser.currentIndex);
    final String result;

    switch (current) {
      case '(':
        result =
            namespaceMethodSignature(
                allocator, fileName, parser, constant, replacements, classFileAfter);
        break;
      case 'L':
        result =
            namespaceTypeSignature(
                allocator, fileName, parser, constant, replacements, classFileAfter);
        break;
      default:
        result = NOT_SIGNATURE;
    }

    return result;
  }

  private static int consumeCpInfoLength(
      final ByteParser parser, final ConstantPoolTag tag, short cp_info_tag) {

    switch (tag) {
      case CONSTANT_Utf8:
        final LongRef length =
            ByteParser.consumeOptionalUnsignedBytes(parser, Utf8ConstantInfo.LENGTH_BYTES);

        if (length == null) {
          return -1;
        }

        return (int) length.value;
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
      case CONSTANT_Unused_0:
        // fallthrough;
      case CONSTANT_Unused_2:
        // fallthrough;
      case CONSTANT_Unused_13:
        // fallthrough;
      case CONSTANT_Unused_14:
        // fallthrough;
      default:
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

    ConstantPoolTag() {
      final int ordinal = ordinal();
      if (ASSERT_ENABLED) {
        Assert.assertFalse(
            ordinal < Byte.MIN_VALUE || Byte.MAX_VALUE < ordinal,
            () -> "ConstantPoolTag must fit into a byte.");
      }
      this.ordinal = (byte) ordinal;
    }

    private static final ConstantPoolTag[] VALUES = ConstantPoolTag.values();
  }

  /**
   * Lookup table to determine if an ASCII character is a valid Java ClassTypeSignature as described
   * in the JVM spec section 4.7.9.1 Signatures
   * (https://docs.oracle.com/javase/specs/jvms/se22/html/jvms-4.html#jvms-ClassTypeSignature).
   *
   * <p>NOTE: this table also allows '$' (which is not allowed in the spec) so that code can be
   * reused for class names, file names, internal class names, and class type signatures. The
   * namespacer avoids strictly validating strings and signatures for the sake of performance.
   */
  private static boolean[] ASCII_CLASS_TYPE_SIGNATURE_CHARS;

  static {
    ASCII_CLASS_TYPE_SIGNATURE_CHARS = new boolean[128];

    for (int i = 0; i < ASCII_CLASS_TYPE_SIGNATURE_CHARS.length; i++) {
      ASCII_CLASS_TYPE_SIGNATURE_CHARS[i] =
          '$' == i
              || ('.' <= i && i <= '9')
              || ('A' <= i && i <= 'Z')
              || ('a' <= i && i <= 'z')
              || '_' == i;
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
    final StringBuilder stringBuilder = new StringBuilder(Long.BYTES * 2);
    for (int i = 0; i < ZEROS.length; i++) {
      ZEROS[i] = stringBuilder.append('0').append('0').toString();
    }
  }

  private static final class ByteParser {
    private Allocator allocator;
    private GrowableByteArray bytes;
    private int currentIndex;
    private int maxIndexExclusive;

    private ByteParser(final Allocator allocator) {
      this.allocator = allocator;
    }

    private static void reset(final ByteParser parser, final GrowableByteArray bytes) {
      parser.bytes = assertNotNull(bytes);
      parser.currentIndex = 0;
      parser.maxIndexExclusive = GrowableByteArray.size(bytes);
    }

    private static boolean reachedEnd(final ByteParser parser) {
      return parser.maxIndexExclusive <= parser.currentIndex;
    }

    private static boolean currentMatches(final ByteParser parser, final char char_) {
      Assert.assertAsciiPrintable(char_);
      return currentMatches(parser, (byte) char_);
    }

    private static boolean currentMatches(final ByteParser parser, final byte byte_) {
      return parser.currentIndex < parser.maxIndexExclusive
          && GrowableByteArray.get(parser.bytes, parser.currentIndex) == byte_;
    }

    private static boolean consumeOptional(final ByteParser parser, final char ascii) {

      assertAsciiPrintable(ascii);

      if (parser.maxIndexExclusive <= parser.currentIndex) {
        return false;
      }

      if (ascii != GrowableByteArray.get(parser.bytes, parser.currentIndex)) {
        return false;
      }

      parser.currentIndex++;
      return true;
    }

    private static boolean consumeOptional(final ByteParser parser, final int i4) {

      final int startIndex = parser.currentIndex;

      if (parser.maxIndexExclusive <= (startIndex + Integer.BYTES)) {
        return false;
      }

      for (int i = (Integer.BYTES - 1); i >= 0; i--) {
        if (((byte) ((i4 >> (i * Byte.SIZE)) & 0xFF))
            != GrowableByteArray.get(parser.bytes, parser.currentIndex++)) {
          parser.currentIndex = startIndex;
          return false;
        }
      }

      return true;
    }

    private static LongRef consumeOptionalUnsignedBytes(
        final ByteParser parser, final int sizeInBytes) {

      assertTrue(
          0 < sizeInBytes && sizeInBytes < 8,
          () -> "Unsigned size must fit into 7 bytes, but size was " + sizeInBytes);

      final int startIndex = parser.currentIndex;

      if (parser.maxIndexExclusive <= (startIndex + sizeInBytes)) {
        return null;
      }

      long u8 = 0;

      for (int i = (sizeInBytes - 1); i >= 0; i--) {
        u8 =
            (u8
                | (Byte.toUnsignedLong(GrowableByteArray.get(parser.bytes, parser.currentIndex++))
                    << (i * Byte.SIZE)));
      }

      final LongRef u8Ref = parser.allocator.allocateLongRef();
      u8Ref.value = u8;
      return u8Ref;
    }

    private static int consumeUntil(final ByteParser parser, final char expected) {

      byte current = GrowableByteArray.get(parser.bytes, parser.currentIndex);

      for (;
          current != expected && parser.currentIndex < parser.maxIndexExclusive;
          parser.currentIndex++) {
        current = GrowableByteArray.get(parser.bytes, parser.currentIndex);
      }

      if (current == expected) {
        return parser.currentIndex;
      }

      return -1;
    }
  }
}
