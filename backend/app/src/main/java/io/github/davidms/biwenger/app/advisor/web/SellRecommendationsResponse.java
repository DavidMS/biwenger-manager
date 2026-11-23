package io.github.davidms.biwenger.app.advisor.web;

import com.fasterxml.jackson.annotation.JsonInclude;
import io.github.davidms.biwenger.app.advisor.domain.SellRecommendation;
import io.github.davidms.biwenger.app.advisor.domain.SellRecommendation.CoveredBy;
import io.github.davidms.biwenger.app.advisor.domain.SellRecommendation.NotAStarter;
import io.github.davidms.biwenger.app.advisor.domain.SellRecommendations;
import io.github.davidms.biwenger.app.advisor.domain.SellSignal;
import io.github.davidms.biwenger.app.advisor.domain.SellSignal.FallingValue;
import io.github.davidms.biwenger.app.advisor.domain.SellSignal.LongInjury;
import io.github.davidms.biwenger.app.advisor.domain.SellSignal.LowValueForMoney;
import io.github.davidms.biwenger.app.advisor.domain.SellSignal.Surplus;
import io.github.davidms.biwenger.app.player.domain.PlayerStatus;
import io.github.davidms.biwenger.app.player.domain.Position;
import java.math.BigDecimal;
import java.util.List;

/**
 * {@code GET /api/advisor/sells}. Money is whole euros. Each recommendation carries its signals,
 * what happens to your XI without him, and his full {@link ScoreResponse score}. {@code
 * missingPlayerIds} are players you own that the daily sync never stored, so they weren't scored.
 */
public record SellRecommendationsResponse(
    List<Recommendation> recommendations, List<Long> missingPlayerIds) {

  static SellRecommendationsResponse from(SellRecommendations sells) {
    return new SellRecommendationsResponse(
        sells.recommendations().stream().map(Recommendation::from).toList(),
        sells.missingPlayerIds());
  }

  public record Recommendation(
      long id,
      String name,
      long teamId,
      Position position,
      PlayerStatus status,
      long value,
      List<Signal> signals,
      Cover cover,
      BigDecimal xiLoss,
      ScoreResponse score,
      String explanation) {

    static Recommendation from(SellRecommendation sell) {
      var player = sell.player().player();
      return new Recommendation(
          player.id(),
          player.name(),
          player.teamId(),
          player.position(),
          player.status(),
          player.price().euros(),
          sell.signals().stream().map(Signal::from).toList(),
          Cover.from(sell.cover()),
          sell.xiLoss(),
          ScoreResponse.from(sell.player().score()),
          sell.explanation());
    }
  }

  /** One reason to sell. {@code type} says which, and only its own numbers are present. */
  @JsonInclude(JsonInclude.Include.NON_NULL)
  public record Signal(
      String type,
      BigDecimal percent,
      Integer days,
      Integer matchesMissed,
      BigDecimal pointsPerMillion,
      BigDecimal squadMedian,
      BigDecimal threshold,
      Integer depth,
      Integer kept) {

    static Signal from(SellSignal signal) {
      return switch (signal) {
        case FallingValue(var percent, var days) ->
            new Signal("FALLING_VALUE", percent, days, null, null, null, null, null, null);
        case LongInjury(var matches) ->
            new Signal("LONG_INJURY", null, null, matches, null, null, null, null, null);
        case LowValueForMoney(var ppm, var median, var threshold) ->
            new Signal("LOW_VALUE_FOR_MONEY", null, null, null, ppm, median, threshold, null, null);
        case Surplus(var depth, var kept) ->
            new Signal("SURPLUS", null, null, null, null, null, null, depth, kept);
      };
    }
  }

  /**
   * What happens to your XI without him: {@code NOT_A_STARTER}, or {@code COVERED_BY} with the
   * player who would start instead.
   */
  @JsonInclude(JsonInclude.Include.NON_NULL)
  public record Cover(String type, Long id, String name, BigDecimal expectedPoints) {

    static Cover from(SellRecommendation.Cover cover) {
      return switch (cover) {
        case NotAStarter _ -> new Cover("NOT_A_STARTER", null, null, null);
        case CoveredBy(var player) ->
            new Cover(
                "COVERED_BY",
                player.player().id(),
                player.player().name(),
                player.score().expectedPoints());
      };
    }
  }
}
