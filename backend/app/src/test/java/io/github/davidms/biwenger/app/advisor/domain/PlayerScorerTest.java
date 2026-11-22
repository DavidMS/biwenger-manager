package io.github.davidms.biwenger.app.advisor.domain;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import io.github.davidms.biwenger.app.common.Money;
import io.github.davidms.biwenger.app.player.domain.DailyPrice;
import io.github.davidms.biwenger.app.player.domain.MatchResult;
import io.github.davidms.biwenger.app.player.domain.MatchResult.Missed;
import io.github.davidms.biwenger.app.player.domain.MatchResult.Played;
import io.github.davidms.biwenger.app.player.domain.PlayerProfile;
import io.github.davidms.biwenger.app.player.domain.PlayerStatus;
import io.github.davidms.biwenger.app.player.domain.Position;
import java.math.BigDecimal;
import java.time.Duration;
import java.time.Instant;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import org.junit.jupiter.api.Test;

class PlayerScorerTest {

  private static final Instant SEASON_START = Instant.parse("2026-08-15T19:00:00Z");
  private static final LocalDate FIRST_DAY = LocalDate.of(2026, 11, 1);
  private static final ScoringWeights WEIGHTS = ScoringWeights.DEFAULTS;

  private static PlayerProfile player(long id, PlayerStatus status, long price) {
    return new PlayerProfile(
        id, "Player " + id, "player-" + id, 3, Position.MIDFIELDER, status, Money.euros(price));
  }

  /** Matches one week apart, oldest first, each home or away as its fixture says. */
  private static List<MatchResult> matches(Fixture... fixtures) {
    List<MatchResult> matches = new ArrayList<>();
    for (int i = 0; i < fixtures.length; i++) {
      matches.add(
          new MatchResult(
              100 + i,
              10 + i,
              SEASON_START.plus(Duration.ofDays(7L * i)),
              fixtures[i].home(),
              fixtures[i].outcome()));
    }
    return matches;
  }

  private record Fixture(boolean home, MatchResult.Outcome outcome) {}

  private static Fixture home(int points) {
    return new Fixture(true, new Played(points, 90));
  }

  private static Fixture away(int points) {
    return new Fixture(false, new Played(points, 90));
  }

  private static Fixture missed(PlayerStatus reason) {
    return new Fixture(true, new Missed(reason));
  }

  private static ScoringInput input(List<MatchResult> matches) {
    return input(PlayerStatus.AVAILABLE, matches, Venue.UNKNOWN);
  }

  private static ScoringInput input(
      PlayerStatus status, List<MatchResult> matches, Venue nextVenue) {
    return new ScoringInput(player(7, status, 5_000_000), List.of(), 30, matches, nextVenue);
  }

  private static BigDecimal decimal(String value) {
    return new BigDecimal(value);
  }

  @Test
  void weightsRecentFormByRecencyAndMixesInTheSeasonAverage() {
    PlayerScore score =
        PlayerScorer.score(input(matches(home(2), away(4), home(6), away(8), home(10))), WEIGHTS);

    // Form, newest first: (5×10 + 4×8 + 3×6 + 2×4 + 1×2) / 15 = 110 / 15 = 7.33
    assertThat(score.recentForm()).isEqualTo(decimal("7.33"));
    assertThat(score.formMatchesUsed()).isEqualTo(5);
    // Season: 30 / 5 = 6.00
    assertThat(score.seasonAverage()).isEqualTo(decimal("6.00"));
    assertThat(score.gamesPlayed()).isEqualTo(5);
    // Base: 0.6 × 7.33 + 0.4 × 6.00 = 6.798 → 6.80
    assertThat(score.base()).isEqualTo(decimal("6.80"));
    assertThat(score.venueFactor()).isEqualTo(decimal("1.0000"));
    assertThat(score.statusMultiplier()).isEqualTo(BigDecimal.ONE);
    assertThat(score.expectedPoints()).isEqualTo(decimal("6.80"));
  }

  @Test
  void formUsesOnlyTheLastMatchesButTheSeasonAverageUsesThemAll() {
    PlayerScore score =
        PlayerScorer.score(
            input(matches(home(0), away(0), home(2), away(4), home(6), away(8), home(10))),
            WEIGHTS);

    assertThat(score.recentForm()).isEqualTo(decimal("7.33"));
    assertThat(score.formMatchesUsed()).isEqualTo(5);
    // Season: 30 / 7 = 4.29. Base: 0.6 × 7.33 + 0.4 × 4.29 = 6.114 → 6.11
    assertThat(score.seasonAverage()).isEqualTo(decimal("4.29"));
    assertThat(score.base()).isEqualTo(decimal("6.11"));
  }

  @Test
  void countsAFitPlayerLeftOutAsZeroAndSkipsInjuries() {
    PlayerScore score =
        PlayerScorer.score(
            input(
                matches(
                    home(6),
                    missed(PlayerStatus.INJURED),
                    missed(PlayerStatus.AVAILABLE),
                    away(9))),
            WEIGHTS);

    // Form, newest first: 9, 0 (left out), 6; the injury is skipped. (45 + 0 + 18) / 12 = 5.25
    assertThat(score.recentForm()).isEqualTo(decimal("5.25"));
    assertThat(score.formMatchesUsed()).isEqualTo(3);
    // Season: 15 / 2 = 7.50. Base: 0.6 × 5.25 + 0.4 × 7.50 = 6.15
    assertThat(score.seasonAverage()).isEqualTo(decimal("7.50"));
    assertThat(score.gamesPlayed()).isEqualTo(2);
    assertThat(score.base()).isEqualTo(decimal("6.15"));
  }

  @Test
  void scoresZeroWithNoMatches() {
    PlayerScore score = PlayerScorer.score(input(List.of()), WEIGHTS);

    assertThat(score.recentForm()).isEqualTo(decimal("0.00"));
    assertThat(score.formMatchesUsed()).isZero();
    assertThat(score.seasonAverage()).isEqualTo(decimal("0.00"));
    assertThat(score.gamesPlayed()).isZero();
    assertThat(score.expectedPoints()).isEqualTo(decimal("0.00"));
  }

  @Test
  void appliesTheStatusMultiplier() {
    List<MatchResult> matches = matches(home(2), away(4), home(6), away(8), home(10));

    assertThat(
            PlayerScorer.score(input(PlayerStatus.DOUBTFUL, matches, Venue.UNKNOWN), WEIGHTS)
                .expectedPoints())
        .isEqualTo(decimal("3.40"));
    assertThat(
            PlayerScorer.score(input(PlayerStatus.INJURED, matches, Venue.UNKNOWN), WEIGHTS)
                .expectedPoints())
        .isEqualTo(decimal("0.00"));
    assertThat(
            PlayerScorer.score(input(PlayerStatus.SUSPENDED, matches, Venue.UNKNOWN), WEIGHTS)
                .statusMultiplier())
        .isEqualTo(BigDecimal.ZERO);
    assertThat(
            PlayerScorer.score(input(PlayerStatus.UNKNOWN, matches, Venue.UNKNOWN), WEIGHTS)
                .expectedPoints())
        .isEqualTo(decimal("3.40"));
  }

  @Test
  void adjustsForTheNextVenueByHisOwnHomeAwaySplit() {
    List<MatchResult> matches = matches(home(9), home(9), home(9), away(3), away(3), away(3));

    // Form: (15 + 12 + 9 + 18 + 9) / 15 = 4.20. Season: 36 / 6 = 6.00.
    // Base: 0.6 × 4.20 + 0.4 × 6.00 = 4.92
    PlayerScore atHome =
        PlayerScorer.score(input(PlayerStatus.AVAILABLE, matches, Venue.HOME), WEIGHTS);
    assertThat(atHome.base()).isEqualTo(decimal("4.92"));
    // Home: (6.00 + 0.5 × (9.00 − 6.00)) / 6.00 = 1.25, so 4.92 × 1.25 = 6.15
    assertThat(atHome.venue()).isEqualTo(Venue.HOME);
    assertThat(atHome.homeGames()).isEqualTo(3);
    assertThat(atHome.awayGames()).isEqualTo(3);
    assertThat(atHome.venueAverage()).isEqualTo(decimal("9.00"));
    assertThat(atHome.venueFactor()).isEqualTo(decimal("1.2500"));
    assertThat(atHome.expectedPoints()).isEqualTo(decimal("6.15"));

    // Away: (6.00 + 0.5 × (3.00 − 6.00)) / 6.00 = 0.75, so 4.92 × 0.75 = 3.69
    PlayerScore away =
        PlayerScorer.score(input(PlayerStatus.AVAILABLE, matches, Venue.AWAY), WEIGHTS);
    assertThat(away.venueAverage()).isEqualTo(decimal("3.00"));
    assertThat(away.venueFactor()).isEqualTo(decimal("0.7500"));
    assertThat(away.expectedPoints()).isEqualTo(decimal("3.69"));

    PlayerScore unknown =
        PlayerScorer.score(input(PlayerStatus.AVAILABLE, matches, Venue.UNKNOWN), WEIGHTS);
    assertThat(unknown.venueAverage()).isEqualTo(decimal("0.00"));
    assertThat(unknown.venueFactor()).isEqualTo(decimal("1.0000"));
    assertThat(unknown.expectedPoints()).isEqualTo(decimal("4.92"));
  }

  @Test
  void roundsTheVenueFactorOnce() {
    List<MatchResult> matches = matches(home(7), home(7), home(7), away(5), away(5), away(5));

    PlayerScore score =
        PlayerScorer.score(input(PlayerStatus.AVAILABLE, matches, Venue.HOME), WEIGHTS);

    // (6.00 + 0.5 × (7.00 − 6.00)) / 6.00 = 1.08333… → 1.0833 (rounding the ratio 7/6 first
    // would give 1.0834). Form: (25 + 20 + 15 + 14 + 7) / 15 = 5.40, base 0.6 × 5.40 + 0.4 × 6.00
    // = 5.64, expected 5.64 × 1.0833 = 6.1098 → 6.11
    assertThat(score.venueFactor()).isEqualTo(decimal("1.0833"));
    assertThat(score.base()).isEqualTo(decimal("5.64"));
    assertThat(score.expectedPoints()).isEqualTo(decimal("6.11"));
  }

  @Test
  void ignoresTheVenueAndKeepsNegativeExpectationsWhenTheSeasonAverageIsNotPositive() {
    List<MatchResult> matches = matches(home(-1), home(-1), home(-1), away(-1), away(-1), away(-1));

    PlayerScore score =
        PlayerScorer.score(input(PlayerStatus.AVAILABLE, matches, Venue.HOME), WEIGHTS);

    assertThat(score.venueFactor()).isEqualTo(decimal("1.0000"));
    assertThat(score.base()).isEqualTo(decimal("-1.00"));
    assertThat(score.expectedPoints()).isEqualTo(decimal("-1.00"));
  }

  @Test
  void skipsMatchesMissedForAnyReasonButBeingLeftOut() {
    PlayerScore score =
        PlayerScorer.score(
            input(
                matches(
                    away(4),
                    missed(PlayerStatus.DOUBTFUL),
                    missed(PlayerStatus.SUSPENDED),
                    missed(PlayerStatus.UNKNOWN))),
            WEIGHTS);

    assertThat(score.recentForm()).isEqualTo(decimal("4.00"));
    assertThat(score.formMatchesUsed()).isEqualTo(1);
  }

  @Test
  void roundsHalfUp() {
    // Season: 1 / 8 = 0.125 → 0.13 (half-even would give 0.12)
    PlayerScore score =
        PlayerScorer.score(
            input(matches(home(1), home(0), home(0), home(0), away(0), away(0), away(0), away(0))),
            WEIGHTS);

    assertThat(score.seasonAverage()).isEqualTo(decimal("0.13"));
  }

  @Test
  void ignoresTheVenueWithTooFewGamesOnOneSide() {
    List<MatchResult> matches = matches(home(9), home(9), home(9), away(3), away(3));

    PlayerScore score =
        PlayerScorer.score(input(PlayerStatus.AVAILABLE, matches, Venue.HOME), WEIGHTS);

    assertThat(score.venueFactor()).isEqualTo(decimal("1.0000"));
  }

  @Test
  void neverLetsTheVenueFactorGoBelowZero() {
    ScoringWeights fullVenue =
        new ScoringWeights(
            5,
            WEIGHTS.formWeight(),
            BigDecimal.ONE,
            3,
            WEIGHTS.trendDays(),
            WEIGHTS.statusMultipliers());
    List<MatchResult> matches = matches(home(-2), home(-2), home(-2), away(10), away(10), away(10));

    // Season 4.00, home −2.00: (4.00 + 1 × (−2.00 − 4.00)) / 4.00 = −0.5, floored at 0
    PlayerScore score =
        PlayerScorer.score(input(PlayerStatus.AVAILABLE, matches, Venue.HOME), fullVenue);

    assertThat(score.venueFactor()).isEqualTo(decimal("0.0000"));
    assertThat(score.expectedPoints()).isEqualTo(decimal("0.00"));
  }

  @Test
  void measuresTheValueTrendOverTheLastDaysInAnyInputOrder() {
    List<DailyPrice> prices = new ArrayList<>();
    for (int day = 10; day >= 1; day--) {
      prices.add(
          new DailyPrice(FIRST_DAY.plusDays(day - 1), Money.euros(1_000_000 + day * 10_000)));
    }

    PlayerScore score =
        PlayerScorer.score(
            new ScoringInput(
                player(7, PlayerStatus.AVAILABLE, 1_100_000), prices, 0, List.of(), Venue.UNKNOWN),
            WEIGHTS);

    // 7 days with day 10 included start at day 4: 1,100,000 − 1,040,000 = 60,000, 5.77 % of it
    assertThat(score.valueTrend()).isEqualTo(Money.euros(60_000));
    assertThat(score.valueTrendPercent()).isEqualTo(decimal("5.77"));
    assertThat(score.trendDays()).isEqualTo(7);
  }

  @Test
  void hasNoValueTrendWithASinglePrice() {
    PlayerScore score =
        PlayerScorer.score(
            new ScoringInput(
                player(7, PlayerStatus.AVAILABLE, 1_000_000),
                List.of(new DailyPrice(FIRST_DAY, Money.euros(1_000_000))),
                0,
                List.of(),
                Venue.UNKNOWN),
            WEIGHTS);

    assertThat(score.valueTrend()).isEqualTo(Money.ZERO);
    assertThat(score.valueTrendPercent()).isEqualTo(decimal("0.00"));
  }

  @Test
  void hasNoTrendPercentageWhenTheOldestPriceIsZero() {
    PlayerScore score =
        PlayerScorer.score(
            new ScoringInput(
                player(7, PlayerStatus.AVAILABLE, 100_000),
                List.of(
                    new DailyPrice(FIRST_DAY, Money.ZERO),
                    new DailyPrice(FIRST_DAY.plusDays(1), Money.euros(100_000))),
                0,
                List.of(),
                Venue.UNKNOWN),
            WEIGHTS);

    assertThat(score.valueTrend()).isEqualTo(Money.euros(100_000));
    assertThat(score.valueTrendPercent()).isEqualTo(decimal("0.00"));
  }

  @Test
  void rejectsTwoPricesOnTheSameDay() {
    List<DailyPrice> prices =
        List.of(
            new DailyPrice(FIRST_DAY, Money.euros(1_000_000)),
            new DailyPrice(FIRST_DAY, Money.euros(1_100_000)));

    assertThatThrownBy(
            () ->
                new ScoringInput(
                    player(7, PlayerStatus.AVAILABLE, 1), prices, 0, List.of(), Venue.UNKNOWN))
        .isInstanceOf(IllegalArgumentException.class)
        .hasMessageContaining("Two prices for player 7");
  }

  @Test
  void dividesSeasonPointsByMillionsOfValue() {
    assertThat(PlayerScorer.score(input(List.of()), WEIGHTS).pointsPerMillion())
        .isEqualTo(decimal("6.00"));

    PlayerScore free =
        PlayerScorer.score(
            new ScoringInput(
                player(7, PlayerStatus.AVAILABLE, 0), List.of(), 30, List.of(), Venue.UNKNOWN),
            WEIGHTS);
    assertThat(free.pointsPerMillion()).isEqualTo(decimal("0.00"));
  }

  @Test
  void givesTheSameScoreWhateverTheOrderOfTheMatches() {
    List<MatchResult> matches =
        matches(
            home(9), missed(PlayerStatus.AVAILABLE), away(3), home(7), away(1), home(5), away(6));

    PlayerScore oldestFirst =
        PlayerScorer.score(input(PlayerStatus.AVAILABLE, matches, Venue.HOME), WEIGHTS);
    PlayerScore latestFirst =
        PlayerScorer.score(input(PlayerStatus.AVAILABLE, matches.reversed(), Venue.HOME), WEIGHTS);

    assertThat(oldestFirst.venueFactor()).isNotEqualTo(decimal("1.0000"));
    assertThat(latestFirst).isEqualTo(oldestFirst);
  }

  @Test
  void breaksExpectedPointsTiesByPlayerId() {
    List<MatchResult> matches = matches(home(5));
    PlayerScore high =
        PlayerScorer.score(
            new ScoringInput(
                player(30, PlayerStatus.AVAILABLE, 1), List.of(), 0, matches(home(9)), Venue.HOME),
            WEIGHTS);
    PlayerScore tiedHigherId =
        PlayerScorer.score(
            new ScoringInput(
                player(20, PlayerStatus.AVAILABLE, 1), List.of(), 0, matches, Venue.HOME),
            WEIGHTS);
    PlayerScore tiedLowerId =
        PlayerScorer.score(
            new ScoringInput(
                player(10, PlayerStatus.AVAILABLE, 1), List.of(), 0, matches, Venue.HOME),
            WEIGHTS);

    List<PlayerScore> ranked =
        List.of(tiedHigherId, high, tiedLowerId).stream()
            .sorted(PlayerScore.BY_EXPECTED_POINTS)
            .toList();

    assertThat(ranked).extracting(PlayerScore::playerId).containsExactly(30L, 10L, 20L);
  }
}
