package io.github.davidms.biwenger.app.player.domain;

import java.time.Instant;
import java.util.Objects;

/** A player's part in one club match: played with points and minutes, or missed with a reason. */
public record MatchResult(long matchId, long roundId, Instant date, boolean home, Outcome outcome) {

  public MatchResult {
    Objects.requireNonNull(date, "date");
    Objects.requireNonNull(outcome, "outcome");
  }

  public sealed interface Outcome {}

  /** Points in the league's scoring system, and minutes on the pitch. */
  public record Played(int points, int minutes) implements Outcome {

    public Played {
      if (minutes < 0) {
        throw new IllegalArgumentException("Minutes can't be negative: " + minutes);
      }
    }
  }

  /** Why he didn't play. {@code AVAILABLE} means fit but not picked. */
  public record Missed(PlayerStatus reason) implements Outcome {

    public Missed {
      Objects.requireNonNull(reason, "reason");
    }
  }
}
