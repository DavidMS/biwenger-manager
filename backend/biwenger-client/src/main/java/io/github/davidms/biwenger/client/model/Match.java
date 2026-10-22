package io.github.davidms.biwenger.client.model;

import java.util.Objects;

/** A club match: when, the two teams with their score, and the round it belongs to. */
public record Match(
    long id, long date, String status, MatchTeam home, MatchTeam away, MatchRound round) {

  public Match {
    Objects.requireNonNull(status, "status");
    Objects.requireNonNull(home, "home");
    Objects.requireNonNull(away, "away");
    Objects.requireNonNull(round, "round");
  }
}
