package io.github.davidms.biwenger.app.advisor.domain;

import java.util.List;

/**
 * The sell recommendations, best first. {@code missingPlayerIds} are players Biwenger says you own
 * that the daily sync has never stored, so the advisor couldn't score them; they still count toward
 * the minimum squad size.
 */
public record SellRecommendations(
    List<SellRecommendation> recommendations, List<Long> missingPlayerIds) {

  public SellRecommendations {
    recommendations = List.copyOf(recommendations);
    missingPlayerIds = List.copyOf(missingPlayerIds);
  }
}
