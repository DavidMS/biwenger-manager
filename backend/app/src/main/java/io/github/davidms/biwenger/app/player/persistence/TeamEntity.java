package io.github.davidms.biwenger.app.player.persistence;

import io.github.davidms.biwenger.app.player.domain.Team;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import java.time.Instant;

/** A club as the latest catalog had it (table {@code team}). */
@Entity
@Table(name = "team")
public class TeamEntity {

  @Id private long id;

  @Column(nullable = false)
  private String name;

  @Column(nullable = false)
  private String slug;

  @Column(name = "updated_at", nullable = false)
  private Instant updatedAt;

  protected TeamEntity() {}

  public static TeamEntity from(Team team, Instant updatedAt) {
    var entity = new TeamEntity();
    entity.id = team.id();
    entity.name = team.name();
    entity.slug = team.slug();
    entity.updatedAt = updatedAt;
    return entity;
  }

  public Team toDomain() {
    return new Team(id, name, slug);
  }

  public Instant updatedAt() {
    return updatedAt;
  }
}
