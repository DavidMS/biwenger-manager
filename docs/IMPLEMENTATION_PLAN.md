# Biwenger Manager — Implementation Plan

> **Status:** Reviewed (2026-10-20). All open questions resolved (§7)
> **Next step:** review and validate this plan → create a GitHub Project → turn every task in this plan into a GitHub issue → implement them one by one with Claude Code.

## 1. Vision

Biwenger Manager is a personal web app for managing a [Biwenger](https://biwenger.as.com/) fantasy football account. It:

1. **Shows** my account in a simple UI: squad, balance, market, league standings and player details.
2. **Advises** me. Buttons on the Advisor page run deterministic algorithms that propose players to buy, players to sell and the best lineup for the next round, with the numbers behind each suggestion.
3. **Acts on my behalf, with confirmation.** It can place bids, list players for sale and save lineups, but only after I explicitly approve.

The project has a **second purpose, equally important**: it is a **tutorial on building software with AI**. The repository documents the whole process: how the plan was produced, how the AI tooling (CLAUDE.md, skills, agents, hooks) was set up, how each issue was implemented, reviewed and merged, and what went well or badly. Anyone reading the repo should be able to follow and reproduce the workflow.

### Non-goals (for now)

- Multi-user SaaS. This is a single-user app that runs locally (deployment comes later, Phase 7).
- Multiple leagues or competitions. The app supports **one league** in **La Liga** only.
- AI inside the app. There is no chat and no LLM call. Buy, sell and lineup recommendations come from **deterministic algorithms** (Phase 5), run with a button. AI is used to *build* the app, not inside it.
- Automatic actions with no human confirmation.
- Scraping or anything that hits Biwenger aggressively. We respect rate limits and use caching (see §4.3).

---

## 2. Architecture overview

```
┌─────────────────────┐      REST/JSON       ┌──────────────────────────────┐
│  frontend/          │ ───────────────────▶ │  backend/                    │
│  React + TypeScript │ ◀─────────────────── │  Java + Spring Boot          │
│  (Vite)             │                      │                              │
└─────────────────────┘                      │  ┌────────────────────────┐  │
                                             │  │ biwenger-client module │──┼──▶ biwenger.as.com/api/v2
                                             │  └────────────────────────┘  │    cf.biwenger.com/api/v2
                                             │  ┌────────────────────────┐  │
                                             │  │ advisor (scoring,      │  │
                                             │  │ optimizer)             │  │
                                             │  └────────────────────────┘  │
                                             └──────────────┬───────────────┘
                                                            │ JPA / Flyway
                                                     ┌──────▼──────┐
                                                     │ PostgreSQL  │  price & points snapshots,
                                                     └─────────────┘  recommendations, audit log
```

### Tech stack (decisions in §7)

| Area | Choice | Why |
|---|---|---|
| Backend language | **Java 25 (LTS)** | Requested; records, sealed types and pattern matching suit the domain modelling. |
| Backend framework | **Spring Boot 4.x** | Industry standard. Gives `RestClient`, scheduling, validation and Actuator out of the box. |
| Build | **Maven** (multi-module) | Simple and well known. Claude handles it well. |
| Persistence | **PostgreSQL + Flyway + Spring Data JPA** | We need history (prices change daily) that Biwenger doesn't keep for us. |
| HTTP client tests | **WireMock** with recorded Biwenger responses | Tests never hit the real API. |
| Testing | JUnit 5, AssertJ, Testcontainers (Postgres) | |
| Frontend | **React + TypeScript + Vite** | Requested. Vite is fast and simple. |
| UI | **Tailwind CSS + shadcn/ui** | A clean UI with little custom CSS. |
| Data fetching | **TanStack Query** | Caching and loading/error states for free. |
| Frontend tests | Vitest + React Testing Library, Playwright (E2E, later) | |
| Local env | **Docker Compose** (Postgres + backend + frontend) | One command to run everything. |
| CI | **GitHub Actions** | Build, test and lint on every PR. |

### Repository layout (target)

```
biwenger-manager/
├── CLAUDE.md                     # Project memory for Claude Code (always loaded)
├── .claude/
│   ├── settings.json             # Shared permissions + hooks
│   ├── skills/                   # Reusable workflows (see Phase 0)
│   └── agents/                   # Specialised subagents (see Phase 0)
├── backend/
│   ├── CLAUDE.md                 # Backend-specific conventions
│   ├── biwenger-client/          # Biwenger API client (no Spring web deps)
│   └── app/                      # Spring Boot app: domain, advisor, REST API
├── frontend/
│   ├── CLAUDE.md                 # Frontend-specific conventions
│   └── src/
├── docs/
│   ├── IMPLEMENTATION_PLAN.md    # This file
│   ├── tutorial/                 # The "how we built this with AI" tutorial, one chapter per phase
│   ├── adr/                      # Architecture Decision Records
│   └── biwenger-api/             # Reverse-engineered API reference + sample payloads
├── .github/
│   ├── ISSUE_TEMPLATE/
│   ├── pull_request_template.md
│   └── workflows/
└── docker-compose.yml
```

---

## 3. The AI-assisted development workflow (what the tutorial teaches)

Every phase and every issue follows the same loop. Writing it down is the core of the tutorial.

```
 ┌─────────┐   ┌──────────┐   ┌───────────┐   ┌──────────┐   ┌──────────┐   ┌──────────┐
 │ 1. Plan │──▶│ 2. Issue │──▶│ 3. Branch │──▶│ 4. Build │──▶│ 5. Review│──▶│ 6. Merge │
 └─────────┘   └──────────┘   └───────────┘   └──────────┘   └──────────┘   └────┬─────┘
      ▲                                                                          │
      └──────────────────── 7. Document (tutorial chapter / journal) ◀───────────┘
```

1. **Plan.** A human and Claude agree on *what* to build (this document, then refined per issue in plan mode).
2. **Issue.** Each task is a GitHub issue with acceptance criteria. Claude reads it with `gh issue view`.
3. **Branch.** One branch per issue: `feat/<issue-number>-<slug>`.
4. **Build.** Claude implements the issue following CLAUDE.md conventions and the matching skill (`backend-feature`, `frontend-feature`, …), tests first where practical.
5. **Review.** Specialised reviewer agents (`java-reviewer`, `react-reviewer`) plus `/code-review` and `/security-review`. Then the human reviews the PR.
6. **Merge.** CI must be green. Squash-merge, which closes the issue.
7. **Document.** The `tutorial-scribe` agent appends a journal entry: the prompt used, what Claude did, what needed correcting and lessons learned.

**Golden rules** (these go into CLAUDE.md):
- The human owns decisions, and Claude owns the typing. Ambiguous product decisions go back to the human.
- Small issues, small PRs. One issue = one PR, ideally under 400 changed lines.
- Tests are part of "done".
- Never commit secrets. Biwenger credentials live only in `.env` (gitignored).
- Anything that changes Biwenger state (bid, sell, lineup) needs explicit user confirmation in the UI.

---

## 4. Research notes

### 4.1 Biwenger game mechanics that drive the features

- **Budget/balance.** Each manager has a cash balance. **If your balance is negative when a round starts, you score 0 points.** The app must warn about this.
- **Market.** Every day a set of free players (and players listed by managers) appears on the market. Managers place sealed **bids**, and the highest bid wins when the market closes. Managers can also send **offers** for players owned by others.
- **Player value.** Values change daily based on performance and demand. Tracking these changes over time is valuable, because Biwenger only exposes limited history (`prices` field on the cf player endpoint).
- **Clauses (cláusulas).** Depending on league settings (`settings.clause`, `clauseIncrement`), you can buy another manager's player by paying their release clause (a "clausulazo"). This brings both opportunities and risks, and some leagues penalise it.
- **Scoring.** Configurable per league (`scoreID`): SofaScore, AS picas, mixed stats, social, Jornada Perfecta, or weighted combinations. The catalog endpoint takes a `score=<id>` parameter.
- **Lineups.** Formation, optional captain, coach, reserves and a max-players-per-club rule (`lineupCaptain`, `lineupCoach`, `lineupReserves`, `lineupMaxClubPlayers`, `lineupMultiPos`). Players who don't play score 0, so injuries, suspensions and probable lineups matter.
- **Player status.** `status` / `statusInfo` (injured, doubtful, suspended…) and `fitness` (last 5 scores) are present in the catalog.

### 4.2 Known Biwenger API surface (from [pybiwenger](https://github.com/pablominue/pybiwenger))

Unofficial and reverse engineered. It can change without notice.

| Purpose | Method & URL | Notes |
|---|---|---|
| Login | `POST https://biwenger.as.com/api/v2/auth/login` body `{email, password}` | Returns `{token}`. Send it as `Authorization: Bearer <token>` |
| Account | `GET /api/v2/account` | Leagues, the user in each league (balance, points, position) and league settings |
| League | `GET /api/v2/league/{leagueId}` | Users/standings. Requires `X-League` and `X-User` headers |
| My squad | `GET /api/v2/user?fields=players(id,owner)` | |
| Market | `GET /api/v2/market` | `data.sales[]`. `user == null` means a free player |
| Player catalog | `GET /api/v2/competitions/la-liga/data?lang=es&score={scoreId}[&season=YYYY]` | All players: price, priceIncrement, fitness, points, status, position, team |
| Rounds | `GET /api/v2/rounds/league` | |
| Board (transfers) | `GET /api/v2/league/{leagueId}/board?type=transfer,market` | Transfer history in the league |
| Player detail (CDN) | `GET https://cf.biwenger.com/api/v2/players/la-liga/{slug}?lang=es&season=YYYY&fields=*,prices` | Price history |
| Player reports (CDN) | same with `&fields=*,reports(points,home,events,status(status,statusInfo),match(*,round,home,away))` | Per-match points and stats |

Required headers: `Authorization`, `X-League`, `X-User`, `X-Lang: es`, `Accept: application/json`.

**Gap:** Neither pybiwenger nor the archived [biwenger-java-api](https://github.com/jbujalance/biwenger-java-api) implements **write** operations (place a bid, list a player, accept an offer, save a lineup). These have to be discovered with the browser DevTools Network tab while using biwenger.as.com. This is a dedicated spike in Phase 2 and gates Phase 6.

### 4.3 Responsible API usage

- Cache the catalog (it is large and changes about once a day). Snapshot it once a day via a scheduler.
- Rate-limit and retry with backoff in the client. Identify ourselves with a sensible User-Agent.
- Never store the Biwenger password in the DB. Keep it in env vars, and keep the token in memory.
- The API is unofficial, so use is at our own risk and should respect Biwenger's terms of service.

---

## 5. Suggested features (prioritised)

| # | Feature | Value | Phase |
|---|---|---|---|
| F1 | **Dashboard**: balance, team value, league position, next round countdown, negative-balance warning | ★★★ | 4 |
| F2 | **My squad**: players with value, value trend, status, form (fitness) and points | ★★★ | 4 |
| F3 | **Market view**: today's market with filters, value trend, "bargain score" | ★★★ | 4 |
| F4 | **Standings & rivals**: league table, rival squads and estimated rival balances | ★★ | 4 / 7 |
| F5 | **Player detail**: price chart, points per match, upcoming fixtures | ★★ | 4 |
| F6 | **Price history tracking**: daily snapshots of the catalog, so we keep the history Biwenger doesn't | ★★★ | 3 |
| F7 | **Buy recommendations**: rank market players by expected points per euro, form, trend, position need and budget | ★★★ | 5 |
| F8 | **Bid advisor**: suggested bid amount based on value, trend and competition | ★★ | 5 |
| F9 | **Sell recommendations**: players losing value, injured, low points per euro, surplus in a position | ★★★ | 5 |
| F10 | **Lineup optimizer**: best XI for the next round given formation rules, status, form, home/away and max-per-club | ★★★ | 5 |
| F11 | **What-if simulator**: "buy X + sell Y → balance, squad value, best XI" | ★★ | 5 |
| F12 | **Execute actions**: place a bid, list for sale, accept or reject offers, save a lineup, all with confirmation | ★★ | 6 |
| F13 | **Clause watch**: clausulazo opportunities (clause < real value) and which of my players are at risk | ★★ | 7 |
| F14 | **Alerts**: injury of a player in my XI, negative balance before round lock, market bargain (email/Telegram) | ★★ | 7 |
| F15 | **Season analytics**: my points per round versus the league, best and worst transfers, ROI per player | ★ | 7 |

---

## 6. Phases

Every task below becomes **one GitHub issue**. Labels: `phase:N`, `area:backend|frontend|ai-tooling|docs|infra`, `type:feature|chore|spike|docs`.
Every phase ends with a **tutorial chapter** in `docs/tutorial/`.

---

### Phase 0 — AI-ready foundation 🤖

**Goal:** Before writing app code, set up everything that makes working with Claude Code comfortable, consistent and documented. This phase *is* the first lesson of the tutorial.

#### 0.1 `CLAUDE.md` (root) — project memory
Keep it short, because it is loaded every session. Contents:
- One-paragraph project description, plus a link to this plan.
- Repo map (backend/, frontend/, docs/).
- How to build, test and run (`./mvnw verify`, `npm test`, `docker compose up`).
- Golden rules from §3 (confirm Biwenger write actions, no secrets, tests required, small PRs).
- Workflow: issue → branch naming → commit convention (Conventional Commits) → PR.
- Pointers to the nested `backend/CLAUDE.md` and `frontend/CLAUDE.md`. Those are created in Phase 1 once the code exists, with module conventions (package layout, DTO vs domain, error handling, component structure, styling rules).

#### 0.2 `.claude/settings.json` — shared permissions & hooks
- **Permissions allowlist**: `./mvnw *`, `npm run *`, `npx vitest *`, `gh issue view *`, `gh pr *`, `git *` (except push --force), `docker compose *`.
- **Deny**: reading `.env` and `**/secrets/**`.
- **Hooks**:
  - `PostToolUse` on Edit/Write: auto-format the touched file (Spotless for `.java`, Prettier for `.ts/.tsx`).
  - `PreToolUse` on Bash: block `git push --force` to `main` and commands that `cat .env`.
- Personal overrides go in `.claude/settings.local.json` (gitignored).

#### 0.3 Skills (`.claude/skills/<name>/SKILL.md`)
Skills hold reusable, on-demand know-how. They load only when relevant.

| Skill | Purpose |
|---|---|
| `implement-issue` | The full loop: `gh issue view N` → plan → branch → implement → test → run reviewer agents → open PR linked to the issue → ask `tutorial-scribe` for a journal entry. |
| `backend-feature` | How to add a backend feature end to end: domain → service → REST controller → DTO → tests (unit + WebMvc + WireMock), following the package conventions. |
| `frontend-feature` | How to add a page or feature: API client function → TanStack Query hook → components → route → Vitest tests. |
| `biwenger-api` | Domain knowledge: the endpoint table from §4.2, headers, auth flow, sample payloads in `docs/biwenger-api/`, game rules from §4.1, and how to capture a new endpoint with DevTools. |
| `write-adr` | Template and rules for an Architecture Decision Record in `docs/adr/NNNN-title.md`. |
| `tutorial-chapter` | How to write or extend a tutorial chapter or journal entry: structure, tone, include the real prompts. |

#### 0.4 Subagents (`.claude/agents/<name>.md`)
Agents give context isolation and specialised focus. They start read-only and get only the tools they need.

| Agent | Tools | Role |
|---|---|---|
| `java-reviewer` | Read, Grep, Glob, Bash (tests only) | Reviews backend diffs: Spring idioms, null safety, error handling, test quality, layering. |
| `react-reviewer` | Read, Grep, Glob, Bash (tests/lint) | Reviews frontend diffs: hooks rules, accessibility, state management, component size, types. |
| `test-writer` | Read, Edit, Write, Bash | Writes missing tests for a given class or component. Never changes production code. |
| `biwenger-api-explorer` | Read, WebFetch, Bash (curl) | Investigates the Biwenger API: compares responses against our models and documents new endpoints in `docs/biwenger-api/`. |
| `tutorial-scribe` | Read, Write, Edit, Bash (git log, gh) | After each issue, writes the journal entry and keeps the tutorial index up to date. |

#### 0.5 Documentation skeleton
- `docs/tutorial/README.md`: tutorial index and how to read it.
- `docs/tutorial/00-planning-with-ai.md`: how this plan was produced (the prompts, the research on pybiwenger, decisions made in review).
- `docs/tutorial/01-setting-up-claude-code.md`: what CLAUDE.md, skills, agents, hooks and permissions are; why we chose each one; how to verify they work.
- `docs/tutorial/journal/`: one short entry per issue (prompt → result → corrections → lesson).
- `docs/adr/0001-record-architecture-decisions.md`, `0002-tech-stack.md`, `0003-monorepo-layout.md`.

#### 0.6 GitHub scaffolding
- Issue templates: `feature.yml`, `spike.yml`, `bug.yml`. Each has *Context*, *Acceptance criteria*, *Out of scope* and *Notes for Claude* sections.
- `pull_request_template.md` with a checklist (tests, docs, journal entry, screenshots).
- Labels from §6 and a GitHub Project board (Backlog → Ready → In progress → In review → Done).
- Update README.md: what the project is, plus links to the plan and the tutorial.
- `.gitignore` additions: `.env`, `node_modules/`, `target/`, `.claude/settings.local.json`, IDE files.

**Done when:** a fresh Claude Code session in the repo understands the project, `/implement-issue` is available, the reviewer agents can be invoked, hooks fire on edit, and chapters 00–01 are written.

---

### Phase 1 — Project skeleton 🏗️

| Issue | Description | Acceptance criteria |
|---|---|---|
| 1.1 | Backend skeleton: Maven multi-module (`biwenger-client`, `app`), Spring Boot 4, Java 25, Spotless, `/actuator/health` | `./mvnw verify` green. The health endpoint returns UP |
| 1.2 | Frontend skeleton: Vite + React + TS, Tailwind, shadcn/ui, TanStack Query, React Router, ESLint + Prettier, Vitest | `npm run build && npm test` green. A layout page with nav renders |
| 1.3 | Docker Compose: Postgres + backend + frontend, `.env.example` | `docker compose up` gives a working app at localhost |
| 1.4 | CI: GitHub Actions for backend and frontend jobs on PRs | PRs show both checks |
| 1.5 | Nested `backend/CLAUDE.md` and `frontend/CLAUDE.md` with real conventions | Reviewed by the human |
| 1.6 | Tutorial chapter 02: scaffolding a project with AI | |

---

### Phase 2 — Biwenger API client (read-only) 🔌

| Issue | Description | Acceptance criteria |
|---|---|---|
| 2.1 | **Spike:** capture and document real responses for every endpoint in §4.2. Store anonymised samples in `docs/biwenger-api/samples/` | One markdown file per endpoint, with a sample JSON |
| 2.2 | Auth: login, token caching, re-login on 401, `X-League`/`X-User` headers. Use the account's single La Liga league (fail clearly if none is found) | WireMock tests for success, bad credentials and token expiry |
| 2.3 | Typed models (Java records) + client methods: account, league/standings, my squad, market, catalog, rounds, board | Deserialization tests against the samples |
| 2.4 | CDN endpoints: player price history and per-match reports | Tests against the samples |
| 2.5 | Resilience: rate limiter, retry with backoff, timeouts, error mapping to `BiwengerApiException` | Tests |
| 2.6 | **Spike:** discover the *write* endpoints (bid, cancel bid, list for sale, accept/reject offer, save lineup) via DevTools and document them. Don't implement them yet | `docs/biwenger-api/write-endpoints.md` |
| 2.7 | Tutorial chapter 03: reverse-engineering an API with AI (pybiwenger as a reference, the explorer agent) | |

---

### Phase 3 — Domain, persistence & history 🗄️

| Issue | Description |
|---|---|
| 3.1 | Domain model: `Player`, `Team`, `Manager`, `League`, `MarketListing`, `Round`, `Position`, `PlayerStatus` (independent from the API DTOs, plus a mapper) |
| 3.2 | Flyway schema: `player`, `player_price_snapshot`, `player_points_report`, `market_snapshot`, `league_standing_snapshot` |
| 3.3 | Daily sync job (`@Scheduled`): catalog → price snapshots, market → market snapshot, standings. Idempotent per day |
| 3.4 | Backfill: import price history from the cf `prices` field and points from `reports` for the current season |
| 3.5 | Manual "sync now" endpoint + last-sync status |
| 3.6 | Tutorial chapter 04: letting AI design a schema (and where we corrected it) |

---

### Phase 4 — Read-only UI 🖥️

Backend REST endpoints and frontend pages are built together, one issue per feature (backend + frontend in the same PR, or two linked issues if large).

| Issue | Feature |
|---|---|
| 4.1 | Settings page: show which Biwenger account and league are connected, and the last sync (F1) |
| 4.2 | Dashboard: balance, team value, position, next round, negative-balance warning (F1) |
| 4.3 | My squad page with value trend sparkline, status badges and fitness (F2) |
| 4.4 | Market page with filters (position, price, team, status) and sort (F3) |
| 4.5 | Standings + rival squads (F4) |
| 4.6 | Player detail: price chart, points per match, status (F5) |
| 4.7 | Tutorial chapter 05: building UI with AI (screenshots, iteration loop) |

---

### Phase 5 — Advisor engine (deterministic) 🧠

Recommendations are **deterministic algorithms**: the same data and the same configuration always produce the same buys, sells and lineup. There is no LLM and no randomness, and ties are broken by a fixed rule (for example, player id). Every recommendation comes with the numbers that produced it, so you can check it by hand.

| Issue | Description |
|---|---|
| 5.1 | Player scoring model: expected points (weighted recent form, season average, home/away, status penalty), value trend (n-day delta) and points-per-million. Configurable weights |
| 5.2 | Buy recommendations (F7): rank market players by score, filtered by budget and position needs. Return explanations ("rising +8% in 3 days, avg 7.2 pts, your DEF line is weak") |
| 5.3 | Sell recommendations (F9): falling value, injured long-term, low points-per-million, position surplus. Respect a minimum squad size |
| 5.4 | Bid advisor (F8): suggested bid range |
| 5.5 | Lineup optimizer (F10): choose formation + XI maximizing expected points under league rules (valid formations, max per club, captain if enabled). Exhaustive search over formations + greedy/ILP per position |
| 5.6 | What-if simulator (F11) |
| 5.7 | UI: "Advisor" page with three buttons, **Suggest buys**, **Suggest sells** and **Best lineup**. Each one runs its algorithm and shows the results with their explanations |
| 5.8 | Tutorial chapter 06: designing deterministic algorithms with AI (Claude writes the code and tests, the algorithm itself has no AI in it) |

---

### Phase 6 — Actions with confirmation ✅

Gated on spike 2.6.

| Issue | Description |
|---|---|
| 6.1 | Client: write endpoints (bid, cancel bid, list for sale, accept/reject offer, save lineup) with WireMock tests |
| 6.2 | Action service with a **two-step flow**: `propose` (preview + validation: budget, negative balance, squad size) → `confirm` (executes). Audit log table |
| 6.3 | UI: action buttons on market, squad, advisor and lineup pages with a confirmation dialog showing the consequences (e.g. "Apply this lineup", "Bid on this player") |
| 6.4 | Tutorial chapter 07: safety when software acts on your behalf |

---

### Phase 7 — Advanced & polish 🚀

| Issue | Description |
|---|---|
| 7.1 | Clause watch (F13) |
| 7.2 | Rival intelligence: estimated balances from the board/transfers, rival bidding patterns (F4) |
| 7.3 | Alerts (F14): injury in my XI, negative balance before lock, bargains. Email or Telegram bot |
| 7.4 | Season analytics (F15) |
| 7.5 | E2E tests with Playwright for the main flows |
| 7.6 | Deployment option (e.g. a small VPS or home server with Docker Compose + basic auth) |
| 7.7 | Tutorial chapter 08: retrospective. What worked, what didn't, metrics (issues, PRs, % of AI-written code, time spent) |

---

## 7. Decisions (review of 2026-10-20)

| Topic | Decision |
|---|---|
| Build tool | Maven |
| Database | PostgreSQL |
| Hosting | Local machine for now. Deployment is planned later (7.6) |
| UI language | English |
| Tutorial language | English |
| Competition | La Liga only |
| Leagues | One league |
| Recommendations | Deterministic algorithms (Phase 5), triggered by buttons on the Advisor page |
| AI chat assistant | Dropped. The app makes no calls to an LLM, so it needs no Claude API key and has no running cost |

---

## 8. From plan to GitHub (after validation)

1. Grant the CLI permission to manage Projects: `gh auth refresh -s project` (the current token lacks the `project` scope).
2. Create labels (`phase:0..7`, `area:*`, `type:*`).
3. Create milestones, one per phase.
4. Create the GitHub Project "Biwenger Manager" with columns Backlog / Ready / In progress / In review / Done.
5. Create one issue per task above (using the templates from 0.6) and add them to the project and milestone.
6. Start with Phase 0, issue by issue, using the workflow in §3.
