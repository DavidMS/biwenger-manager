package io.github.davidms.biwenger.client.model;

import com.fasterxml.jackson.annotation.JsonProperty;
import java.util.List;
import java.util.Objects;

/** The league with its standings, sorted by position. */
public record StandingsLeague(
    long id, String name, @JsonProperty("scoreID") int scoreId, List<Standing> standings) {

  public StandingsLeague {
    Objects.requireNonNull(name, "name");
    standings = standings == null ? List.of() : List.copyOf(standings);
  }
}
