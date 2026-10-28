package io.github.davidms.biwenger.app.league.web;

import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import io.github.davidms.biwenger.app.common.Money;
import io.github.davidms.biwenger.app.league.domain.LeagueEntry;
import io.github.davidms.biwenger.app.league.domain.LeagueTable;
import io.github.davidms.biwenger.app.league.domain.Manager;
import io.github.davidms.biwenger.app.league.domain.RivalSquad;
import io.github.davidms.biwenger.app.league.domain.Standing;
import io.github.davidms.biwenger.app.league.service.ManagerNotFoundException;
import io.github.davidms.biwenger.app.league.service.StandingsService;
import io.github.davidms.biwenger.app.player.domain.PlayerProfile;
import io.github.davidms.biwenger.app.player.domain.PlayerStatus;
import io.github.davidms.biwenger.app.player.domain.Position;
import io.github.davidms.biwenger.client.BiwengerApiException;
import java.time.Instant;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

@WebMvcTest(StandingsController.class)
class StandingsControllerTest {

  private static final Instant SAVED = Instant.parse("2026-10-26T10:00:00Z");

  @Autowired MockMvc mvc;
  @MockitoBean StandingsService standings;

  private static LeagueEntry entry(long id, String name, int position, int points) {
    return new LeagueEntry(
        new Standing(
            new Manager(id, name), position, points, Money.euros(64_530_000), Money.euros(-70_000)),
        List.of(2001L),
        SAVED);
  }

  @Test
  void showsTheTableAndMarksYourRow() throws Exception {
    when(standings.table())
        .thenReturn(
            new LeagueTable(
                List.of(
                    entry(2_000_004, "Manager 4", 1, 515), entry(2_000_001, "Manager 1", 2, 220)),
                2_000_001));

    mvc.perform(get("/api/standings"))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.standings[0].managerId").value(2_000_004))
        .andExpect(jsonPath("$.standings[0].name").value("Manager 4"))
        .andExpect(jsonPath("$.standings[0].position").value(1))
        .andExpect(jsonPath("$.standings[0].points").value(515))
        .andExpect(jsonPath("$.standings[0].teamValue").value(64_530_000))
        .andExpect(jsonPath("$.standings[0].teamValueChange").value(-70_000))
        .andExpect(jsonPath("$.standings[0].you").value(false))
        .andExpect(jsonPath("$.standings[1].you").value(true));
  }

  @Test
  void showsAManagersSquad() throws Exception {
    var player =
        new PlayerProfile(
            2001,
            "Ayoze",
            "ayoze-perez",
            19,
            Position.FORWARD,
            PlayerStatus.INJURED,
            Money.euros(4_780_000));
    when(standings.squad(2_000_004))
        .thenReturn(
            new RivalSquad(
                entry(2_000_004, "Manager 4", 1, 515),
                List.of(new RivalSquad.Member(player, 40)),
                List.of(33691L)));

    mvc.perform(get("/api/standings/2000004/squad"))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.managerId").value(2_000_004))
        .andExpect(jsonPath("$.name").value("Manager 4"))
        .andExpect(jsonPath("$.position").value(1))
        .andExpect(jsonPath("$.teamValue").value(64_530_000))
        .andExpect(jsonPath("$.lineupSavedAt").value("2026-10-26T10:00:00Z"))
        .andExpect(jsonPath("$.players[0].id").value(2001))
        .andExpect(jsonPath("$.players[0].position").value("FORWARD"))
        .andExpect(jsonPath("$.players[0].status").value("INJURED"))
        .andExpect(jsonPath("$.players[0].value").value(4_780_000))
        .andExpect(jsonPath("$.players[0].points").value(40))
        .andExpect(jsonPath("$.missingPlayerIds[0]").value(33691));
  }

  @Test
  void anUnknownManagerIsNotFound() throws Exception {
    when(standings.squad(42)).thenThrow(new ManagerNotFoundException(42));

    mvc.perform(get("/api/standings/42/squad"))
        .andExpect(status().isNotFound())
        .andExpect(jsonPath("$.title").value("Manager not found"))
        .andExpect(jsonPath("$.detail").value("No manager 42 in the league"));
  }

  @Test
  void aBiwengerFailureIsBadGateway() throws Exception {
    when(standings.table())
        .thenThrow(new BiwengerApiException("Biwenger answered HTTP 503 to GET /rounds/league"));

    mvc.perform(get("/api/standings"))
        .andExpect(status().isBadGateway())
        .andExpect(jsonPath("$.title").value("Biwenger unavailable"));
  }
}
