package io.github.davidms.biwenger.app.web;

import io.github.davidms.biwenger.app.sync.service.SyncAlreadyRunningException;
import io.github.davidms.biwenger.client.BiwengerApiException;
import io.github.davidms.biwenger.client.BiwengerAuthenticationException;
import java.lang.System.Logger.Level;
import org.springframework.http.HttpStatus;
import org.springframework.http.ProblemDetail;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.servlet.mvc.method.annotation.ResponseEntityExceptionHandler;

/**
 * The one place where exceptions become HTTP errors, as RFC 9457 {@code ProblemDetail}
 * (backend/CLAUDE.md). Extending {@link ResponseEntityExceptionHandler} makes Spring's own errors
 * (bad JSON, unknown route, wrong method) come back the same way.
 */
@RestControllerAdvice
class ApiExceptionHandler extends ResponseEntityExceptionHandler {

  // The messages are built from statuses, paths and fixed text: safe to log (no bodies, no tokens).
  private static final System.Logger LOG = System.getLogger(ApiExceptionHandler.class.getName());

  /** No login set, or Biwenger rejects it: the app can't reach the account until that's fixed. */
  @ExceptionHandler(BiwengerAuthenticationException.class)
  ProblemDetail biwengerNotConnected(BiwengerAuthenticationException e) {
    LOG.log(Level.WARNING, "Biwenger not connected: {0}", e.getMessage());
    var problem = ProblemDetail.forStatusAndDetail(HttpStatus.SERVICE_UNAVAILABLE, e.getMessage());
    problem.setTitle("Biwenger not connected");
    return problem;
  }

  /** Biwenger failed or answered something we can't read. Its message says which call. */
  @ExceptionHandler(BiwengerApiException.class)
  ProblemDetail biwengerFailed(BiwengerApiException e) {
    LOG.log(Level.WARNING, "Biwenger unavailable: {0}", e.getMessage());
    var problem = ProblemDetail.forStatusAndDetail(HttpStatus.BAD_GATEWAY, e.getMessage());
    problem.setTitle("Biwenger unavailable");
    return problem;
  }

  @ExceptionHandler(SyncAlreadyRunningException.class)
  ProblemDetail syncAlreadyRunning(SyncAlreadyRunningException e) {
    var problem = ProblemDetail.forStatusAndDetail(HttpStatus.CONFLICT, e.getMessage());
    problem.setTitle("Sync already running");
    return problem;
  }
}
