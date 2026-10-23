package io.github.davidms.biwenger.app.biwenger;

import io.github.davidms.biwenger.client.BiwengerClient;
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

  /** Four calls (account, catalog, market, standings), one after another, rate-limited. */
  public DailyData dailyData() {
    long leagueId = client.leagueContext().leagueId();
    return new DailyData(
        leagueId,
        BiwengerMapper.players(client.catalog()),
        BiwengerMapper.listings(client.market()),
        BiwengerMapper.standings(client.roundStandings()));
  }
}
