package io.github.davidms.biwenger.app.sync.domain;

import java.time.Instant;
import java.time.LocalDate;
import java.util.Objects;
import java.util.Optional;

/**
 * One sync run. While it runs it has no end and no counts; when it succeeded it has the day it
 * stored and how much; when it failed it has the reason.
 */
public sealed interface SyncRun {

  SyncTrigger trigger();

  Instant startedAt();

  SyncResult result();

  /** When it ended, empty while it runs. */
  default Optional<Instant> finishedAt() {
    return Optional.empty();
  }

  record Running(SyncTrigger trigger, Instant startedAt) implements SyncRun {

    public Running {
      Objects.requireNonNull(trigger, "trigger");
      Objects.requireNonNull(startedAt, "startedAt");
    }

    @Override
    public SyncResult result() {
      return SyncResult.RUNNING;
    }
  }

  record Succeeded(
      SyncTrigger trigger,
      Instant startedAt,
      Instant endedAt,
      LocalDate day,
      int players,
      int marketListings,
      int standings)
      implements SyncRun {

    public Succeeded {
      Objects.requireNonNull(trigger, "trigger");
      Objects.requireNonNull(startedAt, "startedAt");
      Objects.requireNonNull(endedAt, "endedAt");
      Objects.requireNonNull(day, "day");
    }

    @Override
    public SyncResult result() {
      return SyncResult.SUCCEEDED;
    }

    @Override
    public Optional<Instant> finishedAt() {
      return Optional.of(endedAt);
    }
  }

  record Failed(SyncTrigger trigger, Instant startedAt, Instant endedAt, String error)
      implements SyncRun {

    public Failed {
      Objects.requireNonNull(trigger, "trigger");
      Objects.requireNonNull(startedAt, "startedAt");
      Objects.requireNonNull(endedAt, "endedAt");
      Objects.requireNonNull(error, "error");
    }

    @Override
    public SyncResult result() {
      return SyncResult.FAILED;
    }

    @Override
    public Optional<Instant> finishedAt() {
      return Optional.of(endedAt);
    }
  }
}
