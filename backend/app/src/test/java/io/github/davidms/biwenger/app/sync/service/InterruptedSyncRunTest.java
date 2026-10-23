package io.github.davidms.biwenger.app.sync.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import io.github.davidms.biwenger.app.TestcontainersConfiguration;
import io.github.davidms.biwenger.app.sync.domain.SyncRun;
import io.github.davidms.biwenger.app.sync.domain.SyncTrigger;
import io.github.davidms.biwenger.app.sync.persistence.SyncRunEntityAccess;
import java.time.Duration;
import java.time.Instant;
import java.time.LocalDate;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Import;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.jdbc.core.JdbcTemplate;

/** A run the app never finished (a stop, a crash) is marked failed at the next startup. */
@SpringBootTest
@Import(TestcontainersConfiguration.class)
class InterruptedSyncRunTest {

  @Autowired SyncRunRecorder recorder;
  @Autowired DailySyncService sync;
  @Autowired JdbcTemplate jdbc;

  @AfterEach
  void emptyTable() {
    jdbc.execute("TRUNCATE sync_run");
  }

  @Test
  void aRunLeftRunningBeforeThisStartIsMarkedFailed() {
    recorder.started(SyncTrigger.SCHEDULED, Instant.parse("2026-10-22T05:30:00Z"));

    recorder.markInterruptedRuns();

    assertThat(sync.latestRun())
        .get()
        .isInstanceOfSatisfying(
            SyncRun.Failed.class, run -> assertThat(run.error()).startsWith("Interrupted"));
  }

  @Test
  void aRunStartedSinceThisStartIsLeftAlone() {
    // The web server and the scheduler start before the app is ready: a run may already be going.
    long runId = recorder.started(SyncTrigger.MANUAL, Instant.now().plusSeconds(1));

    recorder.markInterruptedRuns();

    assertThat(sync.latestRun()).get().isInstanceOf(SyncRun.Running.class);
    recorder.succeeded(runId, new SyncSummary(LocalDate.of(2026, 10, 22), 1, 2, 3, Duration.ZERO));
    assertThat(sync.latestRun()).get().isInstanceOf(SyncRun.Succeeded.class);
  }

  @Test
  void theDatabaseRejectsInconsistentRuns() {
    // Succeeded without counts; running with an end; failed without a reason.
    for (String result : new String[] {"SUCCEEDED", "RUNNING", "FAILED"}) {
      assertThatThrownBy(
              () ->
                  jdbc.update(
                      "INSERT INTO sync_run (sync_trigger, started_at, finished_at, result)"
                          + " VALUES ('MANUAL', now(), now(), ?)",
                      result))
          .as(result)
          .isInstanceOf(DataIntegrityViolationException.class);
    }
  }

  @Test
  void longErrorsAreCutWithoutSplittingACharacter() {
    String text = "x".repeat(499) + "\uD83D\uDE00" + "y";

    assertThat(SyncRunEntityAccess.truncate(text)).hasSize(499);
  }
}
