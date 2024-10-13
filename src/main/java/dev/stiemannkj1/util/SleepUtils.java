package dev.stiemannkj1.util;

import java.util.concurrent.TimeUnit;
import java.util.function.BooleanSupplier;

public final class SleepUtils {

  public static void busyWaitUnchecked(final BooleanSupplier predicate, final long timeoutMs) {
    busyWait(true, predicate, timeoutMs);
  }

  @SuppressWarnings("RedundantThrows")
  public static void busyWait(final BooleanSupplier predicate, final long timeoutMs)
      throws InterruptedException {
    busyWait(false, predicate, timeoutMs);
  }

  private static void busyWait(
      final boolean interruptionIsAnError, final BooleanSupplier predicate, final long timeoutMs) {

    final long startNs = System.nanoTime();
    final long timeoutNs = TimeUnit.MILLISECONDS.toNanos(timeoutMs);

    while (predicate.getAsBoolean() && ((System.nanoTime() - startNs) < timeoutNs)) {

      if (Thread.interrupted()) {
        if (interruptionIsAnError) {
          Thread.currentThread().interrupt();
          throw new InterruptedError("Busy waiting interrupted.");
        } else {
          throw sneaky(new InterruptedException("Busy waiting interrupted."));
        }
      }

      // Busy wait.
    }
  }

  public static <E extends Throwable> E sneaky(final Throwable e) throws E {
    @SuppressWarnings("unchecked")
    final E sneaky = (E) e;
    throw sneaky;
  }

  public static void sleepUnchecked(final long timeoutMs) {
    try {
      Thread.sleep(timeoutMs);
    } catch (final InterruptedException e) {
      Thread.currentThread().interrupt();
      throw new InterruptedError(e);
    }
  }

  private static final class InterruptedError extends Error {
    private InterruptedError(final String message) {
      super(message);
    }

    private InterruptedError(final InterruptedException e) {
      super(e);
    }
  }

  private SleepUtils() {}
}
