package io.github.davidms.biwenger.client.model;

import java.util.Objects;

/** {@code GET /api/v2/rounds/league}: the current round's id and the league standings. */
public record RoundStandings(RoundRef round, StandingsLeague league) {

  public RoundStandings {
    Objects.requireNonNull(round, "round");
    Objects.requireNonNull(league, "league");
  }
}
