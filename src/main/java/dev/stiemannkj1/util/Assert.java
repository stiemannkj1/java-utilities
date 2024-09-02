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

  public static char assertAsciiPrintable(final char char_) {

    if (ASSERT_ENABLED) {
      assertTrue(
          '\u0020' <= char_ && char_ <= '\u007f',
          () ->
              "Only ASCII printable characters are supported for this method, but found: " + char_);
    }

    return char_;
  }
}
