package io.github.davidms.biwenger.app.player.persistence;

import io.github.davidms.biwenger.app.common.Money;
import io.github.davidms.biwenger.app.player.domain.DailyPrice;
import io.github.davidms.biwenger.app.player.domain.Player;
import io.github.davidms.biwenger.app.player.domain.PlayerStatus;
import jakarta.persistence.Column;
import jakarta.persistence.EmbeddedId;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Table;
import java.time.Instant;
import java.time.LocalDate;
import java.util.Optional;

/** A player's value on one day (table {@code player_price_snapshot}). */
@Entity
@Table(name = "player_price_snapshot")
public class PlayerPriceSnapshotEntity {

  @EmbeddedId private PriceSnapshotId id;

  @Column(nullable = false)
  private long price;

  @Column(name = "price_change")
  private Long priceChange;

  @Enumerated(EnumType.STRING)
  private PlayerStatus status;

  private Integer points;

  @Enumerated(EnumType.STRING)
  @Column(nullable = false)
  private PriceSnapshotSource source;

  @Column(name = "synced_at", nullable = false)
  private Instant syncedAt;

  protected PlayerPriceSnapshotEntity() {}

  /** What the daily sync records: everything the catalog says about the player that day. */
  public static PlayerPriceSnapshotEntity daily(Player player, LocalDate day, Instant syncedAt) {
    var entity = new PlayerPriceSnapshotEntity();
    entity.syncedAt = syncedAt;
    entity.id = new PriceSnapshotId(player.id(), day);
    entity.price = player.price().euros();
    entity.priceChange = player.priceChange().euros();
    entity.status = player.status();
    entity.points = player.points();
    entity.source = PriceSnapshotSource.DAILY_SYNC;
    return entity;
  }

  /**
   * What the backfill (3.4) records from the price history, which only has the price. It must never
   * replace a {@code DAILY_SYNC} row of the same day (ADR 0005), so don't {@code save()} it
   * blindly.
   */
  public static PlayerPriceSnapshotEntity backfill(
      long playerId, LocalDate day, Money price, Instant syncedAt) {
    var entity = new PlayerPriceSnapshotEntity();
    entity.syncedAt = syncedAt;
    entity.id = new PriceSnapshotId(playerId, day);
    entity.price = price.euros();
    entity.source = PriceSnapshotSource.BACKFILL;
    return entity;
  }

  public DailyPrice toDomain() {
    return new DailyPrice(id.snapshotDate(), Money.euros(price));
  }

  public PriceSnapshotId id() {
    return id;
  }

  public Money price() {
    return Money.euros(price);
  }

  public Optional<Money> priceChange() {
    return Optional.ofNullable(priceChange).map(Money::euros);
  }

  public Optional<PlayerStatus> status() {
    return Optional.ofNullable(status);
  }

  public Optional<Integer> points() {
    return Optional.ofNullable(points);
  }

  public PriceSnapshotSource source() {
    return source;
  }

  public Instant syncedAt() {
    return syncedAt;
  }
}
