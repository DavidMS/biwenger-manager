package io.github.davidms.biwenger.client.model;

import com.fasterxml.jackson.annotation.JsonProperty;
import java.util.List;

/** The league with its standings, sorted by position. */
public record StandingsLeague(
    long id, String name, @JsonProperty("scoreID") int scoreId, List<Standing> standings) {

  public StandingsLeague {
    standings = standings == null ? List.of() : List.copyOf(standings);
  }
}
