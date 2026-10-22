package io.github.davidms.biwenger.client.model;

import java.util.List;

/** The season ({@code id} is its end year as a string, {@code "2027"}) and its rounds. */
public record Season(String id, String name, List<Round> rounds) {

  public Season {
    rounds = rounds == null ? List.of() : List.copyOf(rounds);
  }
}
