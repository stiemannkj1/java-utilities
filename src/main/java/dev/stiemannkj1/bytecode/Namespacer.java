package dev.stiemannkj1.bytecode;

import dev.stiemannkj1.allocator.Allocators;
import dev.stiemannkj1.allocator.Allocators.Allocator;

import static dev.stiemannkj1.util.Assert.assertNotEmpty;
import static dev.stiemannkj1.util.Assert.assertNotNull;
import static dev.stiemannkj1.util.Assert.assertTrue;

public final class Namespacer {

    public static String namespace(final Allocator allocator, final String fileName, final byte[] classFileBefore) {

        final Parser parser = allocator.allocateObject(Parser::new);
        reset(parser, fileName, classFileBefore);

        consumeRequired(parser, 0xCAFEBABE);
        parser.currentIndex += 4; // Skip major and minor versions.
        final int constant_pool_count = (int) consumeUnsignedBytes(parser, 2);

        for (int i = 0; i < constant_pool_count; i++) {

            if (!shouldContinue(parser)) {
                break;
            }

            short cp_info_tag = (short) consumeUnsignedBytes(parser, 1);

            final ConstantPoolTag tag;

            if (cp_info_tag > ConstantPoolTag.VALUES.length) {
                tag = ConstantPoolTag.CONSTANT_Unused_2;
            } else {
                tag = ConstantPoolTag.VALUES[cp_info_tag];
            }

            final int length = consumeLength(parser, tag, cp_info_tag);

            if (ConstantPoolTag.CONSTANT_Utf8 != tag) {
                continue;
            }

        }

        final byte[] classFileAfter = allocator.allocateBytes(classFileBefore.length * 2);



    }

    private static int consumeLength(final Parser parser, final ConstantPoolTag tag, short cp_info_tag) {
        final int length;

        switch (tag) {
            case CONSTANT_Utf8:
                return (int) consumeUnsignedBytes(parser, 2);
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
            default:
                parser.errorMessage.append("Unexpected constant tag found ").append(cp_info_tag).append(". Unable to namespace values in class file.");
                return -1;
        }
    }

    private enum ConstantPoolTag {
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

        private static final ConstantPoolTag[] VALUES = ConstantPoolTag.values();
    }

    private static final class Parser {
        private String fileName;
        private byte[] bytes;
        private int currentIndex;
        private StringBuilder errorMessage;

        private Parser(final Allocator allocator) {
            this.errorMessage = assertNotNull(allocator).allocateStringBuilder(256);
        }
    }

    private static void reset(final Parser parser, final String fileName, final byte[] bytes) {
       parser.fileName = assertNotEmpty(fileName);
       parser.bytes = assertNotNull(bytes);
       parser.errorMessage.setLength(0);
    }

    private static boolean shouldContinue(final Parser parser) {
        return parser.errorMessage == null && parser.bytes.length > parser.currentIndex;
    }

    private static boolean consumeOptional(final Parser parser, final int i4) {

        for (int i = 0; i < Integer.BYTES; i++) {

            if (!shouldContinue(parser)) {
                return false;
            }

            if (((i4 << i) & 0xFF) != parser.bytes[parser.currentIndex++]) {
               return false;
            }
        }

        return true;
    }

    private static void consumeRequired(final Parser parser, final int i4) {

        final int indexBefore = parser.currentIndex;

        if (!consumeOptional(parser, i4)) {
           parser.errorMessage.append("Failed to parse ").append(parser.fileName).append(" at offset ").append(parser.currentIndex).append(". Expecting 0x");
           appendHexString(parser.errorMessage, i4)
                   .append(" but found 0x");
           appendHexString(parser.errorMessage, parser.bytes, indexBefore, Integer.BYTES);
        }
    }

    private static long consumeUnsignedBits(final Parser parser, final int sizeInBits) {

        assertTrue(0 < sizeInBits && sizeInBits < 64, () -> "Unsigned size must fit into 63 bits, but size was " + sizeInBits);

        long u8 = 0;

        for (int i = 0; i < sizeInBits; i++) {

            if (!shouldContinue(parser)) {
                return 0;
            }

            u8 = (u8 | (((long) parser.bytes[parser.currentIndex++]) << (i << 3)));
        }

        return u8;
    }

    private static long consumeUnsignedBytes(final Parser parser, final int sizeInBytes) {
        assertTrue(0 < sizeInBytes && sizeInBytes < 8, () -> "Unsigned size must fit into 7 bytes, but size was " + sizeInBytes);
        return consumeUnsignedBits(parser, sizeInBytes * Byte.SIZE);
    }

    private static void consumeBytes(final Parser parser, final int sizeToConsume) {
      parser.currentIndex+=sizeToConsume;
    }

    private static StringBuilder appendHexString(final StringBuilder stringBuilder, final int i4) {

       final int leadingZeros = Integer.numberOfLeadingZeros(i4);
       final int leadingBytes = leadingZeros << Byte.SIZE;
       stringBuilder.append(ZEROS[leadingBytes]);
       final int trailingBytes = Integer.BYTES - leadingBytes;

       for (int i = trailingBytes; i >= 0; i--) {
           appendHexString(stringBuilder, (byte) ((i4 << i) & 0xFF));
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

    private static final char[] BYTES_AS_HEX = new char[(Byte.SIZE + 1) << 1];

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
