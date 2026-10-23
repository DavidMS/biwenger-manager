package io.github.davidms.biwenger.app.sync.service;

import io.github.davidms.biwenger.app.biwenger.BiwengerGateway;
import io.github.davidms.biwenger.app.biwenger.DailyData;
import io.github.davidms.biwenger.app.sync.domain.SyncRun;
import io.github.davidms.biwenger.app.sync.domain.SyncTrigger;
import io.github.davidms.biwenger.app.sync.persistence.SyncRunEntity;
import io.github.davidms.biwenger.app.sync.persistence.SyncRunRepository;
import java.lang.System.Logger.Level;
import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.time.LocalDate;
import java.util.Optional;
import java.util.concurrent.atomic.AtomicBoolean;
import org.springframework.stereotype.Service;

/**
 * Takes today's snapshot: reads the catalog, market and standings from Biwenger, then stores them
 * for today's Madrid date. Every Biwenger call happens before the database transaction starts, so
 * no transaction waits on the network.
 *
 * <p>Only one sync runs at a time, whoever starts it (the schedule or {@code POST /api/sync}): two
 * at once would clash on the same day's rows and double the calls to Biwenger. Every run is
 * recorded in {@code sync_run}. The guard is in memory: one app instance, as this single-user app
 * runs.
 */
@Service
public class DailySyncService {

  private static final System.Logger LOG = System.getLogger(DailySyncService.class.getName());

  private final BiwengerGateway biwenger;
  private final SnapshotWriter writer;
  private final SyncRunRecorder recorder;
  private final SyncRunRepository runs;
  private final Clock clock;
  private final AtomicBoolean running = new AtomicBoolean();

  DailySyncService(
      BiwengerGateway biwenger,
      SnapshotWriter writer,
      SyncRunRecorder recorder,
      SyncRunRepository runs,
      Clock clock) {
    this.biwenger = biwenger;
    this.writer = writer;
    this.recorder = recorder;
    this.runs = runs;
    this.clock = clock;
  }

  /**
   * Syncs now, on this thread.
   *
   * @throws SyncAlreadyRunningException if a sync is already running
   */
  public SyncSummary syncToday(SyncTrigger trigger) {
    claim();
    try {
      Instant started = clock.instant();
      return sync(recordStart(trigger, started), started);
    } finally {
      running.set(false);
    }
  }

  /**
   * Starts a sync on its own thread and returns at once.
   *
   * @throws SyncAlreadyRunningException if a sync is already running
   */
  public void startInBackground() {
    claim();
    try {
      // Recorded before the thread starts, so the caller's next status read already shows it.
      Instant started = clock.instant();
      long runId = recordStart(SyncTrigger.MANUAL, started);
      Thread.ofVirtual().name("manual-sync").start(() -> syncAndRelease(runId, started));
    } catch (RuntimeException e) {
      running.set(false);
      throw e;
    }
  }

  public boolean isRunning() {
    return running.get();
  }

  /** The latest recorded run, running or finished. */
  public Optional<SyncRun> latestRun() {
    return runs.findFirstByOrderByStartedAtDescIdDesc().map(SyncRunEntity::toDomain);
  }

  private void claim() {
    if (!running.compareAndSet(false, true)) {
      throw new SyncAlreadyRunningException();
    }
  }

  /** Not even the start could be recorded (the database is down?): say so, then give up. */
  private long recordStart(SyncTrigger trigger, Instant started) {
    try {
      return recorder.started(trigger, started);
    } catch (RuntimeException e) {
      LOG.log(Level.ERROR, "Sync not started: its run could not be recorded", e);
      throw e;
    }
  }

  private void syncAndRelease(long runId, Instant started) {
    try {
      sync(runId, started);
    } catch (RuntimeException e) {
      // Already recorded in sync_run and logged by sync(); nothing else waits for this thread.
    } finally {
      running.set(false);
    }
  }

  private SyncSummary sync(long runId, Instant started) {
    try {
      LocalDate day = LocalDate.ofInstant(started, clock.getZone());
      DailyData data = biwenger.dailyData();
      writer.write(day, started, data);
      var summary =
          new SyncSummary(
              day,
              data.players().size(),
              data.market().size(),
              data.standings().size(),
              Duration.between(started, clock.instant()));
      recorder.succeeded(runId, summary);
      LOG.log(Level.INFO, "Daily sync done, {0}", summary);
      return summary;
    } catch (RuntimeException e) {
      // Log first: if recording the failure fails too (the database is down?), this still shows.
      LOG.log(Level.ERROR, "Sync failed: " + e.getMessage(), e);
      try {
        recorder.failed(
            runId, e.getMessage() != null ? e.getMessage() : e.getClass().getSimpleName());
      } catch (RuntimeException recording) {
        e.addSuppressed(recording);
        LOG.log(Level.ERROR, "The failed sync could not be recorded either", recording);
      }
      throw e;
    }
  }
}
