package io.github.davidms.biwenger.app.advisor.domain;

import io.github.davidms.biwenger.app.advisor.domain.BuyRecommendation.EmptySlot;
import io.github.davidms.biwenger.app.advisor.domain.BuyRecommendation.Replaces;
import io.github.davidms.biwenger.app.advisor.domain.BuyRecommendation.Slot;
import io.github.davidms.biwenger.app.common.Money;
import io.github.davidms.biwenger.app.player.domain.PlayerStatus;
import io.github.davidms.biwenger.app.player.domain.Position;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.Comparator;
import java.util.List;
import java.util.Locale;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * Buy recommendations (#35, ADR 0007). Pure and deterministic, like the {@link PlayerScorer} it
 * builds on.
 *
 * <ol>
 *   <li>Your starters in a position are your best {@code starters} players there by expected points
 *       (ties by player id), as the {@link ReferenceFormation} says.
 *   <li>A candidate's gain is his expected points minus your weakest starter's in his position, or
 *       minus 0 when you have fewer starters than the formation needs. So a weak line gives big
 *       gains and rises on its own.
 *   <li>Only players you don't own, whose asking price fits in your balance, in a position with
 *       starters, with a positive gain.
 *   <li>Highest gain first, then highest gain per million of asking price, then lowest player id;
 *       the first {@code limit}. A listing at 0 € (Biwenger doesn't make them) has 0 gain per
 *       million, so it would rank last among equal gains.
 * </ol>
 */
public final class BuyAdvisor {

  private static final int SCALE = 2;
  private static final BigDecimal MILLION = BigDecimal.valueOf(1_000_000);

  static final Comparator<BuyRecommendation> RANKING =
      Comparator.comparing(BuyRecommendation::gain)
          .thenComparing(BuyRecommendation::gainPerMillion)
          .reversed()
          .thenComparingLong(r -> r.candidate().player().id());

  private BuyAdvisor() {}

  public static List<BuyRecommendation> recommend(
      List<BuyCandidate> market,
      List<ScoredPlayer> squad,
      Money balance,
      ReferenceFormation formation,
      BuyWeights weights) {
    Set<Long> owned = squad.stream().map(s -> s.player().id()).collect(Collectors.toSet());
    return market.stream()
        .filter(candidate -> !owned.contains(candidate.player().player().id()))
        .filter(candidate -> candidate.askingPrice().compareTo(balance) <= 0)
        .filter(candidate -> formation.startersAt(position(candidate)) > 0)
        .map(candidate -> recommendation(candidate, squad, balance, formation))
        .filter(recommendation -> recommendation.gain().signum() > 0)
        .sorted(RANKING)
        .limit(weights.limit())
        .toList();
  }

  private static BuyRecommendation recommendation(
      BuyCandidate candidate,
      List<ScoredPlayer> squad,
      Money balance,
      ReferenceFormation formation) {
    Position position = position(candidate);
    int needed = formation.startersAt(position);
    List<ScoredPlayer> starters =
        ReferenceFormation.depthChart(squad, position).stream().limit(needed).toList();
    Slot slot = starters.size() < needed ? new EmptySlot() : new Replaces(starters.getLast());
    BigDecimal expected = candidate.player().score().expectedPoints();
    BigDecimal gain =
        switch (slot) {
          case EmptySlot _ -> expected;
          case Replaces(var starter) -> expected.subtract(starter.score().expectedPoints());
        };
    BigDecimal gainPerMillion =
        candidate.askingPrice().euros() == 0
            ? BigDecimal.ZERO.setScale(SCALE)
            : gain.multiply(MILLION)
                .divide(
                    BigDecimal.valueOf(candidate.askingPrice().euros()),
                    SCALE,
                    RoundingMode.HALF_UP);
    Money budgetLeft = balance.minus(candidate.askingPrice());
    return new BuyRecommendation(
        candidate.player(),
        candidate.askingPrice(),
        slot,
        gain,
        gainPerMillion,
        budgetLeft,
        explain(candidate.player(), slot, gain, gainPerMillion, budgetLeft));
  }

  private static Position position(BuyCandidate candidate) {
    return candidate.player().player().position();
  }

  /**
   * One sentence per number, in a fixed locale so the text is the same on every machine. For
   * example: "Expected 6.80 points (form 7.33, season average 6.00). Would replace Pedri (2.10) in
   * your midfield: +4.70 (1.57 per million of asking price). Value up 5.77% in 7 days. 6.00 season
   * points per million of value. Leaves 1,200,000 €."
   */
  static String explain(
      ScoredPlayer candidate,
      Slot slot,
      BigDecimal gain,
      BigDecimal gainPerMillion,
      Money budgetLeft) {
    PlayerScore score = candidate.score();
    String line = line(candidate.player().position());
    StringBuilder text =
        new StringBuilder()
            .append("Expected ")
            .append(score.expectedPoints().toPlainString())
            .append(" points (form ")
            .append(score.recentForm().toPlainString())
            .append(", season average ")
            .append(score.seasonAverage().toPlainString())
            .append(").");
    PlayerStatus status = candidate.player().status();
    if (status != PlayerStatus.AVAILABLE) {
      text.append(' ')
          .append(capitalized(status))
          .append(": expected points × ")
          .append(score.statusMultiplier().toPlainString())
          .append('.');
    }
    switch (slot) {
      case EmptySlot _ -> text.append(" Fills an empty place in your ").append(line);
      case Replaces(var starter) ->
          text.append(" Would replace ")
              .append(starter.player().name())
              .append(" (")
              .append(starter.score().expectedPoints().toPlainString())
              .append(") in your ")
              .append(line);
    }
    text.append(": +")
        .append(gain.toPlainString())
        .append(" (")
        .append(gainPerMillion.toPlainString())
        .append(" per million of asking price).");
    BigDecimal trend = score.valueTrendPercent();
    text.append(" Value ");
    switch (trend.signum()) {
      case 1 -> text.append("up ").append(trend.toPlainString()).append('%');
      case -1 -> text.append("down ").append(trend.negate().toPlainString()).append('%');
      default -> text.append("steady");
    }
    text.append(" in ")
        .append(score.trendDays())
        .append(" days. ")
        .append(score.pointsPerMillion().toPlainString())
        .append(" season points per million of value. Leaves ")
        .append(String.format(Locale.ROOT, "%,d €", budgetLeft.euros()))
        .append('.');
    return text.toString();
  }

  private static String line(Position position) {
    return switch (position) {
      case GOALKEEPER -> "goal";
      case DEFENDER -> "defence";
      case MIDFIELDER -> "midfield";
      case FORWARD -> "attack";
      case COACH -> "coach slot";
    };
  }

  private static String capitalized(PlayerStatus status) {
    String name = status.name().toLowerCase(Locale.ROOT);
    return Character.toUpperCase(name.charAt(0)) + name.substring(1);
  }
}
