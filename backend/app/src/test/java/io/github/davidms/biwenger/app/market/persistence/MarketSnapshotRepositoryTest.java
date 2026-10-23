package io.github.davidms.biwenger.app.market.persistence;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import io.github.davidms.biwenger.app.Samples;
import io.github.davidms.biwenger.app.TestcontainersConfiguration;
import io.github.davidms.biwenger.app.biwenger.BiwengerMapper;
import io.github.davidms.biwenger.app.market.domain.MarketListing;
import io.github.davidms.biwenger.client.model.Market;
import jakarta.persistence.EntityManager;
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

  @Autowired MarketSnapshotRepository market;
  @Autowired EntityManager entityManager;
  @Autowired JdbcTemplate jdbc;

  private static List<MarketListing> listings() {
    return BiwengerMapper.listings(Samples.data("market.json", Market.class));
  }

  @Test
  void storesTheWholeMarketAndReadsItBackUnchanged() {
    listings().forEach(listing -> market.save(MarketSnapshotEntity.from(DAY, listing)));
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
    listings().forEach(listing -> market.save(MarketSnapshotEntity.from(DAY, listing)));
    entityManager.flush();
    listings().forEach(listing -> market.save(MarketSnapshotEntity.from(DAY, listing)));
    entityManager.flush();

    assertThat(market.count()).isEqualTo(19);
  }

  @Test
  void theDatabaseRejectsASellerWithoutOwnershipData() {
    assertThatThrownBy(
            () ->
                jdbc.update(
                    """
                    INSERT INTO market_snapshot (snapshot_date, player_id, seller_manager_id,
                      seller_name, price, listed_at, closes_at, clause)
                    VALUES (DATE '2026-10-22', 1, 2000002, 'Manager 2', 1, now(), now(), NULL)
                    """))
        .isInstanceOf(DataIntegrityViolationException.class);
  }
}
