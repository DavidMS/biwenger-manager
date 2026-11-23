package io.github.davidms.biwenger.app.advisor.domain;

/**
 * The tunable numbers of the {@link BuyAdvisor}, set from {@code biwenger.advisor.buy.*}. The
 * formation it measures gains against is the shared {@link ReferenceFormation}.
 *
 * @param limit the most recommendations returned
 */
public record BuyWeights(int limit) {

  public static final BuyWeights DEFAULTS = new BuyWeights(10);

  public BuyWeights {
    if (limit < 1) {
      throw new IllegalArgumentException("limit must be at least 1: " + limit);
    }
  }
}
