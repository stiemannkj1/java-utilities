package dev.stiemannkj1.util;

public final class Hex {

  private static final int BITS_PER_HEX_CHAR = 4;
  private static final char[] HEX_CHARS =
      new char[] {'0', '1', '2', '3', '4', '5', '6', '7', '8', '9', 'A', 'B', 'C', 'D', 'E', 'F'};

  public static void appendHexString(final StringBuilder stringBuilder, final int i4) {

    stringBuilder.append("0x");

    for (int i = Integer.SIZE - BITS_PER_HEX_CHAR; i >= 0; i -= BITS_PER_HEX_CHAR) {
      stringBuilder.append(HEX_CHARS[(i4 >> i) & 0x0F]);
    }
  }

  private Hex() {}
}
