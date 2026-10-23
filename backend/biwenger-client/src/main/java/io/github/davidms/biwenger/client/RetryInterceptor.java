package io.github.davidms.biwenger.client;

import java.io.IOException;
import java.lang.System.Logger.Level;
import java.time.Duration;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpRequest;
import org.springframework.http.client.ClientHttpRequestExecution;
import org.springframework.http.client.ClientHttpRequestInterceptor;
import org.springframework.http.client.ClientHttpResponse;

/**
 * Tries a {@code GET} again after a 5xx answer or a network error, with exponential backoff. Other
 * methods are never repeated: a write (Phase 6) sent twice could place a bid twice. A 4xx is our
 * request's fault, so it is not retried either.
 */
final class RetryInterceptor implements ClientHttpRequestInterceptor {

  private static final System.Logger LOG = System.getLogger(RetryInterceptor.class.getName());

  private final BiwengerClientSettings settings;
  private final Pause pause;

  RetryInterceptor(BiwengerClientSettings settings, Pause pause) {
    this.settings = settings;
    this.pause = pause;
  }

  @Override
  public ClientHttpResponse intercept(
      HttpRequest request, byte[] body, ClientHttpRequestExecution execution) throws IOException {
    if (!HttpMethod.GET.equals(request.getMethod())) {
      return execution.execute(request, body);
    }
    for (int attempt = 1; ; attempt++) {
      boolean last = attempt == settings.maxAttempts();
      String failure;
      try {
        ClientHttpResponse response = execution.execute(request, body);
        try {
          if (last || !response.getStatusCode().is5xxServerError()) {
            return response;
          }
          failure = "HTTP " + response.getStatusCode().value();
        } catch (IOException | RuntimeException e) {
          response.close();
          throw e;
        }
        response.close();
      } catch (IOException e) {
        // An interrupt means stop, not try again. (Not by type: SocketTimeoutException is an
        // InterruptedIOException too, and a timeout is worth a retry.)
        if (last || Thread.currentThread().isInterrupted()) {
          throw e;
        }
        failure = e.getClass().getSimpleName();
      }
      Duration wait = settings.backoffAfter(attempt);
      LOG.log(
          Level.WARNING,
          "GET {0} failed ({1}); attempt {2} of {3} in {4} ms",
          request.getURI().getPath(),
          failure,
          attempt + 1,
          settings.maxAttempts(),
          wait.toMillis());
      pause.pauseOrFail(wait);
    }
  }
}
