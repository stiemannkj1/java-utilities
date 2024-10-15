package dev.stiemannkj1.util;

public final class NumberUtils {

  public static boolean isPowerOf2(final int number) {
    // https://graphics.stanford.edu/~seander/bithacks.html#DetermineIfPowerOf2
    return number != 0 && (number & (number - 1)) == 0;
  }

  private NumberUtils() {}
}
