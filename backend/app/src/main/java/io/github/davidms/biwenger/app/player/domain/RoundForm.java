package io.github.davidms.biwenger.app.player.domain;

import java.util.Objects;

/**
 * What a player did in one recent round: scored points, missed it for a reason, or wasn't picked.
 */
public sealed interface RoundForm {

  /** Played and scored these points (can be negative). */
  record Played(int points) implements RoundForm {}

  /** Didn't play because he was injured, doubtful or suspended. */
  record Absent(PlayerStatus reason) implements RoundForm {

    public Absent {
      Objects.requireNonNull(reason, "reason");
    }
  }

  /** Fit, but didn't play. */
  record NotPicked() implements RoundForm {}
}
