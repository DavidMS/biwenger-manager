package io.github.davidms.biwenger.app.squad.web;

import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import io.github.davidms.biwenger.app.common.Money;
import io.github.davidms.biwenger.app.player.domain.DailyPrice;
import io.github.davidms.biwenger.app.player.domain.MatchResult;
import io.github.davidms.biwenger.app.player.domain.PlayerProfile;
import io.github.davidms.biwenger.app.player.domain.PlayerStatus;
import io.github.davidms.biwenger.app.player.domain.Position;
import io.github.davidms.biwenger.app.squad.domain.Squad;
import io.github.davidms.biwenger.app.squad.domain.SquadPlayer;
import io.github.davidms.biwenger.app.squad.service.SquadService;
import io.github.davidms.biwenger.client.BiwengerApiException;
import java.time.Instant;
import java.time.LocalDate;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

@WebMvcTest(SquadController.class)
class SquadControllerTest {

  private static final LocalDate TODAY = LocalDate.of(2026, 10, 28);
  private static final Instant MATCH = Instant.parse("2026-10-25T16:00:00Z");

  @Autowired MockMvc mvc;
  @MockitoBean SquadService squads;

  @Test
  void showsThePlayersWithTrendPointsAndForm() throws Exception {
    var injured =
        new PlayerProfile(
            2001,
            "Ayoze Pérez",
            "ayoze-perez",
            15,
            Position.FORWARD,
            PlayerStatus.INJURED,
            Money.euros(4_780_000));
    when(squads.squad())
        .thenReturn(
            new Squad(
                List.of(
                    new SquadPlayer(
                        injured,
                        List.of(
                            new DailyPrice(TODAY.minusDays(1), Money.euros(4_900_000)),
                            new DailyPrice(TODAY, Money.euros(4_780_000))),
                        40,
                        List.of(
                            new MatchResult(
                                2, 4908, MATCH, true, new MatchResult.Missed(PlayerStatus.INJURED)),
                            new MatchResult(
                                1, 4907, MATCH, false, new MatchResult.Played(-2, 90))))),
                List.of(33691L)));

    mvc.perform(get("/api/squad"))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.players[0].id").value(2001))
        .andExpect(jsonPath("$.players[0].name").value("Ayoze Pérez"))
        .andExpect(jsonPath("$.players[0].position").value("FORWARD"))
        .andExpect(jsonPath("$.players[0].status").value("INJURED"))
        .andExpect(jsonPath("$.players[0].price").value(4_780_000))
        .andExpect(jsonPath("$.players[0].trendChange").value(-120_000))
        .andExpect(jsonPath("$.players[0].trend[0].day").value("2026-10-27"))
        .andExpect(jsonPath("$.players[0].trend[0].price").value(4_900_000))
        .andExpect(jsonPath("$.players[0].points").value(40))
        .andExpect(jsonPath("$.players[0].form[0].played").value(false))
        .andExpect(jsonPath("$.players[0].form[0].reason").value("INJURED"))
        .andExpect(jsonPath("$.players[0].form[0].points").doesNotExist())
        .andExpect(jsonPath("$.players[0].form[1].played").value(true))
        .andExpect(jsonPath("$.players[0].form[1].points").value(-2))
        .andExpect(jsonPath("$.players[0].form[1].reason").doesNotExist())
        .andExpect(jsonPath("$.missingPlayerIds[0]").value(33691));
  }

  @Test
  void aBiwengerFailureIsBadGateway() throws Exception {
    when(squads.squad())
        .thenThrow(new BiwengerApiException("Biwenger answered HTTP 503 to GET /api/v2/user"));

    mvc.perform(get("/api/squad"))
        .andExpect(status().isBadGateway())
        .andExpect(jsonPath("$.title").value("Biwenger unavailable"));
  }
}
