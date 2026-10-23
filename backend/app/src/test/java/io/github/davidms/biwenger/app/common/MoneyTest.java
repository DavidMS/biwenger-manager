package io.github.davidms.biwenger.app.common;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.math.RoundingMode;
import java.util.List;
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
  void scalesExactly() {
    // The maximum bid in the market sample: balance + 25 % of team value.
    Money balance = Money.euros(51_420_000);
    Money teamValue = Money.euros(22_340_000);
    assertThat(balance.plus(teamValue.percent(25, RoundingMode.FLOOR)))
        .isEqualTo(Money.euros(57_005_000));
    assertThat(Money.euros(3).times(4)).isEqualTo(Money.euros(12));
    assertThat(Money.sum(List.of(Money.euros(1), Money.euros(2), Money.euros(-4))))
        .isEqualTo(Money.euros(-1));
    assertThat(Money.sum(List.of())).isEqualTo(Money.ZERO);
  }

  @Test
  void percentRoundsAsAsked() {
    assertThat(Money.euros(5).percent(50, RoundingMode.FLOOR)).isEqualTo(Money.euros(2));
    assertThat(Money.euros(5).percent(50, RoundingMode.HALF_UP)).isEqualTo(Money.euros(3));
    assertThat(Money.euros(-5).percent(50, RoundingMode.FLOOR)).isEqualTo(Money.euros(-3));
  }

  @Test
  void everyOperationFailsInsteadOfOverflowing() {
    assertThatThrownBy(() -> Money.euros(Long.MIN_VALUE).minus(Money.euros(1)))
        .isInstanceOf(ArithmeticException.class);
    assertThatThrownBy(() -> Money.euros(Long.MAX_VALUE).times(2))
        .isInstanceOf(ArithmeticException.class);
    assertThatThrownBy(() -> Money.euros(Long.MAX_VALUE).percent(200, RoundingMode.FLOOR))
        .isInstanceOf(ArithmeticException.class);
  }

  @Test
  void comparesByAmount() {
    assertThat(Money.euros(2)).isGreaterThan(Money.euros(1));
    assertThat(Money.euros(4_780_000)).hasToString("4780000 €");
  }
}
