package io.github.davidms.biwenger.app.dashboard.domain;

import io.github.davidms.biwenger.app.common.Money;
import io.github.davidms.biwenger.app.league.domain.ScheduledRound;
import java.time.Instant;
import java.util.Comparator;
import java.util.List;
import java.util.Objects;
import java.util.Optional;

/**
 * Your team at a glance: cash, season points, place in a league of {@code managers}, team value and
 * its change since Biwenger's last update, and the rounds whose start is known.
 */
public record Dashboard(
    Money balance,
    int points,
    int position,
    int managers,
    Money teamValue,
    Money teamValueChange,
    List<ScheduledRound> rounds) {

  public Dashboard {
    Objects.requireNonNull(balance, "balance");
    Objects.requireNonNull(teamValue, "teamValue");
    Objects.requireNonNull(teamValueChange, "teamValueChange");
    rounds = List.copyOf(rounds);
    if (position < 1 || position > managers) {
      throw new IllegalArgumentException(
          "Position " + position + " is outside a league of " + managers + " managers");
    }
  }

  /** A negative balance when a round starts scores 0 points that round. */
  public boolean balanceNegative() {
    return balance.isNegative();
  }

  /**
   * The first round that starts after {@code now}; ties by id, so the answer is always the same.
   */
  public Optional<ScheduledRound> nextRound(Instant now) {
    return rounds.stream()
        .filter(round -> round.startsAt().isAfter(now))
        .min(Comparator.comparing(ScheduledRound::startsAt).thenComparingLong(ScheduledRound::id));
  }
}
