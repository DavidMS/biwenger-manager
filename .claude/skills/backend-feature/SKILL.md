---
name: backend-feature
description: How to add a backend feature to the Java 25 / Spring Boot 4 app in backend/, end to end - domain, service, REST controller, DTOs and tests (unit, WebMvc, WireMock, Testcontainers). Use when writing or changing Java code under backend/.
---

# Add a backend feature

> **Provisional.** The backend doesn't exist yet. Phase 1 (#12) creates `backend/CLAUDE.md` with the real conventions and refines this skill. When the two disagree, `backend/CLAUDE.md` wins.

Read `backend/CLAUDE.md` first if it exists.

## Modules

| Module | Holds | Must not depend on |
|---|---|---|
| `backend/biwenger-client` | HTTP client for the Biwenger API (Spring `RestClient`, built and configured by `app`), its request/response records, auth, rate limiting, retries | Spring Boot, Spring MVC/WebFlux, the `app` module, the database (ADR 0004) |
| `backend/app` | Domain model, persistence, sync jobs, advisor algorithms, REST API | Biwenger response records outside the mapping layer |

## Order of work

Build inside out, so each layer is tested before the next one uses it.

1. **Domain.** Records, enums and sealed types in the feature's `domain` package. No Spring annotations, no JSON annotations. Validate invariants in the compact constructor.
2. **Persistence** (if needed). A Flyway migration `V<n>__<description>.sql`, the JPA entity and a Spring Data repository. Never edit a migration that is already on `main`; add a new one.
3. **Service.** Business logic in a `@Service` with constructor injection. It works with domain types, not entities or DTOs. Advisor code must be deterministic: same input gives the same output, ties broken by player id, no randomness, no LLM calls.
4. **DTOs.** Request and response records for the REST API, kept separate from domain types. Map explicitly; no reflection mappers.
5. **Controller.** A `@RestController` under `/api/<feature>`. Validate input with Bean Validation. Errors go through one `@RestControllerAdvice` that returns `ProblemDetail`.
6. **Tests** (see below), then `./mvnw verify`.

Suggested package layout, by feature rather than by layer:

```
app/src/main/java/<base>/<feature>/
  domain/      records, enums, pure logic
  persistence/ entities, repositories
  service/     use cases
  web/         controller, DTOs, mappers
```

## Tests

| What | How |
|---|---|
| Domain and advisor logic | Plain JUnit 5 + AssertJ, no Spring context. Cover ties and edge cases. |
| Controller | `@WebMvcTest` with the service mocked. Check status codes, JSON shape and validation errors. |
| Biwenger client | WireMock serving the JSON samples in `docs/biwenger-api/samples/`. Never the real API. |
| Repositories, migrations, sync jobs | `@SpringBootTest` or `@DataJpaTest` with Testcontainers PostgreSQL. |

Name tests after behaviour: `returnsEmptyListWhenMarketIsClosed()`, not `test1()`.

## Rules

- Biwenger write calls (bid, sell, accept offer, save lineup) only go through the propose → confirm action service (Phase 6). Never call them directly from a controller or a job.
- Credentials come from environment variables, never from code, config files in git or the database. Keep the token in memory.
- Log the reason for a failure, never tokens or passwords.
- Use `Optional` for absent return values, not `null`. No field injection.
