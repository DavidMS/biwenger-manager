package io.github.davidms.biwenger.app.advisor.domain;

/**
 * Where a player's club plays its next match. {@code UNKNOWN} when the fixture isn't known, and
 * then the scoring model makes no home/away adjustment.
 */
public enum Venue {
  HOME,
  AWAY,
  UNKNOWN
}
