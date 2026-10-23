# #18 — Client resilience: rate limiting, retries, timeouts, errors

- **Issue:** [#18 [2.5] Client resilience: rate limiting, retries, timeouts, error mapping](https://github.com/DavidMS/biwenger-manager/issues/18)
- **Phase:** 2 — Biwenger API client (read-only)
- **Date:** 2026-10-23

## Prompt

> merged, continue with #18

## What Claude did

1. Moved #17 to *Done* and #18 to *In progress*, and branched.
2. Built it with a few small classes and **no new library**:
   - **`BiwengerClientSettings`:** the minimum gap between requests, the number of attempts, and the first and maximum backoff.
   - **`RateLimiter`:** each request starts at least the gap after the previous one. One limiter is shared by everything: API, CDN, login, re-logins and retries.
   - **`RetryInterceptor`:** retries a `GET` on 5xx and network errors, with doubling backoff and a cap. It never retries a 4xx or anything but a `GET`.
   - **Error messages that say what happened:** "answered HTTP 503 to GET …", "did not answer … in time", "could not reach …".
3. **Chose to retry only `GET`s.** The only `POST` today is the login. In Phase 6 the writes will be `POST`s, and a retried bid could be placed twice. A test fails if the retry ever applies to a `POST`.
4. Put the interceptors in a fixed order on every connection: retry (outermost), then auth, then the rate limit (innermost). Every request that reaches the network waits its turn, including a retry and a re-login.
5. **Tested timing without waiting.** The client takes a clock and a pause, and the test versions record each pause and move time only when something pauses. So the tests assert exact waits ("100 ms, then 150 ms, capped") in about a second. The tests not about timing use a `NO_WAITING` setting: the suite had grown from 2 s to 30 s with the real defaults.
6. Made the settings configurable in the app (`biwenger.min-request-interval`, `max-attempts`, `initial-backoff`, `max-backoff`). Tested a **real** read timeout: WireMock answers after 2 s, the timeout is 300 ms, and the result is "did not answer GET /api/v2/market in time" after 2 attempts.
7. Broke the `GET`-only rule on purpose: the login was sent 3 times and the test caught it.
8. Updated `backend/CLAUDE.md` and the `biwenger-api` skill.

## Review

`java-reviewer`: **approve with nits.** It confirmed the interceptor order works and that no lock is held while waiting. All five findings applied:

- **An interrupted thread kept retrying.** The interrupt arrived as an `IOException`, which the loop treated like a network error. Fixed: an interrupt stops the call. It's checked with the thread's flag, not the exception type, because a timeout is also an `InterruptedIOException` and is worth retrying. The new test failed without the fix.
- **A response could leak** if reading its status failed. It is now closed.
- **The connect timeout had no test.** Testing it end to end needs an unroutable address, which is slow and flaky in CI. The mapping is now tested directly.
- **Nits:**
  - `backend/CLAUDE.md` now gives the worst case for one `GET` (about 80 s when Biwenger is down), with the rule that jobs call Biwenger one call at a time.
  - It also warns never to make the JDK resend `POST`s (see Corrections).

## Corrections

- **The JDK's HTTP client retries too.** A test expected 3 requests and saw 6: on a reset connection, the JDK client resends an idempotent `GET` once, by itself, as it would for a stale pooled connection. That is sensible, so Claude kept it. The test now counts *our* attempts by their backoff pauses, and the docs warn never to enable the JDK setting that would extend this to `POST`s.
- **An interrupted thread doesn't send at all.** Claude's first version of the interrupt test expected one request and got none: the JDK client refuses to send on an interrupted thread. The test now checks what matters: no backoff and no retry.

Human review may add more corrections. Record them here before merging.

## Decisions made along the way

- **No resilience library.** Resilience4j or Spring Retry would add a dependency and configuration for about a hundred lines of code we can test exactly.
- **Defaults: 500 ms between requests; 3 attempts, waiting 1 s, then 2 s.** That is gentle for a single user who syncs a few times a day.
- **4xx is never retried,** including 429: Biwenger telling us to slow down is not a reason to ask again. The rate limiter is how we avoid it.

## Lesson

Make time a parameter. Rate limits and backoff are about waiting, and tests that really wait are slow and flaky. With an injected clock and pause, every wait became a value the test can assert. And the most important rule here, "never resend a write", got a test of its own before the first write exists.
