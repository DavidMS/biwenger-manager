package io.github.davidms.biwenger.app.league.persistence;

import static org.assertj.core.api.Assertions.assertThat;

import io.github.davidms.biwenger.app.Samples;
import io.github.davidms.biwenger.app.TestcontainersConfiguration;
import io.github.davidms.biwenger.app.biwenger.BiwengerMapper;
import io.github.davidms.biwenger.app.league.domain.Standing;
import io.github.davidms.biwenger.client.model.RoundStandings;
import jakarta.persistence.EntityManager;
import java.time.Instant;
import java.time.LocalDate;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.data.jpa.test.autoconfigure.DataJpaTest;
import org.springframework.boot.jdbc.test.autoconfigure.AutoConfigureTestDatabase;
import org.springframework.context.annotation.Import;

@DataJpaTest
@AutoConfigureTestDatabase(replace = AutoConfigureTestDatabase.Replace.NONE)
@Import(TestcontainersConfiguration.class)
class LeagueStandingSnapshotRepositoryTest {

  private static final LocalDate DAY = LocalDate.of(2026, 10, 22);
  private static final long LEAGUE_ID = 1_000_001;
  private static final Instant SYNCED = Instant.parse("2026-10-22T05:10:00Z");

  @Autowired LeagueStandingSnapshotRepository standings;
  @Autowired EntityManager entityManager;

  @Test
  void storesTheTableAndReadsItBackByPosition() {
    List<Standing> table =
        BiwengerMapper.standings(Samples.data("rounds-league.json", RoundStandings.class));
    // Saved in reverse, read back by position.
    table
        .reversed()
        .forEach(
            standing ->
                standings.save(
                    LeagueStandingSnapshotEntity.from(DAY, LEAGUE_ID, standing, SYNCED)));
    entityManager.flush();
    entityManager.clear();

    var stored = standings.findByIdSnapshotDateOrderByPositionAsc(DAY);

    assertThat(stored)
        .extracting(LeagueStandingSnapshotEntity::toDomain)
        .containsExactlyElementsOf(table);
    assertThat(stored).allMatch(snapshot -> snapshot.leagueId() == LEAGUE_ID);
  }

  @Test
  void aSecondSyncOnTheSameDayReplacesTheTable() {
    List<Standing> table =
        BiwengerMapper.standings(Samples.data("rounds-league.json", RoundStandings.class));
    table.forEach(
        s -> standings.save(LeagueStandingSnapshotEntity.from(DAY, LEAGUE_ID, s, SYNCED)));
    entityManager.flush();
    // The same day again, the leader with more points: updated in place.
    table.stream()
        .map(
            s ->
                s.position() == 1
                    ? new Standing(
                        s.manager(), 1, s.points() + 7, s.teamValue(), s.teamValueChange())
                    : s)
        .forEach(s -> standings.save(LeagueStandingSnapshotEntity.from(DAY, LEAGUE_ID, s, SYNCED)));
    entityManager.flush();
    table.forEach(
        s ->
            standings.save(
                LeagueStandingSnapshotEntity.from(DAY.plusDays(1), LEAGUE_ID, s, SYNCED)));
    entityManager.flush();

    entityManager.clear();
    assertThat(standings.count()).isEqualTo(12);
    assertThat(standings.findByIdSnapshotDateOrderByPositionAsc(DAY).getFirst().toDomain().points())
        .isEqualTo(table.getFirst().points() + 7);
  }
}
