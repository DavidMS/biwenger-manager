package io.github.davidms.biwenger.client.model;

import com.fasterxml.jackson.annotation.JsonProperty;
import java.util.Objects;

/** A league in the account, with your team in it and its rules. */
public record AccountLeague(
    long id,
    String name,
    String competition,
    @JsonProperty("scoreID") int scoreId,
    LeagueMember user,
    LeagueSettings settings) {

  public AccountLeague {
    Objects.requireNonNull(name, "name");
    Objects.requireNonNull(competition, "competition");
    Objects.requireNonNull(user, "user");
    Objects.requireNonNull(settings, "settings");
  }
}
