package io.github.davidms.biwenger.app.league.domain;

import java.util.List;
import java.util.Objects;

/** The league: its managers, and the scoring system its points are counted in. */
public record League(long id, String name, int scoringSystemId, List<Manager> managers) {

  public League {
    Objects.requireNonNull(name, "name");
    managers = List.copyOf(managers);
  }
}
