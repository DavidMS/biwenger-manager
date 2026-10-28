package io.github.davidms.biwenger.app.squad.web;

import io.github.davidms.biwenger.app.squad.service.SquadService;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/** Your players with their value trend, status, points and recent form. */
@RestController
@RequestMapping("/api/squad")
class SquadController {

  private final SquadService squads;

  SquadController(SquadService squads) {
    this.squads = squads;
  }

  /** {@code 503} when no login is set; {@code 502} when Biwenger fails. */
  @GetMapping
  SquadResponse squad() {
    return SquadResponse.from(squads.squad());
  }
}
