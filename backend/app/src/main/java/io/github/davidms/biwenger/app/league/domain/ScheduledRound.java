package io.github.davidms.biwenger.app.league.domain;

import java.time.Instant;
import java.util.Objects;

/** A round with a known start: its first match kicks off at {@code startsAt}. */
public record ScheduledRound(long id, String name, Instant startsAt) {

  public ScheduledRound {
    Objects.requireNonNull(name, "name");
    Objects.requireNonNull(startsAt, "startsAt");
  }
}
