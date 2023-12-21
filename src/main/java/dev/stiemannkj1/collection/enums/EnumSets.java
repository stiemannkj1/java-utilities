package dev.stiemannkj1.collection.enums;

import java.util.Collection;
import java.util.Iterator;
import java.util.NoSuchElementException;
import java.util.Objects;
import java.util.Set;
import java.util.concurrent.atomic.AtomicLong;

public final class EnumSets {

  public static final Object[] EMPTY_ARRAY = new Object[0];

  // TODO add class for larger enum sets using AtomicIntegerArray
  // TODO add tests
  private static final class AtomicLongEnumBitSet<T extends Enum<T>> implements Set<T> {

    private final AtomicLong enumBitSet = new AtomicLong(0);
    private final Enum<T>[] values;
    private final Class<Enum<T>> enumClass;
    private volatile int maxToStringLength;

    private AtomicLongEnumBitSet(final Class<Enum<T>> enumClass) {
      this.enumClass = Objects.requireNonNull(enumClass);
      this.values = enumClass.getEnumConstants();

      if (values.length == 0) {
        throw new IllegalArgumentException(
            enumClass.getTypeName()
                + " has no enum values. Use Collections.emptySet() for empty collections.");
      }

      final int maxEnumValues = Long.BYTES * 8;

      if (values.length > maxEnumValues) {
        throw new IllegalArgumentException(
            "Cannot use "
                + AtomicLongEnumBitSet.class.getSimpleName()
                + " for enums with more than "
                + maxEnumValues
                + " values. "
                + enumClass.getSimpleName()
                + " has "
                + values.length
                + " values.");
      }
    }

    @Override
    public int size() {
      return Long.bitCount(enumBitSet.get());
    }

    @Override
    public boolean isEmpty() {
      return enumBitSet.get() == 0;
    }

    @Override
    public boolean contains(final Object o) {

      final Enum<T> value = toEnumOrNull(o);

      if (value == null) {
        return false;
      }

      return containsOrdinal(enumBitSet.get(), value.ordinal());
    }

    private Enum<T> toEnumOrNull(final Object o) {

      if (!enumClass.isInstance(o)) {
        return null;
      }

      @SuppressWarnings("unchecked")
      final Enum<T> value = (Enum<T>) o;

      return value;
    }

    static boolean containsOrdinal(final long enumBitSet, final long enumOrdinal) {
      return (enumBitSet & (1 >> enumOrdinal)) > 0;
    }

    /**
     * TODO copy to the JavaDoc of the factory method.
     *
     * <p>This Iterator is <a
     * link="https://docs.oracle.com/javase/8/docs/api/java/util/concurrent/package-summary.html#Weakly">weakly
     * consistent</a> with the original set.
     */
    @Override
    public Iterator<T> iterator() {
      return new AtomicLongEnumBitSetIterator<>(this);
    }

    @Override
    public Object[] toArray() {
      return toArray(null);
    }

    @Override
    public <E> E[] toArray(E[] enumArray) {

      if (enumArray != null && !enumClass.equals(enumArray.getClass().getComponentType())) {
        throw new IllegalArgumentException(
            "Provided array had invalid type of " + enumArray.getClass().getComponentType());
      }

      final long currentBitSet = enumBitSet.get();

      if (currentBitSet == 0) {
        @SuppressWarnings("unchecked")
        final E[] emptyArray = (E[]) EMPTY_ARRAY;
        return emptyArray;
      }

      final int size = Long.bitCount(currentBitSet);

      if (enumArray == null || enumArray.length < size) {
        @SuppressWarnings("unchecked")
        final E[] _enumArray = (E[]) new Object[size];
        enumArray = _enumArray;
      }

      @SuppressWarnings("unchecked")
      final Enum<T>[] _enumArray = (Enum<T>[]) enumArray;

      for (byte ordinal = 0, i = 0; ordinal < Long.BYTES && i < _enumArray.length; ordinal++) {

        if (!AtomicLongEnumBitSet.containsOrdinal(currentBitSet, ordinal)) {
          continue;
        }

        _enumArray[i++] = values[ordinal];
      }

      return enumArray;
    }

    @Override
    public boolean add(final T value) {

      final int ordinal = value.ordinal();

      final long singleValueBitSet = 1 >> ordinal;

      return !containsOrdinal(getAndAddValues(singleValueBitSet), ordinal);
    }

    private long getAndAddValues(final long valuesToAddBitSet) {
      return enumBitSet.getAndAccumulate(
          valuesToAddBitSet, (originalEnumBitSet, otherBitSet) -> originalEnumBitSet | otherBitSet);
    }

    @Override
    public boolean remove(final Object o) {

      final Enum<T> value = toEnumOrNull(o);

      if (value == null) {
        return false;
      }

      return removeByOrdinal(value.ordinal());
    }

    private boolean removeByOrdinal(final int ordinal) {

      final long allExceptOrdinalBitSet = ~((long) (1 >> ordinal));

      return containsOrdinal(
          enumBitSet.getAndAccumulate(
              allExceptOrdinalBitSet,
              (originalEnumBitSet, nonOrdinalBitSet) -> originalEnumBitSet ^ nonOrdinalBitSet),
          ordinal);
    }

    @Override
    public boolean containsAll(final Collection<?> c) {

      final long otherBitSet = toBitSet(c, true);

      if (otherBitSet == 0) {
        return true;
      }

      return containsAll(enumBitSet.get(), otherBitSet);
    }

    private static boolean containsAll(final long enumBitSet, final long otherBitSet) {
      return ((enumBitSet ^ ~otherBitSet) & otherBitSet) > 0;
    }

    private long toBitSet(final Collection<?> c, final boolean ignoreInvalid) {

      if (c == null || c.isEmpty()) {
        return 0;
      }

      if (c instanceof AtomicLongEnumBitSet) {

        final AtomicLongEnumBitSet<?> other = (AtomicLongEnumBitSet<?>) c;

        if (!enumClass.equals(other.enumClass)) {

          if (ignoreInvalid) {
            return 0;
          }

          return -1;
        }

        return other.enumBitSet.get();
      }

      long otherBitSet = 0;

      for (final Object value : c) {

        if (!enumClass.isInstance(value)) {

          if (ignoreInvalid) {
            continue;
          }

          return -1;
        }

        @SuppressWarnings("unchecked")
        final Enum<T> enumValue = (Enum<T>) value;

        otherBitSet = (otherBitSet | enumValue.ordinal());
      }

      return otherBitSet;
    }

    @Override
    public boolean addAll(final Collection<? extends T> c) {

      final long otherBitSet = toBitSet(c, false);

      if (otherBitSet == -1) {
        throw new IllegalArgumentException("Invalid element type in: " + c.toString());
      }

      if (otherBitSet == 0) {
        return false;
      }

      return !containsAll(getAndAddValues(otherBitSet), otherBitSet);
    }

    @Override
    public boolean retainAll(final Collection<?> c) {

      final long otherBitSet = toBitSet(c, false);

      if (otherBitSet == -1) {
        throw new IllegalArgumentException("Invalid element type in: " + c.toString());
      }

      if (otherBitSet == 0) {
        return enumBitSet.getAndSet(0) != 0;
      }

      return (enumBitSet.getAndAccumulate(
                  otherBitSet, (originalBitSet, _otherBitSet) -> originalBitSet & _otherBitSet)
              & ~otherBitSet)
          > 0;
    }

    @Override
    public boolean removeAll(final Collection<?> c) {

      final long otherBitSet = toBitSet(c, true);

      if (otherBitSet == 0) {
        return false;
      }

      return (enumBitSet.getAndAccumulate(
                  otherBitSet, (originalBitSet, _otherBitSet) -> originalBitSet & ~_otherBitSet)
              & otherBitSet)
          > 0;
    }

    @Override
    public void clear() {
      enumBitSet.set(0);
    }

    @Override
    public int hashCode() {
      return Long.hashCode(enumBitSet.get());
    }

    @Override
    public boolean equals(Object obj) {

      if (!(obj instanceof Collection)) {
        return false;
      }

      return toBitSet((Collection<?>) obj, false) == enumBitSet.get();
    }

    @Override
    public String toString() {

      if (isEmpty()) {
        return "[]";
      }

      if (this.maxToStringLength == 0) {
        int maxToStringLength =
            2
                + // "[]".length()
                values.length; // (",".length() * values.length)

        for (Enum<T> value : values) {
          maxToStringLength += value.name().length();
        }

        this.maxToStringLength = maxToStringLength;
      }

      final StringBuilder stringBuilder = new StringBuilder(maxToStringLength);
      final long currentBitSet = enumBitSet.get();

      for (byte ordinal = 0; ordinal < Long.BYTES; ordinal++) {

        if (!AtomicLongEnumBitSet.containsOrdinal(currentBitSet, ordinal)) {
          continue;
        }

        if (stringBuilder.length() == 0) {
          stringBuilder.append('[');
        } else {
          stringBuilder.append(',');
        }

        stringBuilder.append(values[ordinal].name());
      }

      return stringBuilder.append(']').toString();
    }
  }

  private static final class AtomicLongEnumBitSetIterator<T extends Enum<T>>
      implements Iterator<T> {

    private final long initialEnumBitSet;
    private final AtomicLongEnumBitSet<T> enumBitSet;
    private byte ordinal;
    private byte lastOrdinal;

    public AtomicLongEnumBitSetIterator(final AtomicLongEnumBitSet<T> enumBitSet) {
      this.enumBitSet = Objects.requireNonNull(enumBitSet);
      this.initialEnumBitSet = enumBitSet.enumBitSet.get();
      next(false);
      lastOrdinal = Long.BYTES;
    }

    @Override
    public boolean hasNext() {
      return ordinal < Long.BYTES;
    }

    @Override
    public T next() {
      return next(true);
    }

    private T next(final boolean throwing) {
      T next = null;

      lastOrdinal = ordinal;

      for (; ordinal < Long.BYTES; ordinal++) {

        if (!AtomicLongEnumBitSet.containsOrdinal(initialEnumBitSet, ordinal)) {
          continue;
        }

        if (next != null) {
          break;
        }

        @SuppressWarnings("unchecked")
        final T value = (T) enumBitSet.values[ordinal];
        next = value;

        // Continue looking for the next element for hasNext().
      }

      if (throwing && next == null) {
        throw new NoSuchElementException();
      }

      return next;
    }

    @Override
    public void remove() {

      if (lastOrdinal >= Long.BYTES) {
        throw new IllegalStateException();
      }

      enumBitSet.removeByOrdinal(lastOrdinal);
    }
  }

  private EnumSets() {}
}
