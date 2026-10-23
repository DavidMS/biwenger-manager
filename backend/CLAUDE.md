# Backend conventions

Java 25, Spring Boot 4.1, Maven multi-module. These rules add to the root `CLAUDE.md`. The `backend-feature` skill gives the order of work for a new feature, and this file wins when the two disagree.

Use JDK 25 (`sdk env` here, or `JAVA_HOME`). `./mvnw verify` runs the enforcer rules, the tests and the Spotless format check, and CI runs the same command.

## Modules

| Module | Base package | Holds | Must not depend on |
|---|---|---|---|
| `biwenger-client` | `io.github.davidms.biwenger.client` | HTTP calls to Biwenger, auth, rate limiting, retries, Biwenger's request and response records | Spring Boot, Spring MVC/WebFlux, the database, `app` (ADR 0004; the enforcer rule in its `pom.xml` fails the build) |
| `app` | `io.github.davidms.biwenger.app` | Domain, persistence, sync jobs, advisor algorithms, REST API, all Spring configuration | Biwenger's records outside the `biwenger` package |

`biwenger-client` is a plain library. It takes a configured `RestClient` in its constructor. `app` builds that `RestClient` (base URL, timeouts, User-Agent) and wires the client as a bean. Only `app.biwenger` calls the client: feature services ask `app.biwenger` and get domain types back, so they never see Biwenger's records.

Biwenger's JSON is read into records in `io.github.davidms.biwenger.client.model`, one per object in `docs/biwenger-api/`. They mirror the JSON, not our domain:

- Names follow the JSON. Rename only with `@JsonProperty` (`scoreID` → `scoreId`). Money is whole euros, and times stay Unix seconds as `long`.
- **A field is required unless it is marked `@Nullable` (JSpecify).** Jackson 3 already fails on a missing primitive; required objects and strings get an `Objects.requireNonNull` in the compact constructor. Either way a field Biwenger drops shows up as "doesn't match docs/biwenger-api/" instead of a silent 0 or `null`. Optional values are boxed and `@Nullable`; a list Biwenger leaves out becomes empty.
- Maps keyed by id are sorted by id, so iterating them is deterministic.
- Unknown fields are ignored by the client's own JSON mapper, whatever the caller configures. Add a field to a record only when a feature needs it, together with a test against its sample.

## Package layout in `app`

Packages are by feature, not by layer. Inside a feature, the subpackages are layers:

```
io.github.davidms.biwenger.app
  BiwengerManagerApplication
  <feature>/           e.g. squad, market, standings, advisor, actions
    domain/            records, enums, sealed types, pure logic
    persistence/       JPA entities, Spring Data repositories
    service/           use cases (@Service)
    web/               @RestController, request and response DTOs
  biwenger/            the only code that sees biwenger-client's records: maps them to domain types
  config/              @Configuration and @ConfigurationProperties shared by several features
  web/                 cross-feature web code: the @RestControllerAdvice
```

Create a subpackage when it gets its first class, not before. A feature may use another feature's `domain` and `service`, never its `persistence` or `web`.

## Domain, entities and DTOs

Three kinds of types, never mixed:

| Kind | Where | Looks like | Annotations |
|---|---|---|---|
| Domain | `<feature>/domain` | `record Player(long id, String name, Position position, int price)` | None: no Spring, JPA or Jackson |
| Entity | `<feature>/persistence` | `class PlayerSnapshotEntity` | JPA only |
| DTO | `<feature>/web` | `record PlayerResponse(...)`, `record BidRequest(...)` | Jackson and Bean Validation only |

- Services take and return domain types. Entities don't leave `persistence`, and DTOs don't leave `web`.
- Map by hand: a `toDomain()` method on the entity, a `static from(domainObject)` factory on the response DTO. No MapStruct or reflection mappers.
- Validate invariants in the record's compact constructor (`if (price < 0) throw new IllegalArgumentException(...)`).
- Money is a whole number of euros, as Biwenger sends it: `long` for budgets, team values and sums, so they can't overflow. No `double` for money.
- The domain names its own things. Biwenger's field names and codes (for example position `1`–`4`) are translated in `app.biwenger`, so a change in Biwenger's JSON touches the client records and that package only.

## Java style

- Records for data, sealed interfaces for closed sets of variants, `switch` with pattern matching over them. No Lombok.
- Constructor injection only, with no `@Autowired` on fields. One constructor, so Spring needs no annotation.
- `Optional` for a value that may be absent in a return type, never `null`. Don't use `Optional` for fields or parameters.
- Configuration through `@ConfigurationProperties` records under a `biwenger.*` prefix, set by environment variables (`BIWENGER_EMAIL` binds to `biwenger.email`). Spring doesn't read `.env`: Docker Compose passes it in, and for `./mvnw spring-boot:run` you export the variables in your shell. No secrets in `application.properties`.
- A properties record that holds a secret overrides `toString()` to mask it, because a record's generated `toString()` prints every field.
- Code that depends on the time takes a `java.time.Clock` bean, so tests can fix it and recommendations stay deterministic (root golden rule 5). The app's zone is `Europe/Madrid`, La Liga's and Biwenger's time: the `Clock` bean uses it, and dates shown to the user (rounds, market closing) are in it.
- Formatting is google-java-format through Spotless. The edit hook formats each file, and `./mvnw spotless:apply` fixes the whole tree.

## Error handling

- The API reports errors as RFC 9457 `ProblemDetail`, through one `@RestControllerAdvice` in `app.web` that extends `ResponseEntityExceptionHandler`, so Spring's own errors (failed `@Valid`, unreadable JSON, unknown route) come back as `ProblemDetail` too. Controllers don't catch exceptions to build error responses.
- Domain and service code throw specific unchecked exceptions (for example `PlayerNotFoundException`). The advice maps each one to a status: not found → 404, invalid input from the request → 400, a Biwenger failure → 502. Anything unexpected is a 500, including an `IllegalArgumentException` from a compact constructor fed bad Biwenger data: that is our bug or theirs, not the user's.
- `biwenger-client` wraps every HTTP, network or parsing failure in `BiwengerApiException`, which says what failed: "answered HTTP 503 to GET …", "did not answer … in time", "could not reach …", "doesn't match docs/biwenger-api/". It never returns `null` or an empty object instead of failing. `BiwengerAuthenticationException` is the subclass for bad credentials.
- The client is gentle on its own (`BiwengerClientSettings`, set from `biwenger.*` in `app`): at least `min-request-interval` (500 ms) between any two requests, and a `GET` that hits a 5xx or a network error is retried with exponential backoff (`max-attempts` 3, `initial-backoff` 1 s, `max-backoff` 10 s). **Only `GET`s are retried.** A write must never be sent twice automatically, so don't add retries to `POST`, `PUT` or `DELETE` calls.
- Log a failure once, where it is handled, with the reason. Never log tokens, passwords or full Biwenger responses.
- Don't catch an exception just to ignore it. If skipping is the right behaviour, log why.

## Tests

| What | How |
|---|---|
| Domain and advisor logic | Plain JUnit 5 + AssertJ, no Spring context and no mocks. Cover ties, empty input and limits. |
| Services | JUnit 5 with hand-written fakes or Mockito for their dependencies |
| Controllers | `@WebMvcTest(XController.class)` with the service mocked through `@MockitoBean`. Check status, JSON shape and `ProblemDetail` errors. |
| Biwenger client | WireMock serving files from `docs/biwenger-api/samples/`. Never the real API (root golden rule 3). |
| Persistence, migrations, sync jobs | `@DataJpaTest` or `@SpringBootTest` with a Testcontainers `PostgreSQLContainer` wired through `@ServiceConnection` |
| The whole app | A few `@SpringBootTest(webEnvironment = RANDOM_PORT)` tests, like `HealthEndpointTest` |

- Test classes are package-private, in the same package as the code, named `<ClassUnderTest>Test`. Integration tests may be named after the behaviour instead, like `HealthEndpointTest`.
- Method names describe behaviour: `breaksScoreTiesByPlayerId()`, not `test1()`.
- Assert with AssertJ (`assertThat`), not JUnit's `assertEquals`.
- Fix the clock and sort inputs explicitly. A test must give the same result on every run.

## Persistence (from Phase 3)

- The schema changes only through Flyway migrations in `app/src/main/resources/db/migration`, named `V<n>__<what>.sql`. Never edit a migration that is on `main`: add a new one.
- Hibernate validates the schema and never changes it (`spring.jpa.hibernate.ddl-auto=validate`).
- `@Transactional` goes on service methods only, never on controllers or repositories you write. `spring.jpa.open-in-view=false`, so entities are not loaded lazily during JSON rendering.
