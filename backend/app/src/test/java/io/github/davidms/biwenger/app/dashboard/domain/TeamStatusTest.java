package io.github.davidms.biwenger.app.dashboard.domain;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import io.github.davidms.biwenger.app.common.Money;
import org.junit.jupiter.api.Test;

class TeamStatusTest {

  @Test
  void onlyABalanceBelowZeroIsNegative() {
    assertThat(team(Money.euros(-1), 6).balanceNegative()).isTrue();
    assertThat(team(Money.ZERO, 6).balanceNegative()).isFalse();
  }

  @Test
  void thePositionIsInsideTheLeague() {
    assertThatThrownBy(() -> team(Money.ZERO, 7))
        .isInstanceOf(IllegalArgumentException.class)
        .hasMessageContaining("league of 6");
    assertThatThrownBy(() -> team(Money.ZERO, 0)).isInstanceOf(IllegalArgumentException.class);
  }

  private static TeamStatus team(Money balance, int position) {
    return new TeamStatus(balance, 220, position, 6, Money.euros(22_340_000), Money.ZERO);
  }
}
