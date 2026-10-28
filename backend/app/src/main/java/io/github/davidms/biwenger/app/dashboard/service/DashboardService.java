package io.github.davidms.biwenger.app.dashboard.service;

import io.github.davidms.biwenger.app.biwenger.BiwengerGateway;
import io.github.davidms.biwenger.app.dashboard.domain.Dashboard;
import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import org.springframework.stereotype.Service;

/**
 * The dashboard, read live from Biwenger and kept for {@link #FRESH_FOR}. Live because the balance
 * changes during the day (a won bid, a sale) and the negative-balance warning must not be a day
 * old; kept briefly because one read costs four rate-limited calls. A failure isn't kept, so the
 * next read tries again.
 */
@Service
public class DashboardService {

  static final Duration FRESH_FOR = Duration.ofMinutes(1);

  private final BiwengerGateway biwenger;
  private final Clock clock;
  private Cached cached;

  DashboardService(BiwengerGateway biwenger, Clock clock) {
    this.biwenger = biwenger;
    this.clock = clock;
  }

  public synchronized Dashboard dashboard() {
    Instant now = clock.instant();
    if (cached == null || !now.isBefore(cached.readAt().plus(FRESH_FOR))) {
      cached = new Cached(biwenger.dashboard(), now);
    }
    return cached.dashboard();
  }

  private record Cached(Dashboard dashboard, Instant readAt) {}
}
