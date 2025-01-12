package dev.stiemannkj1.util;

import java.util.concurrent.TimeUnit;
import java.util.function.BooleanSupplier;

public final class SleepUtils {

  public static boolean uncheckedBusyWaitUntil(
      final BooleanSupplier predicate, final long timeoutMs) {
    try {
      return busyWaitUntil(predicate, timeoutMs);
    } catch (final InterruptedException e) {
      throw new NoStackTraceInterruptedError("Busy waiting interrupted.", e);
    }
  }

  @SuppressWarnings("BusyWait")
  public static boolean busyWaitUntil(final BooleanSupplier predicate, final long timeoutMs)
      throws InterruptedException {

    final long sleepMs = timeoutMs >= 500 ? 50 : Math.min(timeoutMs, 10);
    final long startNs = System.nanoTime();
    final long timeoutNs = TimeUnit.MILLISECONDS.toNanos(timeoutMs);

    boolean result;

    while ((result = predicate.getAsBoolean()) && ((System.nanoTime() - startNs) < timeoutNs)) {

      if (Thread.interrupted()) {
        throw new NoStackTraceInterruptedException("Busy waiting interrupted.");
      }

      // Busy wait.
      Thread.sleep(sleepMs);
    }

    return result;
  }

  public static void uncheckedSleep(final long timeoutMs) {
    try {
      sleep(timeoutMs);
    } catch (final InterruptedException e) {
      throw new NoStackTraceInterruptedError(e);
    }
  }

  public static void sleep(final long timeoutMs) throws InterruptedException {
    Thread.sleep(timeoutMs);
  }

  /**
   * Some threads or tasks should never be interrupted, so interruption should be an unchecked
   * {@link Error}.
   */
  private static final class NoStackTraceInterruptedError extends Error {
    private NoStackTraceInterruptedError(final InterruptedException cause) {
      super(cause);
      Thread.currentThread().interrupt();
    }

    private NoStackTraceInterruptedError(final String message, final InterruptedException cause) {
      super(message, cause);
      Thread.currentThread().interrupt();
    }

    @Override
    public synchronized Throwable fillInStackTrace() {
      // Avoid costly stack trace generation.
      return this;
    }
  }

  private static final class NoStackTraceInterruptedException extends InterruptedException {
    private NoStackTraceInterruptedException(final String message) {
      super(message);
    }

    @Override
    public synchronized Throwable fillInStackTrace() {
      // Avoid costly stack trace generation.
      return this;
    }
  }

  private SleepUtils() {}
}
