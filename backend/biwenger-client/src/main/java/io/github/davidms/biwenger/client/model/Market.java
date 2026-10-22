package io.github.davidms.biwenger.client.model;

import java.util.List;
import java.util.Objects;

/** {@code GET /api/v2/market}: your cash and maximum bid, and the players on sale. */
public record Market(MarketStatus status, List<Sale> sales) {

  public Market {
    Objects.requireNonNull(status, "status");
    sales = sales == null ? List.of() : List.copyOf(sales);
  }
}
