package io.github.davidms.biwenger.app.advisor.domain;

import java.math.BigDecimal;
import java.util.Objects;

/** One reason to sell a player, with the numbers behind it. */
public sealed interface SellSignal {

  /** His value fell by {@code percent} (a positive number) over the last {@code days} days. */
  record FallingValue(BigDecimal percent, int days) implements SellSignal {

    public FallingValue {
      Objects.requireNonNull(percent, "percent");
    }
  }

  /** Injured now, and missed his latest {@code matchesMissed} matches in a row through injury. */
  record LongInjury(int matchesMissed) implements SellSignal {}

  /** Season points per million of value under {@code threshold}, a share of the squad's median. */
  record LowValueForMoney(BigDecimal pointsPerMillion, BigDecimal squadMedian, BigDecimal threshold)
      implements SellSignal {

    public LowValueForMoney {
      Objects.requireNonNull(pointsPerMillion, "pointsPerMillion");
      Objects.requireNonNull(squadMedian, "squadMedian");
      Objects.requireNonNull(threshold, "threshold");
    }
  }

  /**
   * He is number {@code depth} (from 1) in his position, behind the {@code kept} players you keep
   * there: the formation's starters plus backups.
   */
  record Surplus(int depth, int kept) implements SellSignal {}
}
