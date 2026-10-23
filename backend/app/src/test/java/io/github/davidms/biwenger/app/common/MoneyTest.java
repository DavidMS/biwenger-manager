package io.github.davidms.biwenger.app.common;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import org.junit.jupiter.api.Test;

class MoneyTest {

  @Test
  void addsAndSubtractsWholeEuros() {
    assertThat(Money.euros(51_420_000).minus(Money.euros(57_005_000)))
        .isEqualTo(Money.euros(-5_585_000));
    assertThat(Money.euros(1).plus(Money.ZERO)).isEqualTo(Money.euros(1));
  }

  @Test
  void aBidAboveTheBalanceLeavesItNegative() {
    assertThat(Money.euros(1_000).minus(Money.euros(1_500)).isNegative()).isTrue();
    assertThat(Money.ZERO.isNegative()).isFalse();
  }

  @Test
  void failsInsteadOfOverflowing() {
    assertThatThrownBy(() -> Money.euros(Long.MAX_VALUE).plus(Money.euros(1)))
        .isInstanceOf(ArithmeticException.class);
  }

  @Test
  void comparesByAmount() {
    assertThat(Money.euros(2)).isGreaterThan(Money.euros(1));
    assertThat(Money.euros(4_780_000)).hasToString("4780000 €");
  }
}
