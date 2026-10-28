package io.github.davidms.biwenger.app.league.domain;

import static org.assertj.core.api.Assertions.assertThat;

import io.github.davidms.biwenger.app.common.Money;
import java.time.Instant;
import java.util.List;
import org.junit.jupiter.api.Test;

class LeagueEntryTest {

  private static final Standing STANDING =
      new Standing(new Manager(2_000_004, "Manager 4"), 1, 515, Money.euros(1), Money.ZERO);

  @Test
  void keepsEachSquadPlayerOnceInIdOrder() {
    var entry = new LeagueEntry(STANDING, List.of(30L, 10L, 30L, 20L), Instant.EPOCH);

    assertThat(entry.squadPlayerIds()).containsExactly(10L, 20L, 30L);
    assertThat(entry.managerId()).isEqualTo(2_000_004);
  }

  @Test
  void findsAnEntryByManager() {
    var table =
        new LeagueTable(List.of(new LeagueEntry(STANDING, List.of(), Instant.EPOCH)), 2_000_001);

    assertThat(table.entry(2_000_004)).isPresent();
    assertThat(table.entry(2_000_001)).isEmpty();
  }
}
