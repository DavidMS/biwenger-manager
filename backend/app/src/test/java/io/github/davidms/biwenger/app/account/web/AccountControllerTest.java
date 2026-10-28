package io.github.davidms.biwenger.app.account.web;

import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import io.github.davidms.biwenger.app.account.domain.ConnectedAccount;
import io.github.davidms.biwenger.app.account.service.AccountService;
import io.github.davidms.biwenger.client.BiwengerApiException;
import io.github.davidms.biwenger.client.BiwengerAuthenticationException;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.http.MediaType;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

@WebMvcTest(AccountController.class)
class AccountControllerTest {

  @Autowired MockMvc mvc;
  @MockitoBean AccountService accounts;

  @Test
  void showsTheConnectedAccount() throws Exception {
    when(accounts.connectedAccount())
        .thenReturn(new ConnectedAccount("Account Owner", "Manager 1", "Example League"));

    mvc.perform(get("/api/account"))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.accountName").value("Account Owner"))
        .andExpect(jsonPath("$.teamName").value("Manager 1"))
        .andExpect(jsonPath("$.leagueName").value("Example League"));
  }

  @Test
  void noLoginIsServiceUnavailable() throws Exception {
    when(accounts.connectedAccount())
        .thenThrow(
            new BiwengerAuthenticationException(
                "Biwenger credentials are not set: define BIWENGER_EMAIL and BIWENGER_PASSWORD"));

    mvc.perform(get("/api/account"))
        .andExpect(status().isServiceUnavailable())
        .andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_PROBLEM_JSON))
        .andExpect(jsonPath("$.title").value("Biwenger not connected"))
        .andExpect(
            jsonPath("$.detail").value(org.hamcrest.Matchers.containsString("BIWENGER_EMAIL")));
  }

  @Test
  void aBiwengerFailureIsBadGateway() throws Exception {
    when(accounts.connectedAccount())
        .thenThrow(new BiwengerApiException("Biwenger answered HTTP 503 to GET /api/v2/account"));

    mvc.perform(get("/api/account"))
        .andExpect(status().isBadGateway())
        .andExpect(jsonPath("$.title").value("Biwenger unavailable"))
        .andExpect(jsonPath("$.detail").value("Biwenger answered HTTP 503 to GET /api/v2/account"));
  }
}
