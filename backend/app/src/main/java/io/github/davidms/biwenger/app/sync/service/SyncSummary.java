package io.github.davidms.biwenger.app.sync.service;

import java.time.Duration;
import java.time.LocalDate;

/** What one sync stored, and how long it took. */
public record SyncSummary(
    LocalDate day, int players, int marketListings, int standings, Duration duration) {

  @Override
  public String toString() {
    return "day "
        + day
        + ": "
        + players
        + " players, "
        + marketListings
        + " market listings, "
        + standings
        + " standings in "
        + duration.toMillis()
        + " ms";
  }
}
