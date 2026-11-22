package io.github.davidms.biwenger.app.advisor.domain;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import io.github.davidms.biwenger.app.player.domain.Position;
import java.util.EnumMap;
import java.util.Map;
import org.junit.jupiter.api.Test;

class BuyWeightsTest {

  @Test
  void rejectsAMissingPosition() {
    assertThatThrownBy(() -> new BuyWeights(Map.of(Position.GOALKEEPER, 1), 10))
        .isInstanceOf(IllegalArgumentException.class)
        .hasMessageContaining("DEFENDER");
  }

  @Test
  void rejectsANegativeCount() {
    Map<Position, Integer> starters = new EnumMap<>(BuyWeights.DEFAULTS.starters());
    starters.put(Position.FORWARD, -1);

    assertThatThrownBy(() -> new BuyWeights(starters, 10)).hasMessageContaining("FORWARD");
  }

  @Test
  void rejectsALimitBelowOne() {
    assertThatThrownBy(() -> new BuyWeights(BuyWeights.DEFAULTS.starters(), 0))
        .hasMessageContaining("limit");
  }

  @Test
  void keepsItsOwnCopyInPositionOrder() {
    Map<Position, Integer> starters = new EnumMap<>(BuyWeights.DEFAULTS.starters());
    BuyWeights weights = new BuyWeights(starters, 10);
    starters.put(Position.DEFENDER, 9);

    assertThat(weights.startersAt(Position.DEFENDER)).isEqualTo(4);
    assertThat(weights.starters().keySet()).containsExactly(Position.values());
  }
}
