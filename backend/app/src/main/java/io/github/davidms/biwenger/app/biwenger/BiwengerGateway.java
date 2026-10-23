package io.github.davidms.biwenger.app.biwenger;

import io.github.davidms.biwenger.app.player.domain.MatchResult;
import io.github.davidms.biwenger.client.BiwengerClient;
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
        BiwengerMapper.rounds(catalog),
        BiwengerMapper.listings(client.market()),
        BiwengerMapper.standings(client.roundStandings()));
  }
}
