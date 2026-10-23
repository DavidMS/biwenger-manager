package io.github.davidms.biwenger.app.sync.web;

import io.github.davidms.biwenger.app.sync.service.DailySyncService;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/** Start a sync now, and see how the last one went. */
@RestController
@RequestMapping("/api/sync")
class SyncController {

  private final DailySyncService sync;

  SyncController(DailySyncService sync) {
    this.sync = sync;
  }

  /**
   * Starts a sync in the background and answers at once with {@code 202 Accepted}: a sync takes
   * seconds, more if Biwenger is slow. {@code 409 Conflict} if one is already running.
   */
  @PostMapping
  ResponseEntity<SyncStatusResponse> start() {
    sync.startInBackground();
    return ResponseEntity.accepted().header("Location", "/api/sync/status").body(status());
  }

  @GetMapping("/status")
  SyncStatusResponse status() {
    return SyncStatusResponse.from(sync.isRunning(), sync.latestRun().orElse(null));
  }
}
