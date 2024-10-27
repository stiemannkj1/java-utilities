package dev.stiemannkj1.util;

import static dev.stiemannkj1.util.Assert.assertPositive;
import static dev.stiemannkj1.util.Assert.assertTrue;

import java.io.File;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;

public final class StringUtils {

  private static final int BITS_PER_HEX_CHAR = 4;
  private static final char[] HEX_CHARS =
      new char[] {'0', '1', '2', '3', '4', '5', '6', '7', '8', '9', 'A', 'B', 'C', 'D', 'E', 'F'};

  public static void appendHexString(
      final StringBuilder stringBuilder, final long i8, final int bytes) {

    assertPositive(bytes, "bytes");
    assertTrue(bytes <= Long.BYTES, () -> "bytes must be less than or equal to " + Long.BYTES);

    for (int i = (bytes * Byte.SIZE) - BITS_PER_HEX_CHAR; i >= 0; i -= BITS_PER_HEX_CHAR) {
      stringBuilder.append(HEX_CHARS[(int) ((i8 >> i) & 0x0F)]);
    }
  }

  public static boolean isAsciiPrintable(final char char_) {
    return '\u0020' <= char_ && char_ <= '\u007e';
  }

  /**
   * Checks if a string begins with a particular prefix. Always returns the same result as {@link
   * String#startsWith(String)} (<strong>NOT</strong> {@link String#startsWith(String, int)}).
   *
   * <p>The provided offset is only used for optimization. For example, you may want to check if a
   * string starts with "META-INF/services/". If you know that "META-INF/" is an extremely common
   * prefix and "services/" is uncommon at offset 8 unless preceded by "META-INF/", the following
   * checks will be faster:
   *
   * <pre>{@code
   * string.startsWith("services/", "META-INF/".length()) && string.startsWith("META-INF/")
   * }</pre>
   *
   * <p>The above code is equivalent to calling this method with:
   *
   * <pre>{@code
   * startsWith(string, "META-INF/services/", "META-INF/".length());
   * }</pre>
   *
   * @param string the string to search.
   * @param prefix the prefix to search for. <strong>NOTE:</strong> the <strong>entire</strong>
   *     prefix must match for this method to return true.
   * @param offsetForOptimization the prefix offset to start searching at. The offset is only used
   *     to optimize the checks and does <strong>NOT</strong> limit matching to subsequences of the
   *     prefix.
   * @return true if the string begins with the prefix string (regardless of the offset value).
   */
  public static boolean startsWith(
      final CharSequence string, final CharSequence prefix, final int offsetForOptimization) {

    Require.notNull(string, "string");
    final int length = Require.notNull(prefix, "prefix").length();

    if (length == 0) {
      return true;
    }

    if (length > string.length()) {
      return false;
    }

    if (offsetForOptimization < 0 || length <= offsetForOptimization) {
      throw new IllegalArgumentException(
          "Offset must be a valid index into the prefix string. Expected offset "
              + offsetForOptimization
              + " to be less than "
              + length);
    }

    final int max = length - 1;

    for (int i = 0; i <= max; i++) {

      final int index =
          (offsetForOptimization + i)
              // Wrap around once the max value is reached.
              & max;

      if (string.charAt(index) != prefix.charAt(index)) {
        return false;
      }
    }

    return true;
  }

  // TODO minimize allocations here. No need to allocate the byte array at all.
  public static String readUtf8String(final File file) throws IOException {
    return new String(Files.readAllBytes(file.toPath()), StandardCharsets.UTF_8);
  }

  private StringUtils() {}
}
