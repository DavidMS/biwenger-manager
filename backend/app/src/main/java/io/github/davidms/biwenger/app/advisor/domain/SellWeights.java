package io.github.davidms.biwenger.app.advisor.domain;

import java.math.BigDecimal;
import java.util.Objects;

/**
 * The tunable numbers of the {@link SellAdvisor}, set from {@code biwenger.advisor.sell.*}. {@link
 * #DEFAULTS} are the values chosen in #36.
 *
 * @param minSquadSize the squad never drops below this many players after the suggested sales
 * @param backups players kept per position beyond the formation's starters; anyone deeper is
 *     surplus
 * @param fallingPercent value falling by this percentage or more over the trend is a signal
 * @param injuryMatches an injured player who missed at least this many of his latest matches in a
 *     row through injury is out long-term
 * @param lowValueShare season points per million below this share of the squad's median is low
 *     value for money, between 0 and 1
 * @param limit the most recommendations returned
 */
public record SellWeights(
    int minSquadSize,
    int backups,
    BigDecimal fallingPercent,
    int injuryMatches,
    BigDecimal lowValueShare,
    int limit) {

  public static final SellWeights DEFAULTS =
      new SellWeights(15, 1, new BigDecimal("3"), 2, new BigDecimal("0.5"), 10);

  public SellWeights {
    Objects.requireNonNull(fallingPercent, "fallingPercent");
    Objects.requireNonNull(lowValueShare, "lowValueShare");
    if (minSquadSize < 0) {
      throw new IllegalArgumentException("minSquadSize can't be negative: " + minSquadSize);
    }
    if (backups < 0) {
      throw new IllegalArgumentException("backups can't be negative: " + backups);
    }
    if (fallingPercent.signum() <= 0) {
      throw new IllegalArgumentException("fallingPercent must be positive: " + fallingPercent);
    }
    if (injuryMatches < 1) {
      throw new IllegalArgumentException("injuryMatches must be at least 1: " + injuryMatches);
    }
    if (lowValueShare.signum() < 0 || lowValueShare.compareTo(BigDecimal.ONE) > 0) {
      throw new IllegalArgumentException("lowValueShare must be between 0 and 1: " + lowValueShare);
    }
    if (limit < 1) {
      throw new IllegalArgumentException("limit must be at least 1: " + limit);
    }
  }
}
