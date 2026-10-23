package io.github.davidms.biwenger.app.league.domain;

/** Where a round stands. {@code UNKNOWN} is a status Biwenger added that we don't know yet. */
public enum RoundStatus {
  FINISHED,
  ACTIVE,
  PENDING,
  UNKNOWN
}
