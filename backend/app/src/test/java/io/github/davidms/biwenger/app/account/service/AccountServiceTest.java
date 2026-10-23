package io.github.davidms.biwenger.app.account.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import io.github.davidms.biwenger.app.MutableClock;
import io.github.davidms.biwenger.app.account.domain.ConnectedAccount;
import io.github.davidms.biwenger.app.biwenger.BiwengerGateway;
import io.github.davidms.biwenger.client.BiwengerApiException;
import java.time.Duration;
import java.time.Instant;
import java.time.ZoneId;
import org.junit.jupiter.api.Test;

class AccountServiceTest {

  private static final ConnectedAccount ACCOUNT =
      new ConnectedAccount("Account Owner", "Manager 1", "Example League");

  private final BiwengerGateway biwenger = mock(BiwengerGateway.class);
  private final MutableClock clock =
      new MutableClock(Instant.parse("2026-10-22T05:30:00Z"), ZoneId.of("Europe/Madrid"));
  private final AccountService accounts = new AccountService(biwenger, clock);

  @Test
  void readsBiwengerOnceForTenMinutes() {
    when(biwenger.connectedAccount()).thenReturn(ACCOUNT);

    accounts.connectedAccount();
    clock.advance(Duration.ofMinutes(9));
    accounts.connectedAccount();

    verify(biwenger, times(1)).connectedAccount();
    clock.advance(Duration.ofMinutes(1));
    assertThat(accounts.connectedAccount()).isEqualTo(ACCOUNT);
    verify(biwenger, times(2)).connectedAccount();
  }

  @Test
  void aFailureIsNotKept() {
    when(biwenger.connectedAccount())
        .thenThrow(new BiwengerApiException("Biwenger answered HTTP 503"))
        .thenReturn(ACCOUNT);

    assertThatThrownBy(accounts::connectedAccount).isInstanceOf(BiwengerApiException.class);
    assertThat(accounts.connectedAccount()).isEqualTo(ACCOUNT);
  }
}
