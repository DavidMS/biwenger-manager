package io.github.davidms.biwenger.app.advisor.web;

import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import io.github.davidms.biwenger.app.advisor.domain.BuyRecommendation;
import io.github.davidms.biwenger.app.advisor.domain.BuyRecommendation.EmptySlot;
import io.github.davidms.biwenger.app.advisor.domain.BuyRecommendation.Replaces;
import io.github.davidms.biwenger.app.advisor.domain.BuyRecommendations;
import io.github.davidms.biwenger.app.advisor.domain.PlayerScore;
import io.github.davidms.biwenger.app.advisor.domain.ScoredPlayer;
import io.github.davidms.biwenger.app.advisor.domain.SellRecommendation;
import io.github.davidms.biwenger.app.advisor.domain.SellRecommendation.CoveredBy;
import io.github.davidms.biwenger.app.advisor.domain.SellRecommendation.NotAStarter;
import io.github.davidms.biwenger.app.advisor.domain.SellRecommendations;
import io.github.davidms.biwenger.app.advisor.domain.SellSignal.FallingValue;
import io.github.davidms.biwenger.app.advisor.domain.SellSignal.LongInjury;
import io.github.davidms.biwenger.app.advisor.domain.SellSignal.LowValueForMoney;
import io.github.davidms.biwenger.app.advisor.domain.SellSignal.Surplus;
import io.github.davidms.biwenger.app.advisor.domain.Venue;
import io.github.davidms.biwenger.app.advisor.service.BuyAdvisorService;
import io.github.davidms.biwenger.app.advisor.service.SellAdvisorService;
import io.github.davidms.biwenger.app.common.Money;
import io.github.davidms.biwenger.app.player.domain.PlayerProfile;
import io.github.davidms.biwenger.app.player.domain.PlayerStatus;
import io.github.davidms.biwenger.app.player.domain.Position;
import io.github.davidms.biwenger.client.BiwengerApiException;
import io.github.davidms.biwenger.client.BiwengerAuthenticationException;
import java.math.BigDecimal;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

@WebMvcTest(AdvisorController.class)
class AdvisorControllerTest {

  @Autowired MockMvc mvc;
  @MockitoBean BuyAdvisorService buys;
  @MockitoBean SellAdvisorService sells;

  private static ScoredPlayer player(long id, String expected) {
    PlayerProfile profile =
        new PlayerProfile(
            id,
            "Player " + id,
            "player-" + id,
            3,
            Position.DEFENDER,
            PlayerStatus.AVAILABLE,
            Money.euros(5_000_000));
    BigDecimal points = new BigDecimal(expected);
    return new ScoredPlayer(
        profile,
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
            BigDecimal.ONE,
            Money.euros(60_000),
            new BigDecimal("5.77"),
            7,
            new BigDecimal("6.00")));
  }

  @Test
  void showsEachRecommendationWithItsScoreAndTheStarterItReplaces() throws Exception {
    var replacing =
        new BuyRecommendation(
            player(100, "5.00"),
            Money.euros(2_000_000),
            new Replaces(player(4, "2.00")),
            new BigDecimal("3.00"),
            new BigDecimal("1.50"),
            Money.euros(8_000_000),
            "Expected 5.00 points.");
    var filling =
        new BuyRecommendation(
            player(101, "1.00"),
            Money.euros(1_000_000),
            new EmptySlot(),
            new BigDecimal("1.00"),
            new BigDecimal("1.00"),
            Money.euros(9_000_000),
            "Fills an empty place.");
    when(buys.recommendations())
        .thenReturn(
            new BuyRecommendations(
                Money.euros(10_000_000),
                List.of(replacing, filling),
                List.of(99_999L),
                List.of(99_998L)));

    mvc.perform(get("/api/advisor/buys"))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.balance").value(10_000_000))
        .andExpect(jsonPath("$.recommendations[0].id").value(100))
        .andExpect(jsonPath("$.recommendations[0].name").value("Player 100"))
        .andExpect(jsonPath("$.recommendations[0].teamId").value(3))
        .andExpect(jsonPath("$.recommendations[0].position").value("DEFENDER"))
        .andExpect(jsonPath("$.recommendations[0].status").value("AVAILABLE"))
        .andExpect(jsonPath("$.recommendations[0].askingPrice").value(2_000_000))
        .andExpect(jsonPath("$.recommendations[0].value").value(5_000_000))
        .andExpect(jsonPath("$.recommendations[0].gain").value(3.00))
        .andExpect(jsonPath("$.recommendations[0].gainPerMillion").value(1.50))
        .andExpect(jsonPath("$.recommendations[0].budgetLeft").value(8_000_000))
        .andExpect(jsonPath("$.recommendations[0].replaces.id").value(4))
        .andExpect(jsonPath("$.recommendations[0].replaces.name").value("Player 4"))
        .andExpect(jsonPath("$.recommendations[0].replaces.expectedPoints").value(2.00))
        .andExpect(jsonPath("$.recommendations[0].score.expectedPoints").value(5.00))
        .andExpect(jsonPath("$.recommendations[0].score.recentForm").value(7.33))
        .andExpect(jsonPath("$.recommendations[0].score.venue").value("UNKNOWN"))
        .andExpect(jsonPath("$.recommendations[0].score.venueFactor").value(1.0))
        .andExpect(jsonPath("$.recommendations[0].score.valueTrend").value(60_000))
        .andExpect(jsonPath("$.recommendations[0].score.valueTrendPercent").value(5.77))
        .andExpect(jsonPath("$.recommendations[0].score.trendDays").value(7))
        .andExpect(jsonPath("$.recommendations[0].explanation").value("Expected 5.00 points."))
        .andExpect(jsonPath("$.recommendations[1].replaces").doesNotExist())
        .andExpect(jsonPath("$.missingSquadPlayerIds[0]").value(99_999))
        .andExpect(jsonPath("$.missingMarketPlayerIds[0]").value(99_998));
  }

  @Test
  void noLoginIsServiceUnavailable() throws Exception {
    when(buys.recommendations())
        .thenThrow(new BiwengerAuthenticationException("No Biwenger login is set"));

    mvc.perform(get("/api/advisor/buys"))
        .andExpect(status().isServiceUnavailable())
        .andExpect(jsonPath("$.title").value("Biwenger not connected"));
  }

  @Test
  void aBiwengerFailureIsBadGateway() throws Exception {
    when(buys.recommendations())
        .thenThrow(new BiwengerApiException("Biwenger answered HTTP 503 to GET /api/v2/market"));

    mvc.perform(get("/api/advisor/buys"))
        .andExpect(status().isBadGateway())
        .andExpect(jsonPath("$.title").value("Biwenger unavailable"));
  }

  @Test
  void showsEachSaleWithItsSignalsAndWhoWouldStartInstead() throws Exception {
    var bench =
        new SellRecommendation(
            player(16, "1.00"),
            List.of(
                new FallingValue(new BigDecimal("3.00"), 7),
                new LongInjury(2),
                new LowValueForMoney(
                    new BigDecimal("2.00"), new BigDecimal("6.00"), new BigDecimal("3.00")),
                new Surplus(6, 5)),
            new NotAStarter(),
            new BigDecimal("0.00"),
            "Your XI loses 0.00: he doesn't start.");
    var starter =
        new SellRecommendation(
            player(14, "4.00"),
            List.of(new FallingValue(new BigDecimal("5.00"), 7)),
            new CoveredBy(player(15, "2.00")),
            new BigDecimal("2.00"),
            "Player 15 (2.00) would start instead.");
    when(sells.recommendations())
        .thenReturn(new SellRecommendations(List.of(bench, starter), List.of(99_999L)));

    mvc.perform(get("/api/advisor/sells"))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.recommendations[0].id").value(16))
        .andExpect(jsonPath("$.recommendations[0].position").value("DEFENDER"))
        .andExpect(jsonPath("$.recommendations[0].value").value(5_000_000))
        .andExpect(jsonPath("$.recommendations[0].signals[0].type").value("FALLING_VALUE"))
        .andExpect(jsonPath("$.recommendations[0].signals[0].percent").value(3.00))
        .andExpect(jsonPath("$.recommendations[0].signals[0].days").value(7))
        .andExpect(jsonPath("$.recommendations[0].signals[0].depth").doesNotExist())
        .andExpect(jsonPath("$.recommendations[0].signals[1].type").value("LONG_INJURY"))
        .andExpect(jsonPath("$.recommendations[0].signals[1].matchesMissed").value(2))
        .andExpect(jsonPath("$.recommendations[0].signals[2].type").value("LOW_VALUE_FOR_MONEY"))
        .andExpect(jsonPath("$.recommendations[0].signals[2].pointsPerMillion").value(2.00))
        .andExpect(jsonPath("$.recommendations[0].signals[2].squadMedian").value(6.00))
        .andExpect(jsonPath("$.recommendations[0].signals[2].threshold").value(3.00))
        .andExpect(jsonPath("$.recommendations[0].signals[3].type").value("SURPLUS"))
        .andExpect(jsonPath("$.recommendations[0].signals[3].depth").value(6))
        .andExpect(jsonPath("$.recommendations[0].signals[3].kept").value(5))
        .andExpect(jsonPath("$.recommendations[0].cover.type").value("NOT_A_STARTER"))
        .andExpect(jsonPath("$.recommendations[0].cover.id").doesNotExist())
        .andExpect(jsonPath("$.recommendations[0].xiLoss").value(0.00))
        .andExpect(jsonPath("$.recommendations[0].score.expectedPoints").value(1.00))
        .andExpect(
            jsonPath("$.recommendations[0].explanation")
                .value("Your XI loses 0.00: he doesn't start."))
        .andExpect(jsonPath("$.recommendations[1].cover.type").value("COVERED_BY"))
        .andExpect(jsonPath("$.recommendations[1].cover.id").value(15))
        .andExpect(jsonPath("$.recommendations[1].cover.name").value("Player 15"))
        .andExpect(jsonPath("$.recommendations[1].cover.expectedPoints").value(2.00))
        .andExpect(jsonPath("$.recommendations[1].xiLoss").value(2.00))
        .andExpect(jsonPath("$.missingPlayerIds[0]").value(99_999));
  }

  @Test
  void aBiwengerFailureOnSellsIsBadGateway() throws Exception {
    when(sells.recommendations())
        .thenThrow(new BiwengerApiException("Biwenger answered HTTP 503 to GET /api/v2/user"));

    mvc.perform(get("/api/advisor/sells"))
        .andExpect(status().isBadGateway())
        .andExpect(jsonPath("$.title").value("Biwenger unavailable"));
  }
}
