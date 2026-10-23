package io.github.davidms.biwenger.app.market.persistence;

import jakarta.persistence.Column;
import jakarta.persistence.Embeddable;
import java.time.LocalDate;

/** One player in the market on one day. */
@Embeddable
public record MarketSnapshotId(
    @Column(name = "snapshot_date") LocalDate snapshotDate,
    @Column(name = "player_id") long playerId) {}
