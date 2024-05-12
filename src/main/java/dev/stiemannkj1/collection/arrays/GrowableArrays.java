package dev.stiemannkj1.collection.arrays;

import dev.stiemannkj1.allocator.Allocators.Allocator;
import dev.stiemannkj1.util.Assert;

import java.io.IOException;
import java.io.InputStream;

public final class GrowableArrays {

    public static final class GrowableByteArray {
        private Allocator allocator;
        private byte[] bytes;
        private int size;

        public GrowableByteArray(final Allocator allocator) {
            this.allocator = Assert.assertNotNull(allocator);
        }

        public static byte[] bytes(final GrowableByteArray array) {
            return array.bytes;
        }

        public static int size(final GrowableByteArray array) {
            return array.size;
        }

        public static byte get(final GrowableByteArray array, final int index) {
            return array.bytes[index];
        }

        public static boolean growIfNecessary(final GrowableByteArray array, final int newSize) {

            if (array.bytes == null) {
               array.bytes = array.allocator.allocateBytes(newSize);
               return true;
            }

            checkMaxValue(newSize);

            if (newSize <= array.bytes.length) {
                return false;
            }

            final byte[] oldArray = array.bytes;
            array.bytes = array.allocator.allocateBytes(newSize);
            System.arraycopy(oldArray, 0, array.bytes, 0, oldArray.length);
            array.allocator.deallocateObject(oldArray);
            return true;
        }

        public static void append(final GrowableByteArray array, final byte i1) {

            growIfNecessary(array, array.size + 1);
            array.bytes[array.size++] = i1;
        }

        public static void appendBytes(final GrowableByteArray array, final long bytes, final int sizeInBytes) {

            if (Assert.ASSERT_ENABLED) {
                Assert.assertFalse(sizeInBytes < 1 || 8 < sizeInBytes, () -> "Bytes to write must be between 1 and " + Long.BYTES);
            }

            growIfNecessary(array, sizeInBytes);

            for (int i = (sizeInBytes - 1); i >= 0; i--) {
                append(array, (byte) (bytes >> (i * Byte.SIZE)));
            }
        }

        public static void copyBytes(final GrowableByteArray src, final int srcPos, final GrowableByteArray dest, final int destPos, final int length) {
           arraycopy(src.bytes, srcPos, dest, destPos, length);
        }

        public static void appendBytes(final GrowableByteArray src, final int srcPos, final GrowableByteArray dest, final int length) {
           arraycopy(src.bytes, srcPos, dest, size(dest), length);
        }

        public static void appendBytes(final byte[] src, final int srcPos, final GrowableByteArray dest, final int length) {
            arraycopy(src, srcPos, dest, size(dest), length);
        }

        private static void arraycopy(final byte[] src, final int srcPos, final GrowableByteArray dest, final int destPos, final int length) {

            growIfNecessary(dest, destPos + length);

            System.arraycopy(src, srcPos, dest.bytes, destPos, length);
            dest.size += length;
        }

        public static int read(final InputStream inputStream, final GrowableByteArray array, final int bytesToRead) throws IOException {
            growIfNecessary(array, array.size + bytesToRead);
            final int read = inputStream.read(array.bytes, array.size, bytesToRead);

            if (read < 1) {
                return read;
            }

            array.size += read;
            return read;
        }

        private static void checkMaxValue(final int size) {
            if (size < 0) {
                throw new IndexOutOfBoundsException((((long) (-size)) + Integer.MAX_VALUE) + " out of bounds for array with size: " + Integer.MAX_VALUE);
            }
        }
    }


    private GrowableArrays() {}
}
