package io.github.davidms.biwenger.app.player.persistence;

import io.github.davidms.biwenger.app.player.domain.MatchResult;
import java.sql.Timestamp;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;

/**
 * Writes a player's match reports in one batch: used by the backfill (#24) and by the refresh after
 * each round (#78). A newer answer replaces an older one, so running it twice changes nothing.
 */
@Repository
public class MatchReportWriter {

  private static final String MATCH =
      """
      INSERT INTO player_points_report (player_id, match_id, round_id, match_date, home, played,
        score_id, points, minutes_played, absence, synced_at)
      VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?)
      ON CONFLICT (player_id, match_id) DO UPDATE SET
        round_id = EXCLUDED.round_id, match_date = EXCLUDED.match_date, home = EXCLUDED.home,
        played = EXCLUDED.played, score_id = EXCLUDED.score_id, points = EXCLUDED.points,
        minutes_played = EXCLUDED.minutes_played, absence = EXCLUDED.absence,
        synced_at = EXCLUDED.synced_at
      """;

  private final JdbcTemplate jdbc;

  MatchReportWriter(JdbcTemplate jdbc) {
    this.jdbc = jdbc;
  }

  /** Points are in {@code scoreId}, the league's scoring system (ADR 0005). */
  @Transactional
  public void upsert(long playerId, int scoreId, List<MatchResult> matches, Instant syncedAt) {
    Timestamp synced = Timestamp.from(syncedAt);
    jdbc.batchUpdate(MATCH, matches.stream().map(m -> row(playerId, scoreId, m, synced)).toList());
  }

  private static Object[] row(long playerId, int scoreId, MatchResult match, Timestamp synced) {
    List<Object> row =
        new ArrayList<>(
            List.of(
                playerId,
                match.matchId(),
                match.roundId(),
                Timestamp.from(match.date()),
                match.home()));
    switch (match.outcome()) {
      case MatchResult.Played(int points, int minutes) -> {
        row.add(true);
        row.add(scoreId);
        row.add(points);
        row.add(minutes);
        row.add(null);
      }
      case MatchResult.Missed(var reason) -> {
        row.add(false);
        row.add(scoreId);
        row.add(null);
        row.add(0);
        row.add(reason.name());
      }
    }
    row.add(synced);
    return row.toArray();
  }
}
