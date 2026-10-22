package io.github.davidms.biwenger.client.model;

import java.util.List;
import java.util.Objects;

/**
 * {@code GET cf.biwenger.com/api/v2/players/la-liga/{slug}?fields=*,prices}: a player's daily value
 * for about the last year, oldest first. The last entry equals {@code price}.
 */
public record PlayerPrices(
    long id,
    String name,
    String slug,
    int position,
    long price,
    long priceIncrement,
    String status,
    List<PricePoint> prices) {

  public PlayerPrices {
    Objects.requireNonNull(name, "name");
    Objects.requireNonNull(slug, "slug");
    Objects.requireNonNull(status, "status");
    prices = prices == null ? List.of() : List.copyOf(prices);
  }
}
