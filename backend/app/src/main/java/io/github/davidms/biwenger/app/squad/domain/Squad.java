package io.github.davidms.biwenger.app.squad.domain;

import java.util.List;

/**
 * Your players, by id. {@code missingPlayerIds} are players Biwenger says you own that the daily
 * sync has never stored: players who left La Liga before it first ran.
 */
public record Squad(List<SquadPlayer> players, List<Long> missingPlayerIds) {

  /** Days of value shown as the trend, today included. */
  public static final int TREND_DAYS = 14;

  /** Matches shown as recent form. */
  public static final int FORM_MATCHES = 5;

  public Squad {
    players = List.copyOf(players);
    missingPlayerIds = List.copyOf(missingPlayerIds);
  }
}
