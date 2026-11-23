package io.github.davidms.biwenger.app.advisor.domain;

import io.github.davidms.biwenger.app.advisor.domain.SellRecommendation.Cover;
import io.github.davidms.biwenger.app.advisor.domain.SellRecommendation.CoveredBy;
import io.github.davidms.biwenger.app.advisor.domain.SellRecommendation.NotAStarter;
import io.github.davidms.biwenger.app.advisor.domain.SellSignal.FallingValue;
import io.github.davidms.biwenger.app.advisor.domain.SellSignal.LongInjury;
import io.github.davidms.biwenger.app.advisor.domain.SellSignal.LowValueForMoney;
import io.github.davidms.biwenger.app.advisor.domain.SellSignal.Surplus;
import io.github.davidms.biwenger.app.player.domain.PlayerStatus;
import io.github.davidms.biwenger.app.player.domain.Position;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.EnumMap;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Optional;
import java.util.Set;

/**
 * Sell recommendations (#36, ADR 0008). Pure and deterministic, like the {@link PlayerScorer} it
 * builds on.
 *
 * <ol>
 *   <li>Signals: value down {@code fallingPercent} or more over the trend; injured and out for at
 *       least {@code injuryMatches} matches in a row; season points per million under {@code
 *       lowValueShare} of the squad's median; deeper in his position than the formation's starters
 *       plus {@code backups}. A player with at least one signal is a candidate.
 *   <li>XI loss: for a starter, his expected points minus the best player outside the XI in his
 *       position, who would start instead; 0 for anyone else. A starter nobody could replace is
 *       never suggested.
 *   <li>Order: lowest XI loss, then more signals, then steepest value fall, then lowest player id.
 *   <li>Taken in that order, a sale is suggested only if, together with the ones before it, the
 *       squad keeps at least {@code minSquadSize} players and each position keeps its formation
 *       starters and at least one player, the player named to start instead of him is neither sold
 *       nor already covering another sale, and he isn't covering one himself. The first {@code
 *       limit} are returned.
 * </ol>
 *
 * <p>Each XI loss is measured against today's squad, as if he were the only one sold.
 */
public final class SellAdvisor {

  private static final int SCALE = 2;

  static final Comparator<SellRecommendation> RANKING =
      Comparator.comparing(SellRecommendation::xiLoss)
          .thenComparing(r -> r.signals().size(), Comparator.reverseOrder())
          .thenComparing(r -> r.player().score().valueTrendPercent())
          .thenComparingLong(r -> r.player().player().id());

  private SellAdvisor() {}

  /**
   * @param unscored players you own that couldn't be scored: they count toward the minimum squad
   *     size, but not toward any position, since their position isn't known
   */
  public static List<SellRecommendation> recommend(
      List<SellCandidate> squad, int unscored, ReferenceFormation formation, SellWeights weights) {
    List<ScoredPlayer> players = squad.stream().map(SellCandidate::player).toList();
    BigDecimal median = medianPointsPerMillion(players);

    List<SellRecommendation> ranked =
        squad.stream()
            .flatMap(
                candidate ->
                    recommendation(candidate, players, median, formation, weights).stream())
            .sorted(RANKING)
            .toList();

    int squadSize = squad.size() + unscored;
    Map<Position, Integer> perPosition = new EnumMap<>(Position.class);
    players.forEach(p -> perPosition.merge(p.player().position(), 1, Integer::sum));
    List<SellRecommendation> suggested = new ArrayList<>();
    Set<Long> sold = new HashSet<>();
    Set<Long> covering = new HashSet<>();
    for (SellRecommendation recommendation : ranked) {
      if (suggested.size() == weights.limit()) {
        break;
      }
      long id = recommendation.player().player().id();
      Position position = recommendation.player().player().position();
      int keep = Math.max(formation.startersAt(position), 1);
      boolean squadKept =
          squadSize - 1 >= weights.minSquadSize() && perPosition.get(position) - 1 >= keep;
      // Keep the list consistent: the player named to start instead must still be there and
      // not already covering another suggested sale, and nobody named as a cover is sold.
      boolean coverKept =
          switch (recommendation.cover()) {
            case NotAStarter _ -> true;
            case CoveredBy(var cover) ->
                !sold.contains(cover.player().id()) && !covering.contains(cover.player().id());
          };
      if (squadKept && coverKept && !covering.contains(id)) {
        suggested.add(recommendation);
        sold.add(id);
        if (recommendation.cover() instanceof CoveredBy(var cover)) {
          covering.add(cover.player().id());
        }
        squadSize--;
        perPosition.merge(position, -1, Integer::sum);
      }
    }
    return List.copyOf(suggested);
  }

  /** The recommendation for {@code candidate}, or empty when no signal fires. */
  private static Optional<SellRecommendation> recommendation(
      SellCandidate candidate,
      List<ScoredPlayer> squad,
      BigDecimal median,
      ReferenceFormation formation,
      SellWeights weights) {
    ScoredPlayer player = candidate.player();
    PlayerScore score = player.score();
    Position position = player.player().position();
    List<ScoredPlayer> depthChart = ReferenceFormation.depthChart(squad, position);
    int depth = depthChart.indexOf(player) + 1;
    int starters = formation.startersAt(position);

    List<SellSignal> signals = new ArrayList<>();
    if (score.valueTrendPercent().compareTo(weights.fallingPercent().negate()) <= 0) {
      signals.add(new FallingValue(score.valueTrendPercent().negate(), score.trendDays()));
    }
    if (player.player().status() == PlayerStatus.INJURED
        && candidate.injuryStreak() >= weights.injuryMatches()) {
      signals.add(new LongInjury(candidate.injuryStreak()));
    }
    BigDecimal threshold =
        median.multiply(weights.lowValueShare()).setScale(SCALE, RoundingMode.HALF_UP);
    if (median.signum() > 0 && score.pointsPerMillion().compareTo(threshold) < 0) {
      signals.add(new LowValueForMoney(score.pointsPerMillion(), median, threshold));
    }
    int kept = starters + weights.backups();
    if (depth > kept) {
      signals.add(new Surplus(depth, kept));
    }
    if (signals.isEmpty()) {
      return Optional.empty();
    }

    Cover cover;
    BigDecimal loss;
    if (depth > starters) {
      cover = new NotAStarter();
      loss = BigDecimal.ZERO.setScale(SCALE);
    } else if (depthChart.size() > starters) {
      ScoredPlayer replacement = depthChart.get(starters);
      cover = new CoveredBy(replacement);
      loss = score.expectedPoints().subtract(replacement.score().expectedPoints());
    } else {
      // A starter nobody can cover: selling him would leave the formation short.
      return Optional.empty();
    }
    return Optional.of(
        new SellRecommendation(
            player, signals, cover, loss, explain(player, signals, cover, loss)));
  }

  /** Middle value, or the mean of the two middle values, rounded half up; zero with no players. */
  static BigDecimal medianPointsPerMillion(List<ScoredPlayer> squad) {
    List<BigDecimal> sorted =
        squad.stream().map(p -> p.score().pointsPerMillion()).sorted().toList();
    if (sorted.isEmpty()) {
      return BigDecimal.ZERO.setScale(SCALE);
    }
    int middle = sorted.size() / 2;
    BigDecimal median =
        sorted.size() % 2 == 1
            ? sorted.get(middle)
            : sorted.get(middle - 1).add(sorted.get(middle)).divide(BigDecimal.TWO);
    return median.setScale(SCALE, RoundingMode.HALF_UP);
  }

  /**
   * One sentence per number, in a fixed locale so the text is the same on every machine. For
   * example: "Expected 1.20 points, value 4,780,000 €. Value down 4.20% in 7 days. Defender number
   * 6 of yours, behind the 5 you keep. Your XI loses 0.00: he doesn't start."
   */
  static String explain(
      ScoredPlayer player, List<SellSignal> signals, Cover cover, BigDecimal loss) {
    StringBuilder text =
        new StringBuilder()
            .append("Expected ")
            .append(player.score().expectedPoints().toPlainString())
            .append(" points, value ")
            .append(String.format(Locale.ROOT, "%,d €", player.player().price().euros()))
            .append('.');
    for (SellSignal signal : signals) {
      text.append(' ');
      switch (signal) {
        case FallingValue(var percent, var days) ->
            text.append("Value down ")
                .append(percent.toPlainString())
                .append("% in ")
                .append(days)
                .append(" days.");
        case LongInjury(var matches) ->
            text.append("Injured, missed his last ")
                .append(matches)
                .append(matches == 1 ? " match." : " matches.");
        case LowValueForMoney(var ppm, var median, var threshold) ->
            text.append(ppm.toPlainString())
                .append(" season points per million, under ")
                .append(threshold.toPlainString())
                .append(" (your squad's median is ")
                .append(median.toPlainString())
                .append(").");
        case Surplus(var depth, var kept) ->
            text.append(capitalized(player.player().position()))
                .append(" number ")
                .append(depth)
                .append(" of yours, behind the ")
                .append(kept)
                .append(" you keep.");
      }
    }
    text.append(" Your XI loses ").append(loss.toPlainString()).append(": ");
    switch (cover) {
      case NotAStarter _ -> text.append("he doesn't start.");
      case CoveredBy(var replacement) ->
          text.append(replacement.player().name())
              .append(" (")
              .append(replacement.score().expectedPoints().toPlainString())
              .append(") would start instead.");
    }
    return text.toString();
  }

  private static String capitalized(Position position) {
    String name = position.name().toLowerCase(Locale.ROOT);
    return Character.toUpperCase(name.charAt(0)) + name.substring(1);
  }
}
