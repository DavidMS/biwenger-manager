package io.github.davidms.biwenger.app.market.web;

import io.github.davidms.biwenger.app.market.service.MarketService;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/** Today's market with each player's status, value trend, points and seller. */
@RestController
@RequestMapping("/api/market")
class MarketController {

  private final MarketService markets;

  MarketController(MarketService markets) {
    this.markets = markets;
  }

  /** {@code 503} when no login is set; {@code 502} when Biwenger fails. */
  @GetMapping
  MarketResponse market() {
    return MarketResponse.from(markets.market());
  }
}
