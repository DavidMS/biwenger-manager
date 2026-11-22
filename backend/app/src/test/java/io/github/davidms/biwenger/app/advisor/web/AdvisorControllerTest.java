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
import io.github.davidms.biwenger.app.advisor.domain.Venue;
import io.github.davidms.biwenger.app.advisor.service.BuyAdvisorService;
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
}
