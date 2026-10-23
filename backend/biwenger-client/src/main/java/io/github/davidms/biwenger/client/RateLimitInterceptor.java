package io.github.davidms.biwenger.client;

import java.io.IOException;
import org.springframework.http.HttpRequest;
import org.springframework.http.client.ClientHttpRequestExecution;
import org.springframework.http.client.ClientHttpRequestInterceptor;
import org.springframework.http.client.ClientHttpResponse;

/**
 * The last interceptor before the network, so every request waits its turn: retries and re-logins
 * included.
 */
final class RateLimitInterceptor implements ClientHttpRequestInterceptor {

  private final RateLimiter limiter;

  RateLimitInterceptor(RateLimiter limiter) {
    this.limiter = limiter;
  }

  @Override
  public ClientHttpResponse intercept(
      HttpRequest request, byte[] body, ClientHttpRequestExecution execution) throws IOException {
    limiter.acquire();
    return execution.execute(request, body);
  }
}
