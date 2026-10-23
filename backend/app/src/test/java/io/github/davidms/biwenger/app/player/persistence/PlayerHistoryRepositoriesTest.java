package io.github.davidms.biwenger.app.player.persistence;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import io.github.davidms.biwenger.app.Samples;
import io.github.davidms.biwenger.app.TestcontainersConfiguration;
import io.github.davidms.biwenger.app.biwenger.BiwengerMapper;
import io.github.davidms.biwenger.app.common.Money;
import io.github.davidms.biwenger.app.player.domain.Player;
import io.github.davidms.biwenger.app.player.domain.PlayerStatus;
import io.github.davidms.biwenger.app.player.domain.Position;
import io.github.davidms.biwenger.client.model.Catalog;
import jakarta.persistence.EntityManager;
import java.time.Instant;
import java.time.LocalDate;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.data.jpa.test.autoconfigure.DataJpaTest;
import org.springframework.boot.jdbc.test.autoconfigure.AutoConfigureTestDatabase;
import org.springframework.context.annotation.Import;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.jdbc.core.JdbcTemplate;

/** Player tables against a real PostgreSQL, migrated by Flyway. */
@DataJpaTest
@AutoConfigureTestDatabase(replace = AutoConfigureTestDatabase.Replace.NONE)
@Import(TestcontainersConfiguration.class)
class PlayerHistoryRepositoriesTest {

  private static final LocalDate DAY = LocalDate.of(2026, 10, 22);
  private static final Instant NOW = Instant.parse("2026-10-22T05:10:00Z");

  @Autowired PlayerRepository players;
  @Autowired PlayerPriceSnapshotRepository prices;
  @Autowired PlayerPointsReportRepository reports;
  @Autowired EntityManager entityManager;
  @Autowired JdbcTemplate jdbc;

  private static Player ayoze() {
    return BiwengerMapper.players(Samples.data("catalog.json", Catalog.class)).stream()
        .filter(player -> player.id() == 2001)
        .findFirst()
        .orElseThrow();
  }

  /**
   * Writes, then forgets everything the session cached, so the next read comes from the database.
   */
  private void flushAndClear() {
    entityManager.flush();
    entityManager.clear();
  }

  @Test
  void savingAPlayerAgainUpdatesHim() {
    players.save(PlayerEntity.from(ayoze(), NOW));
    flushAndClear();
    players.save(PlayerEntity.from(ayoze(), NOW.plusSeconds(86_400)));
    flushAndClear();

    assertThat(players.count()).isEqualTo(1);
    PlayerEntity saved = players.findById(2001L).orElseThrow();
    assertThat(saved.position()).isEqualTo(Position.FORWARD);
    assertThat(saved.price()).isEqualTo(4_780_000);
    assertThat(saved.updatedAt()).isEqualTo(NOW.plusSeconds(86_400));
  }

  @Test
  void aSecondDailySnapshotOnTheSameDayReplacesTheFirst() {
    prices.save(PlayerPriceSnapshotEntity.daily(ayoze(), DAY));
    flushAndClear();
    prices.save(PlayerPriceSnapshotEntity.daily(ayoze(), DAY));
    flushAndClear();

    assertThat(prices.count()).isEqualTo(1);
    PlayerPriceSnapshotEntity saved = prices.findById(new PriceSnapshotId(2001, DAY)).orElseThrow();
    assertThat(saved.price()).isEqualTo(Money.euros(4_780_000));
    assertThat(saved.priceChange()).contains(Money.euros(30_000));
    assertThat(saved.status()).contains(PlayerStatus.AVAILABLE);
    assertThat(saved.points()).contains(35);
    assertThat(saved.source()).isEqualTo(PriceSnapshotSource.DAILY_SYNC);
  }

  @Test
  void priceHistoryComesOldestFirstAndBackfillHasOnlyThePrice() {
    prices.save(PlayerPriceSnapshotEntity.daily(ayoze(), DAY));
    prices.save(PlayerPriceSnapshotEntity.backfill(2001, DAY.minusDays(2), Money.euros(4_700_000)));
    prices.save(PlayerPriceSnapshotEntity.backfill(2001, DAY.minusDays(1), Money.euros(4_750_000)));
    flushAndClear();

    var history = prices.findByIdPlayerIdOrderByIdSnapshotDateAsc(2001);

    assertThat(history)
        .extracting(snapshot -> snapshot.id().snapshotDate())
        .containsExactly(DAY.minusDays(2), DAY.minusDays(1), DAY);
    assertThat(history.getFirst().status()).isEmpty();
    assertThat(history.getFirst().points()).isEmpty();
    assertThat(prices.findByIdSnapshotDateOrderByIdPlayerIdAsc(DAY)).hasSize(1);
  }

  @Test
  void savesPlayedAndMissedMatches() {
    var played = new PointsReportId(2001, 50715);
    var missed = new PointsReportId(2001, 50721);
    reports.save(
        PlayerPointsReportEntity.missed(
            missed, 4900, NOW.plusSeconds(3_600), true, 1, PlayerStatus.INJURED));
    reports.save(PlayerPointsReportEntity.played(played, 4899, NOW, false, 1, 2, 56));
    flushAndClear();

    var matches = reports.findByIdPlayerIdOrderByMatchDateAsc(2001);

    assertThat(matches).extracting(r -> r.id().matchId()).containsExactly(50715L, 50721L);
    assertThat(matches.get(0).points()).contains(2);
    assertThat(matches.get(0).minutesPlayed()).isEqualTo(56);
    assertThat(matches.get(1).played()).isFalse();
    assertThat(matches.get(1).points()).isEmpty();
    assertThat(matches.get(1).absence()).contains(PlayerStatus.INJURED);
  }

  @Test
  void theDatabaseRejectsAPlayedMatchWithoutPoints() {
    // The last line of defence if code ever writes an inconsistent report.
    assertThatThrownBy(
            () ->
                jdbc.update(
                    """
                    INSERT INTO player_points_report (player_id, match_id, round_id, match_date,
                      home, played, score_id, points, minutes_played)
                    VALUES (1, 1, 1, now(), true, true, 1, NULL, 90)
                    """))
        .isInstanceOf(DataIntegrityViolationException.class);
  }
}
