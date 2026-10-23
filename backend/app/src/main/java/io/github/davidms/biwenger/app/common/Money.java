package io.github.davidms.biwenger.app.common;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.Collection;

/**
 * An amount of money in whole euros, as Biwenger counts it. A type of its own so that a price can't
 * be passed where an id is expected (both would otherwise be {@code long}). Arithmetic fails on
 * overflow instead of wrapping around.
 */
public record Money(long euros) implements Comparable<Money> {

  public static final Money ZERO = new Money(0);

  public static Money euros(long euros) {
    return new Money(euros);
  }

  public Money plus(Money other) {
    return new Money(Math.addExact(euros, other.euros));
  }

  public Money minus(Money other) {
    return new Money(Math.subtractExact(euros, other.euros));
  }

  public Money times(long factor) {
    return new Money(Math.multiplyExact(euros, factor));
  }

  /**
   * This amount's {@code percent} %, rounded to whole euros as {@code rounding} says. Exact and
   * deterministic: no floating point.
   */
  public Money percent(long percent, RoundingMode rounding) {
    return new Money(
        BigDecimal.valueOf(euros)
            .multiply(BigDecimal.valueOf(percent))
            .divide(BigDecimal.valueOf(100), 0, rounding)
            .longValueExact());
  }

  public static Money sum(Collection<Money> amounts) {
    Money total = ZERO;
    for (Money amount : amounts) {
      total = total.plus(amount);
    }
    return total;
  }

  public boolean isNegative() {
    return euros < 0;
  }

  @Override
  public int compareTo(Money other) {
    return Long.compare(euros, other.euros);
  }

  @Override
  public String toString() {
    return euros + " €";
  }
}
