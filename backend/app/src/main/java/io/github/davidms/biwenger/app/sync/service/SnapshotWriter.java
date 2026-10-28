package io.github.davidms.biwenger.app.sync.service;

import io.github.davidms.biwenger.app.biwenger.DailyData;
import io.github.davidms.biwenger.app.league.persistence.LeagueStandingSnapshotEntity;
import io.github.davidms.biwenger.app.league.persistence.LeagueStandingSnapshotRepository;
import io.github.davidms.biwenger.app.market.persistence.MarketSnapshotEntity;
import io.github.davidms.biwenger.app.market.persistence.MarketSnapshotRepository;
import io.github.davidms.biwenger.app.player.persistence.PlayerEntity;
import io.github.davidms.biwenger.app.player.persistence.PlayerPriceSnapshotEntity;
import io.github.davidms.biwenger.app.player.persistence.PlayerPriceSnapshotRepository;
import io.github.davidms.biwenger.app.player.persistence.PlayerRepository;
import io.github.davidms.biwenger.app.player.persistence.TeamEntity;
import io.github.davidms.biwenger.app.player.persistence.TeamRepository;
import java.time.Instant;
import java.time.LocalDate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Stores one day of data in a single transaction: all of it or none of it. A second run on the same
 * day replaces that day's rows (ADR 0005), so the sync can be repeated safely.
 */
@Service
class SnapshotWriter {

  private final TeamRepository teams;
  private final PlayerRepository players;
  private final PlayerPriceSnapshotRepository prices;
  private final MarketSnapshotRepository market;
  private final LeagueStandingSnapshotRepository standings;

  SnapshotWriter(
      TeamRepository teams,
      PlayerRepository players,
      PlayerPriceSnapshotRepository prices,
      MarketSnapshotRepository market,
      LeagueStandingSnapshotRepository standings) {
    this.teams = teams;
    this.players = players;
    this.prices = prices;
    this.market = market;
    this.standings = standings;
  }

  @Transactional
  public void write(LocalDate day, Instant syncedAt, DailyData data) {
    for (var team : data.teams()) {
      teams.save(TeamEntity.from(team, syncedAt));
    }
    for (var player : data.players()) {
      players.save(PlayerEntity.from(player, syncedAt));
      prices.save(PlayerPriceSnapshotEntity.daily(player, day, syncedAt));
    }
    // The market and the table of a day are what Biwenger shows now: a listing or a manager gone
    // since the first run today is no longer in them. Players stay (their history is kept).
    market.deleteAll(market.findByIdSnapshotDateOrderByIdPlayerIdAsc(day));
    market.flush();
    for (var listing : data.market()) {
      market.save(MarketSnapshotEntity.from(day, listing, syncedAt));
    }
    standings.deleteAll(standings.findByIdSnapshotDateOrderByPositionAsc(day));
    standings.flush();
    for (var standing : data.standings()) {
      standings.save(LeagueStandingSnapshotEntity.from(day, data.leagueId(), standing, syncedAt));
    }
  }
}
