package io.github.davidms.biwenger.client;

/** Request and response bodies of the login call (docs/biwenger-api/login.md). */
final class AuthRecords {

  private AuthRecords() {}

  record LoginRequest(String email, String password) {

    @Override
    public String toString() {
      return "LoginRequest[<hidden>]";
    }
  }

  /** The only response without the {@code {status, data}} envelope. */
  record LoginResponse(String token) {

    @Override
    public String toString() {
      return "LoginResponse[<hidden>]";
    }
  }
}
