package io.github.davidms.biwenger.app.player.persistence;

import io.github.davidms.biwenger.app.player.domain.PlayerStatus;
import jakarta.persistence.Column;
import jakarta.persistence.EmbeddedId;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Table;
import java.time.Instant;
import java.util.Objects;
import java.util.Optional;

/**
 * A player's report for one club match (table {@code player_points_report}). Points are in the
 * scoring system {@code scoreId}, the league's when it was recorded. Filled by the backfill (3.4).
 */
@Entity
@Table(name = "player_points_report")
public class PlayerPointsReportEntity {

  @EmbeddedId private PointsReportId id;

  @Column(name = "round_id", nullable = false)
  private long roundId;

  @Column(name = "match_date", nullable = false)
  private Instant matchDate;

  @Column(nullable = false)
  private boolean home;

  @Column(nullable = false)
  private boolean played;

  @Column(name = "score_id", nullable = false)
  private int scoreId;

  private Integer points;

  @Column(name = "minutes_played", nullable = false)
  private int minutesPlayed;

  @Enumerated(EnumType.STRING)
  private PlayerStatus absence;

  protected PlayerPointsReportEntity() {}

  /** A match he played: points in {@code scoreId} and minutes on the pitch. */
  public static PlayerPointsReportEntity played(
      PointsReportId id,
      long roundId,
      Instant matchDate,
      boolean home,
      int scoreId,
      int points,
      int minutesPlayed) {
    var entity = match(id, roundId, matchDate, home, scoreId);
    entity.played = true;
    entity.points = points;
    entity.minutesPlayed = minutesPlayed;
    return entity;
  }

  /** A match he missed, and why ({@code AVAILABLE} when he was fit but not picked). */
  public static PlayerPointsReportEntity missed(
      PointsReportId id,
      long roundId,
      Instant matchDate,
      boolean home,
      int scoreId,
      PlayerStatus absence) {
    var entity = match(id, roundId, matchDate, home, scoreId);
    entity.played = false;
    entity.absence = Objects.requireNonNull(absence, "absence");
    return entity;
  }

  private static PlayerPointsReportEntity match(
      PointsReportId id, long roundId, Instant matchDate, boolean home, int scoreId) {
    var entity = new PlayerPointsReportEntity();
    entity.id = id;
    entity.roundId = roundId;
    entity.matchDate = matchDate;
    entity.home = home;
    entity.scoreId = scoreId;
    return entity;
  }

  public PointsReportId id() {
    return id;
  }

  public long roundId() {
    return roundId;
  }

  public Instant matchDate() {
    return matchDate;
  }

  public boolean home() {
    return home;
  }

  public boolean played() {
    return played;
  }

  public int scoreId() {
    return scoreId;
  }

  public Optional<Integer> points() {
    return Optional.ofNullable(points);
  }

  public int minutesPlayed() {
    return minutesPlayed;
  }

  public Optional<PlayerStatus> absence() {
    return Optional.ofNullable(absence);
  }
}
