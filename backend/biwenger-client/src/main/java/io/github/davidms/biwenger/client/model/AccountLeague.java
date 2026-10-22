package io.github.davidms.biwenger.client.model;

import com.fasterxml.jackson.annotation.JsonProperty;

/** A league in the account, with your team in it and its rules. */
public record AccountLeague(
    long id,
    String name,
    String competition,
    @JsonProperty("scoreID") int scoreId,
    LeagueMember user,
    LeagueSettings settings) {}
