package io.github.davidms.biwenger.app.league.web;

import io.github.davidms.biwenger.app.league.domain.RivalSquad;
import io.github.davidms.biwenger.app.player.domain.PlayerStatus;
import io.github.davidms.biwenger.app.player.domain.Position;
import java.time.Instant;
import java.util.List;

/**
 * {@code GET /api/standings/{managerId}/squad}: a manager's squad as his lineup saved at {@code
 * lineupSavedAt} shows it. Money is whole euros; players are by id.
 */
public record RivalSquadResponse(
    long managerId,
    String name,
    int position,
    long teamValue,
    Instant lineupSavedAt,
    List<Player> players,
    List<Long> missingPlayerIds) {

  static RivalSquadResponse from(RivalSquad squad) {
    var entry = squad.entry();
    var standing = entry.standing();
    return new RivalSquadResponse(
        entry.managerId(),
        standing.manager().name(),
        standing.position(),
        standing.teamValue().euros(),
        entry.lineupSavedAt(),
        squad.players().stream().map(Player::from).toList(),
        squad.missingPlayerIds());
  }

  public record Player(
      long id,
      String name,
      long teamId,
      Position position,
      PlayerStatus status,
      long value,
      int points) {

    static Player from(RivalSquad.Member member) {
      var player = member.player();
      return new Player(
          player.id(),
          player.name(),
          player.teamId(),
          player.position(),
          player.status(),
          player.price().euros(),
          member.points());
    }
  }
}
