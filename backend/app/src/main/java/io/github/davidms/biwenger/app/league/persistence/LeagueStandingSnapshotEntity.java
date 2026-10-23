package io.github.davidms.biwenger.app.league.persistence;

import io.github.davidms.biwenger.app.common.Money;
import io.github.davidms.biwenger.app.league.domain.Manager;
import io.github.davidms.biwenger.app.league.domain.Standing;
import jakarta.persistence.Column;
import jakarta.persistence.EmbeddedId;
import jakarta.persistence.Entity;
import jakarta.persistence.Table;
import java.time.Instant;
import java.time.LocalDate;

/** A manager's place in the league table on one day (table {@code league_standing_snapshot}). */
@Entity
@Table(name = "league_standing_snapshot")
public class LeagueStandingSnapshotEntity {

  @EmbeddedId private StandingSnapshotId id;

  @Column(name = "league_id", nullable = false)
  private long leagueId;

  @Column(name = "manager_name", nullable = false)
  private String managerName;

  @Column(nullable = false)
  private int position;

  @Column(nullable = false)
  private int points;

  @Column(name = "team_value", nullable = false)
  private long teamValue;

  @Column(name = "team_value_change", nullable = false)
  private long teamValueChange;

  @Column(name = "synced_at", nullable = false)
  private Instant syncedAt;

  protected LeagueStandingSnapshotEntity() {}

  public static LeagueStandingSnapshotEntity from(
      LocalDate day, long leagueId, Standing standing, Instant syncedAt) {
    var entity = new LeagueStandingSnapshotEntity();
    entity.syncedAt = syncedAt;
    entity.id = new StandingSnapshotId(day, standing.manager().id());
    entity.leagueId = leagueId;
    entity.managerName = standing.manager().name();
    entity.position = standing.position();
    entity.points = standing.points();
    entity.teamValue = standing.teamValue().euros();
    entity.teamValueChange = standing.teamValueChange().euros();
    return entity;
  }

  public StandingSnapshotId id() {
    return id;
  }

  public long leagueId() {
    return leagueId;
  }

  public Standing toDomain() {
    return new Standing(
        new Manager(id.managerId(), managerName),
        position,
        points,
        Money.euros(teamValue),
        Money.euros(teamValueChange));
  }

  public Instant syncedAt() {
    return syncedAt;
  }
}
