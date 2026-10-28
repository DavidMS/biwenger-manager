package io.github.davidms.biwenger.app.league.domain;

import java.util.List;
import java.util.Optional;

/** The league table by position, and which manager is you. */
public record LeagueTable(List<LeagueEntry> entries, long yourManagerId) {

  public LeagueTable {
    entries = List.copyOf(entries);
  }

  public Optional<LeagueEntry> entry(long managerId) {
    return entries.stream().filter(entry -> entry.managerId() == managerId).findFirst();
  }
}
