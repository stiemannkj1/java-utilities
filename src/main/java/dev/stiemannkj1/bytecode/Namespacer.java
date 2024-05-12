package dev.stiemannkj1.bytecode;

import dev.stiemannkj1.allocator.Allocators.Allocator;
import dev.stiemannkj1.collection.arrays.GrowableArrays.GrowableByteArray;
import dev.stiemannkj1.util.Assert;
import dev.stiemannkj1.util.Pair;

import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.Map;

import static dev.stiemannkj1.util.Assert.assertNotEmpty;
import static dev.stiemannkj1.util.Assert.assertNotNull;
import static dev.stiemannkj1.util.Assert.assertTrue;

public final class Namespacer {

    public static final int CONSTANT_UTF8_INFO_HEADER_SIZE = 3;

    public static String namespace(final Allocator allocator, final String fileName, final byte[] classFileBefore, final Map<String, String> replacementsMap, final GrowableByteArray classFileAfter) {

            final List<Pair<byte[], byte[]>> replacements = allocator.allocateList(replacementsMap.size());
            // defer$(() -> allocator.deallocateObject(replacements));

            for (final Map.Entry<String, String> entry : replacementsMap.entrySet()) {
                replacements.add(Pair.of(entry.getKey().getBytes(StandardCharsets.UTF_8), entry.getValue().getBytes(StandardCharsets.UTF_8)));
            }

            final ByteParser parser = allocator.allocateObject(ByteParser::new);
            ByteParser.reset(parser, classFileBefore, fileName);

            ByteParser.consumeRequired(parser, 0xCAFEBABE);
            parser.currentIndex += 4; // Skip major and minor versions.
            final int constant_pool_count = (int) ByteParser.consumeUnsignedBytes(parser, 2) - 1;
            final Writer writer = allocator.allocateObject(Writer::new);
            Writer.initialize(writer, classFileAfter, fileName);
            Writer.write(writer, classFileBefore, 0, parser.currentIndex);

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
                    Writer.write(writer, classFileBefore, constantStartIndex, length + 1);

                    continue;
                }

                int finalLength = length;
                int remainingLength = length;

                replacing:
                for (final Pair<byte[], byte[]> replacement : replacements) {

                    for (int j = 0; j < replacement.left().length; j++) {
                        if (replacement.left()[j] != classFileBefore[parser.currentIndex + j]) {
                            continue replacing;
                        }
                    }

                    Writer.write(writer, replacement.right(), 0, replacement.right().length);
                    remainingLength = length - replacement.left().length;
                    finalLength = replacement.right().length + remainingLength;
                    // TODO add simple test for feedback
                    // TODO fix bugs
                    // TODO add complex tests with field refs, method refs, lambdas, invokedynamic, method descriptors etc.
                    break;
                }

                if (remainingLength < length) {
                    parser.currentIndex += (length - remainingLength);
                    GrowableByteArray.growIfNecessary(classFileAfter, classFileAfter.size + CONSTANT_UTF8_INFO_HEADER_SIZE);
                    classFileAfter.array[classFileAfter.size++] = ConstantPoolTag.CONSTANT_Utf8.ordinal;
                    // TODO write size here
                } else {
                    Writer.write(writer, classFileBefore, constantStartIndex, CONSTANT_UTF8_INFO_HEADER_SIZE);
                }

                final int start = parser.currentIndex;
                parser.currentIndex += remainingLength;
                Writer.write(writer, parser.bytes, start, remainingLength);
            }

            if (!ByteParser.shouldContinue(parser)) {
                return parser.errorMessage.toString();
            }

            if (Writer.hasError(writer)) {
                return writer.errorMessage.toString();
            }

            return null;
    }

    private static int consumeCpInfoLength(final ByteParser parser, final ConstantPoolTag tag, short cp_info_tag) {

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
                parser.errorMessage.append("Unexpected constant tag found ").append(cp_info_tag).append(". Unable to namespace values in class file.");
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
                Assert.assertFalse(ordinal < Byte.MIN_VALUE || Byte.MAX_VALUE < ordinal, () -> "ConstantPoolTag must fit into a byte.");
            }
            this.ordinal = (byte) ordinal;
        }

        private static final ConstantPoolTag[] VALUES = ConstantPoolTag.values();
    }

    private static final class ByteParser {
        private byte[] bytes;
        private int currentIndex;
        private String fileName;
        private StringBuilder errorMessage;

        private ByteParser(final Allocator allocator) {
            this.errorMessage = assertNotNull(allocator).allocateStringBuilder(256);
        }

        private static void reset(final ByteParser parser, final byte[] bytes, final String fileName) {
            parser.bytes = assertNotNull(bytes);
            parser.fileName = assertNotEmpty(fileName);
            parser.errorMessage.setLength(0);
        }

        private static boolean shouldContinue(final ByteParser parser) {
            return parser.errorMessage.length() == 0 && parser.bytes.length > parser.currentIndex;
        }

        private static boolean consumeOptional(final ByteParser parser, final int i4) {

            for (int i = (Integer.BYTES - 1); i >= 0; i--) {

                if (!shouldContinue(parser)) {
                    return false;
                }

                if (((byte) ((i4 >> (i * Byte.SIZE)) & 0xFF)) != parser.bytes[parser.currentIndex++]) {
                    return false;
                }
            }

            return true;
        }

        private static void consumeRequired(final ByteParser parser, final int i4) {

            final int indexBefore = parser.currentIndex;

            if (!consumeOptional(parser, i4)) {
                parser.errorMessage.append("Failed to parse ").append(parser.fileName).append(" at offset ").append(parser.currentIndex).append(". Expecting 0x");
                appendHexString(parser.errorMessage, i4)
                        .append(" but found 0x");
                appendHexString(parser.errorMessage, parser.bytes, indexBefore, Integer.BYTES);
            }
        }

        private static long consumeUnsignedBytes(final ByteParser parser, final int sizeInBytes) {
            assertTrue(0 < sizeInBytes && sizeInBytes < 8, () -> "Unsigned size must fit into 7 bytes, but size was " + sizeInBytes);

            long u8 = 0;

            for (int i = (sizeInBytes - 1); i >= 0; i--) {

                if (!shouldContinue(parser)) {
                    return 0;
                }

                u8 = (u8 | (((long) parser.bytes[parser.currentIndex++]) << (i * Byte.SIZE)));
            }

            return u8;
        }

        private static void consumeBytes(final ByteParser parser, final int sizeToConsume) {
            parser.currentIndex+=sizeToConsume;
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

        private static StringBuilder appendHexString(final StringBuilder stringBuilder, final byte[] bytes, int offset, int size) {

            for (int i = offset; i < size; i++) {
                appendHexString(stringBuilder, bytes[i]);
            }

            return stringBuilder;
        }

        private static StringBuilder appendHexString(final StringBuilder stringBuilder, final byte i1) {
            final int i4 = Byte.toUnsignedInt(i1);
            return stringBuilder.append(BYTES_AS_HEX[i4]).append(BYTES_AS_HEX[i4 + 1]);
        }

        private static final char[] HEX_CHARS = new char[] {
                '0',
                '1',
                '2',
                '3',
                '4',
                '5',
                '6',
                '7',
                '8',
                '9',
                'A',
                'B',
                'C',
                'D',
                'E',
                'F'
        };

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

    private static final class Writer {
        private GrowableByteArray classFileAfter;
        private String fileName;
        private StringBuilder errorMessage;

        private Writer(final Allocator allocator) {
            this.errorMessage = Assert.assertNotNull(allocator).allocateStringBuilder(256);
        }

        private static void initialize(final Writer writer, final GrowableByteArray classFileAfter, final String fileName) {
            writer.classFileAfter = assertNotNull(classFileAfter);
            writer.fileName = assertNotEmpty(fileName);
            writer.errorMessage.setLength(0);
        }

        private static void write(final Writer writer, final byte[] bytesToWrite, final int start, final int length) {

            final long minimumRequiredSize = (long) writer.classFileAfter.size + length;

            if (minimumRequiredSize > Integer.MAX_VALUE) {
                writer.errorMessage.append("Failed to write bytes to ").append(writer.fileName).append(". Max Java array size exceeded.");
                return;
            }

            GrowableByteArray.growIfNecessary(writer.classFileAfter, (int) minimumRequiredSize);

            System.arraycopy(bytesToWrite, start, writer.classFileAfter.array, writer.classFileAfter.size, length);
            writer.classFileAfter.size+=length;
        }

        private static boolean hasError(final Writer writer) {
            return writer.errorMessage.length() > 0;
        }
    }
}
