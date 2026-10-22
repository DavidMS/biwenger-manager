package io.github.davidms.biwenger.client.model;

import com.fasterxml.jackson.annotation.JsonProperty;
import java.util.List;
import java.util.Objects;

/** {@code GET /api/v2/league/{id}}: the league and its managers, not in standings order. */
public record League(
    long id, String name, @JsonProperty("scoreID") int scoreId, List<Manager> users) {

  public League {
    Objects.requireNonNull(name, "name");
    users = users == null ? List.of() : List.copyOf(users);
  }
}
