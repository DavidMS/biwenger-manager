package io.github.davidms.biwenger.app.market.domain;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import io.github.davidms.biwenger.app.common.Money;
import io.github.davidms.biwenger.app.player.domain.DailyPrice;
import io.github.davidms.biwenger.app.player.domain.PlayerProfile;
import io.github.davidms.biwenger.app.player.domain.PlayerStatus;
import io.github.davidms.biwenger.app.player.domain.Position;
import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;
import java.util.List;
import org.junit.jupiter.api.Test;

class MarketPlayerTest {

  private static final Instant LISTED = Instant.parse("2026-10-28T05:05:00Z");
  private static final LocalDate DAY = LocalDate.of(2026, 10, 28);
  private static final PlayerProfile PLAYER =
      new PlayerProfile(
          16738,
          "Player",
          "player",
          3,
          Position.MIDFIELDER,
          PlayerStatus.AVAILABLE,
          Money.euros(5_000_000));

  private static MarketPlayer listedAt(long askingPrice, int points) {
    return new MarketPlayer(
        new MarketListing(
            16738, new Seller.Game(), Money.euros(askingPrice), LISTED, LISTED.plusSeconds(86_400)),
        PLAYER,
        List.of(),
        points);
  }

  @Test
  void pointsPerMillionUseTheAskingPriceToTwoDecimals() {
    assertThat(listedAt(5_640_000, 40).pointsPerMillion()).isEqualTo(new BigDecimal("7.09"));
    // 1 / 0.8 M = 1.25 exactly; 1 / 0.6 M = 1.666… rounds up.
    assertThat(listedAt(800_000, 1).pointsPerMillion()).isEqualTo(new BigDecimal("1.25"));
    assertThat(listedAt(600_000, 1).pointsPerMillion()).isEqualTo(new BigDecimal("1.67"));
    assertThat(listedAt(600_000, -1).pointsPerMillion()).isEqualTo(new BigDecimal("-1.67"));
  }

  @Test
  void aPlayerListedAtNoPriceHasZeroPointsPerMillion() {
    assertThat(listedAt(0, 40).pointsPerMillion()).isEqualTo(new BigDecimal("0.00"));
  }

  @Test
  void theTrendChangeIsTheLastDayMinusTheFirst() {
    var player =
        new MarketPlayer(
            listedAt(5_640_000, 40).listing(),
            PLAYER,
            List.of(
                new DailyPrice(DAY.minusDays(1), Money.euros(5_100_000)),
                new DailyPrice(DAY, Money.euros(5_000_000))),
            40);

    assertThat(player.trendChange()).isEqualTo(Money.euros(-100_000));
  }

  @Test
  void rejectsAListingJoinedWithAnotherPlayer() {
    var listing =
        new MarketListing(1, new Seller.Game(), Money.euros(1), LISTED, LISTED.plusSeconds(86_400));

    assertThatThrownBy(() -> new MarketPlayer(listing, PLAYER, List.of(), 0))
        .isInstanceOf(IllegalArgumentException.class);
  }
}
