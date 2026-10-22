package io.github.davidms.biwenger.client.model;

import java.util.List;
import java.util.Objects;

/** The season ({@code id} is its end year as a string, {@code "2027"}) and its rounds. */
public record Season(String id, String name, List<Round> rounds) {

  public Season {
    Objects.requireNonNull(id, "id");
    Objects.requireNonNull(name, "name");
    rounds = rounds == null ? List.of() : List.copyOf(rounds);
  }
}
