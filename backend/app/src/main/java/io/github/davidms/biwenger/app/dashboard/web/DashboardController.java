package io.github.davidms.biwenger.app.dashboard.web;

import io.github.davidms.biwenger.app.dashboard.service.DashboardService;
import java.time.Clock;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/** Your team at a glance: balance, team value, league position and the next round. */
@RestController
@RequestMapping("/api/dashboard")
class DashboardController {

  private final DashboardService dashboards;
  private final Clock clock;

  DashboardController(DashboardService dashboards, Clock clock) {
    this.dashboards = dashboards;
    this.clock = clock;
  }

  /**
   * The next round is picked now, not when the data was read, so a cached dashboard doesn't show a
   * round that has just started. {@code 503} when no login is set; {@code 502} when Biwenger fails.
   */
  @GetMapping
  DashboardResponse dashboard() {
    return DashboardResponse.from(dashboards.dashboard(), clock.instant());
  }
}
