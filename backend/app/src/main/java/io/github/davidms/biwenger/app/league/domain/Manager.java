package io.github.davidms.biwenger.app.league.domain;

import java.util.Objects;

/** A manager (a team) in the league. */
public record Manager(long id, String name) {

  public Manager {
    Objects.requireNonNull(name, "name");
  }
}
