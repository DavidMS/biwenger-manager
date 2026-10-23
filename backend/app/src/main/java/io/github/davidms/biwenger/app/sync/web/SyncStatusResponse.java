package io.github.davidms.biwenger.app.sync.web;

import com.fasterxml.jackson.annotation.JsonInclude;
import io.github.davidms.biwenger.app.sync.domain.SyncRun;
import java.time.Instant;
import java.time.LocalDate;

/**
 * {@code GET /api/sync/status}: whether a sync runs now, and the latest run. Fields that don't
 * apply to a run (counts of a failed one, the end of a running one) are left out.
 */
@JsonInclude(JsonInclude.Include.NON_NULL)
public record SyncStatusResponse(boolean running, LastRun lastRun) {

  static SyncStatusResponse from(boolean running, SyncRun latest) {
    return new SyncStatusResponse(running, latest == null ? null : LastRun.from(latest));
  }

  @JsonInclude(JsonInclude.Include.NON_NULL)
  public record LastRun(
      String trigger,
      String result,
      Instant startedAt,
      Instant finishedAt,
      LocalDate day,
      Integer players,
      Integer marketListings,
      Integer standings,
      String error) {

    static LastRun from(SyncRun run) {
      Instant finishedAt = run.finishedAt().orElse(null);
      String trigger = run.trigger().name();
      String result = run.result().name();
      return switch (run) {
        case SyncRun.Running r ->
            new LastRun(trigger, result, r.startedAt(), null, null, null, null, null, null);
        case SyncRun.Succeeded s ->
            new LastRun(
                trigger,
                result,
                s.startedAt(),
                finishedAt,
                s.day(),
                s.players(),
                s.marketListings(),
                s.standings(),
                null);
        case SyncRun.Failed f ->
            new LastRun(
                trigger, result, f.startedAt(), finishedAt, null, null, null, null, f.error());
      };
    }
  }
}
