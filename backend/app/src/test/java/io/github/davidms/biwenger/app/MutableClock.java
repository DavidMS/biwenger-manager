package io.github.davidms.biwenger.app;

import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.time.ZoneId;

/** A clock tests can set and move, in the app's zone. */
public final class MutableClock extends Clock {

  private Instant now;
  private final ZoneId zone;

  public MutableClock(Instant now, ZoneId zone) {
    this.now = now;
    this.zone = zone;
  }

  public synchronized void set(Instant instant) {
    now = instant;
  }

  public synchronized void advance(Duration duration) {
    now = now.plus(duration);
  }

  @Override
  public synchronized Instant instant() {
    return now;
  }

  @Override
  public ZoneId getZone() {
    return zone;
  }

  @Override
  public Clock withZone(ZoneId other) {
    return new MutableClock(instant(), other);
  }
}
