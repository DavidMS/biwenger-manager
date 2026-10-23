package io.github.davidms.biwenger.app.backfill.service;

import java.time.Duration;

/** What one backfill run did. {@code remaining} is what a later run will still do. */
public record BackfillSummary(
    String season, int completed, int alreadyDone, int failed, int remaining, Duration duration) {

  public boolean isFinished() {
    return remaining == 0;
  }

  @Override
  public String toString() {
    return "season "
        + season
        + ": "
        + completed
        + " players done now, "
        + alreadyDone
        + " already done, "
        + failed
        + " failed, "
        + remaining
        + " remaining, in "
        + duration.toSeconds()
        + " s";
  }
}
