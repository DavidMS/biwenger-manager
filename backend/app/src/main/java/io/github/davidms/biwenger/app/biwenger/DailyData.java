package io.github.davidms.biwenger.app.biwenger;

import io.github.davidms.biwenger.app.league.domain.Standing;
import io.github.davidms.biwenger.app.market.domain.MarketListing;
import io.github.davidms.biwenger.app.player.domain.Player;
import java.util.List;

/** What the daily sync reads from Biwenger: the catalog, today's market and the league table. */
public record DailyData(
    long leagueId, List<Player> players, List<MarketListing> market, List<Standing> standings) {

  public DailyData {
    players = List.copyOf(players);
    market = List.copyOf(market);
    standings = List.copyOf(standings);
  }
}
