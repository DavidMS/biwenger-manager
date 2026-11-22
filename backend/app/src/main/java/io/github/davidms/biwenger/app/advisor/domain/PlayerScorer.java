package io.github.davidms.biwenger.app.advisor.domain;

import io.github.davidms.biwenger.app.common.Money;
import io.github.davidms.biwenger.app.player.domain.DailyPrice;
import io.github.davidms.biwenger.app.player.domain.MatchResult;
import io.github.davidms.biwenger.app.player.domain.MatchResult.Missed;
import io.github.davidms.biwenger.app.player.domain.MatchResult.Played;
import io.github.davidms.biwenger.app.player.domain.PlayerStatus;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import java.util.function.Predicate;

/**
 * The player scoring model (#34, ADR 0006): expected points for the next match, value trend and
 * points per million. Pure and deterministic: no I/O, no clock, no floating point. Every step works
 * on the rounded numbers the {@link PlayerScore} reports, so a score can be recomputed by hand from
 * them.
 *
 * <ol>
 *   <li>Recent form: the last {@code formMatches} matches he played or was fit and left out of,
 *       newest first, weighted {@code formMatches}, {@code formMatches − 1}, … A match he was left
 *       out of counts as 0 points. One he missed for any other reason (injured, doubtful, suspended
 *       or a status we don't know) is skipped, because the status multiplier already covers his
 *       availability.
 *   <li>Season average: points per match played.
 *   <li>Base: {@code formWeight × form + (1 − formWeight) × seasonAverage}.
 *   <li>Venue factor: {@code (seasonAverage + venueWeight × (venueAverage − seasonAverage)) ÷
 *       seasonAverage}, never below 0. 1 when the venue is unknown, he has played fewer than {@code
 *       venueMinGames} at home or away, or his season average isn't positive.
 *   <li>Expected points: {@code base × venueFactor × statusMultiplier}.
 * </ol>
 */
public final class PlayerScorer {

  private static final int SCALE = 2;
  private static final int FACTOR_SCALE = 4;
  private static final BigDecimal HUNDRED = BigDecimal.valueOf(100);
  private static final BigDecimal MILLION = BigDecimal.valueOf(1_000_000);

  private PlayerScorer() {}

  public static PlayerScore score(ScoringInput input, ScoringWeights weights) {
    List<Integer> form = recentForm(input.matches(), weights.formMatches());
    BigDecimal recentForm = weightedAverage(form, weights.formMatches());

    List<Played> played = played(input.matches(), match -> true);
    BigDecimal seasonAverage = average(played);

    // With no form there is no match played either, so both terms are zero.
    BigDecimal base =
        round(
            weights
                .formWeight()
                .multiply(recentForm)
                .add(BigDecimal.ONE.subtract(weights.formWeight()).multiply(seasonAverage)));

    List<Played> home = played(input.matches(), MatchResult::home);
    List<Played> away = played(input.matches(), match -> !match.home());
    BigDecimal venueAverage =
        switch (input.nextVenue()) {
          case HOME -> average(home);
          case AWAY -> average(away);
          case UNKNOWN -> zero();
        };
    boolean venueCounts =
        input.nextVenue() != Venue.UNKNOWN
            && seasonAverage.signum() > 0
            && home.size() >= weights.venueMinGames()
            && away.size() >= weights.venueMinGames();
    BigDecimal venueFactor =
        venueCounts
            ? venueFactor(venueAverage, seasonAverage, weights.venueWeight())
            : BigDecimal.ONE.setScale(FACTOR_SCALE);

    BigDecimal statusMultiplier = weights.statusMultipliers().of(input.player().status());
    BigDecimal expectedPoints = round(base.multiply(venueFactor).multiply(statusMultiplier));

    List<DailyPrice> window = trendWindow(input.prices(), weights.trendDays());
    Money valueTrend = DailyPrice.change(window);
    BigDecimal valueTrendPercent =
        window.size() < 2 ? zero() : percent(valueTrend, window.getFirst().price());

    return new PlayerScore(
        input.player().id(),
        expectedPoints,
        recentForm,
        form.size(),
        seasonAverage,
        played.size(),
        base,
        input.nextVenue(),
        home.size(),
        away.size(),
        venueAverage,
        venueFactor,
        statusMultiplier,
        valueTrend,
        valueTrendPercent,
        weights.trendDays(),
        pointsPerMillion(input.seasonPoints(), input.player().price()));
  }

  /** Points of the matches that count as form, newest first: 0 for a fit player left out. */
  private static List<Integer> recentForm(List<MatchResult> matches, int limit) {
    List<Integer> form = new ArrayList<>();
    for (MatchResult match : matches.reversed()) {
      if (form.size() == limit) {
        break;
      }
      switch (match.outcome()) {
        case Played p -> form.add(p.points());
        case Missed m when m.reason() == PlayerStatus.AVAILABLE -> form.add(0);
        case Missed m -> {
          // Not available: no sign of form either way, and the status multiplier covers it.
        }
      }
    }
    return form;
  }

  /** Newest weighs {@code topWeight}, each older match one less. */
  private static BigDecimal weightedAverage(List<Integer> newestFirst, int topWeight) {
    if (newestFirst.isEmpty()) {
      return zero();
    }
    long sum = 0;
    long totalWeight = 0;
    for (int i = 0; i < newestFirst.size(); i++) {
      int weight = topWeight - i;
      sum += (long) weight * newestFirst.get(i);
      totalWeight += weight;
    }
    return divide(sum, totalWeight);
  }

  /** One division, so the factor is the formula's value rounded once. */
  private static BigDecimal venueFactor(
      BigDecimal venueAverage, BigDecimal seasonAverage, BigDecimal venueWeight) {
    BigDecimal factor =
        seasonAverage
            .add(venueWeight.multiply(venueAverage.subtract(seasonAverage)))
            .divide(seasonAverage, FACTOR_SCALE, RoundingMode.HALF_UP);
    return factor.signum() < 0 ? BigDecimal.ZERO.setScale(FACTOR_SCALE) : factor;
  }

  /** The outcomes of the matches he played that pass {@code which}. */
  private static List<Played> played(List<MatchResult> matches, Predicate<MatchResult> which) {
    List<Played> played = new ArrayList<>();
    for (MatchResult match : matches) {
      if (match.outcome() instanceof Played p && which.test(match)) {
        played.add(p);
      }
    }
    return played;
  }

  private static BigDecimal average(List<Played> played) {
    if (played.isEmpty()) {
      return zero();
    }
    long sum = played.stream().mapToLong(Played::points).sum();
    return divide(sum, played.size());
  }

  /** Prices of the last {@code days} days, the latest included (as the market's trend). */
  private static List<DailyPrice> trendWindow(List<DailyPrice> oldestFirst, int days) {
    if (oldestFirst.isEmpty()) {
      return List.of();
    }
    LocalDate from = oldestFirst.getLast().day().minusDays(days - 1L);
    return oldestFirst.stream().filter(p -> !p.day().isBefore(from)).toList();
  }

  private static BigDecimal percent(Money change, Money of) {
    if (of.euros() == 0) {
      return zero();
    }
    return BigDecimal.valueOf(change.euros())
        .multiply(HUNDRED)
        .divide(BigDecimal.valueOf(of.euros()), SCALE, RoundingMode.HALF_UP);
  }

  /** The market page's formula, over his current value: zero at no value. */
  private static BigDecimal pointsPerMillion(int seasonPoints, Money price) {
    if (price.euros() == 0) {
      return zero();
    }
    return BigDecimal.valueOf(seasonPoints)
        .multiply(MILLION)
        .divide(BigDecimal.valueOf(price.euros()), SCALE, RoundingMode.HALF_UP);
  }

  private static BigDecimal divide(long dividend, long divisor) {
    return BigDecimal.valueOf(dividend)
        .divide(BigDecimal.valueOf(divisor), SCALE, RoundingMode.HALF_UP);
  }

  private static BigDecimal round(BigDecimal value) {
    return value.setScale(SCALE, RoundingMode.HALF_UP);
  }

  private static BigDecimal zero() {
    return BigDecimal.ZERO.setScale(SCALE);
  }
}
