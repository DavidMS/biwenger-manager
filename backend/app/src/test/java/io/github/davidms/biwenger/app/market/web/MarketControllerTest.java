package io.github.davidms.biwenger.app.market.web;

import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import io.github.davidms.biwenger.app.common.Money;
import io.github.davidms.biwenger.app.league.domain.Manager;
import io.github.davidms.biwenger.app.market.domain.Market;
import io.github.davidms.biwenger.app.market.domain.MarketListing;
import io.github.davidms.biwenger.app.market.domain.MarketPlayer;
import io.github.davidms.biwenger.app.market.domain.Ownership;
import io.github.davidms.biwenger.app.market.domain.Seller;
import io.github.davidms.biwenger.app.market.service.MarketService;
import io.github.davidms.biwenger.app.player.domain.DailyPrice;
import io.github.davidms.biwenger.app.player.domain.PlayerProfile;
import io.github.davidms.biwenger.app.player.domain.PlayerStatus;
import io.github.davidms.biwenger.app.player.domain.Position;
import io.github.davidms.biwenger.app.player.domain.Team;
import io.github.davidms.biwenger.client.BiwengerApiException;
import java.time.Instant;
import java.time.LocalDate;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

@WebMvcTest(MarketController.class)
class MarketControllerTest {

  private static final Instant LISTED = Instant.parse("2026-10-27T05:05:00Z");
  private static final Instant CLOSES = Instant.parse("2026-10-28T05:00:00Z");
  private static final LocalDate TODAY = LocalDate.of(2026, 10, 28);

  @Autowired MockMvc mvc;
  @MockitoBean MarketService markets;

  private static PlayerProfile player(long id, PlayerStatus status) {
    return new PlayerProfile(
        id, "Player " + id, "player-" + id, 3, Position.DEFENDER, status, Money.euros(5_000_000));
  }

  @Test
  void showsFreePlayersAndManagersSalesWithTheirNumbers() throws Exception {
    var free =
        new MarketPlayer(
            new MarketListing(1, new Seller.Game(), Money.euros(5_640_000), LISTED, CLOSES),
            player(1, PlayerStatus.DOUBTFUL),
            List.of(
                new DailyPrice(TODAY.minusDays(1), Money.euros(4_900_000)),
                new DailyPrice(TODAY, Money.euros(5_000_000))),
            40);
    var byManager =
        new MarketPlayer(
            new MarketListing(
                2,
                new Seller.ByManager(
                    new Manager(2_000_003, "Manager 3"),
                    new Ownership(
                        Money.euros(21_221_640), Money.euros(10_610_820), LISTED, LISTED)),
                Money.euros(6_930_000),
                LISTED,
                CLOSES),
            player(2, PlayerStatus.AVAILABLE),
            List.of(),
            0);
    when(markets.market())
        .thenReturn(
            new Market(
                List.of(free, byManager),
                List.of(99_999L),
                List.of(new Team(3, "Barcelona", "barcelona"))));

    mvc.perform(get("/api/market"))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.players[0].id").value(1))
        .andExpect(jsonPath("$.players[0].teamId").value(3))
        .andExpect(jsonPath("$.players[0].position").value("DEFENDER"))
        .andExpect(jsonPath("$.players[0].status").value("DOUBTFUL"))
        .andExpect(jsonPath("$.players[0].price").value(5_640_000))
        .andExpect(jsonPath("$.players[0].value").value(5_000_000))
        .andExpect(jsonPath("$.players[0].trendChange").value(100_000))
        .andExpect(jsonPath("$.players[0].trend[1].day").value("2026-10-28"))
        .andExpect(jsonPath("$.players[0].points").value(40))
        .andExpect(jsonPath("$.players[0].pointsPerMillion").value(7.09))
        .andExpect(jsonPath("$.players[0].closesAt").value("2026-10-28T05:00:00Z"))
        .andExpect(jsonPath("$.players[0].seller.free").value(true))
        .andExpect(jsonPath("$.players[0].seller.managerName").doesNotExist())
        .andExpect(jsonPath("$.players[1].seller.free").value(false))
        .andExpect(jsonPath("$.players[1].seller.managerId").value(2_000_003))
        .andExpect(jsonPath("$.players[1].seller.managerName").value("Manager 3"))
        .andExpect(jsonPath("$.players[1].pointsPerMillion").value(0.0))
        .andExpect(jsonPath("$.teams[0].id").value(3))
        .andExpect(jsonPath("$.teams[0].name").value("Barcelona"))
        .andExpect(jsonPath("$.missingPlayerIds[0]").value(99_999));
  }

  @Test
  void aBiwengerFailureIsBadGateway() throws Exception {
    when(markets.market())
        .thenThrow(new BiwengerApiException("Biwenger answered HTTP 503 to GET /api/v2/market"));

    mvc.perform(get("/api/market"))
        .andExpect(status().isBadGateway())
        .andExpect(jsonPath("$.title").value("Biwenger unavailable"));
  }
}
