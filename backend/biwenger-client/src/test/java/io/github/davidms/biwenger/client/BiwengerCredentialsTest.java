package io.github.davidms.biwenger.client;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.Test;

class BiwengerCredentialsTest {

  @Test
  void toStringHidesEmailAndPassword() {
    var credentials = new BiwengerCredentials("manager@example.com", "s3cret-password");

    assertThat(credentials.toString())
        .isEqualTo("BiwengerCredentials[email=<set>, password=<set>]")
        .doesNotContain("manager@example.com", "s3cret-password");
  }

  @Test
  void missingValuesAreEmptyAndIncomplete() {
    var credentials = new BiwengerCredentials(null, "");

    assertThat(credentials.isComplete()).isFalse();
    assertThat(credentials.toString())
        .isEqualTo("BiwengerCredentials[email=<empty>, password=<empty>]");
  }

  @Test
  void loginRequestToStringHidesTheBody() {
    assertThat(new AuthRecords.LoginRequest("manager@example.com", "s3cret-password").toString())
        .doesNotContain("manager@example.com", "s3cret-password");
  }
}
