package io.github.davidms.biwenger.app.sync.web;

import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import io.github.davidms.biwenger.app.sync.domain.SyncRun;
import io.github.davidms.biwenger.app.sync.domain.SyncTrigger;
import io.github.davidms.biwenger.app.sync.service.DailySyncService;
import io.github.davidms.biwenger.app.sync.service.SyncAlreadyRunningException;
import java.time.Instant;
import java.time.LocalDate;
import java.util.Optional;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.http.MediaType;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

/** The HTTP side only: the service is mocked. */
@WebMvcTest(SyncController.class)
class SyncControllerTest {

  static final Instant STARTED = Instant.parse("2026-10-22T05:30:00Z");
  static final Instant FINISHED = Instant.parse("2026-10-22T05:30:04Z");

  @Autowired MockMvc mvc;
  @MockitoBean DailySyncService sync;

  @Test
  void startsASyncAndAnswersAtOnce() throws Exception {
    when(sync.isRunning()).thenReturn(true);
    when(sync.latestRun())
        .thenReturn(Optional.of(new SyncRun.Running(SyncTrigger.MANUAL, STARTED)));

    mvc.perform(post("/api/sync"))
        .andExpect(status().isAccepted())
        .andExpect(header().string("Location", "/api/sync/status"))
        .andExpect(jsonPath("$.running").value(true))
        .andExpect(jsonPath("$.lastRun.result").value("RUNNING"))
        .andExpect(jsonPath("$.lastRun.trigger").value("MANUAL"))
        .andExpect(jsonPath("$.lastRun.finishedAt").doesNotExist());
    verify(sync).startInBackground();
  }

  @Test
  void refusesASecondSyncWithAProblemDetail() throws Exception {
    doThrow(new SyncAlreadyRunningException()).when(sync).startInBackground();

    mvc.perform(post("/api/sync"))
        .andExpect(status().isConflict())
        .andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_PROBLEM_JSON))
        .andExpect(jsonPath("$.title").value("Sync already running"))
        .andExpect(jsonPath("$.status").value(409))
        .andExpect(jsonPath("$.detail").value("A sync is already running"));
  }

  @Test
  void showsTheLastSuccessfulRunWithItsCounts() throws Exception {
    when(sync.latestRun())
        .thenReturn(
            Optional.of(
                new SyncRun.Succeeded(
                    SyncTrigger.SCHEDULED,
                    STARTED,
                    FINISHED,
                    LocalDate.of(2026, 10, 22),
                    551,
                    19,
                    6)));

    mvc.perform(get("/api/sync/status"))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.running").value(false))
        .andExpect(jsonPath("$.lastRun.result").value("SUCCEEDED"))
        .andExpect(jsonPath("$.lastRun.startedAt").value("2026-10-22T05:30:00Z"))
        .andExpect(jsonPath("$.lastRun.finishedAt").value("2026-10-22T05:30:04Z"))
        .andExpect(jsonPath("$.lastRun.day").value("2026-10-22"))
        .andExpect(jsonPath("$.lastRun.players").value(551))
        .andExpect(jsonPath("$.lastRun.marketListings").value(19))
        .andExpect(jsonPath("$.lastRun.standings").value(6))
        .andExpect(jsonPath("$.lastRun.error").doesNotExist());
  }

  @Test
  void showsWhyTheLastRunFailed() throws Exception {
    when(sync.latestRun())
        .thenReturn(
            Optional.of(
                new SyncRun.Failed(
                    SyncTrigger.MANUAL,
                    STARTED,
                    FINISHED,
                    "Biwenger answered HTTP 503 to GET /api/v2/market")));

    mvc.perform(get("/api/sync/status"))
        .andExpect(jsonPath("$.lastRun.result").value("FAILED"))
        .andExpect(
            jsonPath("$.lastRun.error").value("Biwenger answered HTTP 503 to GET /api/v2/market"))
        .andExpect(jsonPath("$.lastRun.players").doesNotExist());
  }

  @Test
  void showsNoRunBeforeTheFirstSync() throws Exception {
    when(sync.latestRun()).thenReturn(Optional.empty());

    mvc.perform(get("/api/sync/status"))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.running").value(false))
        .andExpect(jsonPath("$.lastRun").doesNotExist());
  }

  @Test
  void anUnsupportedMethodIsAProblemDetailToo() throws Exception {
    mvc.perform(get("/api/sync"))
        .andExpect(status().isMethodNotAllowed())
        .andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_PROBLEM_JSON));
  }
}
