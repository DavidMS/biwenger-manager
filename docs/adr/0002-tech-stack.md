# 0002. Tech stack

- **Status:** Accepted
- **Date:** 2026-10-20
- **Issue:** #6

## Context

Biwenger Manager is a single-user web app that runs on the owner's machine (deployment comes later, Phase 7). It reads data from the unofficial Biwenger API, keeps a daily history of prices and points that Biwenger doesn't keep, computes buy, sell and lineup recommendations, and later executes Biwenger actions after confirmation.

The owner asked for a Java backend and a React frontend. The other choices were made in the plan review (plan §7) on 2026-10-20. Claude Code writes most of the code, so well-known, well-documented tools are preferred over novel ones.

## Options considered

1. **Build tool: Maven or Gradle.** Gradle is more flexible. Maven is simpler, declarative and very common, which suits AI-written builds.
2. **Database: PostgreSQL, SQLite or H2.** SQLite and H2 need no server. PostgreSQL runs easily in Docker Compose, matches what a later deployment would use and works with Testcontainers for realistic tests.
3. **Recommendations: deterministic algorithms or an LLM inside the app.** A chat assistant calling the Claude API was in the first draft. It adds an API key, a running cost and output that can't be reproduced or tested exactly. The owner dropped it.

## Decision

We will use:

| Area | Choice |
|---|---|
| Backend | Java 25 (LTS), Spring Boot 4.x |
| Build | Maven, multi-module, with the Maven wrapper |
| Persistence | PostgreSQL, Flyway, Spring Data JPA |
| Backend tests | JUnit 5, AssertJ, WireMock (Biwenger API), Testcontainers (PostgreSQL) |
| Frontend | React, TypeScript, Vite |
| UI | Tailwind CSS, shadcn/ui |
| Data fetching | TanStack Query |
| Frontend tests | Vitest, React Testing Library, Playwright later |
| Local environment | Docker Compose (PostgreSQL, backend, frontend) |
| CI | GitHub Actions |

Recommendations are **deterministic algorithms**: the same data and configuration always give the same output, with no randomness and ties broken by player id. **The app makes no LLM calls.** AI is used to build the app, not inside it.

The scope is La Liga only, one league, a single user, an English UI.

## Consequences

- No API key or running cost for the app itself. Recommendations can be unit-tested exactly and explained with the numbers that produced them.
- Recommendations can't answer free-form questions. The Advisor page offers three buttons (Suggest buys, Suggest sells, Best lineup) instead.
- Docker is required to run the database locally and for Testcontainers.
- Java 25 and Spring Boot 4 are recent. If a library doesn't support them yet, that is a reason for a new ADR, not a silent downgrade.
- Supporting more competitions or leagues later means revisiting this ADR.
