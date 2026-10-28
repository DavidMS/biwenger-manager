package io.github.davidms.biwenger.app.dashboard.service;

import io.github.davidms.biwenger.app.biwenger.BiwengerGateway;
import io.github.davidms.biwenger.app.dashboard.domain.Dashboard;
import io.github.davidms.biwenger.app.dashboard.domain.TeamStatus;
import io.github.davidms.biwenger.app.league.domain.ScheduledRound;
import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.util.List;
import org.springframework.stereotype.Service;

/**
 * The dashboard, read live from Biwenger in two parts kept for different times. The team is kept
 * for {@link #TEAM_FRESH_FOR}: the balance changes during the day (a won bid, a sale) and the
 * negative-balance warning must not be a day old, so two calls (account, standings) once a minute
 * at most. The round schedule comes from the catalog, about 230 KB that Biwenger updates once a
 * day, so it is kept until the next round starts (then the catalog names the one after), or for
 * {@link #ROUNDS_FRESH_FOR} at most. A failure isn't kept, so the next read tries again.
 */
@Service
public class DashboardService {

  static final Duration TEAM_FRESH_FOR = Duration.ofMinutes(1);
  static final Duration ROUNDS_FRESH_FOR = Duration.ofHours(6);

  private final BiwengerGateway biwenger;
  private final Clock clock;
  private Cached<TeamStatus> team;
  private Cached<List<ScheduledRound>> rounds;

  DashboardService(BiwengerGateway biwenger, Clock clock) {
    this.biwenger = biwenger;
    this.clock = clock;
  }

  public synchronized Dashboard dashboard() {
    Instant now = clock.instant();
    if (team == null || !now.isBefore(team.freshUntil())) {
      team = new Cached<>(biwenger.teamStatus(), now.plus(TEAM_FRESH_FOR));
    }
    if (rounds == null || !now.isBefore(rounds.freshUntil())) {
      var read = biwenger.scheduledRounds();
      Instant until = now.plus(ROUNDS_FRESH_FOR);
      Instant nextStart =
          Dashboard.nextRound(read, now).map(ScheduledRound::startsAt).orElse(until);
      rounds = new Cached<>(read, nextStart.isBefore(until) ? nextStart : until);
    }
    return new Dashboard(team.value(), rounds.value());
  }

  private record Cached<T>(T value, Instant freshUntil) {}
}
