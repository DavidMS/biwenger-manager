package io.github.davidms.biwenger.client.model;

import com.fasterxml.jackson.annotation.JsonProperty;
import java.util.Collections;
import java.util.Map;
import java.util.Objects;
import java.util.TreeMap;

/**
 * {@code GET /api/v2/competitions/la-liga/data}: every player and club, keyed by id, and the season
 * with its rounds. Points are in the scoring system {@code scoreId}.
 */
public record Catalog(
    Map<Long, CatalogPlayer> players,
    Map<Long, Team> teams,
    Season season,
    @JsonProperty("scoreID") int scoreId) {

  public Catalog {
    Objects.requireNonNull(season, "season");
    // Ordered by id, so iterating gives the same order on every run (root golden rule 5).
    players = players == null ? Map.of() : Collections.unmodifiableMap(new TreeMap<>(players));
    teams = teams == null ? Map.of() : Collections.unmodifiableMap(new TreeMap<>(teams));
  }
}
