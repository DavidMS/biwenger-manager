package io.github.davidms.biwenger.app.league.web;

import io.github.davidms.biwenger.app.league.domain.LeagueTable;
import java.util.List;

/** {@code GET /api/standings}: the table by position. Money is whole euros. */
public record StandingsResponse(List<Row> standings) {

  static StandingsResponse from(LeagueTable table) {
    return new StandingsResponse(
        table.entries().stream()
            .map(
                entry -> {
                  var standing = entry.standing();
                  return new Row(
                      entry.managerId(),
                      standing.manager().name(),
                      standing.position(),
                      standing.points(),
                      standing.teamValue().euros(),
                      standing.teamValueChange().euros(),
                      entry.managerId() == table.yourManagerId());
                })
            .toList());
  }

  /** {@code you} marks your own row. */
  public record Row(
      long managerId,
      String name,
      int position,
      int points,
      long teamValue,
      long teamValueChange,
      boolean you) {}
}
