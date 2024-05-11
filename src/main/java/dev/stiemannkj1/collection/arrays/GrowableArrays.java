package dev.stiemannkj1.collection.arrays;

import dev.stiemannkj1.allocator.Allocators.Allocator;
import dev.stiemannkj1.util.Assert;

public final class GrowableArrays {

    public static final class GrowableByteArray {
        private Allocator allocator;
        public byte[] array;
        public int size;

        public GrowableByteArray(final Allocator allocator) {
            this.allocator = Assert.assertNotNull(allocator);
        }

        public static boolean growIfNecessary(final GrowableByteArray growableByteArray, final int newSize) {

            if (growableByteArray.array == null) {
               growableByteArray.array = growableByteArray.allocator.allocateBytes(newSize);
               return true;
            }

            if (newSize <= growableByteArray.array.length) {
                return false;
            }

            final byte[] oldArray = growableByteArray.array;
            growableByteArray.array = growableByteArray.allocator.allocateBytes(newSize);
            System.arraycopy(oldArray, 0, growableByteArray.array, 0, oldArray.length);
            growableByteArray.allocator.deallocateObject(oldArray);
            return true;
        }
    }

    private GrowableArrays() {}
}
