package io.github.davidms.biwenger.app.squad.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.anyList;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import io.github.davidms.biwenger.app.MutableClock;
import io.github.davidms.biwenger.app.biwenger.BiwengerGateway;
import io.github.davidms.biwenger.app.common.Money;
import io.github.davidms.biwenger.app.player.domain.DailyPrice;
import io.github.davidms.biwenger.app.player.domain.MatchResult;
import io.github.davidms.biwenger.app.player.domain.PlayerProfile;
import io.github.davidms.biwenger.app.player.domain.PlayerStatus;
import io.github.davidms.biwenger.app.player.domain.Position;
import io.github.davidms.biwenger.app.player.service.PlayerReader;
import io.github.davidms.biwenger.app.player.service.StoredPlayers;
import io.github.davidms.biwenger.app.squad.domain.Squad;
import io.github.davidms.biwenger.app.squad.domain.SquadPlayer;
import io.github.davidms.biwenger.client.BiwengerApiException;
import java.time.Duration;
import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneId;
import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.Test;

class SquadServiceTest {

  // 00:30 in Madrid on the 28th, still the 27th in UTC: the trend uses Madrid's day.
  private static final Instant NOW = Instant.parse("2026-10-27T23:30:00Z");
  private static final LocalDate TODAY = LocalDate.of(2026, 10, 28);
  private static final List<Long> OWNED = List.of(2001L, 33691L);
  private static final PlayerProfile AYOZE =
      new PlayerProfile(
          2001,
          "Ayoze Pérez",
          "ayoze-perez",
          15,
          Position.FORWARD,
          PlayerStatus.AVAILABLE,
          Money.euros(4_780_000));
  private static final List<DailyPrice> TREND =
      List.of(
          new DailyPrice(TODAY.minusDays(1), Money.euros(4_700_000)),
          new DailyPrice(TODAY, Money.euros(4_780_000)));
  private static final List<MatchResult> FORM =
      List.of(new MatchResult(1, 4907, NOW, true, new MatchResult.Played(6, 90)));

  private final BiwengerGateway biwenger = mock(BiwengerGateway.class);
  private final PlayerReader reader = mock(PlayerReader.class);
  private final MutableClock clock = new MutableClock(NOW, ZoneId.of("Europe/Madrid"));
  private final SquadService squads = new SquadService(biwenger, reader, clock);

  private void storedHistory() {
    when(reader.read(OWNED, TODAY.minusDays(13), 5))
        .thenReturn(
            new StoredPlayers(
                List.of(AYOZE), Map.of(2001L, TREND), Map.of(2001L, 40), Map.of(2001L, FORM)));
  }

  @Test
  void joinsTheOwnedPlayersWithTheirStoredHistory() {
    when(biwenger.squadPlayerIds()).thenReturn(OWNED);
    storedHistory();

    assertThat(squads.squad())
        .isEqualTo(new Squad(List.of(new SquadPlayer(AYOZE, TREND, 40, FORM)), List.of(33691L)));
  }

  @Test
  void aPlayerWithNoHistoryHasAnEmptyTrendNoPointsAndNoForm() {
    when(biwenger.squadPlayerIds()).thenReturn(List.of(2001L));
    when(reader.read(List.of(2001L), TODAY.minusDays(13), 5))
        .thenReturn(new StoredPlayers(List.of(AYOZE), Map.of(), Map.of(), Map.of()));

    assertThat(squads.squad().players())
        .containsExactly(new SquadPlayer(AYOZE, List.of(), 0, List.of()));
  }

  @Test
  void anEmptySquadIsEmpty() {
    when(biwenger.squadPlayerIds()).thenReturn(List.of());
    when(reader.read(List.of(), TODAY.minusDays(13), 5))
        .thenReturn(new StoredPlayers(List.of(), Map.of(), Map.of(), Map.of()));

    assertThat(squads.squad()).isEqualTo(new Squad(List.of(), List.of()));
  }

  @Test
  void readsTheOwnedPlayersOncePerMinute() {
    when(biwenger.squadPlayerIds()).thenReturn(OWNED);
    storedHistory();

    squads.squad();
    clock.advance(Duration.ofSeconds(59));
    squads.squad();
    verify(biwenger, times(1)).squadPlayerIds();

    clock.advance(Duration.ofSeconds(1));
    squads.squad();
    verify(biwenger, times(2)).squadPlayerIds();
    // The stored history is read every time: the database is cheap and the sync may have run.
    verify(reader, times(3)).read(anyList(), any(), anyInt());
  }

  @Test
  void aFailureIsNotKept() {
    when(biwenger.squadPlayerIds())
        .thenThrow(new BiwengerApiException("Biwenger answered HTTP 503"))
        .thenReturn(OWNED);
    storedHistory();

    assertThatThrownBy(squads::squad).isInstanceOf(BiwengerApiException.class);
    assertThat(squads.squad().players()).hasSize(1);
  }
}
