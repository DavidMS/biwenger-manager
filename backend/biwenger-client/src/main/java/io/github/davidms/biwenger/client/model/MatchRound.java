package io.github.davidms.biwenger.client.model;

import com.fasterxml.jackson.annotation.JsonProperty;
import java.util.Objects;
import org.jspecify.annotations.Nullable;

/** The round of a match, matching a {@link Round} in the catalog by id. */
public record MatchRound(
    long id, String name, @JsonProperty("short") String shortName, @Nullable Integer part) {

  public MatchRound {
    Objects.requireNonNull(name, "name");
    Objects.requireNonNull(shortName, "shortName");
  }
}
