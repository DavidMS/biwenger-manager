package io.github.davidms.biwenger.app.player.domain;

/** Whether a player can play. {@code UNKNOWN} is a status Biwenger added that we don't know yet. */
public enum PlayerStatus {
  AVAILABLE,
  DOUBTFUL,
  INJURED,
  SUSPENDED,
  UNKNOWN;

  public boolean isAvailable() {
    return this == AVAILABLE;
  }
}
