package io.github.davidms.biwenger.app.sync.service;

import io.github.davidms.biwenger.app.backfill.service.BackfillService;
import io.github.davidms.biwenger.app.biwenger.BiwengerGateway;
import io.github.davidms.biwenger.app.biwenger.DailyData;
import io.github.davidms.biwenger.app.league.domain.Round;
import io.github.davidms.biwenger.app.league.domain.RoundStatus;
import io.github.davidms.biwenger.app.player.persistence.MatchReportWriter;
import io.github.davidms.biwenger.client.BiwengerApiException;
import java.lang.System.Logger.Level;
import java.sql.Timestamp;
import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.util.HashSet;
import java.util.List;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;

/**
 * Keeps match points current (#78). Part of the daily sync, so it never overlaps one: when the
 * catalog shows a finished round that hasn't been refreshed, it fetches every player's reports (one
 * CDN call each, which returns the whole season) and records the rounds as refreshed. It waits for
 * a running backfill by skipping that day, and leaves the rounds pending if any player failed, so
 * the next sync tries again. It never throws: the day's snapshot is already stored.
 */
@Service
class PointsRefresher {

  static final int MAX_FAILURES_IN_A_ROW = 3;
  private static final System.Logger LOG = System.getLogger(PointsRefresher.class.getName());

  private final BiwengerGateway biwenger;
  private final MatchReportWriter reports;
  private final BackfillService backfill;
  private final JdbcTemplate jdbc;
  private final Clock clock;

  PointsRefresher(
      BiwengerGateway biwenger,
      MatchReportWriter reports,
      BackfillService backfill,
      JdbcTemplate jdbc,
      Clock clock) {
    this.biwenger = biwenger;
    this.reports = reports;
    this.backfill = backfill;
    this.jdbc = jdbc;
    this.clock = clock;
  }

  void refreshIfRoundsFinished(DailyData data) {
    try {
      refresh(data);
    } catch (RuntimeException e) {
      LOG.log(Level.ERROR, "Points refresh failed; the next sync tries again", e);
    }
  }

  private void refresh(DailyData data) {
    List<Long> pending = pendingRounds(data.rounds());
    if (pending.isEmpty()) {
      return;
    }
    if (backfill.isRunning()) {
      LOG.log(Level.INFO, "Points refresh waits: the backfill is running. Next sync tries again");
      return;
    }
    Instant started = clock.instant();
    int done = 0;
    int failed = 0;
    int failuresInARow = 0;
    for (var player : data.players()) {
      if (failuresInARow == MAX_FAILURES_IN_A_ROW) {
        LOG.log(Level.WARNING, "Points refresh stops after {0} failures in a row", failuresInARow);
        break;
      }
      try {
        var matches = biwenger.matchResults(player.slug(), data.scoreId());
        reports.upsert(player.id(), data.scoreId(), matches, clock.instant());
        done++;
        failuresInARow = 0;
      } catch (RuntimeException e) {
        failed++;
        failuresInARow++;
        if (e instanceof BiwengerApiException) {
          LOG.log(
              Level.WARNING, "Points refresh skipped player {0}: {1}", player.id(), e.getMessage());
        } else {
          LOG.log(Level.WARNING, "Points refresh skipped player " + player.id(), e);
        }
      }
    }
    boolean complete = done == data.players().size();
    if (complete) {
      markRefreshed(pending, done);
    }
    LOG.log(
        Level.INFO,
        "Points refresh {0}: rounds {1}, {2} players done, {3} failed, in {4} s",
        complete ? "done" : "incomplete (rounds stay pending)",
        pending,
        done,
        failed,
        Duration.between(started, clock.instant()).toSeconds());
  }

  /** Finished rounds not refreshed yet, in the catalog's order. */
  private List<Long> pendingRounds(List<Round> rounds) {
    var refreshed =
        new HashSet<>(jdbc.queryForList("SELECT round_id FROM points_refresh", Long.class));
    return rounds.stream()
        .filter(round -> round.status() == RoundStatus.FINISHED)
        .map(Round::id)
        .filter(id -> !refreshed.contains(id))
        .toList();
  }

  private void markRefreshed(List<Long> rounds, int players) {
    Timestamp now = Timestamp.from(clock.instant());
    jdbc.batchUpdate(
        "INSERT INTO points_refresh (round_id, refreshed_at, players) VALUES (?, ?, ?)"
            + " ON CONFLICT (round_id) DO NOTHING",
        rounds.stream().map(id -> new Object[] {id, now, players}).toList());
  }
}
