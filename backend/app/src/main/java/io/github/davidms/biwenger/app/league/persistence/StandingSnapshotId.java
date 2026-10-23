package io.github.davidms.biwenger.app.league.persistence;

import jakarta.persistence.Column;
import jakarta.persistence.Embeddable;
import java.time.LocalDate;

/** One manager in the league table on one day. */
@Embeddable
public record StandingSnapshotId(
    @Column(name = "snapshot_date") LocalDate snapshotDate,
    @Column(name = "manager_id") long managerId) {}
