package io.github.davidms.biwenger.app.player.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import io.github.davidms.biwenger.app.Samples;
import io.github.davidms.biwenger.app.TestcontainersConfiguration;
import io.github.davidms.biwenger.app.biwenger.BiwengerMapper;
import io.github.davidms.biwenger.app.common.Money;
import io.github.davidms.biwenger.app.player.domain.DailyPrice;
import io.github.davidms.biwenger.app.player.domain.MatchResult;
import io.github.davidms.biwenger.app.player.domain.Player;
import io.github.davidms.biwenger.app.player.domain.PlayerProfile;
import io.github.davidms.biwenger.app.player.domain.PlayerStatus;
import io.github.davidms.biwenger.app.player.domain.Position;
import io.github.davidms.biwenger.app.player.domain.Team;
import io.github.davidms.biwenger.app.player.persistence.PlayerEntity;
import io.github.davidms.biwenger.app.player.persistence.PlayerPointsReportEntity;
import io.github.davidms.biwenger.app.player.persistence.PlayerPointsReportRepository;
import io.github.davidms.biwenger.app.player.persistence.PlayerPriceSnapshotEntity;
import io.github.davidms.biwenger.app.player.persistence.PlayerPriceSnapshotRepository;
import io.github.davidms.biwenger.app.player.persistence.PlayerRepository;
import io.github.davidms.biwenger.app.player.persistence.PointsReportId;
import io.github.davidms.biwenger.app.player.persistence.TeamEntity;
import io.github.davidms.biwenger.app.player.persistence.TeamRepository;
import io.github.davidms.biwenger.client.model.Catalog;
import jakarta.persistence.EntityManager;
import java.time.Duration;
import java.time.Instant;
import java.time.LocalDate;
import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.data.jpa.test.autoconfigure.DataJpaTest;
import org.springframework.boot.jdbc.test.autoconfigure.AutoConfigureTestDatabase;
import org.springframework.context.annotation.Import;

/** The reader against a real PostgreSQL, migrated by Flyway. */
@DataJpaTest
@AutoConfigureTestDatabase(replace = AutoConfigureTestDatabase.Replace.NONE)
@Import({TestcontainersConfiguration.class, PlayerReader.class})
class PlayerReaderTest {

  private static final LocalDate DAY = LocalDate.of(2026, 10, 28);
  private static final Instant NOW = Instant.parse("2026-10-28T05:10:00Z");

  @Autowired PlayerReader reader;
  @Autowired PlayerRepository players;
  @Autowired PlayerPriceSnapshotRepository prices;
  @Autowired PlayerPointsReportRepository reports;
  @Autowired TeamRepository teams;
  @Autowired EntityManager entityManager;

  private static Player catalogPlayer(long id) {
    return BiwengerMapper.players(Samples.data("catalog.json", Catalog.class)).stream()
        .filter(player -> player.id() == id)
        .findFirst()
        .orElseThrow();
  }

  private void flushAndClear() {
    entityManager.flush();
    entityManager.clear();
  }

  @Test
  void readsTheStoredProfilesByIdAndLeavesOutUnknownOnes() {
    players.save(PlayerEntity.from(catalogPlayer(2602), NOW));
    players.save(PlayerEntity.from(catalogPlayer(2001), NOW));
    flushAndClear();

    assertThat(reader.read(List.of(2602L, 2001L, 99L), DAY, 5).profiles())
        .extracting(PlayerProfile::id)
        .containsExactly(2001L, 2602L);
    assertThat(reader.read(List.of(2001L), DAY, 5).profiles().getFirst())
        .isEqualTo(
            new PlayerProfile(
                2001,
                "Ayoze",
                "ayoze-perez",
                19,
                Position.FORWARD,
                PlayerStatus.AVAILABLE,
                Money.euros(4_780_000)));
  }

  @Test
  void readsThePricesSinceADayOldestFirst() {
    prices.save(PlayerPriceSnapshotEntity.backfill(2001, DAY.minusDays(14), Money.euros(1), NOW));
    prices.save(PlayerPriceSnapshotEntity.daily(catalogPlayer(2001), DAY, NOW));
    prices.save(
        PlayerPriceSnapshotEntity.backfill(2001, DAY.minusDays(13), Money.euros(4_700_000), NOW));
    prices.save(PlayerPriceSnapshotEntity.daily(catalogPlayer(2602), DAY, NOW));
    flushAndClear();

    Map<Long, List<DailyPrice>> trends = reader.read(List.of(2001L), DAY.minusDays(13), 5).prices();

    assertThat(trends).containsOnlyKeys(2001L);
    assertThat(trends.get(2001L))
        .containsExactly(
            new DailyPrice(DAY.minusDays(13), Money.euros(4_700_000)),
            new DailyPrice(DAY, Money.euros(4_780_000)));
  }

  @Test
  void readsTheSeasonPointsFromTheLatestDayThatHasThem() {
    prices.save(PlayerPriceSnapshotEntity.daily(catalogPlayer(2001), DAY.minusDays(1), NOW));
    // A later backfilled day has no points, so it doesn't count.
    prices.save(PlayerPriceSnapshotEntity.backfill(2001, DAY, Money.euros(4_800_000), NOW));
    flushAndClear();

    assertThat(reader.read(List.of(2001L, 2602L), DAY, 5).seasonPoints())
        .containsExactly(Map.entry(2001L, catalogPlayer(2001).points()));
  }

  @Test
  void readsTheLastMatchesLatestFirst() {
    for (int match = 1; match <= 6; match++) {
      reports.save(
          PlayerPointsReportEntity.played(
              new PointsReportId(2001, match),
              4900 + match,
              NOW.plus(Duration.ofDays(7L * match)),
              true,
              1,
              match,
              90,
              NOW));
    }
    reports.save(
        PlayerPointsReportEntity.missed(
            new PointsReportId(2602, 1), 4901, NOW, false, 1, PlayerStatus.INJURED, NOW));
    flushAndClear();

    Map<Long, List<MatchResult>> form = reader.read(List.of(2001L, 2602L), DAY, 5).lastMatches();

    assertThat(form.get(2001L))
        .extracting(MatchResult::matchId)
        .containsExactly(6L, 5L, 4L, 3L, 2L);
    assertThat(form.get(2001L).getFirst().outcome()).isEqualTo(new MatchResult.Played(6, 90));
    assertThat(form.get(2602L))
        .extracting(MatchResult::outcome)
        .containsExactly(new MatchResult.Missed(PlayerStatus.INJURED));
  }

  @Test
  void breaksATieOnMatchDateByMatchIdLatestFirst() {
    for (long match : List.of(7L, 9L, 8L)) {
      reports.save(
          PlayerPointsReportEntity.played(
              new PointsReportId(2001, match), 4907, NOW, true, 1, (int) match, 90, NOW));
    }
    flushAndClear();

    assertThat(reader.read(List.of(2001L), DAY, 2).lastMatches().get(2001L))
        .extracting(MatchResult::matchId)
        .containsExactly(9L, 8L);
  }

  @Test
  void readsNoMatchesWhenAskedForNone() {
    reports.save(
        PlayerPointsReportEntity.played(
            new PointsReportId(2001, 1), 4907, NOW, true, 1, 6, 90, NOW));
    flushAndClear();

    assertThat(reader.read(List.of(2001L), DAY, 0).lastMatches()).isEmpty();
  }

  @Test
  void readsTheCurrentTeamsByNameThenId() {
    teams.save(TeamEntity.from(new Team(3, "Barcelona", "barcelona"), NOW));
    teams.save(TeamEntity.from(new Team(1, "Athletic", "athletic"), NOW));
    teams.save(TeamEntity.from(new Team(40, "Barcelona", "barcelona-b"), NOW));
    // Relegated: the latest sync didn't see it.
    teams.save(TeamEntity.from(new Team(9, "Almería", "almeria"), NOW.minusSeconds(86_400)));
    flushAndClear();

    assertThat(reader.teams()).extracting(Team::id).containsExactly(1L, 3L, 40L);
  }

  @Test
  void answersNothingForNoIds() {
    players.save(PlayerEntity.from(catalogPlayer(2001), NOW));
    flushAndClear();

    assertThat(reader.read(List.of(), DAY, 5))
        .isEqualTo(new StoredPlayers(List.of(), Map.of(), Map.of(), Map.of()));
  }

  @Test
  void rejectsANegativeNumberOfMatches() {
    assertThatThrownBy(() -> reader.read(List.of(2001L), DAY, -1))
        .isInstanceOf(IllegalArgumentException.class);
  }
}
