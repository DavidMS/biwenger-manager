# Biwenger Manager

A personal web app to manage a [Biwenger](https://biwenger.as.com/) fantasy football account (La Liga, one league). It shows your squad, the market and the standings, suggests buys, sells and the best lineup with **deterministic algorithms**, and will carry out Biwenger actions only after you confirm them.

It is also a **tutorial on building software with AI**. Almost all of it is written by [Claude Code](https://claude.com/claude-code), one GitHub issue at a time, and the whole process is documented: the prompts, what the AI did, what had to be corrected and what we learned.

> **Status:** Phase 1 (project skeleton). The backend, frontend and Docker setup run, but the pages are still empty; Biwenger data arrives in Phases 2–4.

## Start here

| If you want to... | Read |
|---|---|
| Know what is being built and in what order | [Implementation plan](docs/IMPLEMENTATION_PLAN.md) |
| Learn how it is built with AI | [Tutorial](docs/tutorial/README.md), starting with [00 — Planning with AI](docs/tutorial/00-planning-with-ai.md) |
| See why things were decided | [Architecture decision records](docs/adr/) |
| See the rules Claude follows in this repo | [CLAUDE.md](CLAUDE.md) and [`.claude/`](.claude/) |
| Follow the work | [Issues](https://github.com/DavidMS/biwenger-manager/issues) and [pull requests](https://github.com/DavidMS/biwenger-manager/pulls?q=is%3Apr) |

## Stack

Java 25 + Spring Boot 4 (Maven), PostgreSQL, React + TypeScript + Vite, Tailwind + shadcn/ui, TanStack Query, Docker Compose. Details and reasons in [ADR 0002](docs/adr/0002-tech-stack.md).

The app makes no calls to an LLM. AI is used to build it, not inside it.

## How work is organised

Every task in the plan is a GitHub issue. One issue = one branch = one pull request, reviewed and merged by a human.

**Issue templates** (`.github/ISSUE_TEMPLATE/`): *Feature*, *Spike* (a time-boxed investigation whose output is documentation) and *Bug*. Each has *Context*, *Acceptance criteria*, *Out of scope* and *Notes for Claude*. The acceptance criteria define done.

**Labels:**

| Group | Labels |
|---|---|
| Phase | `phase:0` … `phase:7` |
| Area | `area:backend`, `area:frontend`, `area:ai-tooling`, `area:docs`, `area:infra` |
| Type | `type:feature`, `type:spike`, `type:bug`, `type:chore`, `type:docs` |

Each phase also has a **milestone**.

**Project board** ([Biwenger Manager](https://github.com/users/DavidMS/projects/8)): Backlog → Ready → In progress → In review → Done. Issues of the current phase are moved to *Ready*. Claude moves a card to *In progress* when it starts and to *In review* when it opens the PR. A card goes to *Done* when its PR is merged.

**Branches and commits:** `feat/<issue>-<slug>` (or `fix/`, `docs/`, `chore/`), [Conventional Commits](https://www.conventionalcommits.org/). Pull requests use `.github/pull_request_template.md` and say `Closes #<issue>`.

## Running it

You need [Docker](https://www.docker.com/products/docker-desktop/) with Compose.

```bash
cp .env.example .env      # then edit .env: set POSTGRES_PASSWORD (and later your Biwenger login)
docker compose up --build
```

Open http://localhost:8000. Compose starts three containers:

| Service | What it is | Reachable at |
|---|---|---|
| `frontend` | The React app, served by nginx, which also forwards `/api` to the backend | http://localhost:8000 |
| `backend` | The Spring Boot app | Through the frontend: http://localhost:8000/actuator/health |
| `db` | PostgreSQL 18, data kept in the `db-data` volume | `localhost:5432`, for tools like psql (this machine only) |

`.env.example` documents every variable. `.env` holds your Biwenger password, so it is gitignored and never committed. Every port is bound to `127.0.0.1`, so nothing is reachable from other machines.

Stop with `docker compose down`. Add `-v` to also delete the database.

### Developing without Docker

| Part | Needs | Run |
|---|---|---|
| Backend | JDK 25 (`sdk env` in `backend/`) | `cd backend && ./mvnw -pl app -am spring-boot:run` |
| Frontend | Node 24 (`nvm use` in `frontend/`) | `cd frontend && npm install && npm run dev`, then open http://localhost:5173 |

The Vite dev server forwards `/api` to the backend on port 8080, just like nginx does in Docker. The commands for tests, lint and formatting are in [CLAUDE.md](CLAUDE.md#commands).

## Disclaimer

The Biwenger API is unofficial and reverse-engineered (with thanks to [pybiwenger](https://github.com/pablominue/pybiwenger)). It can change without notice. This project is not affiliated with Biwenger. Use it at your own risk and within Biwenger's terms of service.
