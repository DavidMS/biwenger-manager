package io.github.davidms.biwenger.app.player.web;

import io.github.davidms.biwenger.app.player.service.PlayerNotFoundException;
import io.github.davidms.biwenger.app.player.service.PlayerReader;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/** One player's value history, points per match and status, all from the database. */
@RestController
@RequestMapping("/api/players")
class PlayerController {

  private final PlayerReader players;

  PlayerController(PlayerReader players) {
    this.players = players;
  }

  /** {@code 404} when the sync has never stored this player. No call to Biwenger. */
  @GetMapping("/{playerId}")
  PlayerDetailResponse player(@PathVariable long playerId) {
    var detail = players.detail(playerId).orElseThrow(() -> new PlayerNotFoundException(playerId));
    return PlayerDetailResponse.from(detail, players.team(detail.player().teamId()));
  }
}
