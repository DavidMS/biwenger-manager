# #15 — Biwenger authentication and league context

- **Issue:** [#15 [2.2] Biwenger authentication and league context](https://github.com/DavidMS/biwenger-manager/issues/15)
- **Phase:** 2 — Biwenger API client (read-only)
- **Date:** 2026-10-22

## Prompt

> merged, continue with #15

## What Claude did

1. Moved #14 to *Done* and #15 to *In progress*, branched, and read the `login` and `account` pages written in #14. They settled the details the plan had left open: login has no `{status, data}` envelope, and `X-User` is your id in the league, not your account id.
2. Checked library versions on Maven Central itself. The search API's index stopped in mid-2025 and would have suggested an old WireMock. WireMock 4 is still in beta after 41 releases, so it uses the stable 3.13.2, as the standalone jar, which bundles its own Jetty and Jackson so they can't clash with Spring Boot 4's.
3. Wrote the client in `biwenger-client`, still a plain library (ADR 0004):
   - **`BiwengerSession`** logs in on first use and keeps the token in memory only.
   - **`AuthInterceptor`** adds the token and the `X-League`/`X-User` headers. On a `401` it logs in again once and retries; a second `401` throws, so a bad token can't cause a login loop.
   - **`BiwengerClient.league()`** reads the account once and uses its single La Liga league. It fails with "must have exactly one La Liga league, but it has N" otherwise.
   - **Secrets:** the credential, login-request and properties records override `toString()`. No exception carries the email, password, token or a response body.
4. Wired it in `app.biwenger`, which per `backend/CLAUDE.md` is the only package that sees the client:
   - `BiwengerProperties` holds the `biwenger.*` settings, filled from `BIWENGER_EMAIL`/`BIWENGER_PASSWORD`.
   - `BiwengerConfiguration` builds the `RestClient` with the base URL, timeouts and User-Agent, using Boot 4's new `spring-boot-starter-restclient`.
   - The app still starts without a Biwenger login; only calls to Biwenger fail.
5. Tested with WireMock, stubbing the account call with the real anonymised `account.json` from #14, which Maven copies onto the test classpath straight from `docs/biwenger-api/samples/`. In total, 18 client tests and 2 app tests, including the three the issue asks for: success, bad credentials and re-login on an expired token. To check the tests can fail, Claude disabled the re-login: exactly the two re-login tests failed.
6. Added a README note on the item left open in #10: the Biwenger login reaches the container as environment variables, readable with `docker inspect`.

## Review

`java-reviewer`: **approve with nits.** It confirmed the points Claude had asked it to check: running the request twice is safe, there's no deadlock between the two locks, and nothing leaks. It also found real problems:

- **minor, a latent crash.** The interceptor recognised the account call by its full URL path. With a path prefix in the base URL (for example a proxy), the account call would ask for the league, which makes the account call, and so on until `StackOverflowError`. **Fixed** the way the reviewer suggested: the account call goes through a token-only client, so the interceptor doesn't look at paths at all. Claude ran the new regression test against the old code and got the `StackOverflowError`.
- **minor:** some failures escaped as Spring exceptions, whose messages include the response body, and that body can be the account's personal data. A `429` on login would have said "check your credentials". **Fixed:** failures become `BiwengerApiException` with the status and path only, and only 400, 401 and 403 count as bad credentials.
- **nit:** two branches had no test, a 401 on the account call and two threads renewing the same expired token. **Added.**
- **Left for later:** a generic-type overload of `get` for #16, which needs it, and a note that the static WireMock setup would clash with parallel tests.

## Corrections

- **WireMock and the JDK HTTP client disagreed about HTTP/2.** On plain `http://`, the JDK client tries an "h2c" upgrade, and WireMock's Jetty breaks it on requests with a body. The error was "Received RST_STREAM". Biwenger is HTTPS, where HTTP/2 is negotiated properly, so the fix is test-only: WireMock runs with plain-text HTTP/2 off, with a comment saying why. The reviewer agreed that production needs no change.
- **A test assertion didn't compile** (`containsEntry` on a `Map<?, ?>`).

Human review may add more corrections. Record them here before merging.

## Decisions made along the way

- **The client takes a configured `RestClient`** and adds its own headers and interceptor through `mutate()`. The app owns the transport settings (ADR 0004).
- **Credentials are checked when logging in, not at startup,** so the app and its tests run without a Biwenger login.
- **No real Biwenger call yet.** Nothing in the app calls the client until the endpoint methods (#16) and the sync jobs (Phase 3), so the first real login will happen then.
- **Full error mapping, retries and rate limiting stay in 2.5.** This issue only makes sure no failure leaks a secret or blames the wrong cause.

## Lesson

Ask the reviewer about the parts you are least sure of. Claude's prompt listed its own doubts (running the request twice, the two locks, Spring's exceptions carrying response bodies), and the reviewer either confirmed each one or turned it into a concrete finding. It also found the recursion that nobody had asked about. A regression test only counts once it has been seen to fail on the old code.
