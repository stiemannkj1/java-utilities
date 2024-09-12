package dev.stiemannkj1.util;

import java.util.function.Supplier;

public final class Assert {

  public static final boolean ASSERT_ENABLED =
      Boolean.parseBoolean(System.getProperty(Assert.class.getTypeName() + ".enabled"));

  public static void assertTrue(final boolean value, final Supplier<String> message) {

    if (!ASSERT_ENABLED) {
      return;
    }

    if (!value) {
      throw new AssertionError(message.get());
    }
  }

  public static void assertFalse(final boolean value, final Supplier<String> message) {

    if (!ASSERT_ENABLED) {
      return;
    }

    assertTrue(!value, message);
  }

  public static <T> T assertNotNull(final T object, final String name) {

    if (!ASSERT_ENABLED) {
      return object;
    }

    assertFalse(name == null, () -> "Field name must not be null.");
    assertFalse(object == null, () -> name + " must not be null.");
    return object;
  }

  public static <T> T assertNotNull(final T object) {
    return assertNotNull(object, "Value");
  }

  public static String assertNotEmpty(final String string, final String name) {

    if (!ASSERT_ENABLED) {
      return string;
    }

    assertNotNull(string, name);
    assertFalse(string.isEmpty(), () -> name + " must not be empty.");
    return string;
  }

  public static String assertNotEmpty(final String string) {
    return assertNotEmpty(string, "String");
  }

  public static <T> T[] assertNotEmpty(final T[] array) {
    assertNotNull(array);
    assertTrue(array.length > 0, () -> "Array size must be greater than 0.");
    return array;
  }

  public static char assertAsciiPrintable(final char char_) {

    if (!ASSERT_ENABLED) {
      return char_;
    }

    assertTrue(
        '\u0020' <= char_ && char_ <= '\u007f',
        () -> "Only ASCII printable characters are supported for this method, but found: " + char_);
    return char_;
  }

  public static long assertNotNegative(final long long_, final String name) {

    if (!ASSERT_ENABLED) {
      return long_;
    }

    assertTrue(
        0 <= long_, () -> "Expected " + name + " not to be negative, but was " + long_ + ".");

    return long_;
  }

  public static int assertNotNegative(final int int_, final String name) {

    if (!ASSERT_ENABLED) {
      return int_;
    }

    assertTrue(0 <= int_, () -> "Expected " + name + " not to be negative, but was " + int_ + ".");

    return int_;
  }

  public static long assertPositive(final long long_, final String name) {

    if (!ASSERT_ENABLED) {
      return long_;
    }

    assertTrue(0 < long_, () -> "Expected " + name + " to be positive, but was " + long_ + ".");

    return long_;
  }

  public static int assertPositive(final int int_, final String name) {

    if (!ASSERT_ENABLED) {
      return int_;
    }

    assertTrue(0 < int_, () -> "Expected " + name + " to be positive, but was " + int_ + ".");

    return int_;
  }
}
