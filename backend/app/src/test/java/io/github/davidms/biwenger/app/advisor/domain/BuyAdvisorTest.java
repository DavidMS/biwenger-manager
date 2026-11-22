package io.github.davidms.biwenger.app.advisor.domain;

import static org.assertj.core.api.Assertions.assertThat;

import io.github.davidms.biwenger.app.advisor.domain.BuyRecommendation.EmptySlot;
import io.github.davidms.biwenger.app.advisor.domain.BuyRecommendation.Replaces;
import io.github.davidms.biwenger.app.common.Money;
import io.github.davidms.biwenger.app.player.domain.PlayerProfile;
import io.github.davidms.biwenger.app.player.domain.PlayerStatus;
import io.github.davidms.biwenger.app.player.domain.Position;
import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.Test;

class BuyAdvisorTest {

  private static final Money BALANCE = Money.euros(10_000_000);
  private static final BuyWeights WEIGHTS = BuyWeights.DEFAULTS;

  private static ScoredPlayer player(long id, Position position, String expected) {
    return player(id, position, expected, PlayerStatus.AVAILABLE, "0.00");
  }

  private static ScoredPlayer player(
      long id, Position position, String expected, PlayerStatus status, String trendPercent) {
    PlayerProfile profile =
        new PlayerProfile(
            id, "Player " + id, "player-" + id, 3, position, status, Money.euros(5_000_000));
    BigDecimal points = new BigDecimal(expected);
    PlayerScore score =
        new PlayerScore(
            id,
            points,
            new BigDecimal("7.33"),
            5,
            new BigDecimal("6.00"),
            5,
            points,
            Venue.UNKNOWN,
            3,
            2,
            new BigDecimal("0.00"),
            new BigDecimal("1.0000"),
            status == PlayerStatus.AVAILABLE ? BigDecimal.ONE : new BigDecimal("0.5"),
            Money.ZERO,
            new BigDecimal(trendPercent),
            7,
            new BigDecimal("6.00"));
    return new ScoredPlayer(profile, score);
  }

  private static BuyCandidate onSale(ScoredPlayer player, long price) {
    return new BuyCandidate(player, Money.euros(price));
  }

  /** Four defenders (6, 5, 4, 2) and a fifth on the bench; four midfielders at 7. */
  private static List<ScoredPlayer> squad() {
    return List.of(
        player(1, Position.DEFENDER, "6.00"),
        player(2, Position.DEFENDER, "5.00"),
        player(3, Position.DEFENDER, "4.00"),
        player(4, Position.DEFENDER, "2.00"),
        player(5, Position.DEFENDER, "1.00"),
        player(11, Position.MIDFIELDER, "7.00"),
        player(12, Position.MIDFIELDER, "7.00"),
        player(13, Position.MIDFIELDER, "7.00"),
        player(14, Position.MIDFIELDER, "7.00"),
        player(21, Position.GOALKEEPER, "5.00"),
        player(31, Position.FORWARD, "6.00"));
  }

  @Test
  void measuresTheGainAgainstTheWeakestStarterNotTheBench() {
    List<BuyRecommendation> buys =
        BuyAdvisor.recommend(
            List.of(onSale(player(100, Position.DEFENDER, "5.00"), 2_000_000)),
            squad(),
            BALANCE,
            WEIGHTS);

    assertThat(buys).hasSize(1);
    BuyRecommendation buy = buys.getFirst();
    // Starters are the best four defenders (6, 5, 4, 2): he replaces player 4, not the bench's 1.
    assertThat(buy.slot()).isEqualTo(new Replaces(player(4, Position.DEFENDER, "2.00")));
    assertThat(buy.gain()).isEqualTo(new BigDecimal("3.00"));
    // 3.00 × 1,000,000 / 2,000,000 = 1.50
    assertThat(buy.gainPerMillion()).isEqualTo(new BigDecimal("1.50"));
    assertThat(buy.budgetLeft()).isEqualTo(Money.euros(8_000_000));
  }

  @Test
  void ranksAWeakLineAboveABetterPlayerInAStrongOne() {
    List<BuyRecommendation> buys =
        BuyAdvisor.recommend(
            List.of(
                onSale(player(100, Position.MIDFIELDER, "8.00"), 2_000_000),
                onSale(player(101, Position.DEFENDER, "5.00"), 2_000_000)),
            squad(),
            BALANCE,
            WEIGHTS);

    // Defender: 5 − 2 = 3. Midfielder: 8 − 7 = 1.
    assertThat(buys).extracting(b -> b.candidate().player().id()).containsExactly(101L, 100L);
    assertThat(buys)
        .extracting(BuyRecommendation::gain)
        .containsExactly(new BigDecimal("3.00"), new BigDecimal("1.00"));
  }

  @Test
  void fillsAnEmptyPlaceWithHisWholeExpectedPoints() {
    // One forward of the two the formation needs.
    List<BuyRecommendation> buys =
        BuyAdvisor.recommend(
            List.of(onSale(player(100, Position.FORWARD, "3.00"), 1_000_000)),
            squad(),
            BALANCE,
            WEIGHTS);

    assertThat(buys.getFirst().slot()).isEqualTo(new EmptySlot());
    assertThat(buys.getFirst().gain()).isEqualTo(new BigDecimal("3.00"));
  }

  @Test
  void leavesOutPlayersWhoWouldNotImproveTheXi() {
    List<BuyRecommendation> buys =
        BuyAdvisor.recommend(
            List.of(
                onSale(player(100, Position.MIDFIELDER, "7.00"), 1_000_000),
                onSale(player(101, Position.MIDFIELDER, "6.00"), 1_000_000)),
            squad(),
            BALANCE,
            WEIGHTS);

    assertThat(buys).isEmpty();
  }

  @Test
  void onlySuggestsWhatTheBalanceCanPay() {
    List<BuyRecommendation> buys =
        BuyAdvisor.recommend(
            List.of(
                onSale(player(100, Position.DEFENDER, "5.00"), 10_000_000),
                onSale(player(101, Position.DEFENDER, "9.00"), 10_000_001)),
            squad(),
            BALANCE,
            WEIGHTS);

    assertThat(buys).extracting(b -> b.candidate().player().id()).containsExactly(100L);
    assertThat(buys.getFirst().budgetLeft()).isEqualTo(Money.ZERO);
  }

  @Test
  void suggestsNothingWithANegativeBalance() {
    assertThat(
            BuyAdvisor.recommend(
                List.of(onSale(player(100, Position.DEFENDER, "5.00"), 0)),
                squad(),
                Money.euros(-1),
                WEIGHTS))
        .isEmpty();
  }

  @Test
  void neverSuggestsYourOwnPlayers() {
    // You put player 1 on sale yourself; he'd otherwise "replace" your weakest defender.
    List<BuyRecommendation> buys =
        BuyAdvisor.recommend(
            List.of(onSale(player(1, Position.DEFENDER, "6.00"), 1_000_000)),
            squad(),
            BALANCE,
            WEIGHTS);

    assertThat(buys).isEmpty();
  }

  @Test
  void skipsPositionsWithNoStarters() {
    List<BuyRecommendation> buys =
        BuyAdvisor.recommend(
            List.of(onSale(player(100, Position.COACH, "9.00"), 1_000_000)),
            squad(),
            BALANCE,
            WEIGHTS);

    assertThat(buys).isEmpty();
  }

  @Test
  void breaksGainTiesByGainPerMillionThenPlayerId() {
    List<BuyRecommendation> buys =
        BuyAdvisor.recommend(
            List.of(
                onSale(player(300, Position.DEFENDER, "5.00"), 2_000_000),
                onSale(player(200, Position.DEFENDER, "5.00"), 3_000_000),
                onSale(player(100, Position.DEFENDER, "5.00"), 3_000_000)),
            squad(),
            BALANCE,
            WEIGHTS);

    // Same gain 3.00: the cheaper one first (1.50 per million), then 1.00 per million by id.
    assertThat(buys).extracting(b -> b.candidate().player().id()).containsExactly(300L, 100L, 200L);
  }

  @Test
  void returnsAtMostTheLimit() {
    List<BuyCandidate> market = new ArrayList<>();
    for (int i = 0; i < 15; i++) {
      market.add(onSale(player(100 + i, Position.DEFENDER, "5.00"), 1_000_000));
    }

    assertThat(BuyAdvisor.recommend(market, squad(), BALANCE, WEIGHTS)).hasSize(10);
    assertThat(
            BuyAdvisor.recommend(market, squad(), BALANCE, new BuyWeights(WEIGHTS.starters(), 3)))
        .extracting(b -> b.candidate().player().id())
        .containsExactly(100L, 101L, 102L);
  }

  @Test
  void givesTheSameAnswerWhateverTheOrderOfTheInput() {
    List<BuyCandidate> market =
        List.of(
            onSale(player(100, Position.DEFENDER, "5.00"), 2_000_000),
            onSale(player(101, Position.FORWARD, "4.00"), 1_000_000),
            onSale(player(102, Position.MIDFIELDER, "8.00"), 3_000_000));

    assertThat(BuyAdvisor.recommend(market.reversed(), squad().reversed(), BALANCE, WEIGHTS))
        .isEqualTo(BuyAdvisor.recommend(market, squad(), BALANCE, WEIGHTS));
  }

  @Test
  void suggestsNothingFromAnEmptyMarket() {
    assertThat(BuyAdvisor.recommend(List.of(), squad(), BALANCE, WEIGHTS)).isEmpty();
  }

  @Test
  void anEmptySquadHasAnEmptyPlaceEverywhere() {
    List<BuyRecommendation> buys =
        BuyAdvisor.recommend(
            List.of(onSale(player(100, Position.GOALKEEPER, "4.00"), 1_000_000)),
            List.of(),
            BALANCE,
            new BuyWeights(
                Map.of(
                    Position.GOALKEEPER, 1,
                    Position.DEFENDER, 4,
                    Position.MIDFIELDER, 4,
                    Position.FORWARD, 2,
                    Position.COACH, 1),
                10));

    assertThat(buys.getFirst().slot()).isEqualTo(new EmptySlot());
  }

  @Test
  void explainsAReplacementWithEveryNumber() {
    ScoredPlayer candidate = player(100, Position.DEFENDER, "5.00", PlayerStatus.DOUBTFUL, "-2.50");

    String text =
        BuyAdvisor.explain(
            candidate,
            new Replaces(player(4, Position.DEFENDER, "2.00")),
            new BigDecimal("3.00"),
            new BigDecimal("1.50"),
            Money.euros(1_200_000));

    assertThat(text)
        .isEqualTo(
            "Expected 5.00 points (form 7.33, season average 6.00). Doubtful: expected points × 0.5."
                + " Would replace Player 4 (2.00) in your defence: +3.00 (1.50 per million of asking"
                + " price). Value down 2.50% in 7 days. 6.00 season points per million of value."
                + " Leaves 1,200,000 €.");
  }

  @Test
  void explainsAnEmptyPlaceAndARisingValue() {
    ScoredPlayer candidate = player(100, Position.FORWARD, "3.00", PlayerStatus.AVAILABLE, "5.77");

    String text =
        BuyAdvisor.explain(
            candidate, new EmptySlot(), new BigDecimal("3.00"), new BigDecimal("3.00"), Money.ZERO);

    assertThat(text)
        .isEqualTo(
            "Expected 3.00 points (form 7.33, season average 6.00). Fills an empty place in your"
                + " attack: +3.00 (3.00 per million of asking price). Value up 5.77% in 7 days. 6.00"
                + " season points per million of value. Leaves 0 €.");
  }

  @Test
  void callsAFlatValueSteady() {
    assertThat(
            BuyAdvisor.explain(
                player(100, Position.FORWARD, "3.00"),
                new EmptySlot(),
                new BigDecimal("3.00"),
                new BigDecimal("3.00"),
                Money.ZERO))
        .contains("Value steady in 7 days.");
  }
}
