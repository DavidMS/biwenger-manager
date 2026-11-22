package io.github.davidms.biwenger.app.advisor.domain;

import io.github.davidms.biwenger.app.common.Money;
import java.math.BigDecimal;
import java.util.Comparator;
import java.util.Objects;

/**
 * A player's score and every number that produced it, so it can be checked by hand. Decimals are
 * rounded half up to two places, the venue factor to four.
 *
 * <p>{@code expectedPoints = base × venueFactor × statusMultiplier}, where {@code base = formWeight
 * × recentForm + (1 − formWeight) × seasonAverage}.
 *
 * @param recentForm weighted average of the last {@code formMatchesUsed} matches he played or was
 *     fit and left out of (0 points); matches he missed for any other reason don't count
 * @param seasonAverage points per match played, from his match reports
 * @param homeGames matches played at home
 * @param awayGames matches played away
 * @param venueAverage points per match played at {@code venue}; zero when the venue is unknown
 * @param venueFactor {@code (seasonAverage + venueWeight × (venueAverage − seasonAverage)) ÷
 *     seasonAverage}, never below 0; 1 when the venue is unknown, he has too few games at home or
 *     away, or his season average isn't positive
 * @param valueTrend change in value over the last {@code trendDays} days, the latest included:
 *     latest price minus the oldest one in that window
 * @param valueTrendPercent that change as a percentage of the oldest price
 * @param pointsPerMillion season points per million euros of his current value (not of a market
 *     asking price)
 */
public record PlayerScore(
    long playerId,
    BigDecimal expectedPoints,
    BigDecimal recentForm,
    int formMatchesUsed,
    BigDecimal seasonAverage,
    int gamesPlayed,
    BigDecimal base,
    Venue venue,
    int homeGames,
    int awayGames,
    BigDecimal venueAverage,
    BigDecimal venueFactor,
    BigDecimal statusMultiplier,
    Money valueTrend,
    BigDecimal valueTrendPercent,
    int trendDays,
    BigDecimal pointsPerMillion) {

  /** Highest expected points first; ties broken by player id, lowest first (root golden rule 5). */
  public static final Comparator<PlayerScore> BY_EXPECTED_POINTS =
      Comparator.comparing(PlayerScore::expectedPoints)
          .reversed()
          .thenComparingLong(PlayerScore::playerId);

  public PlayerScore {
    Objects.requireNonNull(expectedPoints, "expectedPoints");
    Objects.requireNonNull(recentForm, "recentForm");
    Objects.requireNonNull(seasonAverage, "seasonAverage");
    Objects.requireNonNull(base, "base");
    Objects.requireNonNull(venue, "venue");
    Objects.requireNonNull(venueAverage, "venueAverage");
    Objects.requireNonNull(venueFactor, "venueFactor");
    Objects.requireNonNull(statusMultiplier, "statusMultiplier");
    Objects.requireNonNull(valueTrend, "valueTrend");
    Objects.requireNonNull(valueTrendPercent, "valueTrendPercent");
    Objects.requireNonNull(pointsPerMillion, "pointsPerMillion");
  }
}
