package io.github.davidms.biwenger.app.market.domain;

import io.github.davidms.biwenger.app.player.domain.Team;
import java.util.List;

/**
 * Today's market by player id, and every club by name for the team filter. {@code missingPlayerIds}
 * are players on sale that the daily sync has never stored (new to the catalog since it last ran).
 */
public record Market(List<MarketPlayer> players, List<Long> missingPlayerIds, List<Team> teams) {

  /** Days of value shown as the trend, today included: the same window as the squad's. */
  public static final int TREND_DAYS = 14;

  public Market {
    players = List.copyOf(players);
    missingPlayerIds = List.copyOf(missingPlayerIds);
    teams = List.copyOf(teams);
  }
}
