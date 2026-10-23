package io.github.davidms.biwenger.app.backfill.service;

import io.github.davidms.biwenger.app.biwenger.BackfillPlan;
import io.github.davidms.biwenger.app.biwenger.BiwengerGateway;
import io.github.davidms.biwenger.client.BiwengerApiException;
import java.lang.System.Logger.Level;
import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.time.LocalDate;
import java.util.Set;
import org.springframework.stereotype.Service;

/**
 * Imports the season's price history and match reports for every catalog player, one player at a
 * time through the rate-limited client (about two CDN calls per player). Players already done this
 * season are skipped, so a second run resumes an interrupted one and a finished backfill does
 * nothing. After {@value #MAX_FAILURES_IN_A_ROW} players fail in a row, it stops: Biwenger is
 * probably down, and a later run continues.
 */
@Service
public class BackfillService {

  static final int MAX_FAILURES_IN_A_ROW = 3;
  private static final int LOG_EVERY = 50;
  private static final System.Logger LOG = System.getLogger(BackfillService.class.getName());

  private final BiwengerGateway biwenger;
  private final HistoryWriter writer;
  private final Clock clock;
  private volatile boolean stopRequested;

  BackfillService(BiwengerGateway biwenger, HistoryWriter writer, Clock clock) {
    this.biwenger = biwenger;
    this.writer = writer;
    this.clock = clock;
  }

  /** Asks a running backfill to stop after the current player, for example when the app stops. */
  public void requestStop() {
    stopRequested = true;
  }

  public BackfillSummary run() {
    stopRequested = false;
    Instant started = clock.instant();
    LocalDate today = LocalDate.ofInstant(started, clock.getZone());
    BackfillPlan plan = biwenger.backfillPlan();
    Set<Long> done = writer.completedPlayers(plan.season());
    int alreadyDone = 0;
    int completed = 0;
    int failed = 0;
    int failuresInARow = 0;
    for (var player : plan.players()) {
      if (done.contains(player.id())) {
        alreadyDone++;
        continue;
      }
      if (stopRequested) {
        LOG.log(Level.INFO, "Backfill stops: asked to stop (the app is shutting down?)");
        break;
      }
      if (failuresInARow == MAX_FAILURES_IN_A_ROW) {
        LOG.log(Level.WARNING, "Backfill stops after {0} failures in a row", failuresInARow);
        break;
      }
      try {
        var history = biwenger.playerHistory(player.slug(), plan.scoreId());
        writer.write(plan.season(), player.id(), plan.scoreId(), history, today, clock.instant());
        completed++;
        failuresInARow = 0;
      } catch (RuntimeException e) {
        // Biwenger failing, or this player's data not matching docs/biwenger-api/: skip him, and
        // a later run tries again.
        failed++;
        failuresInARow++;
        if (e instanceof BiwengerApiException) {
          LOG.log(Level.WARNING, "Backfill skipped player {0}: {1}", player.id(), e.getMessage());
        } else {
          // Not Biwenger's fault: maybe our bug. Keep the stack trace.
          LOG.log(Level.WARNING, "Backfill skipped player " + player.id(), e);
        }
      }
      if ((completed + failed) % LOG_EVERY == 0) {
        LOG.log(
            Level.INFO,
            "Backfill progress: {0} of {1} players",
            alreadyDone + completed + failed,
            plan.players().size());
      }
    }
    var summary =
        new BackfillSummary(
            plan.season(),
            completed,
            alreadyDone,
            failed,
            plan.players().size() - alreadyDone - completed,
            Duration.between(started, clock.instant()));
    LOG.log(Level.INFO, "Backfill run done, {0}", summary);
    return summary;
  }
}
