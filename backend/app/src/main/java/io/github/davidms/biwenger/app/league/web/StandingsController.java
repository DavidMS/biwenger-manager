package io.github.davidms.biwenger.app.league.web;

import io.github.davidms.biwenger.app.league.service.StandingsService;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/** The league table, and each manager's squad as his lineup shows it. */
@RestController
@RequestMapping("/api/standings")
class StandingsController {

  private final StandingsService standings;

  StandingsController(StandingsService standings) {
    this.standings = standings;
  }

  /** {@code 503} when no login is set; {@code 502} when Biwenger fails. */
  @GetMapping
  StandingsResponse standings() {
    return StandingsResponse.from(standings.table());
  }

  /** As above, and {@code 404} when no manager with this id is in the league. */
  @GetMapping("/{managerId}/squad")
  RivalSquadResponse squad(@PathVariable long managerId) {
    return RivalSquadResponse.from(standings.squad(managerId));
  }
}
