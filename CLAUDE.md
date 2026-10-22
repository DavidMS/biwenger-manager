# Biwenger Manager

A personal web app to manage a [Biwenger](https://biwenger.as.com/) fantasy football account (La Liga, one league, single user, runs locally). It shows the squad, market and standings, suggests buys, sells and the best lineup with **deterministic algorithms**, and executes Biwenger actions only after explicit user confirmation. The repo is also a **tutorial on building software with AI**, so the process is documented as carefully as the code. Full plan: [docs/IMPLEMENTATION_PLAN.md](docs/IMPLEMENTATION_PLAN.md).

## Status

Phase 1 (project skeleton). The backend (health endpoint only) and frontend (layout and empty pages) skeletons exist. Items marked *(planned)* below don't exist yet.

## Repo map

```
CLAUDE.md                 this file (always loaded, keep it short)
.claude/
  settings.json           shared permissions and hooks (personal overrides: settings.local.json, gitignored)
  hooks/                  hook scripts: guard-bash.sh (blocks force-push to main, reading .env), format-file.sh,
                          agent-guard.py (per-agent command and path limits)
  skills/                 on-demand workflows: implement-issue, backend-feature, frontend-feature,
                          biwenger-api, write-adr, tutorial-chapter
  agents/                 subagents: java-reviewer, react-reviewer, test-writer, biwenger-api-explorer,
                          tutorial-scribe
backend/                  Java 25 + Spring Boot 4.1, Maven multi-module, package io.github.davidms.biwenger
  biwenger-client/        Biwenger API client on Spring's RestClient (the build bans Boot, MVC and DB here)
  app/                    domain, sync jobs, advisor algorithms, REST API
frontend/                 React 19 + TypeScript + Vite, Tailwind 4, shadcn/ui (radix), TanStack Query, React Router
docs/
  IMPLEMENTATION_PLAN.md  phases, tasks and decisions
  tutorial/               "how we built this with AI": README.md index, one chapter per phase
  tutorial/journal/       one entry per issue, start from TEMPLATE.md
  adr/                    architecture decision records (0001-0003: ADRs, tech stack, layout)
  biwenger-api/           reverse-engineered API reference and samples (planned, Phase 2)
.github/                  issue forms (feature, spike, bug) and PR template
```

Module conventions will live in `backend/CLAUDE.md` and `frontend/CLAUDE.md` (Phase 1, issue #12). Read them before working in those folders.

## Commands

The backend needs **JDK 25**: `backend/.sdkmanrc` names it, so run `sdk env` in `backend/` (or set `JAVA_HOME`). The build fails with a clear message on another JDK. The frontend needs **Node 24** (`frontend/.nvmrc`, `nvm use`); `npm install` refuses unsupported versions (`engine-strict`). Docker Compose runs PostgreSQL, backend and frontend; every variable is in `.env.example`.

| Task | Command |
|---|---|
| Backend build + tests + format check | `cd backend && ./mvnw verify` |
| Fix backend formatting | `cd backend && ./mvnw spotless:apply` (the edit hook does this per file) |
| Run the backend | `cd backend && ./mvnw -pl app -am spring-boot:run`, then `curl localhost:8080/actuator/health` |
| Frontend tests | `cd frontend && npm test` (Vitest + Testing Library) |
| Frontend lint + format check | `cd frontend && npm run lint && npm run format:check` |
| Frontend build (includes type check) | `cd frontend && npm run build` |
| Run the frontend | `cd frontend && npm run dev`, then open http://localhost:5173 |
| Run everything locally | `cp .env.example .env` (once), then `docker compose up --build`. UI at http://localhost:8000 |

## Golden rules

1. **The human owns decisions.** Ask when a product or architecture choice is ambiguous. Record significant decisions as an ADR in `docs/adr/`.
2. **Never execute a Biwenger write action** (bid, sell, accept offer, save lineup) without explicit user confirmation. In code, writes always go through the propose → confirm flow (Phase 6).
3. **Never call the real Biwenger API from tests.** Use WireMock with samples from `docs/biwenger-api/samples/`.
4. **Never commit secrets.** Biwenger credentials live only in `.env` (gitignored). Don't read or print `.env`.
5. **Recommendations are deterministic.** Same data and configuration give the same output. No randomness and no LLM calls in the app. Break ties by player id.
6. **Tests are part of done.** A feature without tests is not finished.
7. **Small PRs.** One issue = one branch = one PR, ideally under 400 changed lines. Don't add work outside the issue's scope; suggest a new issue instead.
8. **English everywhere:** code, comments, UI, docs, commits.

## Workflow

1. Read the issue: `gh issue view <n>`. Its acceptance criteria define done.
2. Move its card on the [Project board](https://github.com/users/DavidMS/projects/8) to *In progress*.
3. Branch from an up-to-date `main`: `feat/<n>-<slug>` (use `fix/`, `docs/` or `chore/` when more fitting).
4. Implement and test. Commit with [Conventional Commits](https://www.conventionalcommits.org/): `feat:`, `fix:`, `docs:`, `chore:`, `test:`, `refactor:`.
5. Add a journal entry in `docs/tutorial/journal/` (prompt used, what Claude did, what was corrected, lesson learned).
6. Open a PR whose body says `Closes #<n>` and lists how each acceptance criterion was met. Move the card to *In review*.
7. The human reviews and merges. Never merge or push to `main` yourself.

## Biwenger API

The API is unofficial and reverse-engineered (reference: [pybiwenger](https://github.com/pablominue/pybiwenger)). It can change without notice. Endpoints, headers and game rules are in plan §4. Be gentle with it: cache, rate-limit and retry with backoff.
