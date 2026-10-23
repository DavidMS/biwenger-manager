package io.github.davidms.biwenger.app.sync.service;

import static org.assertj.core.api.Assertions.assertThat;

import io.github.davidms.biwenger.app.TestcontainersConfiguration;
import io.github.davidms.biwenger.app.sync.domain.SyncRun;
import io.github.davidms.biwenger.app.sync.domain.SyncTrigger;
import java.time.Instant;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Import;
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
  void aRunLeftRunningIsMarkedFailed() {
    recorder.started(SyncTrigger.SCHEDULED, Instant.parse("2026-10-22T05:30:00Z"));

    recorder.markInterruptedRuns();

    assertThat(sync.latestRun())
        .get()
        .isInstanceOfSatisfying(
            SyncRun.Failed.class, run -> assertThat(run.error()).startsWith("Interrupted"));
  }
}
