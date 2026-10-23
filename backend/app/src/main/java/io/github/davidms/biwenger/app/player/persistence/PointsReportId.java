package io.github.davidms.biwenger.app.player.persistence;

import jakarta.persistence.Column;
import jakarta.persistence.Embeddable;

/** One player in one club match. */
@Embeddable
public record PointsReportId(
    @Column(name = "player_id") long playerId, @Column(name = "match_id") long matchId) {}
