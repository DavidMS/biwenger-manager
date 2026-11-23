package io.github.davidms.biwenger.app.advisor.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.anyList;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import io.github.davidms.biwenger.app.MutableClock;
import io.github.davidms.biwenger.app.advisor.domain.BuyRecommendation;
import io.github.davidms.biwenger.app.advisor.domain.BuyRecommendation.EmptySlot;
import io.github.davidms.biwenger.app.advisor.domain.BuyRecommendation.Replaces;
import io.github.davidms.biwenger.app.advisor.domain.BuyRecommendations;
import io.github.davidms.biwenger.app.advisor.domain.BuyWeights;
import io.github.davidms.biwenger.app.advisor.domain.ReferenceFormation;
import io.github.davidms.biwenger.app.advisor.domain.ScoringWeights;
import io.github.davidms.biwenger.app.common.Money;
import io.github.davidms.biwenger.app.dashboard.domain.TeamStatus;
import io.github.davidms.biwenger.app.dashboard.service.DashboardService;
import io.github.davidms.biwenger.app.market.domain.Market;
import io.github.davidms.biwenger.app.market.domain.MarketListing;
import io.github.davidms.biwenger.app.market.domain.MarketPlayer;
import io.github.davidms.biwenger.app.market.domain.Seller;
import io.github.davidms.biwenger.app.market.service.MarketService;
import io.github.davidms.biwenger.app.player.domain.DailyPrice;
import io.github.davidms.biwenger.app.player.domain.MatchResult;
import io.github.davidms.biwenger.app.player.domain.PlayerProfile;
import io.github.davidms.biwenger.app.player.domain.PlayerStatus;
import io.github.davidms.biwenger.app.player.domain.Position;
import io.github.davidms.biwenger.app.player.service.PlayerReader;
import io.github.davidms.biwenger.app.player.service.StoredPlayers;
import io.github.davidms.biwenger.app.squad.domain.Squad;
import io.github.davidms.biwenger.app.squad.domain.SquadPlayer;
import io.github.davidms.biwenger.app.squad.service.SquadService;
import io.github.davidms.biwenger.client.BiwengerApiException;
import java.math.BigDecimal;
import java.time.Duration;
import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneId;
import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.Test;

class BuyAdvisorServiceTest {

  // 00:30 in Madrid on the 28th, still the 27th in UTC: dates use Madrid's day.
  private static final Instant NOW = Instant.parse("2026-10-27T23:30:00Z");
  private static final LocalDate TODAY = LocalDate.of(2026, 10, 28);

  /** Seven days of trend, plus one in case today's sync hasn't run yet. */
  private static final LocalDate PRICES_FROM = TODAY.minusDays(7);

  private final MarketService market = mock(MarketService.class);
  private final SquadService squad = mock(SquadService.class);
  private final DashboardService dashboard = mock(DashboardService.class);
  private final PlayerReader reader = mock(PlayerReader.class);
  private final BuyAdvisorService advisor =
      new BuyAdvisorService(
          market,
          squad,
          dashboard,
          reader,
          ScoringWeights.DEFAULTS,
          new ReferenceFormation(
              Map.of(
                  Position.GOALKEEPER, 0,
                  Position.DEFENDER, 1,
                  Position.MIDFIELDER, 0,
                  Position.FORWARD, 0,
                  Position.COACH, 0)),
          BuyWeights.DEFAULTS,
          new MutableClock(NOW, ZoneId.of("Europe/Madrid")));

  private static PlayerProfile defender(long id) {
    return new PlayerProfile(
        id,
        "Player " + id,
        "player-" + id,
        3,
        Position.DEFENDER,
        PlayerStatus.AVAILABLE,
        Money.euros(2_000_000));
  }

  private static MarketPlayer listed(PlayerProfile player, long price) {
    return new MarketPlayer(
        new MarketListing(
            player.id(), new Seller.Game(), Money.euros(price), NOW, NOW.plus(Duration.ofDays(1))),
        player,
        List.of(),
        0);
  }

  private static SquadPlayer owned(PlayerProfile player) {
    return new SquadPlayer(player, List.of(), 0, List.of());
  }

  private static MatchResult played(long matchId, int points) {
    return new MatchResult(
        matchId,
        matchId,
        NOW.minus(Duration.ofDays(30 - matchId)),
        true,
        new MatchResult.Played(points, 90));
  }

  private void given(
      List<MarketPlayer> onSale,
      List<Long> missingFromMarket,
      List<SquadPlayer> mine,
      List<Long> missingFromSquad,
      long balance) {
    when(market.market()).thenReturn(new Market(onSale, missingFromMarket, List.of()));
    when(squad.squad()).thenReturn(new Squad(mine, missingFromSquad));
    when(dashboard.team())
        .thenReturn(new TeamStatus(Money.euros(balance), 0, 1, 10, Money.ZERO, Money.ZERO));
  }

  @Test
  void scoresTheMarketAndYourSquadFromStoredMatchesAndFiltersByYourBalance() {
    PlayerProfile onSale = defender(100);
    PlayerProfile tooDear = defender(101);
    PlayerProfile mine = defender(1);
    given(
        List.of(listed(onSale, 3_000_000), listed(tooDear, 9_000_000)),
        List.of(),
        List.of(owned(mine)),
        List.of(),
        5_000_000);
    // Every match (not just the squad page's last five).
    when(reader.read(List.of(100L, 101L, 1L), PRICES_FROM, Integer.MAX_VALUE))
        .thenReturn(
            new StoredPlayers(
                List.of(mine, onSale, tooDear),
                Map.of(
                    100L,
                    List.of(
                        new DailyPrice(TODAY.minusDays(1), Money.euros(1_900_000)),
                        new DailyPrice(TODAY, Money.euros(2_000_000)))),
                Map.of(100L, 16, 101L, 40, 1L, 4),
                Map.of(
                    100L, List.of(played(1, 8), played(2, 8)),
                    101L, List.of(played(1, 20), played(2, 20)),
                    1L, List.of(played(1, 2), played(2, 2)))));

    BuyRecommendations buys = advisor.recommendations();

    assertThat(buys.balance()).isEqualTo(Money.euros(5_000_000));
    assertThat(buys.recommendations()).hasSize(1);
    BuyRecommendation buy = buys.recommendations().getFirst();
    assertThat(buy.candidate().player()).isEqualTo(onSale);
    assertThat(buy.candidate().score().expectedPoints()).isEqualTo(new BigDecimal("8.00"));
    assertThat(buy.candidate().score().valueTrend()).isEqualTo(Money.euros(100_000));
    assertThat(buy.askingPrice()).isEqualTo(Money.euros(3_000_000));
    assertThat(((Replaces) buy.slot()).starter().player()).isEqualTo(mine);
    // 8.00 − 2.00
    assertThat(buy.gain()).isEqualTo(new BigDecimal("6.00"));
    assertThat(buy.budgetLeft()).isEqualTo(Money.euros(2_000_000));
    // The balance only: the catalog's round schedule isn't needed.
    verify(dashboard, never()).dashboard();
  }

  @Test
  void measuresTheTrendOverSevenDaysWhenTodaysSyncHasNotRunYet() {
    PlayerProfile onSale = defender(100);
    given(List.of(listed(onSale, 1_000_000)), List.of(), List.of(), List.of(), 5_000_000);
    when(reader.read(List.of(100L), PRICES_FROM, Integer.MAX_VALUE))
        .thenReturn(
            new StoredPlayers(
                List.of(onSale),
                Map.of(
                    100L,
                    List.of(
                        new DailyPrice(TODAY.minusDays(7), Money.euros(1_000_000)),
                        new DailyPrice(TODAY.minusDays(1), Money.euros(1_100_000)))),
                Map.of(100L, 8),
                Map.of(100L, List.of(played(1, 8)))));

    BuyRecommendation buy = advisor.recommendations().recommendations().getFirst();

    // Latest price yesterday: its seven days start a week ago, which the read must include.
    assertThat(buy.candidate().score().valueTrend()).isEqualTo(Money.euros(100_000));
  }

  @Test
  void scoresAPlayerWithNoStoredHistoryAsZeroAndReportsTheOnesItCannotScore() {
    PlayerProfile unknownHistory = defender(100);
    given(
        List.of(listed(unknownHistory, 1_000_000)),
        List.of(99_998L),
        List.of(),
        List.of(99_999L),
        5_000_000);
    when(reader.read(List.of(100L), PRICES_FROM, Integer.MAX_VALUE))
        .thenReturn(new StoredPlayers(List.of(unknownHistory), Map.of(), Map.of(), Map.of()));

    BuyRecommendations buys = advisor.recommendations();

    // No matches: 0 expected points, so no gain even in an empty place.
    assertThat(buys.recommendations()).isEmpty();
    assertThat(buys.missingSquadPlayerIds()).containsExactly(99_999L);
    assertThat(buys.missingMarketPlayerIds()).containsExactly(99_998L);
  }

  @Test
  void readsYourOwnListedPlayerOnceAndNeverSuggestsHim() {
    PlayerProfile mine = defender(1);
    given(List.of(listed(mine, 1_000_000)), List.of(), List.of(owned(mine)), List.of(), 5_000_000);
    when(reader.read(List.of(1L), PRICES_FROM, Integer.MAX_VALUE))
        .thenReturn(
            new StoredPlayers(
                List.of(mine), Map.of(), Map.of(1L, 8), Map.of(1L, List.of(played(1, 8)))));

    assertThat(advisor.recommendations().recommendations()).isEmpty();
  }

  @Test
  void fillsAnEmptyPlaceWhenYouHaveNoDefender() {
    PlayerProfile onSale = defender(100);
    given(List.of(listed(onSale, 1_000_000)), List.of(), List.of(), List.of(), 5_000_000);
    when(reader.read(List.of(100L), PRICES_FROM, Integer.MAX_VALUE))
        .thenReturn(
            new StoredPlayers(
                List.of(onSale), Map.of(), Map.of(100L, 8), Map.of(100L, List.of(played(1, 8)))));

    assertThat(advisor.recommendations().recommendations().getFirst().slot())
        .isEqualTo(new EmptySlot());
  }

  @Test
  void letsABiwengerFailureThrough() {
    when(market.market()).thenThrow(new BiwengerApiException("Biwenger answered HTTP 503"));

    assertThatThrownBy(advisor::recommendations).isInstanceOf(BiwengerApiException.class);
    verify(reader, never()).read(anyList(), any(), anyInt());
  }
}
