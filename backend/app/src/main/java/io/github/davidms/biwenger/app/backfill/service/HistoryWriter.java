package io.github.davidms.biwenger.app.backfill.service;

import io.github.davidms.biwenger.app.biwenger.PlayerHistory;
import io.github.davidms.biwenger.app.player.service.MatchReports;
import java.sql.Date;
import java.sql.Timestamp;
import java.time.Instant;
import java.time.LocalDate;
import java.util.HashSet;
import java.util.Set;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Stores one player's history and marks him done, in one transaction, so an interrupted backfill
 * resumes after the last finished player. Writes in batches with plain SQL: about 366 prices per
 * player, where JPA's {@code save()} would first {@code SELECT} each row (ADR 0005).
 */
@Service
class HistoryWriter {

  // A day the daily sync recorded is never overwritten: it knows the status and points (ADR 0005).
  private static final String PRICE =
      """
      INSERT INTO player_price_snapshot (player_id, snapshot_date, price, source, synced_at)
      VALUES (?, ?, ?, 'BACKFILL', ?)
      ON CONFLICT (player_id, snapshot_date) DO NOTHING
      """;

  private static final String DONE =
      """
      INSERT INTO backfill_progress (season, player_id, prices, matches, completed_at)
      VALUES (?, ?, ?, ?, ?)
      ON CONFLICT (season, player_id) DO UPDATE SET
        prices = EXCLUDED.prices, matches = EXCLUDED.matches, completed_at = EXCLUDED.completed_at
      """;

  private final JdbcTemplate jdbc;
  private final MatchReports reports;

  HistoryWriter(JdbcTemplate jdbc, MatchReports reports) {
    this.jdbc = jdbc;
    this.reports = reports;
  }

  /** The players already done this season. */
  Set<Long> completedPlayers(String season) {
    return new HashSet<>(
        jdbc.queryForList(
            "SELECT player_id FROM backfill_progress WHERE season = ?", Long.class, season));
  }

  /**
   * Stores the history before {@code today}. Today's price belongs to the daily sync: writing it
   * here could collide with a sync running at the same moment and roll back its whole day.
   */
  @Transactional
  public void write(
      String season,
      long playerId,
      int scoreId,
      PlayerHistory history,
      LocalDate today,
      Instant syncedAt) {
    Timestamp synced = Timestamp.from(syncedAt);
    var pastPrices =
        history.prices().stream().filter(price -> price.day().isBefore(today)).toList();
    jdbc.batchUpdate(
        PRICE,
        pastPrices.stream()
            .map(
                price ->
                    new Object[] {
                      playerId, Date.valueOf(price.day()), price.price().euros(), synced
                    })
            .toList());
    reports.upsert(playerId, scoreId, history.matches(), syncedAt);
    jdbc.update(DONE, season, playerId, pastPrices.size(), history.matches().size(), synced);
  }
}
