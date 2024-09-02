package dev.stiemannkj1.allocator;

import dev.stiemannkj1.util.References.BooleanRef;
import dev.stiemannkj1.util.References.ByteRef;
import dev.stiemannkj1.util.References.CharRef;
import dev.stiemannkj1.util.References.DoubleRef;
import dev.stiemannkj1.util.References.FloatRef;
import dev.stiemannkj1.util.References.IntRef;
import dev.stiemannkj1.util.References.LongRef;
import dev.stiemannkj1.util.References.ShortRef;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.function.Function;

public final class Allocators {

  public static final Allocator JVM_HEAP = new JvmHeap();

  public interface Allocator extends AutoCloseable {
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

    <T> List<T> allocateList(final int size);

    <T> List<T> allocateListWithFixedSize(final int size);

    BooleanRef allocateBooleanRef();

    ByteRef allocateByteRef();

    ShortRef allocateShortRef();

    IntRef allocateIntRef();

    LongRef allocateLongRef();

    FloatRef allocateFloatRef();

    DoubleRef allocateDoubleRef();

    CharRef allocateCharRef();

    void deallocateObject(final Object object);

    Allocator scopeAllocator();

    /** Alias for {@link #closeScope()}. */
    default void close() {
      closeScope();
    }

    void closeScope();
  }

  private static final class JvmHeap implements Allocator {
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
    public <T> List<T> allocateList(final int size) {
      return new ArrayList<>(size);
    }

    @Override
    public <T> List<T> allocateListWithFixedSize(final int size) {
      @SuppressWarnings("unchecked")
      final List<T> objects = Arrays.asList((T[]) new Object[size]);
      return objects;
    }

    @Override
    public BooleanRef allocateBooleanRef() {
      return new BooleanRef();
    }

    @Override
    public ByteRef allocateByteRef() {
      return new ByteRef();
    }

    @Override
    public ShortRef allocateShortRef() {
      return new ShortRef();
    }

    @Override
    public IntRef allocateIntRef() {
      return new IntRef();
    }

    @Override
    public LongRef allocateLongRef() {
      return new LongRef();
    }

    @Override
    public FloatRef allocateFloatRef() {
      return new FloatRef();
    }

    @Override
    public DoubleRef allocateDoubleRef() {
      return new DoubleRef();
    }

    @Override
    public CharRef allocateCharRef() {
      return new CharRef();
    }

    @Override
    public void deallocateObject(final Object object) {
      // no-op
    }

    @Override
    public Allocator scopeAllocator() {
      return this;
    }

    @Override
    public void closeScope() {
      // no-op
    }
  }

  private Allocators() {}
}
