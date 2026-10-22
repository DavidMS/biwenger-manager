package io.github.davidms.biwenger.client;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import java.util.List;

/** Request and response bodies of the login and account calls (docs/biwenger-api/). */
final class AuthRecords {

  private AuthRecords() {}

  record LoginRequest(String email, String password) {

    @Override
    public String toString() {
      return "LoginRequest[<hidden>]";
    }
  }

  /** The only response without the {@code {status, data}} envelope. */
  @JsonIgnoreProperties(ignoreUnknown = true)
  record LoginResponse(String token) {

    @Override
    public String toString() {
      return "LoginResponse[<hidden>]";
    }
  }

  /** Only the fields needed to find the league; {@code GET /api/v2/account} returns many more. */
  @JsonIgnoreProperties(ignoreUnknown = true)
  record AccountResponse(Data data) {

    @JsonIgnoreProperties(ignoreUnknown = true)
    record Data(List<League> leagues) {}

    @JsonIgnoreProperties(ignoreUnknown = true)
    record League(long id, String competition, User user) {}

    @JsonIgnoreProperties(ignoreUnknown = true)
    record User(long id) {}
  }
}
