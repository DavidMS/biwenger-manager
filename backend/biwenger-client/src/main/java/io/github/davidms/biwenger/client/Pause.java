package io.github.davidms.biwenger.client;

import java.io.IOException;
import java.io.InterruptedIOException;
import java.time.Duration;

/** Waits. Tests replace it so they can check the waits without waiting. */
@FunctionalInterface
interface Pause {

  Pause SLEEP = duration -> Thread.sleep(duration);

  void pause(Duration duration) throws InterruptedException;

  /** Pauses, turning an interrupt into an {@link IOException} that the HTTP layer reports. */
  default void pauseOrFail(Duration duration) throws IOException {
    if (duration.isZero() || duration.isNegative()) {
      return;
    }
    try {
      pause(duration);
    } catch (InterruptedException e) {
      Thread.currentThread().interrupt();
      throw new InterruptedIOException("Interrupted while waiting before a Biwenger request");
    }
  }
}
