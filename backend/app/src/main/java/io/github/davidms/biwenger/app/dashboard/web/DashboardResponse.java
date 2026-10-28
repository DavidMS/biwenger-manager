package io.github.davidms.biwenger.app.dashboard.web;

import com.fasterxml.jackson.annotation.JsonInclude;
import io.github.davidms.biwenger.app.dashboard.domain.Dashboard;
import io.github.davidms.biwenger.app.league.domain.ScheduledRound;
import java.time.Instant;

/**
 * {@code GET /api/dashboard}. Money is whole euros. {@code nextRound} is left out when no upcoming
 * round has a known start (the end of the season).
 */
@JsonInclude(JsonInclude.Include.NON_NULL)
public record DashboardResponse(
    long balance,
    boolean balanceNegative,
    int points,
    int position,
    int managers,
    long teamValue,
    long teamValueChange,
    NextRound nextRound) {

  static DashboardResponse from(Dashboard dashboard, Instant now) {
    return new DashboardResponse(
        dashboard.balance().euros(),
        dashboard.balanceNegative(),
        dashboard.points(),
        dashboard.position(),
        dashboard.managers(),
        dashboard.teamValue().euros(),
        dashboard.teamValueChange().euros(),
        dashboard.nextRound(now).map(NextRound::from).orElse(null));
  }

  public record NextRound(String name, Instant startsAt) {

    static NextRound from(ScheduledRound round) {
      return new NextRound(round.name(), round.startsAt());
    }
  }
}
