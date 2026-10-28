package io.github.davidms.biwenger.app.dashboard.web;

import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import io.github.davidms.biwenger.app.common.Money;
import io.github.davidms.biwenger.app.dashboard.domain.Dashboard;
import io.github.davidms.biwenger.app.dashboard.service.DashboardService;
import io.github.davidms.biwenger.app.league.domain.ScheduledRound;
import io.github.davidms.biwenger.client.BiwengerApiException;
import java.time.Clock;
import java.time.Instant;
import java.time.ZoneId;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Import;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

@WebMvcTest(DashboardController.class)
@Import(DashboardControllerTest.FixedClock.class)
class DashboardControllerTest {

  private static final Instant NOW = Instant.parse("2026-10-28T10:00:00Z");

  @TestConfiguration
  static class FixedClock {
    @Bean
    Clock clock() {
      return Clock.fixed(NOW, ZoneId.of("Europe/Madrid"));
    }
  }

  @Autowired MockMvc mvc;
  @MockitoBean DashboardService dashboards;

  @Test
  void showsTheDashboardWithTheNextRound() throws Exception {
    when(dashboards.dashboard())
        .thenReturn(
            dashboard(
                Money.euros(-1_250_000),
                List.of(
                    new ScheduledRound(4907, "Jornada 9", NOW.minusSeconds(60)),
                    new ScheduledRound(
                        4908, "Jornada 10", Instant.parse("2026-10-30T19:00:00Z")))));

    mvc.perform(get("/api/dashboard"))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.balance").value(-1_250_000))
        .andExpect(jsonPath("$.balanceNegative").value(true))
        .andExpect(jsonPath("$.points").value(220))
        .andExpect(jsonPath("$.position").value(6))
        .andExpect(jsonPath("$.managers").value(6))
        .andExpect(jsonPath("$.teamValue").value(22_340_000))
        .andExpect(jsonPath("$.teamValueChange").value(-70_000))
        .andExpect(jsonPath("$.nextRound.name").value("Jornada 10"))
        .andExpect(jsonPath("$.nextRound.startsAt").value("2026-10-30T19:00:00Z"));
  }

  @Test
  void leavesOutTheNextRoundWhenNoneIsScheduled() throws Exception {
    when(dashboards.dashboard()).thenReturn(dashboard(Money.euros(51_420_000), List.of()));

    mvc.perform(get("/api/dashboard"))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.balanceNegative").value(false))
        .andExpect(jsonPath("$.nextRound").doesNotExist());
  }

  @Test
  void aBiwengerFailureIsBadGateway() throws Exception {
    when(dashboards.dashboard())
        .thenThrow(new BiwengerApiException("Biwenger answered HTTP 503 to GET /api/v2/account"));

    mvc.perform(get("/api/dashboard"))
        .andExpect(status().isBadGateway())
        .andExpect(jsonPath("$.title").value("Biwenger unavailable"));
  }

  private static Dashboard dashboard(Money balance, List<ScheduledRound> rounds) {
    return new Dashboard(balance, 220, 6, 6, Money.euros(22_340_000), Money.euros(-70_000), rounds);
  }
}
