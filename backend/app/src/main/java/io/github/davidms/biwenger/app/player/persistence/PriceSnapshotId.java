package io.github.davidms.biwenger.app.player.persistence;

import jakarta.persistence.Column;
import jakarta.persistence.Embeddable;
import java.time.LocalDate;

/** One player on one day: the natural key that makes a second sync on the same day an update. */
@Embeddable
public record PriceSnapshotId(
    @Column(name = "player_id") long playerId,
    @Column(name = "snapshot_date") LocalDate snapshotDate) {}
