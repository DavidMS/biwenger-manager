package io.github.davidms.biwenger.app.advisor.domain;

import io.github.davidms.biwenger.app.common.Money;
import java.math.BigDecimal;
import java.util.Objects;

/**
 * A market player worth buying, with the numbers that put him here.
 *
 * @param candidate the player on sale and his score
 * @param askingPrice what the listing asks
 * @param slot the starter he would replace in your XI, or an empty slot
 * @param gain his expected points minus the replaced starter's (minus 0 for an empty slot)
 * @param gainPerMillion gain per million euros of asking price, the first tie-breaker
 * @param budgetLeft your balance after paying the asking price
 * @param explanation the numbers above in one readable sentence
 */
public record BuyRecommendation(
    ScoredPlayer candidate,
    Money askingPrice,
    Slot slot,
    BigDecimal gain,
    BigDecimal gainPerMillion,
    Money budgetLeft,
    String explanation) {

  public BuyRecommendation {
    Objects.requireNonNull(candidate, "candidate");
    Objects.requireNonNull(askingPrice, "askingPrice");
    Objects.requireNonNull(slot, "slot");
    Objects.requireNonNull(gain, "gain");
    Objects.requireNonNull(gainPerMillion, "gainPerMillion");
    Objects.requireNonNull(budgetLeft, "budgetLeft");
    Objects.requireNonNull(explanation, "explanation");
  }

  /** Where he would go in your XI. */
  public sealed interface Slot {}

  /** You have fewer starters in his position than the reference formation needs. */
  public record EmptySlot() implements Slot {}

  /** He would replace your weakest starter in his position. */
  public record Replaces(ScoredPlayer starter) implements Slot {

    public Replaces {
      Objects.requireNonNull(starter, "starter");
    }
  }
}
