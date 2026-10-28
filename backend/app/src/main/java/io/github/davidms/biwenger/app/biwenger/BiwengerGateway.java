package io.github.davidms.biwenger.app.biwenger;

import io.github.davidms.biwenger.app.account.domain.ConnectedAccount;
import io.github.davidms.biwenger.app.dashboard.domain.TeamStatus;
import io.github.davidms.biwenger.app.league.domain.ScheduledRound;
import io.github.davidms.biwenger.app.market.domain.MarketListing;
import io.github.davidms.biwenger.app.player.domain.MatchResult;
import io.github.davidms.biwenger.client.BiwengerClient;
import java.util.Comparator;
import java.util.List;
import org.springframework.stereotype.Component;

/**
 * Reads from Biwenger and returns domain types. With {@link BiwengerMapper}, the only code in the
 * app that sees the client (backend/CLAUDE.md). It only reads: writes go through Phase 6's propose
 * → confirm flow.
 */
@Component
public class BiwengerGateway {

  private final BiwengerClient client;

  BiwengerGateway(BiwengerClient client) {
    this.client = client;
  }

  /**
   * The account, your team in the La Liga league, and that league. Two account calls on a cold
   * start (the client reads it once to find the league), one after that; callers cache it.
   */
  public ConnectedAccount connectedAccount() {
    var league = client.leagueContext();
    return BiwengerMapper.connectedAccount(client.account(), league.leagueId());
  }

  /**
   * Your balance and place in the league: two calls (account, standings), three on a cold start.
   */
  public TeamStatus teamStatus() {
    var league = client.leagueContext();
    return BiwengerMapper.teamStatus(
        client.account(), client.roundStandings(), league.leagueId(), league.userId());
  }

  /** The ids of your players, in order: one small call. */
  public List<Long> squadPlayerIds() {
    return BiwengerMapper.squadPlayerIds(client.squad());
  }

  /** Today's market, by player id: one small call. */
  public List<MarketListing> marketListings() {
    return BiwengerMapper.listings(client.market()).stream()
        .sorted(Comparator.comparingLong(MarketListing::playerId))
        .toList();
  }

  /** The active and next rounds' starts: one catalog call, the heaviest there is. */
  public List<ScheduledRound> scheduledRounds() {
    return BiwengerMapper.scheduledRounds(client.catalog());
  }

  /** The season, the league's scoring system and every catalog player: what the backfill does. */
  public BackfillPlan backfillPlan() {
    int scoreId = client.leagueContext().scoreId();
    var catalog = client.catalog();
    return new BackfillPlan(
        catalog.season().id(),
        scoreId,
        BiwengerMapper.players(catalog).stream()
            .map(player -> new BackfillPlan.PlayerRef(player.id(), player.slug()))
            .toList());
  }

  /** One player's matches this season, points in {@code scoreId}: one call to the public CDN. */
  public List<MatchResult> matchResults(String slug, int scoreId) {
    return BiwengerMapper.matches(client.playerReports(slug), scoreId);
  }

  /** One player's price history and match reports: two calls to the public CDN. */
  public PlayerHistory playerHistory(String slug, int scoreId) {
    return new PlayerHistory(
        BiwengerMapper.prices(client.playerPrices(slug)),
        BiwengerMapper.matches(client.playerReports(slug), scoreId));
  }

  /** Four calls (account, catalog, market, standings), one after another, rate-limited. */
  public DailyData dailyData() {
    var league = client.leagueContext();
    var catalog = client.catalog();
    return new DailyData(
        league.leagueId(),
        league.scoreId(),
        BiwengerMapper.players(catalog),
        BiwengerMapper.teams(catalog),
        BiwengerMapper.rounds(catalog),
        BiwengerMapper.listings(client.market()),
        BiwengerMapper.standings(client.roundStandings()));
  }
}
