package io.github.davidms.biwenger.app.advisor.domain;

import java.math.BigDecimal;
import java.util.List;
import java.util.Objects;

/**
 * One of your players worth selling, with the numbers that put him here.
 *
 * @param player your player and his score
 * @param signals why to sell him, at least one
 * @param cover who would start instead if he is a starter
 * @param xiLoss expected points your XI loses: his minus his cover's, or 0 when he doesn't start
 * @param explanation the numbers above in one readable sentence
 */
public record SellRecommendation(
    ScoredPlayer player,
    List<SellSignal> signals,
    Cover cover,
    BigDecimal xiLoss,
    String explanation) {

  public SellRecommendation {
    Objects.requireNonNull(player, "player");
    Objects.requireNonNull(cover, "cover");
    Objects.requireNonNull(xiLoss, "xiLoss");
    Objects.requireNonNull(explanation, "explanation");
    signals = List.copyOf(signals);
    if (signals.isEmpty()) {
      throw new IllegalArgumentException("A sell recommendation needs at least one signal");
    }
  }

  /** What happens to your XI without him. */
  public sealed interface Cover {}

  /** He isn't one of your starters, so the XI doesn't change. */
  public record NotAStarter() implements Cover {}

  /** Your best player outside the XI in his position would start instead. */
  public record CoveredBy(ScoredPlayer player) implements Cover {

    public CoveredBy {
      Objects.requireNonNull(player, "player");
    }
  }
}
