package io.github.davidms.biwenger.app.player.persistence;

import io.github.davidms.biwenger.app.common.Money;
import io.github.davidms.biwenger.app.player.domain.Player;
import io.github.davidms.biwenger.app.player.domain.PlayerProfile;
import io.github.davidms.biwenger.app.player.domain.PlayerStatus;
import io.github.davidms.biwenger.app.player.domain.Position;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import java.time.Instant;

/** A player as the latest catalog had him (table {@code player}). */
@Entity
@Table(name = "player")
public class PlayerEntity {

  @Id private long id;

  @Column(nullable = false)
  private String name;

  @Column(nullable = false)
  private String slug;

  @Column(name = "team_id", nullable = false)
  private long teamId;

  @Enumerated(EnumType.STRING)
  @Column(nullable = false)
  private Position position;

  @Enumerated(EnumType.STRING)
  @Column(nullable = false)
  private PlayerStatus status;

  @Column(nullable = false)
  private long price;

  @Column(name = "updated_at", nullable = false)
  private Instant updatedAt;

  protected PlayerEntity() {}

  public static PlayerEntity from(Player player, Instant updatedAt) {
    var entity = new PlayerEntity();
    entity.id = player.id();
    entity.name = player.name();
    entity.slug = player.slug();
    entity.teamId = player.teamId();
    entity.position = player.position();
    entity.status = player.status();
    entity.price = player.price().euros();
    entity.updatedAt = updatedAt;
    return entity;
  }

  public PlayerProfile toDomain() {
    return new PlayerProfile(id, name, slug, teamId, position, status, Money.euros(price));
  }

  public long id() {
    return id;
  }

  public String name() {
    return name;
  }

  public String slug() {
    return slug;
  }

  public long teamId() {
    return teamId;
  }

  public Position position() {
    return position;
  }

  public PlayerStatus status() {
    return status;
  }

  public long price() {
    return price;
  }

  public Instant updatedAt() {
    return updatedAt;
  }
}
