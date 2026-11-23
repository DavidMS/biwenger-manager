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
import io.github.davidms.biwenger.app.advisor.domain.ReferenceFormation;
import io.github.davidms.biwenger.app.advisor.domain.ScoringWeights;
import io.github.davidms.biwenger.app.advisor.domain.SellRecommendation;
import io.github.davidms.biwenger.app.advisor.domain.SellRecommendations;
import io.github.davidms.biwenger.app.advisor.domain.SellSignal.FallingValue;
import io.github.davidms.biwenger.app.advisor.domain.SellSignal.LongInjury;
import io.github.davidms.biwenger.app.advisor.domain.SellSignal.Surplus;
import io.github.davidms.biwenger.app.advisor.domain.SellWeights;
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

class SellAdvisorServiceTest {

  // 00:30 in Madrid on the 28th, still the 27th in UTC: dates use Madrid's day.
  private static final Instant NOW = Instant.parse("2026-10-27T23:30:00Z");
  private static final LocalDate TODAY = LocalDate.of(2026, 10, 28);

  /** Seven days of trend, plus one in case today's sync hasn't run yet. */
  private static final LocalDate PRICES_FROM = TODAY.minusDays(7);

  private final SquadService squad = mock(SquadService.class);
  private final PlayerReader reader = mock(PlayerReader.class);
  private final SellAdvisorService advisor =
      new SellAdvisorService(
          squad,
          reader,
          ScoringWeights.DEFAULTS,
          new ReferenceFormation(
              Map.of(
                  Position.GOALKEEPER, 0,
                  Position.DEFENDER, 1,
                  Position.MIDFIELDER, 0,
                  Position.FORWARD, 0,
                  Position.COACH, 0)),
          new SellWeights(2, 1, new BigDecimal("3"), 2, new BigDecimal("0.5"), 10),
          new MutableClock(NOW, ZoneId.of("Europe/Madrid")));

  private static PlayerProfile defender(long id, PlayerStatus status) {
    return new PlayerProfile(
        id, "Player " + id, "player-" + id, 3, Position.DEFENDER, status, Money.euros(2_000_000));
  }

  private static MatchResult match(long week, MatchResult.Outcome outcome) {
    return new MatchResult(week, week, NOW.minus(Duration.ofDays(70 - 7 * week)), true, outcome);
  }

  @Test
  void scoresYourSquadFromStoredHistoryAndCountsInjuryStreaks() {
    PlayerProfile starter = defender(1, PlayerStatus.AVAILABLE);
    PlayerProfile falling = defender(2, PlayerStatus.AVAILABLE);
    PlayerProfile injured = defender(3, PlayerStatus.INJURED);
    when(squad.squad())
        .thenReturn(
            new Squad(
                List.of(
                    new SquadPlayer(starter, List.of(), 0, List.of()),
                    new SquadPlayer(falling, List.of(), 0, List.of()),
                    new SquadPlayer(injured, List.of(), 0, List.of())),
                List.of(99_999L)));
    when(reader.read(List.of(1L, 2L, 3L), PRICES_FROM, Integer.MAX_VALUE))
        .thenReturn(
            new StoredPlayers(
                List.of(starter, falling, injured),
                Map.of(
                    2L,
                    List.of(
                        new DailyPrice(TODAY.minusDays(7), Money.euros(2_000_000)),
                        new DailyPrice(TODAY.minusDays(1), Money.euros(1_900_000)))),
                Map.of(1L, 18, 2L, 10, 3L, 12),
                Map.of(
                    1L,
                    List.of(match(1, new MatchResult.Played(9, 90))),
                    2L,
                    List.of(match(1, new MatchResult.Played(5, 90))),
                    3L,
                    List.of(
                        match(1, new MatchResult.Played(6, 90)),
                        match(2, new MatchResult.Missed(PlayerStatus.INJURED)),
                        match(3, new MatchResult.Missed(PlayerStatus.INJURED))))));

    SellRecommendations sells = advisor.recommendations();

    assertThat(sells.missingPlayerIds()).containsExactly(99_999L);
    // Squad of 3 + 1 unscored, minimum 2: two sales allowed, one defender kept.
    assertThat(sells.recommendations())
        .extracting(s -> s.player().player().id())
        .containsExactly(3L, 2L);
    SellRecommendation injury = sells.recommendations().get(0);
    // Injured, so 0 expected points: also the third of three defenders, past the two kept.
    assertThat(injury.signals()).containsExactly(new LongInjury(2), new Surplus(3, 2));
    SellRecommendation fall = sells.recommendations().get(1);
    // Latest price yesterday: 1,900,000 vs 2,000,000 seven days before it, −5.00 %.
    assertThat(fall.signals()).containsExactly(new FallingValue(new BigDecimal("5.00"), 7));
  }

  @Test
  void letsABiwengerFailureThrough() {
    when(squad.squad()).thenThrow(new BiwengerApiException("Biwenger answered HTTP 503"));

    assertThatThrownBy(advisor::recommendations).isInstanceOf(BiwengerApiException.class);
    verify(reader, never()).read(anyList(), any(), anyInt());
  }
}
