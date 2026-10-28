package io.github.davidms.biwenger.app.squad.domain;

import static org.assertj.core.api.Assertions.assertThat;

import io.github.davidms.biwenger.app.common.Money;
import io.github.davidms.biwenger.app.player.domain.DailyPrice;
import io.github.davidms.biwenger.app.player.domain.PlayerProfile;
import io.github.davidms.biwenger.app.player.domain.PlayerStatus;
import io.github.davidms.biwenger.app.player.domain.Position;
import java.time.LocalDate;
import java.util.List;
import org.junit.jupiter.api.Test;

class SquadPlayerTest {

  private static final LocalDate DAY = LocalDate.of(2026, 10, 28);
  private static final PlayerProfile AYOZE =
      new PlayerProfile(
          2001,
          "Ayoze Pérez",
          "ayoze-perez",
          15,
          Position.FORWARD,
          PlayerStatus.AVAILABLE,
          Money.euros(4_780_000));

  private static SquadPlayer withTrend(DailyPrice... trend) {
    return new SquadPlayer(AYOZE, List.of(trend), 40, List.of());
  }

  @Test
  void theTrendChangeIsTheLastDayMinusTheFirst() {
    var player =
        withTrend(
            new DailyPrice(DAY.minusDays(2), Money.euros(4_700_000)),
            new DailyPrice(DAY.minusDays(1), Money.euros(4_900_000)),
            new DailyPrice(DAY, Money.euros(4_650_000)));

    assertThat(player.trendChange()).isEqualTo(Money.euros(-50_000));
  }

  @Test
  void aTrendOfOneDayOrNoneHasNoChange() {
    assertThat(withTrend().trendChange()).isEqualTo(Money.ZERO);
    assertThat(withTrend(new DailyPrice(DAY, Money.euros(4_780_000))).trendChange())
        .isEqualTo(Money.ZERO);
  }
}
