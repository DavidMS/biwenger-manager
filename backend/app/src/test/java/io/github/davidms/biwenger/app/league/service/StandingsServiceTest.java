package io.github.davidms.biwenger.app.league.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import io.github.davidms.biwenger.app.MutableClock;
import io.github.davidms.biwenger.app.biwenger.BiwengerGateway;
import io.github.davidms.biwenger.app.common.Money;
import io.github.davidms.biwenger.app.league.domain.LeagueEntry;
import io.github.davidms.biwenger.app.league.domain.LeagueTable;
import io.github.davidms.biwenger.app.league.domain.Manager;
import io.github.davidms.biwenger.app.league.domain.RivalSquad;
import io.github.davidms.biwenger.app.league.domain.Standing;
import io.github.davidms.biwenger.app.player.domain.PlayerProfile;
import io.github.davidms.biwenger.app.player.domain.PlayerStatus;
import io.github.davidms.biwenger.app.player.domain.Position;
import io.github.davidms.biwenger.app.player.service.PlayerReader;
import io.github.davidms.biwenger.app.player.service.StoredPlayers;
import io.github.davidms.biwenger.client.BiwengerApiException;
import java.time.Duration;
import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneId;
import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.Test;

class StandingsServiceTest {

  // 00:30 in Madrid on the 28th, still the 27th in UTC.
  private static final Instant NOW = Instant.parse("2026-10-27T23:30:00Z");
  private static final LocalDate TODAY = LocalDate.of(2026, 10, 28);
  private static final LeagueEntry RIVAL =
      new LeagueEntry(
          new Standing(
              new Manager(2_000_004, "Manager 4"),
              1,
              515,
              Money.euros(64_530_000),
              Money.euros(-70_000)),
          List.of(2001L, 33691L),
          NOW.minus(Duration.ofDays(2)));
  private static final LeagueTable TABLE = new LeagueTable(List.of(RIVAL), 2_000_001);
  private static final PlayerProfile AYOZE =
      new PlayerProfile(
          2001,
          "Ayoze",
          "ayoze-perez",
          19,
          Position.FORWARD,
          PlayerStatus.AVAILABLE,
          Money.euros(4_780_000));

  private final BiwengerGateway biwenger = mock(BiwengerGateway.class);
  private final PlayerReader reader = mock(PlayerReader.class);
  private final MutableClock clock = new MutableClock(NOW, ZoneId.of("Europe/Madrid"));
  private final StandingsService standings = new StandingsService(biwenger, reader, clock);

  @Test
  void readsTheTableOncePerMinute() {
    when(biwenger.leagueTable()).thenReturn(TABLE);

    standings.table();
    clock.advance(Duration.ofSeconds(59));
    standings.table();
    verify(biwenger, times(1)).leagueTable();

    clock.advance(Duration.ofSeconds(1));
    assertThat(standings.table()).isEqualTo(TABLE);
    verify(biwenger, times(2)).leagueTable();
  }

  @Test
  void aSquadJoinsTheLineupWithTheStoredPlayersAndNamesTheOnesItCannot() {
    when(biwenger.leagueTable()).thenReturn(TABLE);
    when(reader.read(List.of(2001L, 33691L), TODAY, 0))
        .thenReturn(new StoredPlayers(List.of(AYOZE), Map.of(), Map.of(2001L, 40), Map.of()));

    assertThat(standings.squad(2_000_004))
        .isEqualTo(
            new RivalSquad(RIVAL, List.of(new RivalSquad.Member(AYOZE, 40)), List.of(33691L)));
  }

  @Test
  void aSquadUsesTheCachedTable() {
    when(biwenger.leagueTable()).thenReturn(TABLE);
    when(reader.read(List.of(2001L, 33691L), TODAY, 0))
        .thenReturn(new StoredPlayers(List.of(), Map.of(), Map.of(), Map.of()));

    standings.table();
    standings.squad(2_000_004);

    verify(biwenger, times(1)).leagueTable();
  }

  @Test
  void anUnknownManagerIsNotFound() {
    when(biwenger.leagueTable()).thenReturn(TABLE);

    assertThatThrownBy(() -> standings.squad(42))
        .isInstanceOf(ManagerNotFoundException.class)
        .hasMessage("No manager 42 in the league");
  }

  @Test
  void aFailureIsNotKept() {
    when(biwenger.leagueTable())
        .thenThrow(new BiwengerApiException("Biwenger answered HTTP 503"))
        .thenReturn(TABLE);

    assertThatThrownBy(standings::table).isInstanceOf(BiwengerApiException.class);
    assertThat(standings.table()).isEqualTo(TABLE);
  }
}
