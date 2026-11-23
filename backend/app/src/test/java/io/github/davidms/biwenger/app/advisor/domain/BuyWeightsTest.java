package io.github.davidms.biwenger.app.advisor.domain;

import static org.assertj.core.api.Assertions.assertThatThrownBy;

import org.junit.jupiter.api.Test;

class BuyWeightsTest {

  @Test
  void rejectsALimitBelowOne() {
    assertThatThrownBy(() -> new BuyWeights(0))
        .isInstanceOf(IllegalArgumentException.class)
        .hasMessageContaining("limit");
  }
}
