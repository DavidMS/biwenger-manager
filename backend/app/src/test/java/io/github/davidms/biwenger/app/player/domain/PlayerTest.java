package io.github.davidms.biwenger.app.player.domain;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import io.github.davidms.biwenger.app.common.Money;
import java.util.List;
import java.util.Set;
import org.junit.jupiter.api.Test;

class PlayerTest {

  private static Player player(Set<Position> otherPositions, Money price, int gamesHome) {
    return new Player(
        1,
        "Pedri",
        "pedri",
        3,
        Position.MIDFIELDER,
        otherPositions,
        price,
        Money.ZERO,
        PlayerStatus.AVAILABLE,
        "",
        List.of(new RoundForm.Played(6)),
        6,
        6,
        0,
        gamesHome,
        0);
  }

  @Test
  void canPlayHisOwnPositionAndTheOthersHeIsAllowed() {
    Player pedri = player(Set.of(Position.FORWARD), Money.euros(1), 1);

    assertThat(pedri.canPlay(Position.MIDFIELDER)).isTrue();
    assertThat(pedri.canPlay(Position.FORWARD)).isTrue();
    assertThat(pedri.canPlay(Position.DEFENDER)).isFalse();
  }

  @Test
  void otherPositionsIterateInTheSameOrderEveryTime() {
    Player player =
        player(Set.of(Position.FORWARD, Position.DEFENDER, Position.GOALKEEPER), Money.ZERO, 0);

    assertThat(player.otherPositions())
        .containsExactly(Position.GOALKEEPER, Position.DEFENDER, Position.FORWARD);
  }

  @Test
  void rejectsANegativePriceOrGamesCount() {
    assertThatThrownBy(() -> player(Set.of(), Money.euros(-1), 0))
        .isInstanceOf(IllegalArgumentException.class);
    assertThatThrownBy(() -> player(Set.of(), Money.ZERO, -1))
        .isInstanceOf(IllegalArgumentException.class);
  }
}
