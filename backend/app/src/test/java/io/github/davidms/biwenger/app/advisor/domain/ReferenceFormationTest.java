package io.github.davidms.biwenger.app.advisor.domain;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import io.github.davidms.biwenger.app.common.Money;
import io.github.davidms.biwenger.app.player.domain.PlayerProfile;
import io.github.davidms.biwenger.app.player.domain.PlayerStatus;
import io.github.davidms.biwenger.app.player.domain.Position;
import java.math.BigDecimal;
import java.util.EnumMap;
import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.Test;

class ReferenceFormationTest {

  @Test
  void rejectsAMissingPosition() {
    assertThatThrownBy(() -> new ReferenceFormation(Map.of(Position.GOALKEEPER, 1)))
        .isInstanceOf(IllegalArgumentException.class)
        .hasMessageContaining("DEFENDER");
  }

  @Test
  void rejectsANegativeCount() {
    Map<Position, Integer> starters = new EnumMap<>(ReferenceFormation.DEFAULT.starters());
    starters.put(Position.FORWARD, -1);

    assertThatThrownBy(() -> new ReferenceFormation(starters)).hasMessageContaining("FORWARD");
  }

  @Test
  void keepsItsOwnCopyInPositionOrder() {
    Map<Position, Integer> starters = new EnumMap<>(ReferenceFormation.DEFAULT.starters());
    ReferenceFormation formation = new ReferenceFormation(starters);
    starters.put(Position.DEFENDER, 9);

    assertThat(formation.startersAt(Position.DEFENDER)).isEqualTo(4);
    assertThat(formation.starters().keySet()).containsExactly(Position.values());
  }

  @Test
  void ranksAPositionByExpectedPointsThenPlayerId() {
    List<ScoredPlayer> squad =
        List.of(
            player(3, Position.DEFENDER, "4.00"),
            player(9, Position.MIDFIELDER, "9.00"),
            player(2, Position.DEFENDER, "6.00"),
            player(1, Position.DEFENDER, "4.00"));

    assertThat(ReferenceFormation.depthChart(squad, Position.DEFENDER))
        .extracting(s -> s.player().id())
        .containsExactly(2L, 1L, 3L);
    assertThat(ReferenceFormation.depthChart(squad, Position.FORWARD)).isEmpty();
  }

  private static ScoredPlayer player(long id, Position position, String expected) {
    BigDecimal points = new BigDecimal(expected);
    return new ScoredPlayer(
        new PlayerProfile(
            id, "Player " + id, "player-" + id, 3, position, PlayerStatus.AVAILABLE, Money.ZERO),
        new PlayerScore(
            id,
            points,
            points,
            1,
            points,
            1,
            points,
            Venue.UNKNOWN,
            1,
            0,
            BigDecimal.ZERO,
            BigDecimal.ONE,
            BigDecimal.ONE,
            Money.ZERO,
            BigDecimal.ZERO,
            7,
            BigDecimal.ZERO));
  }
}
