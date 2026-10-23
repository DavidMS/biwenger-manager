package io.github.davidms.biwenger.app.sync.domain;

/** Who started a sync: the daily schedule, or the user through {@code POST /api/sync}. */
public enum SyncTrigger {
  SCHEDULED,
  MANUAL
}
