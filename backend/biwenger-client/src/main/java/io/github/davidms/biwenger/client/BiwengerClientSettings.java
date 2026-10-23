package io.github.davidms.biwenger.client;

import java.time.Duration;
import java.util.Objects;

/**
 * How gently the client treats Biwenger. Every HTTP request waits until at least {@code
 * minRequestInterval} has passed since the previous one started. A {@code GET} that fails with a
 * 5xx or a network error is tried up to {@code maxAttempts} times in total, waiting {@code
 * initialBackoff}, then twice as long each time, up to {@code maxBackoff}. Nothing else is retried.
 */
public record BiwengerClientSettings(
    Duration minRequestInterval, int maxAttempts, Duration initialBackoff, Duration maxBackoff) {

  /** Half a second between requests; 3 attempts, waiting 1 s and then 2 s. */
  public static final BiwengerClientSettings DEFAULTS =
      new BiwengerClientSettings(
          Duration.ofMillis(500), 3, Duration.ofSeconds(1), Duration.ofSeconds(10));

  public BiwengerClientSettings {
    Objects.requireNonNull(minRequestInterval, "minRequestInterval");
    Objects.requireNonNull(initialBackoff, "initialBackoff");
    Objects.requireNonNull(maxBackoff, "maxBackoff");
    if (minRequestInterval.isNegative() || initialBackoff.isNegative() || maxBackoff.isNegative()) {
      throw new IllegalArgumentException("Durations must not be negative");
    }
    if (maxAttempts < 1) {
      throw new IllegalArgumentException("maxAttempts must be at least 1, but is " + maxAttempts);
    }
  }

  /** The wait before attempt {@code attempt + 1}, after {@code attempt} failed attempts. */
  Duration backoffAfter(int attempt) {
    Duration wait = initialBackoff;
    for (int i = 1; i < attempt && wait.compareTo(maxBackoff) < 0; i++) {
      wait = wait.multipliedBy(2);
    }
    return wait.compareTo(maxBackoff) > 0 ? maxBackoff : wait;
  }
}
