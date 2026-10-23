package io.github.davidms.biwenger.app;

import static org.assertj.core.api.Assertions.assertThat;

import io.github.davidms.biwenger.app.biwenger.BiwengerProperties;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Import;

/**
 * The safety net in src/test/resources/config/application.properties is in place: without WireMock,
 * a test's Biwenger URLs lead nowhere (golden rule 3). In #78 a test without it called the real
 * public CDN.
 */
@SpringBootTest
@Import(TestcontainersConfiguration.class)
class NoRealBiwengerInTestsTest {

  @Autowired BiwengerProperties properties;

  @Test
  void biwengerUrlsPointNowhereByDefault() {
    assertThat(properties.baseUrl()).hasToString("http://127.0.0.1:9");
    assertThat(properties.cdnBaseUrl()).hasToString("http://127.0.0.1:9");
  }
}
