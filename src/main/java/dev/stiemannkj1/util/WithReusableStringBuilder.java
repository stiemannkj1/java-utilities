package dev.stiemannkj1.util;

public abstract class WithReusableStringBuilder {

  private static final int DEFAULT_CAPACITY = 1 << Byte.SIZE;
  private StringBuilder stringBuilder;

  public WithReusableStringBuilder(final int capacity) {
    this.stringBuilder = new StringBuilder(capacity);
  }

  public WithReusableStringBuilder() {
    this(DEFAULT_CAPACITY);
  }

  public final StringBuilder resetStringBuilder() {
    stringBuilder.setLength(0);
    return stringBuilder;
  }
}
