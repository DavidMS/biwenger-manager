package io.github.davidms.biwenger.app.market.service;

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
import io.github.davidms.biwenger.app.market.domain.Market;
import io.github.davidms.biwenger.app.market.domain.MarketListing;
import io.github.davidms.biwenger.app.market.domain.MarketPlayer;
import io.github.davidms.biwenger.app.market.domain.Seller;
import io.github.davidms.biwenger.app.player.domain.DailyPrice;
import io.github.davidms.biwenger.app.player.domain.PlayerProfile;
import io.github.davidms.biwenger.app.player.domain.PlayerStatus;
import io.github.davidms.biwenger.app.player.domain.Position;
import io.github.davidms.biwenger.app.player.domain.Team;
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

class MarketServiceTest {

  // 00:30 in Madrid on the 28th, still the 27th in UTC: the trend uses Madrid's day.
  private static final Instant NOW = Instant.parse("2026-10-27T23:30:00Z");
  private static final LocalDate TODAY = LocalDate.of(2026, 10, 28);
  private static final MarketListing FREE =
      new MarketListing(
          2001, new Seller.Game(), Money.euros(4_790_000), NOW, NOW.plus(Duration.ofDays(1)));
  private static final MarketListing UNKNOWN =
      new MarketListing(
          99_999, new Seller.Game(), Money.euros(150_000), NOW, NOW.plus(Duration.ofDays(1)));
  private static final PlayerProfile AYOZE =
      new PlayerProfile(
          2001,
          "Ayoze",
          "ayoze-perez",
          19,
          Position.FORWARD,
          PlayerStatus.AVAILABLE,
          Money.euros(4_780_000));
  private static final List<DailyPrice> TREND =
      List.of(new DailyPrice(TODAY, Money.euros(4_780_000)));
  private static final List<Team> TEAMS = List.of(new Team(19, "Villarreal", "villarreal"));

  private final BiwengerGateway biwenger = mock(BiwengerGateway.class);
  private final PlayerReader reader = mock(PlayerReader.class);
  private final MutableClock clock = new MutableClock(NOW, ZoneId.of("Europe/Madrid"));
  private final MarketService markets = new MarketService(biwenger, reader, clock);

  private void stored() {
    when(reader.read(List.of(2001L, 99_999L), TODAY.minusDays(13), 0))
        .thenReturn(
            new StoredPlayers(List.of(AYOZE), Map.of(2001L, TREND), Map.of(2001L, 40), Map.of()));
    when(reader.teams()).thenReturn(TEAMS);
  }

  @Test
  void joinsTheListingsWithTheStoredPlayersAndNamesTheOnesItCannot() {
    when(biwenger.marketListings()).thenReturn(List.of(FREE, UNKNOWN));
    stored();

    assertThat(markets.market())
        .isEqualTo(
            new Market(List.of(new MarketPlayer(FREE, AYOZE, TREND, 40)), List.of(99_999L), TEAMS));
  }

  @Test
  void anEmptyMarketStillListsTheTeams() {
    when(biwenger.marketListings()).thenReturn(List.of());
    when(reader.read(List.of(), TODAY.minusDays(13), 0))
        .thenReturn(new StoredPlayers(List.of(), Map.of(), Map.of(), Map.of()));
    when(reader.teams()).thenReturn(TEAMS);

    assertThat(markets.market()).isEqualTo(new Market(List.of(), List.of(), TEAMS));
  }

  @Test
  void readsTheListingsOncePerMinute() {
    when(biwenger.marketListings()).thenReturn(List.of(FREE, UNKNOWN));
    stored();

    markets.market();
    clock.advance(Duration.ofSeconds(59));
    markets.market();
    verify(biwenger, times(1)).marketListings();

    clock.advance(Duration.ofSeconds(1));
    markets.market();
    verify(biwenger, times(2)).marketListings();
    verify(reader, times(3)).read(anyList(), any(), anyInt());
  }

  @Test
  void aFailureIsNotKept() {
    when(biwenger.marketListings())
        .thenThrow(new BiwengerApiException("Biwenger answered HTTP 503"))
        .thenReturn(List.of(FREE, UNKNOWN));
    stored();

    assertThatThrownBy(markets::market).isInstanceOf(BiwengerApiException.class);
    assertThat(markets.market().players()).hasSize(1);
  }
}
