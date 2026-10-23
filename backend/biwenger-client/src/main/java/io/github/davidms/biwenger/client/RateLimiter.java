package io.github.davidms.biwenger.client;

import java.io.IOException;
import java.time.Duration;
import java.util.function.LongSupplier;

/**
 * Spaces requests: each one starts at least {@code interval} after the previous one started. Shared
 * by every request the client sends, to the API and the CDN.
 */
final class RateLimiter {

  private final long intervalNanos;
  private final LongSupplier nanoTime;
  private final Pause pause;
  private long nextStart = Long.MIN_VALUE;

  RateLimiter(Duration interval, LongSupplier nanoTime, Pause pause) {
    this.intervalNanos = interval.toNanos();
    this.nanoTime = nanoTime;
    this.pause = pause;
  }

  /** Waits for this request's turn. Threads queue in the order they arrive. */
  void acquire() throws IOException {
    long wait;
    synchronized (this) {
      long now = nanoTime.getAsLong();
      long start = nextStart == Long.MIN_VALUE ? now : Math.max(now, nextStart);
      nextStart = start + intervalNanos;
      wait = start - now;
    }
    pause.pauseOrFail(Duration.ofNanos(wait));
  }
}
