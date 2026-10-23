package io.github.davidms.biwenger.app.player.persistence;

/** Where a price snapshot came from. Only the daily sync knows the status and points. */
public enum PriceSnapshotSource {
  DAILY_SYNC,
  BACKFILL
}
