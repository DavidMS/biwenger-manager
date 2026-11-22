package io.github.davidms.biwenger.app.advisor.domain;

import io.github.davidms.biwenger.app.player.domain.PlayerStatus;
import java.math.BigDecimal;
import java.util.Objects;

/**
 * The tunable numbers of the {@link PlayerScorer}. Set from {@code biwenger.advisor.scoring.*} (see
 * {@code ScoringProperties}); {@link #DEFAULTS} are the values chosen in #34.
 *
 * @param formMatches how many recent matches make up recent form. The newest weighs {@code
 *     formMatches}, the next one less, down to 1.
 * @param formWeight share of recent form in the base expectation, between 0 and 1. Season average
 *     gets the rest.
 * @param venueWeight how much of a player's own home/away difference is applied, between 0 and 1. 0
 *     ignores the venue, 1 applies the full difference.
 * @param venueMinGames games he must have played both at home and away before the venue counts
 * @param trendDays the value trend covers this many days, the latest price's day included (as the
 *     market page's trend)
 * @param statusMultipliers what expected points are multiplied by for each status
 */
public record ScoringWeights(
    int formMatches,
    BigDecimal formWeight,
    BigDecimal venueWeight,
    int venueMinGames,
    int trendDays,
    StatusMultipliers statusMultipliers) {

  public static final ScoringWeights DEFAULTS =
      new ScoringWeights(
          5,
          new BigDecimal("0.6"),
          new BigDecimal("0.5"),
          3,
          7,
          new StatusMultipliers(
              BigDecimal.ONE,
              new BigDecimal("0.5"),
              BigDecimal.ZERO,
              BigDecimal.ZERO,
              new BigDecimal("0.5")));

  public ScoringWeights {
    Objects.requireNonNull(formWeight, "formWeight");
    Objects.requireNonNull(venueWeight, "venueWeight");
    Objects.requireNonNull(statusMultipliers, "statusMultipliers");
    if (formMatches < 1) {
      throw new IllegalArgumentException("formMatches must be at least 1: " + formMatches);
    }
    requireFraction("formWeight", formWeight);
    requireFraction("venueWeight", venueWeight);
    if (venueMinGames < 1) {
      throw new IllegalArgumentException("venueMinGames must be at least 1: " + venueMinGames);
    }
    if (trendDays < 1) {
      throw new IllegalArgumentException("trendDays must be at least 1: " + trendDays);
    }
  }

  /** The expected-points multiplier for each status, each between 0 and 1. */
  public record StatusMultipliers(
      BigDecimal available,
      BigDecimal doubtful,
      BigDecimal injured,
      BigDecimal suspended,
      BigDecimal unknown) {

    public StatusMultipliers {
      requireFraction("available", available);
      requireFraction("doubtful", doubtful);
      requireFraction("injured", injured);
      requireFraction("suspended", suspended);
      requireFraction("unknown", unknown);
    }

    public BigDecimal of(PlayerStatus status) {
      return switch (status) {
        case AVAILABLE -> available;
        case DOUBTFUL -> doubtful;
        case INJURED -> injured;
        case SUSPENDED -> suspended;
        case UNKNOWN -> unknown;
      };
    }
  }

  private static void requireFraction(String name, BigDecimal value) {
    Objects.requireNonNull(value, name);
    if (value.signum() < 0 || value.compareTo(BigDecimal.ONE) > 0) {
      throw new IllegalArgumentException(name + " must be between 0 and 1: " + value);
    }
  }
}
