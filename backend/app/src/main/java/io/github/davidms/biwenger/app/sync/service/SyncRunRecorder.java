package io.github.davidms.biwenger.app.sync.service;

import io.github.davidms.biwenger.app.sync.domain.SyncResult;
import io.github.davidms.biwenger.app.sync.domain.SyncTrigger;
import io.github.davidms.biwenger.app.sync.persistence.SyncRunEntity;
import io.github.davidms.biwenger.app.sync.persistence.SyncRunRepository;
import java.time.Clock;
import java.time.Instant;
import org.springframework.boot.context.event.ApplicationReadyEvent;
import org.springframework.context.event.EventListener;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/** Writes each run to {@code sync_run}, in its own short transactions. */
@Service
class SyncRunRecorder {

  private final SyncRunRepository runs;
  private final Clock clock;

  SyncRunRecorder(SyncRunRepository runs, Clock clock) {
    this.runs = runs;
    this.clock = clock;
  }

  @Transactional
  public long started(SyncTrigger trigger, Instant startedAt) {
    return runs.save(SyncRunEntity.started(trigger, startedAt)).id();
  }

  @Transactional
  public void succeeded(long runId, SyncSummary summary) {
    runs.findById(runId)
        .orElseThrow()
        .succeeded(
            clock.instant(),
            summary.day(),
            summary.players(),
            summary.marketListings(),
            summary.standings());
  }

  @Transactional
  public void failed(long runId, String error) {
    runs.findById(runId).orElseThrow().failed(clock.instant(), error);
  }

  /**
   * A run still marked running at startup was cut off by a stop or a crash: nothing can finish it
   * now, so it is marked failed.
   */
  @EventListener(ApplicationReadyEvent.class)
  @Transactional
  public void markInterruptedRuns() {
    for (var run : runs.findByResult(SyncResult.RUNNING)) {
      run.failed(clock.instant(), "Interrupted: the app stopped while this sync was running");
    }
  }
}
