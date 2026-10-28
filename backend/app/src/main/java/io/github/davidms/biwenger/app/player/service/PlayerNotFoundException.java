package io.github.davidms.biwenger.app.player.service;

/** No player with this id has been stored by the sync. */
public class PlayerNotFoundException extends RuntimeException {

  public PlayerNotFoundException(long playerId) {
    super("No player " + playerId + " has been synced");
  }
}
