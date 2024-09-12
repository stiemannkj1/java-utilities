package dev.stiemannkj1.collection.arrays;

import static dev.stiemannkj1.util.Assert.ASSERT_ENABLED;
import static dev.stiemannkj1.util.Assert.assertAsciiPrintable;
import static dev.stiemannkj1.util.Assert.assertFalse;
import static dev.stiemannkj1.util.Assert.assertPositive;
import static dev.stiemannkj1.util.Assert.assertTrue;

import java.io.IOException;
import java.io.InputStream;

// TODO make GrowableByteArray top level and use ByteBuffer as the backing object rather than an
// array so we can take advantage of native memory.
// TODO build slices on top of GrowableByteArray
// TODO build UTF-8 strings on top of GrowableByteArray
// TODO build arena allocator (and possibly others) instead of GrowableByteArray
public final class GrowableArrays {

  public static final class GrowableByteArray {
    private byte[] bytes;
    private int size;

    public GrowableByteArray(final int initialCapacity) {
      bytes = new byte[assertPositive(initialCapacity, "initialCapacity")];
    }

    public static void clear(final GrowableByteArray array) {
      expand(array, 0);
    }

    public static void expand(final GrowableByteArray array, final int newSize) {

      if (ASSERT_ENABLED) {
        assertTrue(newSize >= 0, () -> "Array size must not be negative.");
      }

      growIfNecessary(array, newSize);
      array.size = newSize;
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

    public static void growIfNecessary(final GrowableByteArray array, final int newSize) {

      checkMaxValue(newSize);

      if (newSize <= array.bytes.length) {
        return;
      }

      final byte[] oldArray = array.bytes;
      array.bytes = new byte[newSize];
      System.arraycopy(oldArray, 0, array.bytes, 0, oldArray.length);
    }

    public static void append(final GrowableByteArray array, final char ascii) {

      assertAsciiPrintable(ascii);

      append(array, (byte) ascii);
    }

    public static void append(final GrowableByteArray array, final byte i1) {

      growIfNecessary(array, array.size + 1);
      array.bytes[array.size++] = i1;
    }

    public static void copyBytes(
        final GrowableByteArray array, final long bytes, int index, final int sizeInBytes) {

      if (ASSERT_ENABLED) {
        assertFalse(
            sizeInBytes < 1 || 8 < sizeInBytes,
            () -> "Bytes to write must be between 1 and " + Long.BYTES);
      }

      growIfNecessary(array, index + sizeInBytes + 1);

      for (int i = (sizeInBytes - 1); i >= 0; i--) {
        array.bytes[index] = (byte) (bytes >> (i * Byte.SIZE));
        index++;
      }
    }

    public static void copyBytes(
        final GrowableByteArray src,
        final int srcPos,
        final GrowableByteArray dest,
        final int destPos,
        final int length) {
      arraycopy(src.bytes, srcPos, dest, destPos, length);
    }

    public static void appendBytes(
        final GrowableByteArray src,
        final int srcPos,
        final GrowableByteArray dest,
        final int length) {
      arraycopy(src.bytes, srcPos, dest, size(dest), length);
    }

    public static void appendBytes(
        final byte[] src, final int srcPos, final GrowableByteArray dest, final int length) {
      arraycopy(src, srcPos, dest, size(dest), length);
    }

    private static void arraycopy(
        final byte[] src,
        final int srcPos,
        final GrowableByteArray dest,
        final int destPos,
        final int length) {

      growIfNecessary(dest, destPos + length);

      System.arraycopy(src, srcPos, dest.bytes, destPos, length);
      dest.size = destPos + length;
    }

    public static void readFully(final GrowableByteArray array, final InputStream inputStream)
        throws IOException {
      int read = 0;

      while ((read =
              inputStream.read(
                  GrowableByteArray.bytes(array),
                  read,
                  GrowableByteArray.bytes(array).length - read))
          > -1) {

        array.size += read;

        if (GrowableByteArray.size(array) == GrowableByteArray.bytes(array).length) {
          GrowableByteArray.expand(array, GrowableByteArray.size(array) << 1);
        }
      }
    }

    private static void checkMaxValue(final int size) {
      if (size < 0) {
        throw new IndexOutOfBoundsException(
            (((long) (-size)) + Integer.MAX_VALUE)
                + " out of bounds for array with size: "
                + Integer.MAX_VALUE);
      }
    }
  }

  private GrowableArrays() {}
}
