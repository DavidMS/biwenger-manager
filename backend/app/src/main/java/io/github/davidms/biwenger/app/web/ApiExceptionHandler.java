package io.github.davidms.biwenger.app.web;

import io.github.davidms.biwenger.app.sync.service.SyncAlreadyRunningException;
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

  @ExceptionHandler(SyncAlreadyRunningException.class)
  ProblemDetail syncAlreadyRunning(SyncAlreadyRunningException e) {
    var problem = ProblemDetail.forStatusAndDetail(HttpStatus.CONFLICT, e.getMessage());
    problem.setTitle("Sync already running");
    return problem;
  }
}
