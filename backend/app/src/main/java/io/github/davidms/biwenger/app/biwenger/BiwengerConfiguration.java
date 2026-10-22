package io.github.davidms.biwenger.app.biwenger;

import io.github.davidms.biwenger.client.BiwengerClient;
import io.github.davidms.biwenger.client.BiwengerCredentials;
import java.net.http.HttpClient;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpHeaders;
import org.springframework.http.client.JdkClientHttpRequestFactory;
import org.springframework.web.client.RestClient;

/** Builds the Biwenger client: base URL, timeouts and User-Agent are set here (ADR 0004). */
@Configuration
@EnableConfigurationProperties(BiwengerProperties.class)
class BiwengerConfiguration {

  static final String USER_AGENT =
      "biwenger-manager/0.1 (personal use; +https://github.com/DavidMS/biwenger-manager)";

  @Bean
  BiwengerClient biwengerClient(RestClient.Builder builder, BiwengerProperties properties) {
    var httpClient = HttpClient.newBuilder().connectTimeout(properties.connectTimeout()).build();
    var requestFactory = new JdkClientHttpRequestFactory(httpClient);
    requestFactory.setReadTimeout(properties.readTimeout());
    RestClient.Builder common =
        builder.defaultHeader(HttpHeaders.USER_AGENT, USER_AGENT).requestFactory(requestFactory);
    return new BiwengerClient(
        common.clone().baseUrl(properties.baseUrl()).build(),
        common.clone().baseUrl(properties.cdnBaseUrl()).build(),
        new BiwengerCredentials(properties.email(), properties.password()));
  }
}
