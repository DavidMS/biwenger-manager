---
name: backend-feature
description: How to add a backend feature to the Java 25 / Spring Boot 4 app in backend/, end to end - domain, service, REST controller, DTOs and tests (unit, WebMvc, WireMock, Testcontainers). Use when writing or changing Java code under backend/.
---

# Add a backend feature

Read `backend/CLAUDE.md` first. It holds the conventions: modules, package layout, domain vs entity vs DTO, error handling and test types. This skill is the order of work. When the two disagree, `backend/CLAUDE.md` wins.

## Order of work

Build inside out, so each layer is tested before the next one uses it. The feature lives in `io.github.davidms.biwenger.app.<feature>`.

1. **Domain** (`<feature>/domain`). Records, enums and sealed types with no framework annotations. Invariants in the compact constructor. Write the plain JUnit tests now, including ties and empty input.
2. **Biwenger mapping** (if the data comes from Biwenger). Map the client's records to domain types in `app.biwenger`, with a test that maps a sample from `docs/biwenger-api/samples/`. Use the `biwenger-api` skill for the client side.
3. **Persistence** (if needed). A new Flyway migration `V<n>__<what>.sql`, the entity with `toDomain()`, a Spring Data repository. Test with Testcontainers PostgreSQL (`@ServiceConnection`).
4. **Service** (`<feature>/service`). A `@Service` with constructor injection that takes and returns domain types. Inject `Clock` for anything time-dependent. Advisor code must be deterministic: ties broken by player id, no randomness, no LLM calls.
5. **DTOs and controller** (`<feature>/web`). Response records with `static from(domain)`, request records with Bean Validation. A `@RestController` under `/api/<feature>`. Throw specific exceptions and map them in the shared `@RestControllerAdvice` to `ProblemDetail`. Test with `@WebMvcTest` and `@MockitoBean`.
6. **Check:** `./mvnw verify` (tests, Spotless, enforcer). Run the app and call the new endpoint with `curl` once.

Skip the steps a feature doesn't need, but keep the order.

## Before the PR

- Every new behaviour has a test, or a reason in the PR why not. Plain DTO and configuration records don't need their own.
- No Biwenger record outside `app.biwenger`, no entity outside `persistence`, no DTO outside `web`.
- Biwenger write calls (bid, sell, accept offer, save lineup) only go through the propose → confirm action service (Phase 6), never directly from a controller or a job.
- No credentials, tokens or full Biwenger responses in code, config files or logs.
- If the change adds a convention, update `backend/CLAUDE.md` in the same PR.
