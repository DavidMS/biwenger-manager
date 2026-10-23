package io.github.davidms.biwenger.app.player.persistence;

import io.github.davidms.biwenger.app.common.Money;
import io.github.davidms.biwenger.app.player.domain.Player;
import io.github.davidms.biwenger.app.player.domain.PlayerStatus;
import jakarta.persistence.Column;
import jakarta.persistence.EmbeddedId;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Table;
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

  protected PlayerPriceSnapshotEntity() {}

  /** What the daily sync records: everything the catalog says about the player that day. */
  public static PlayerPriceSnapshotEntity daily(Player player, LocalDate day) {
    var entity = new PlayerPriceSnapshotEntity();
    entity.id = new PriceSnapshotId(player.id(), day);
    entity.price = player.price().euros();
    entity.priceChange = player.priceChange().euros();
    entity.status = player.status();
    entity.points = player.points();
    entity.source = PriceSnapshotSource.DAILY_SYNC;
    return entity;
  }

  /** What the backfill (3.4) records from the price history, which only has the price. */
  public static PlayerPriceSnapshotEntity backfill(long playerId, LocalDate day, Money price) {
    var entity = new PlayerPriceSnapshotEntity();
    entity.id = new PriceSnapshotId(playerId, day);
    entity.price = price.euros();
    entity.source = PriceSnapshotSource.BACKFILL;
    return entity;
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
}
