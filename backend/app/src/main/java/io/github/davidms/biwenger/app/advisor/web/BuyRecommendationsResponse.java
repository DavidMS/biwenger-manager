package io.github.davidms.biwenger.app.advisor.web;

import com.fasterxml.jackson.annotation.JsonInclude;
import io.github.davidms.biwenger.app.advisor.domain.BuyRecommendation;
import io.github.davidms.biwenger.app.advisor.domain.BuyRecommendation.EmptySlot;
import io.github.davidms.biwenger.app.advisor.domain.BuyRecommendation.Replaces;
import io.github.davidms.biwenger.app.advisor.domain.BuyRecommendations;
import io.github.davidms.biwenger.app.player.domain.PlayerStatus;
import io.github.davidms.biwenger.app.player.domain.Position;
import java.math.BigDecimal;
import java.util.List;

/**
 * {@code GET /api/advisor/buys}. Money is whole euros. Each recommendation carries the player's
 * full {@link ScoreResponse score}, so the explanation can be checked against the numbers. The
 * missing ids are players the advisor couldn't score because the daily sync never stored them.
 */
public record BuyRecommendationsResponse(
    long balance,
    List<Recommendation> recommendations,
    List<Long> missingSquadPlayerIds,
    List<Long> missingMarketPlayerIds) {

  static BuyRecommendationsResponse from(BuyRecommendations buys) {
    return new BuyRecommendationsResponse(
        buys.balance().euros(),
        buys.recommendations().stream().map(Recommendation::from).toList(),
        buys.missingSquadPlayerIds(),
        buys.missingMarketPlayerIds());
  }

  /** {@code replaces} is left out when he fills an empty place in your XI. */
  @JsonInclude(JsonInclude.Include.NON_NULL)
  public record Recommendation(
      long id,
      String name,
      long teamId,
      Position position,
      PlayerStatus status,
      long askingPrice,
      long value,
      BigDecimal gain,
      BigDecimal gainPerMillion,
      long budgetLeft,
      Starter replaces,
      ScoreResponse score,
      String explanation) {

    static Recommendation from(BuyRecommendation buy) {
      var player = buy.candidate().player();
      Starter replaces =
          switch (buy.slot()) {
            case EmptySlot _ -> null;
            case Replaces(var starter) ->
                new Starter(
                    starter.player().id(),
                    starter.player().name(),
                    starter.score().expectedPoints());
          };
      return new Recommendation(
          player.id(),
          player.name(),
          player.teamId(),
          player.position(),
          player.status(),
          buy.askingPrice().euros(),
          player.price().euros(),
          buy.gain(),
          buy.gainPerMillion(),
          buy.budgetLeft().euros(),
          replaces,
          ScoreResponse.from(buy.candidate().score()),
          buy.explanation());
    }
  }

  /** The starter of yours he would replace. */
  public record Starter(long id, String name, BigDecimal expectedPoints) {}
}
