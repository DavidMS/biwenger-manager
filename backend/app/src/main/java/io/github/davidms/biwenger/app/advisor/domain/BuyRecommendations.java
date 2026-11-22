package io.github.davidms.biwenger.app.advisor.domain;

import io.github.davidms.biwenger.app.common.Money;
import java.util.List;
import java.util.Objects;

/**
 * The recommendations, best first, with the balance they were filtered by. The missing ids are
 * players Biwenger lists that the daily sync has never stored, so the advisor couldn't score them:
 * a missing squad player isn't counted as a starter, which can make a gain look bigger than it is.
 */
public record BuyRecommendations(
    Money balance,
    List<BuyRecommendation> recommendations,
    List<Long> missingSquadPlayerIds,
    List<Long> missingMarketPlayerIds) {

  public BuyRecommendations {
    Objects.requireNonNull(balance, "balance");
    recommendations = List.copyOf(recommendations);
    missingSquadPlayerIds = List.copyOf(missingSquadPlayerIds);
    missingMarketPlayerIds = List.copyOf(missingMarketPlayerIds);
  }
}
