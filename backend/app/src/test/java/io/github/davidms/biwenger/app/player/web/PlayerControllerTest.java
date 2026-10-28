package io.github.davidms.biwenger.app.player.web;

import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import io.github.davidms.biwenger.app.common.Money;
import io.github.davidms.biwenger.app.player.domain.DailyPrice;
import io.github.davidms.biwenger.app.player.domain.MatchResult;
import io.github.davidms.biwenger.app.player.domain.PlayerDetail;
import io.github.davidms.biwenger.app.player.domain.PlayerProfile;
import io.github.davidms.biwenger.app.player.domain.PlayerStatus;
import io.github.davidms.biwenger.app.player.domain.Position;
import io.github.davidms.biwenger.app.player.domain.Team;
import io.github.davidms.biwenger.app.player.service.PlayerReader;
import java.time.Instant;
import java.time.LocalDate;
import java.util.List;
import java.util.Optional;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

@WebMvcTest(PlayerController.class)
class PlayerControllerTest {

  private static final LocalDate DAY = LocalDate.of(2026, 10, 28);
  private static final Instant MATCH = Instant.parse("2026-10-25T16:00:00Z");
  private static final PlayerDetail AYOZE =
      new PlayerDetail(
          new PlayerProfile(
              2001,
              "Ayoze",
              "ayoze-perez",
              19,
              Position.FORWARD,
              PlayerStatus.DOUBTFUL,
              Money.euros(4_780_000)),
          List.of(
              new DailyPrice(DAY.minusDays(1), Money.euros(4_700_000)),
              new DailyPrice(DAY, Money.euros(4_780_000))),
          40,
          List.of(
              new MatchResult(1, 4907, MATCH, true, new MatchResult.Played(-2, 90)),
              new MatchResult(
                  2,
                  4908,
                  MATCH.plusSeconds(604_800),
                  false,
                  new MatchResult.Missed(PlayerStatus.INJURED))));

  @Autowired MockMvc mvc;
  @MockitoBean PlayerReader players;

  @Test
  void showsThePlayersHistoryMatchesAndClub() throws Exception {
    when(players.detail(2001)).thenReturn(Optional.of(AYOZE));
    when(players.team(19)).thenReturn(Optional.of(new Team(19, "Villarreal", "villarreal")));

    mvc.perform(get("/api/players/2001"))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.id").value(2001))
        .andExpect(jsonPath("$.name").value("Ayoze"))
        .andExpect(jsonPath("$.teamId").value(19))
        .andExpect(jsonPath("$.teamName").value("Villarreal"))
        .andExpect(jsonPath("$.position").value("FORWARD"))
        .andExpect(jsonPath("$.status").value("DOUBTFUL"))
        .andExpect(jsonPath("$.value").value(4_780_000))
        .andExpect(jsonPath("$.points").value(40))
        .andExpect(jsonPath("$.prices[0].day").value("2026-10-27"))
        .andExpect(jsonPath("$.prices[0].price").value(4_700_000))
        .andExpect(jsonPath("$.matches[0].roundId").value(4907))
        .andExpect(jsonPath("$.matches[0].date").value("2026-10-25T16:00:00Z"))
        .andExpect(jsonPath("$.matches[0].home").value(true))
        .andExpect(jsonPath("$.matches[0].played").value(true))
        .andExpect(jsonPath("$.matches[0].points").value(-2))
        .andExpect(jsonPath("$.matches[0].minutes").value(90))
        .andExpect(jsonPath("$.matches[0].reason").doesNotExist())
        .andExpect(jsonPath("$.matches[1].played").value(false))
        .andExpect(jsonPath("$.matches[1].reason").value("INJURED"))
        .andExpect(jsonPath("$.matches[1].points").doesNotExist());
  }

  @Test
  void leavesOutTheClubNameWhenTheClubIsNotStored() throws Exception {
    when(players.detail(2001)).thenReturn(Optional.of(AYOZE));
    when(players.team(19)).thenReturn(Optional.empty());

    mvc.perform(get("/api/players/2001"))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.teamId").value(19))
        .andExpect(jsonPath("$.teamName").doesNotExist());
  }

  @Test
  void aPlayerNeverStoredIsNotFound() throws Exception {
    when(players.detail(99)).thenReturn(Optional.empty());

    mvc.perform(get("/api/players/99"))
        .andExpect(status().isNotFound())
        .andExpect(jsonPath("$.title").value("Player not found"))
        .andExpect(jsonPath("$.detail").value("No player 99 has been synced"));
  }
}
