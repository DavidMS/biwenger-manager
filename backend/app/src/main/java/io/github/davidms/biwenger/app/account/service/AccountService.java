package io.github.davidms.biwenger.app.account.service;

import io.github.davidms.biwenger.app.account.domain.ConnectedAccount;
import io.github.davidms.biwenger.app.biwenger.BiwengerGateway;
import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import org.springframework.stereotype.Service;

/**
 * The connected account, read from Biwenger and kept for {@link #FRESH_FOR}: the names change
 * rarely, and opening the settings page shouldn't cost a request each time. A failure isn't kept,
 * so the next read tries again.
 */
@Service
public class AccountService {

  static final Duration FRESH_FOR = Duration.ofMinutes(10);

  private final BiwengerGateway biwenger;
  private final Clock clock;
  private Cached cached;

  AccountService(BiwengerGateway biwenger, Clock clock) {
    this.biwenger = biwenger;
    this.clock = clock;
  }

  public synchronized ConnectedAccount connectedAccount() {
    Instant now = clock.instant();
    if (cached == null || !now.isBefore(cached.readAt().plus(FRESH_FOR))) {
      cached = new Cached(biwenger.connectedAccount(), now);
    }
    return cached.account();
  }

  private record Cached(ConnectedAccount account, Instant readAt) {}
}
