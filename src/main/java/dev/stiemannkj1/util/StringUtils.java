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

  // TODO minimize allocations here. No need to allocate the byte array at all.
  public static String readUtf8String(final File file) throws IOException {
    return new String(Files.readAllBytes(file.toPath()), StandardCharsets.UTF_8);
  }

  private StringUtils() {}
}
