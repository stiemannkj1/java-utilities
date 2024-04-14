package dev.stiemannkj1.allocator;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.function.Function;
import java.util.function.Supplier;

public final class Allocators {

    public static final Allocator JVM_HEAP = new JvmHeap();

    public interface Allocator {
        boolean[] allocateBooleans(final int size);
        byte[] allocateBytes(final int size);
        short[] allocateShorts(final int size);
        int[] allocateInts(final int size);
        long[] allocateLongs(final int size);
        float[] allocateFloats(final int size);
        double[] allocateDoubles(final int size);
        char[] allocateChars(final int size);
        StringBuilder allocateStringBuilder(final int size);
        <T> T allocateObject(final Function<Allocator, T> noArgsConstructor);
        <T> List<T> allocateObjects(final int size);
        <T> List<T> allocateObjectsWithFixedSize(final int size);
    }

   public static final class JvmHeap implements Allocator {
       private JvmHeap() {}

       @Override
       public boolean[] allocateBooleans(final int size) {
           return new boolean[size];
       }

       @Override
       public byte[] allocateBytes(final int size) {
           return new byte[size];
       }

       @Override
       public short[] allocateShorts(final int size) {
           return new short[size];
       }

       @Override
       public int[] allocateInts(final int size) {
           return new int[size];
       }

       @Override
       public long[] allocateLongs(final int size) {
           return new long[size];
       }

       @Override
       public float[] allocateFloats(final int size) {
           return new float[size];
       }

       @Override
       public double[] allocateDoubles(final int size) {
           return new double[size];
       }

       @Override
       public char[] allocateChars(final int size) {
           return new char[size];
       }

      @Override
       public StringBuilder allocateStringBuilder(final int size) {
           return new StringBuilder(size);
      }

      @Override
       public <T> T allocateObject(final Function<Allocator, T> constructor) {
           return constructor.apply(this);
      }

      @Override
      public <T> List<T> allocateObjects(final int size) {
           return new ArrayList<>(size);
      }

      @Override
       public <T> List<T> allocateObjectsWithFixedSize(final int size) {
           @SuppressWarnings("unchecked")
           final List<T> objects = Arrays.asList((T[]) new Object[size]);
           return objects;
      }
   }

   private Allocators() {}
}
