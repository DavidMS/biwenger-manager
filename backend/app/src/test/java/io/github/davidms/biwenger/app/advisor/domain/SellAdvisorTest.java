package io.github.davidms.biwenger.app.advisor.domain;

import static org.assertj.core.api.Assertions.assertThat;

import io.github.davidms.biwenger.app.advisor.domain.SellRecommendation.CoveredBy;
import io.github.davidms.biwenger.app.advisor.domain.SellRecommendation.NotAStarter;
import io.github.davidms.biwenger.app.advisor.domain.SellSignal.FallingValue;
import io.github.davidms.biwenger.app.advisor.domain.SellSignal.LongInjury;
import io.github.davidms.biwenger.app.advisor.domain.SellSignal.LowValueForMoney;
import io.github.davidms.biwenger.app.advisor.domain.SellSignal.Surplus;
import io.github.davidms.biwenger.app.common.Money;
import io.github.davidms.biwenger.app.player.domain.PlayerProfile;
import io.github.davidms.biwenger.app.player.domain.PlayerStatus;
import io.github.davidms.biwenger.app.player.domain.Position;
import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;
import org.junit.jupiter.api.Test;

class SellAdvisorTest {

  private static final ReferenceFormation FORMATION = ReferenceFormation.DEFAULT;

  /** No minimum squad size, so each test only meets the rule it's about. */
  private static final SellWeights LOOSE = weights(0, 10);

  private static SellWeights weights(int minSquadSize, int limit) {
    return new SellWeights(minSquadSize, 1, new BigDecimal("3"), 2, new BigDecimal("0.5"), limit);
  }

  private record P(
      long id, Position position, String expected, PlayerStatus status, String ppm, String trend) {

    SellCandidate candidate(int injuryStreak) {
      PlayerProfile profile =
          new PlayerProfile(
              id, "Player " + id, "player-" + id, 3, position, status, Money.euros(5_000_000));
      BigDecimal points = new BigDecimal(expected);
      return new SellCandidate(
          new ScoredPlayer(
              profile,
              new PlayerScore(
                  id,
                  points,
                  points,
                  5,
                  points,
                  5,
                  points,
                  Venue.UNKNOWN,
                  3,
                  2,
                  BigDecimal.ZERO,
                  BigDecimal.ONE,
                  BigDecimal.ONE,
                  Money.ZERO,
                  new BigDecimal(trend),
                  7,
                  new BigDecimal(ppm))),
          injuryStreak);
    }

    SellCandidate candidate() {
      return candidate(0);
    }
  }

  private static P p(long id, Position position, String expected) {
    return new P(id, position, expected, PlayerStatus.AVAILABLE, "6.00", "0.00");
  }

  private static P falling(long id, Position position, String expected, String trend) {
    return new P(id, position, expected, PlayerStatus.AVAILABLE, "6.00", trend);
  }

  /**
   * Fifteen players, none with a signal: goalkeepers 6, 3; defenders 7, 6, 5, 4, 2; midfielders 8,
   * 7, 6, 5, 3; forwards 9, 6, 2. Each position has its starters plus one backup.
   */
  private static List<SellCandidate> squad(SellCandidate... replacing) {
    List<SellCandidate> squad = new ArrayList<>();
    List<P> base =
        List.of(
            p(1, Position.GOALKEEPER, "6.00"),
            p(2, Position.GOALKEEPER, "3.00"),
            p(11, Position.DEFENDER, "7.00"),
            p(12, Position.DEFENDER, "6.00"),
            p(13, Position.DEFENDER, "5.00"),
            p(14, Position.DEFENDER, "4.00"),
            p(15, Position.DEFENDER, "2.00"),
            p(21, Position.MIDFIELDER, "8.00"),
            p(22, Position.MIDFIELDER, "7.00"),
            p(23, Position.MIDFIELDER, "6.00"),
            p(24, Position.MIDFIELDER, "5.00"),
            p(25, Position.MIDFIELDER, "3.00"),
            p(31, Position.FORWARD, "9.00"),
            p(32, Position.FORWARD, "6.00"),
            p(33, Position.FORWARD, "2.00"));
    for (P player : base) {
      SellCandidate replacement = null;
      for (SellCandidate candidate : replacing) {
        if (candidate.player().player().id() == player.id()) {
          replacement = candidate;
        }
      }
      squad.add(replacement != null ? replacement : player.candidate());
    }
    for (SellCandidate candidate : replacing) {
      if (squad.stream().noneMatch(c -> c == candidate)) {
        squad.add(candidate);
      }
    }
    return squad;
  }

  private static List<Long> ids(List<SellRecommendation> sells) {
    return sells.stream().map(s -> s.player().player().id()).toList();
  }

  @Test
  void suggestsNothingWhenNoSignalFires() {
    assertThat(SellAdvisor.recommend(squad(), 0, FORMATION, LOOSE)).isEmpty();
  }

  @Test
  void flagsAValueFallingByTheThresholdOrMore() {
    List<SellRecommendation> sells =
        SellAdvisor.recommend(
            squad(
                falling(15, Position.DEFENDER, "2.00", "-3.00").candidate(),
                falling(33, Position.FORWARD, "2.00", "-2.99").candidate()),
            0,
            FORMATION,
            LOOSE);

    assertThat(ids(sells)).containsExactly(15L);
    assertThat(sells.getFirst().signals())
        .containsExactly(new FallingValue(new BigDecimal("3.00"), 7));
  }

  @Test
  void flagsAnInjuryOnlyAfterEnoughMatchesMissed() {
    List<SellRecommendation> sells =
        SellAdvisor.recommend(
            squad(
                new P(2, Position.GOALKEEPER, "0.00", PlayerStatus.INJURED, "6.00", "0.00")
                    .candidate(2),
                new P(15, Position.DEFENDER, "0.00", PlayerStatus.INJURED, "6.00", "0.00")
                    .candidate(1),
                new P(33, Position.FORWARD, "1.00", PlayerStatus.DOUBTFUL, "6.00", "0.00")
                    .candidate(3)),
            0,
            FORMATION,
            LOOSE);

    assertThat(ids(sells)).containsExactly(2L);
    assertThat(sells.getFirst().signals()).containsExactly(new LongInjury(2));
  }

  @Test
  void flagsPointsPerMillionUnderAShareOfTheSquadMedian() {
    List<SellRecommendation> sells =
        SellAdvisor.recommend(
            squad(
                new P(25, Position.MIDFIELDER, "3.00", PlayerStatus.AVAILABLE, "2.99", "0.00")
                    .candidate(),
                new P(33, Position.FORWARD, "2.00", PlayerStatus.AVAILABLE, "3.00", "0.00")
                    .candidate()),
            0,
            FORMATION,
            LOOSE);

    // Median of thirteen 6.00s, a 3.00 and a 2.99 is 6.00; half of it is 3.00.
    assertThat(ids(sells)).containsExactly(25L);
    assertThat(sells.getFirst().signals())
        .containsExactly(
            new LowValueForMoney(
                new BigDecimal("2.99"), new BigDecimal("6.00"), new BigDecimal("3.00")));
  }

  @Test
  void flagsPlayersDeeperThanTheStartersAndBackups() {
    List<SellRecommendation> sells =
        SellAdvisor.recommend(
            squad(p(16, Position.DEFENDER, "1.00").candidate()), 0, FORMATION, LOOSE);

    // Four starters and one backup kept: the sixth defender is surplus.
    assertThat(ids(sells)).containsExactly(16L);
    assertThat(sells.getFirst().signals()).containsExactly(new Surplus(6, 5));
    assertThat(sells.getFirst().cover()).isEqualTo(new NotAStarter());
  }

  @Test
  void measuresTheXiLossAgainstWhoWouldStartInstead() {
    List<SellRecommendation> sells =
        SellAdvisor.recommend(
            squad(
                falling(15, Position.DEFENDER, "2.00", "-5.00").candidate(),
                falling(14, Position.DEFENDER, "4.00", "-5.00").candidate()),
            0,
            FORMATION,
            LOOSE);

    SellRecommendation bench = sells.get(0);
    assertThat(bench.player().player().id()).isEqualTo(15L);
    assertThat(bench.xiLoss()).isEqualTo(new BigDecimal("0.00"));
    // Only one of the two can go: four defenders must stay. The bench player costs nothing.
    assertThat(sells).hasSize(1);

    List<SellRecommendation> starterOnly =
        SellAdvisor.recommend(
            squad(falling(14, Position.DEFENDER, "4.00", "-5.00").candidate()),
            0,
            FORMATION,
            LOOSE);
    // Player 15 (2.00) would start instead: 4.00 − 2.00.
    SellRecommendation starter = starterOnly.getFirst();
    assertThat(starter.cover())
        .isEqualTo(new CoveredBy(p(15, Position.DEFENDER, "2.00").candidate().player()));
    assertThat(starter.xiLoss()).isEqualTo(new BigDecimal("2.00"));
  }

  @Test
  void neverSuggestsAStarterNobodyCouldReplace() {
    // Only the two forwards the formation needs.
    List<SellCandidate> squad =
        new ArrayList<>(squad().stream().filter(c -> c.player().player().id() != 33).toList());
    squad.removeIf(c -> c.player().player().id() == 32);
    squad.add(falling(32, Position.FORWARD, "6.00", "-9.00").candidate());

    assertThat(SellAdvisor.recommend(squad, 0, FORMATION, LOOSE)).isEmpty();
  }

  @Test
  void ranksByXiLossThenSignalsThenFallThenId() {
    List<SellRecommendation> sells =
        SellAdvisor.recommend(
            squad(
                falling(15, Position.DEFENDER, "2.00", "-3.00").candidate(),
                falling(33, Position.FORWARD, "2.00", "-5.00").candidate(),
                falling(24, Position.MIDFIELDER, "5.00", "-10.00").candidate(),
                new P(25, Position.MIDFIELDER, "3.00", PlayerStatus.AVAILABLE, "1.00", "-3.00")
                    .candidate(),
                falling(2, Position.GOALKEEPER, "3.00", "-5.00").candidate()),
            0,
            FORMATION,
            LOOSE);

    // 25: no loss, two signals. 2 and 33: no loss, one signal, −5.00 (by id). 15: no loss, −3.00.
    // 24 is a starter (loss 2.00), and with 25 sold nobody would cover him, so he stays.
    assertThat(ids(sells)).containsExactly(25L, 2L, 33L, 15L);
  }

  @Test
  void keepsTheMinimumSquadSizeAcrossAllSuggestedSales() {
    List<SellCandidate> squad =
        squad(
            falling(15, Position.DEFENDER, "2.00", "-3.00").candidate(),
            falling(33, Position.FORWARD, "2.00", "-5.00").candidate());

    assertThat(ids(SellAdvisor.recommend(squad, 0, FORMATION, weights(14, 10))))
        .containsExactly(33L);
    assertThat(SellAdvisor.recommend(squad, 0, FORMATION, weights(15, 10))).isEmpty();
  }

  @Test
  void countsUnscoredPlayersTowardTheMinimumSquadSize() {
    List<SellCandidate> squad = squad(falling(15, Position.DEFENDER, "2.00", "-3.00").candidate());

    assertThat(SellAdvisor.recommend(squad, 0, FORMATION, weights(15, 10))).isEmpty();
    assertThat(ids(SellAdvisor.recommend(squad, 1, FORMATION, weights(15, 10))))
        .containsExactly(15L);
  }

  @Test
  void neverSellsTheLastPlayerOfAPositionWithNoStarters() {
    List<SellRecommendation> sells =
        SellAdvisor.recommend(
            squad(
                new P(41, Position.COACH, "0.00", PlayerStatus.AVAILABLE, "0.00", "-9.00")
                    .candidate()),
            0,
            FORMATION,
            LOOSE);

    assertThat(sells).isEmpty();
  }

  @Test
  void returnsAtMostTheLimit() {
    List<SellCandidate> squad =
        squad(
            falling(15, Position.DEFENDER, "2.00", "-3.00").candidate(),
            falling(33, Position.FORWARD, "2.00", "-5.00").candidate());

    assertThat(ids(SellAdvisor.recommend(squad, 0, FORMATION, weights(0, 1)))).containsExactly(33L);
  }

  @Test
  void givesTheSameAnswerWhateverTheOrderOfTheSquad() {
    List<SellCandidate> squad =
        squad(
            falling(15, Position.DEFENDER, "2.00", "-3.00").candidate(),
            falling(33, Position.FORWARD, "2.00", "-5.00").candidate(),
            p(16, Position.DEFENDER, "1.00").candidate());

    assertThat(SellAdvisor.recommend(squad.reversed(), 0, FORMATION, LOOSE))
        .isEqualTo(SellAdvisor.recommend(squad, 0, FORMATION, LOOSE));
  }

  @Test
  void takesTheMedianOfOddAndEvenSquads() {
    assertThat(SellAdvisor.medianPointsPerMillion(List.of())).isEqualTo(new BigDecimal("0.00"));
    assertThat(
            SellAdvisor.medianPointsPerMillion(
                List.of(ppm(1, "1.00"), ppm(2, "9.00"), ppm(3, "4.00"))))
        .isEqualTo(new BigDecimal("4.00"));
    // (4.00 + 4.01) / 2 = 4.005 → 4.01
    assertThat(
            SellAdvisor.medianPointsPerMillion(
                List.of(ppm(1, "1.00"), ppm(2, "4.01"), ppm(3, "4.00"), ppm(4, "9.00"))))
        .isEqualTo(new BigDecimal("4.01"));
  }

  private static ScoredPlayer ppm(long id, String ppm) {
    return new P(id, Position.DEFENDER, "1.00", PlayerStatus.AVAILABLE, ppm, "0.00")
        .candidate()
        .player();
  }

  @Test
  void explainsASurplusPlayerWithEveryNumber() {
    SellCandidate player =
        new P(16, Position.DEFENDER, "1.00", PlayerStatus.INJURED, "2.00", "-3.00").candidate(2);

    SellRecommendation sell = SellAdvisor.recommend(squad(player), 0, FORMATION, LOOSE).getFirst();

    assertThat(sell.explanation())
        .isEqualTo(
            "Expected 1.00 points, value 5,000,000 €. Value down 3.00% in 7 days. Injured, missed"
                + " his last 2 matches. 2.00 season points per million, under 3.00 (your squad's"
                + " median is 6.00). Defender number 6 of yours, behind the 5 you keep. Your XI"
                + " loses 0.00: he doesn't start.");
  }

  @Test
  void explainsWhoWouldStartInstead() {
    SellRecommendation sell =
        SellAdvisor.recommend(
                squad(falling(14, Position.DEFENDER, "4.00", "-5.00").candidate()),
                0,
                FORMATION,
                LOOSE)
            .getFirst();

    assertThat(sell.explanation())
        .isEqualTo(
            "Expected 4.00 points, value 5,000,000 €. Value down 5.00% in 7 days. Your XI loses"
                + " 2.00: Player 15 (2.00) would start instead.");
  }

  @Test
  void ranksCoveredStartersByTheirXiLoss() {
    List<SellRecommendation> sells =
        SellAdvisor.recommend(
            squad(
                falling(23, Position.MIDFIELDER, "6.00", "-5.00").candidate(),
                falling(14, Position.DEFENDER, "4.00", "-5.00").candidate()),
            0,
            FORMATION,
            LOOSE);

    // Defender 14: 4.00 − 2.00 (player 15). Midfielder 23: 6.00 − 3.00 (player 25).
    assertThat(ids(sells)).containsExactly(14L, 23L);
    assertThat(sells)
        .extracting(SellRecommendation::xiLoss)
        .containsExactly(new BigDecimal("2.00"), new BigDecimal("3.00"));
  }

  @Test
  void neverSuggestsTwoStartersWhoShareTheirCover() {
    List<SellRecommendation> sells =
        SellAdvisor.recommend(
            squad(
                falling(12, Position.DEFENDER, "6.00", "-5.00").candidate(),
                falling(13, Position.DEFENDER, "5.00", "-5.00").candidate(),
                p(16, Position.DEFENDER, "1.00").candidate()),
            0,
            FORMATION,
            LOOSE);

    // 16 is surplus. 13 (loss 3.00) and 12 (loss 4.00) would both be covered by 15: only 13 goes.
    assertThat(ids(sells)).containsExactly(16L, 13L);
  }

  @Test
  void neverSellsThePlayerNamedToCoverAnotherSale() {
    List<SellRecommendation> sells =
        SellAdvisor.recommend(
            squad(
                new P(14, Position.DEFENDER, "2.00", PlayerStatus.AVAILABLE, "1.00", "-5.00")
                    .candidate(),
                falling(15, Position.DEFENDER, "2.00", "-5.00").candidate(),
                p(16, Position.DEFENDER, "1.00").candidate()),
            0,
            FORMATION,
            LOOSE);

    // 14 and 15 tie on 2.00; 14 starts (lower id) with 15 as his cover, losing 0.00. 14 has two
    // signals, so he ranks first and 15, now his cover, isn't sold.
    assertThat(ids(sells)).containsExactly(14L, 16L);
  }

  @Test
  void sellsAnExtraPlayerOfAPositionWithNoStarters() {
    List<SellRecommendation> sells =
        SellAdvisor.recommend(
            squad(
                p(41, Position.COACH, "0.00").candidate(),
                p(42, Position.COACH, "1.00").candidate()),
            0,
            FORMATION,
            LOOSE);

    // No starters and one backup kept: the second coach is surplus, and one coach stays.
    assertThat(ids(sells)).containsExactly(41L);
    assertThat(sells.getFirst().signals()).containsExactly(new Surplus(2, 1));
  }

  @Test
  void saysMatchForASingleMissedMatch() {
    SellWeights oneMatch = new SellWeights(0, 1, new BigDecimal("3"), 1, new BigDecimal("0.5"), 10);
    SellCandidate player =
        new P(2, Position.GOALKEEPER, "0.00", PlayerStatus.INJURED, "6.00", "0.00").candidate(1);

    assertThat(
            SellAdvisor.recommend(squad(player), 0, FORMATION, oneMatch).getFirst().explanation())
        .contains("Injured, missed his last 1 match.");
  }
}
