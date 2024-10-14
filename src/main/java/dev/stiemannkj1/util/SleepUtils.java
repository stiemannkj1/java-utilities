package dev.stiemannkj1.util;

import java.util.concurrent.TimeUnit;
import java.util.function.BooleanSupplier;

public final class SleepUtils {

  public static void busyWaitUnchecked(final BooleanSupplier predicate, final long timeoutMs) {
    try {
      busyWait(predicate, timeoutMs);
    } catch (final InterruptedException e) {
      throw new InterruptedError("Busy waiting interrupted.", e);
    }
  }

  public static void busyWait(final BooleanSupplier predicate, final long timeoutMs)
      throws InterruptedException {

    final long startNs = System.nanoTime();
    final long timeoutNs = TimeUnit.MILLISECONDS.toNanos(timeoutMs);

    while (predicate.getAsBoolean() && ((System.nanoTime() - startNs) < timeoutNs)) {

      if (Thread.interrupted()) {
        throw new InterruptedException("Busy waiting interrupted.");
      }

      // Busy wait.
    }
  }

  public static void sleepUnchecked(final long timeoutMs) {
    try {
      sleep(timeoutMs);
    } catch (final InterruptedException e) {
      throw new InterruptedError(e);
    }
  }

  public static void sleep(final long timeoutMs) throws InterruptedException {
    Thread.sleep(timeoutMs);
  }

  /**
   * Some threads or tasks should never be interrupted, so interruption should be an unchecked
   * {@link Error}.
   */
  public static final class InterruptedError extends Error {
    public InterruptedError(final InterruptedException cause) {
      super(cause);
      Thread.currentThread().interrupt();
    }

    public InterruptedError(final String message, final InterruptedException cause) {
      super(message, cause);
      Thread.currentThread().interrupt();
    }
  }

  private SleepUtils() {}
}
