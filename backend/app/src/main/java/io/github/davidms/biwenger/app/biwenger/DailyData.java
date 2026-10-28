package io.github.davidms.biwenger.app.biwenger;

import io.github.davidms.biwenger.app.league.domain.Round;
import io.github.davidms.biwenger.app.league.domain.Standing;
import io.github.davidms.biwenger.app.market.domain.MarketListing;
import io.github.davidms.biwenger.app.player.domain.Player;
import io.github.davidms.biwenger.app.player.domain.Team;
import java.util.List;

/**
 * What the daily sync reads from Biwenger: the catalog (players, clubs and the season's rounds),
 * today's market and the league table, with the league's scoring system.
 */
public record DailyData(
    long leagueId,
    int scoreId,
    List<Player> players,
    List<Team> teams,
    List<Round> rounds,
    List<MarketListing> market,
    List<Standing> standings) {

  public DailyData {
    players = List.copyOf(players);
    teams = List.copyOf(teams);
    rounds = List.copyOf(rounds);
    market = List.copyOf(market);
    standings = List.copyOf(standings);
  }
}
