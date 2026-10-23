package io.github.davidms.biwenger.app.market.persistence;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import io.github.davidms.biwenger.app.Samples;
import io.github.davidms.biwenger.app.TestcontainersConfiguration;
import io.github.davidms.biwenger.app.biwenger.BiwengerMapper;
import io.github.davidms.biwenger.app.common.Money;
import io.github.davidms.biwenger.app.market.domain.MarketListing;
import io.github.davidms.biwenger.client.model.Market;
import jakarta.persistence.EntityManager;
import java.time.Instant;
import java.time.LocalDate;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.data.jpa.test.autoconfigure.DataJpaTest;
import org.springframework.boot.jdbc.test.autoconfigure.AutoConfigureTestDatabase;
import org.springframework.context.annotation.Import;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.jdbc.core.JdbcTemplate;

@DataJpaTest
@AutoConfigureTestDatabase(replace = AutoConfigureTestDatabase.Replace.NONE)
@Import(TestcontainersConfiguration.class)
class MarketSnapshotRepositoryTest {

  private static final LocalDate DAY = LocalDate.of(2026, 10, 22);
  private static final Instant SYNCED = Instant.parse("2026-10-22T05:10:00Z");

  @Autowired MarketSnapshotRepository market;
  @Autowired EntityManager entityManager;
  @Autowired JdbcTemplate jdbc;

  private static List<MarketListing> listings() {
    return BiwengerMapper.listings(Samples.data("market.json", Market.class));
  }

  @Test
  void storesTheWholeMarketAndReadsItBackUnchanged() {
    listings().forEach(listing -> market.save(MarketSnapshotEntity.from(DAY, listing, SYNCED)));
    entityManager.flush();
    entityManager.clear();

    List<MarketListing> stored =
        market.findByIdSnapshotDateOrderByIdPlayerIdAsc(DAY).stream()
            .map(MarketSnapshotEntity::toDomain)
            .toList();

    // Free players and managers' sales, with ownership and clause locks, survive the round trip.
    assertThat(stored).hasSize(19).containsExactlyInAnyOrderElementsOf(listings());
    assertThat(stored).extracting(MarketListing::playerId).isSorted();
  }

  @Test
  void aSecondSyncOnTheSameDayDoesNotDuplicate() {
    listings().forEach(listing -> market.save(MarketSnapshotEntity.from(DAY, listing, SYNCED)));
    entityManager.flush();
    // The same day again, with one price changed: an update, not a second row.
    listings().stream()
        .map(l -> l.playerId() == 2169 ? withPrice(l, Money.euros(1)) : l)
        .forEach(l -> market.save(MarketSnapshotEntity.from(DAY, l, SYNCED.plusSeconds(600))));
    entityManager.flush();
    entityManager.clear();

    assertThat(market.count()).isEqualTo(19);
    var updated = market.findById(new MarketSnapshotId(DAY, 2169)).orElseThrow();
    assertThat(updated.toDomain().price()).isEqualTo(Money.euros(1));
    assertThat(updated.syncedAt()).isEqualTo(SYNCED.plusSeconds(600));
  }

  @Test
  void theDatabaseRejectsASellerWithoutOwnershipData() {
    assertThatThrownBy(
            () ->
                jdbc.update(
                    """
                    INSERT INTO market_snapshot (snapshot_date, player_id, seller_manager_id,
                      seller_name, price, listed_at, closes_at, clause, synced_at)
                    VALUES (DATE '2026-10-22', 1, 2000002, 'Manager 2', 1, now(), now(), NULL, now())
                    """))
        .isInstanceOf(DataIntegrityViolationException.class);
  }

  @Test
  void theDatabaseRejectsHalfTheOwnershipData() {
    // A clause but no purchase price: toDomain() could not rebuild it.
    assertThatThrownBy(
            () ->
                jdbc.update(
                    """
                    INSERT INTO market_snapshot (snapshot_date, player_id, seller_manager_id,
                      seller_name, price, listed_at, closes_at, clause, purchase_price,
                      acquired_at, clause_unlocks_at, synced_at)
                    VALUES (DATE '2026-10-22', 2, 2000002, 'Manager 2', 1, now(), now(),
                      100, NULL, now(), now(), now())
                    """))
        .isInstanceOf(DataIntegrityViolationException.class);
  }

  private static MarketListing withPrice(MarketListing listing, Money price) {
    return new MarketListing(
        listing.playerId(), listing.seller(), price, listing.listedAt(), listing.closesAt());
  }
}
