package io.github.davidms.biwenger.app.dashboard.domain;

import io.github.davidms.biwenger.app.league.domain.ScheduledRound;
import java.time.Instant;
import java.util.Comparator;
import java.util.List;
import java.util.Objects;
import java.util.Optional;

/** Your team at a glance, and the rounds whose start is known. */
public record Dashboard(TeamStatus team, List<ScheduledRound> rounds) {

  public Dashboard {
    Objects.requireNonNull(team, "team");
    rounds = List.copyOf(rounds);
  }

  /**
   * The first round that starts after {@code now}; ties by id, so the answer is always the same.
   */
  public Optional<ScheduledRound> nextRound(Instant now) {
    return nextRound(rounds, now);
  }

  /** The first of {@code rounds} that starts after {@code now}; ties by id. */
  public static Optional<ScheduledRound> nextRound(List<ScheduledRound> rounds, Instant now) {
    return rounds.stream()
        .filter(round -> round.startsAt().isAfter(now))
        .min(Comparator.comparing(ScheduledRound::startsAt).thenComparingLong(ScheduledRound::id));
  }
}
