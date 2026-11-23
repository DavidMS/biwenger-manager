package io.github.davidms.biwenger.app.advisor.domain;

import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.math.BigDecimal;
import org.junit.jupiter.api.Test;

class SellWeightsTest {

  private static final BigDecimal THREE = new BigDecimal("3");
  private static final BigDecimal HALF = new BigDecimal("0.5");

  @Test
  void rejectsValuesOutOfRange() {
    assertThatThrownBy(() -> new SellWeights(-1, 1, THREE, 2, HALF, 10))
        .hasMessageContaining("minSquadSize");
    assertThatThrownBy(() -> new SellWeights(15, -1, THREE, 2, HALF, 10))
        .hasMessageContaining("backups");
    assertThatThrownBy(() -> new SellWeights(15, 1, BigDecimal.ZERO, 2, HALF, 10))
        .hasMessageContaining("fallingPercent");
    assertThatThrownBy(() -> new SellWeights(15, 1, THREE, 0, HALF, 10))
        .hasMessageContaining("injuryMatches");
    assertThatThrownBy(() -> new SellWeights(15, 1, THREE, 2, new BigDecimal("1.1"), 10))
        .hasMessageContaining("lowValueShare");
    assertThatThrownBy(() -> new SellWeights(15, 1, THREE, 2, HALF, 0))
        .hasMessageContaining("limit");
  }
}
