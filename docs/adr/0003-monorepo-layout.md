# 0003. Monorepo layout

- **Status:** Accepted. The "doesn't depend on Spring Web" rule is superseded by [0004](0004-biwenger-client-uses-spring-http-client.md)
- **Date:** 2026-10-20
- **Issue:** #6

## Context

The app has a Java backend, a React frontend, documentation that doubles as a tutorial, and Claude Code configuration (`CLAUDE.md`, skills, agents, hooks). Claude Code works best when everything a task touches is in one checkout: it can read the backend DTO while writing the frontend type, and one `CLAUDE.md` can describe the whole project.

The Biwenger API is unofficial and can change without notice. The code that talks to it should be isolated, so a change in the API touches one place and the rest of the app works with our own domain model.

## Options considered

1. **Separate repositories** for backend and frontend. Independent releases, but every feature that crosses both needs two PRs, and Claude can't see both sides in one session.
2. **One repository, one backend module.** Simple, but nothing stops Biwenger response types and HTTP details from spreading through the app.
3. **One repository, with the Biwenger client as its own Maven module.**

## Decision

We will use one repository with this layout (plan §2):

```
CLAUDE.md                 project memory, always loaded
.claude/                  settings, hooks, skills, agents
backend/                  Maven multi-module build
  biwenger-client/        Biwenger API client: HTTP, auth, rate limiting, response records
  app/                    Spring Boot app: domain, persistence, sync jobs, advisor, REST API
frontend/                 React + TypeScript + Vite
docs/                     plan, tutorial and journal, ADRs, Biwenger API reference and samples
.github/                  issue and PR templates, workflows
docker-compose.yml        local environment
```

`biwenger-client` doesn't depend on Spring Web, on `app` or on the database. `app` maps the client's response records to its own domain model at one boundary. `backend/` and `frontend/` each get their own `CLAUDE.md` with module conventions (Phase 1, issue #12).

## Consequences

- A feature that spans backend and frontend can be one issue, one branch and one PR.
- A change in the Biwenger API is contained in `biwenger-client` and the mapping layer.
- CI has to build both sides; the workflow runs separate backend and frontend jobs (issue #11).
- The repo mixes Java and Node tooling, so the Claude settings and hooks handle both (Spotless and Prettier in `format-file.sh`).
