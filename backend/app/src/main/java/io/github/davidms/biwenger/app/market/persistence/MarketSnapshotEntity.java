package io.github.davidms.biwenger.app.market.persistence;

import io.github.davidms.biwenger.app.common.Money;
import io.github.davidms.biwenger.app.league.domain.Manager;
import io.github.davidms.biwenger.app.market.domain.MarketListing;
import io.github.davidms.biwenger.app.market.domain.Ownership;
import io.github.davidms.biwenger.app.market.domain.Seller;
import jakarta.persistence.Column;
import jakarta.persistence.EmbeddedId;
import jakarta.persistence.Entity;
import jakarta.persistence.Table;
import java.time.Instant;
import java.time.LocalDate;

/**
 * A market listing on one day (table {@code market_snapshot}). A free player has no seller and no
 * ownership columns; the database checks that they come together.
 */
@Entity
@Table(name = "market_snapshot")
public class MarketSnapshotEntity {

  @EmbeddedId private MarketSnapshotId id;

  @Column(name = "seller_manager_id")
  private Long sellerManagerId;

  @Column(name = "seller_name")
  private String sellerName;

  @Column(nullable = false)
  private long price;

  @Column(name = "listed_at", nullable = false)
  private Instant listedAt;

  @Column(name = "closes_at", nullable = false)
  private Instant closesAt;

  private Long clause;

  @Column(name = "purchase_price")
  private Long purchasePrice;

  @Column(name = "acquired_at")
  private Instant acquiredAt;

  @Column(name = "clause_unlocks_at")
  private Instant clauseUnlocksAt;

  protected MarketSnapshotEntity() {}

  public static MarketSnapshotEntity from(LocalDate day, MarketListing listing) {
    var entity = new MarketSnapshotEntity();
    entity.id = new MarketSnapshotId(day, listing.playerId());
    entity.price = listing.price().euros();
    entity.listedAt = listing.listedAt();
    entity.closesAt = listing.closesAt();
    if (listing.seller() instanceof Seller.ByManager(Manager manager, Ownership ownership)) {
      entity.sellerManagerId = manager.id();
      entity.sellerName = manager.name();
      entity.clause = ownership.clause().euros();
      entity.purchasePrice = ownership.purchasePrice().euros();
      entity.acquiredAt = ownership.acquiredAt();
      entity.clauseUnlocksAt = ownership.clauseUnlocksAt();
    }
    return entity;
  }

  public MarketSnapshotId id() {
    return id;
  }

  public MarketListing toDomain() {
    Seller seller =
        sellerManagerId == null
            ? new Seller.Game()
            : new Seller.ByManager(
                new Manager(sellerManagerId, sellerName),
                new Ownership(
                    Money.euros(clause), Money.euros(purchasePrice), acquiredAt, clauseUnlocksAt));
    return new MarketListing(id.playerId(), seller, Money.euros(price), listedAt, closesAt);
  }
}
