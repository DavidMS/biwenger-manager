package io.github.davidms.biwenger.client.model;

/** {@code GET /api/v2/rounds/league}: the current round's id and the league standings. */
public record RoundStandings(RoundRef round, StandingsLeague league) {}
