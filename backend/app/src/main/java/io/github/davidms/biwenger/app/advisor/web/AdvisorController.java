package io.github.davidms.biwenger.app.advisor.web;

import io.github.davidms.biwenger.app.advisor.service.BuyAdvisorService;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/** The advisor's recommendations. Each one is computed when asked, never stored. */
@RestController
@RequestMapping("/api/advisor")
class AdvisorController {

  private final BuyAdvisorService buys;

  AdvisorController(BuyAdvisorService buys) {
    this.buys = buys;
  }

  /** {@code 503} when no login is set; {@code 502} when Biwenger fails. */
  @GetMapping("/buys")
  BuyRecommendationsResponse buys() {
    return BuyRecommendationsResponse.from(buys.recommendations());
  }
}
