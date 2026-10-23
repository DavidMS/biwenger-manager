# Building Biwenger Manager with AI — the tutorial

This folder tells the story of how this app was built with [Claude Code](https://claude.com/claude-code), one step at a time, including the mistakes. The app itself is a personal manager for a Biwenger fantasy football account (Java + Spring Boot backend, React frontend). The [implementation plan](../IMPLEMENTATION_PLAN.md) describes what it does.

## How to read it

- **Chapters** (`NN-*.md`) are the narrative. There is one per phase. Each explains the goal, the Claude Code concepts it relies on, how it went, what went wrong and the takeaways. Read them in order.
- **Journal entries** (`journal/NNNN-*.md`) are the raw log. There is one per GitHub issue: the exact prompt, what Claude did, what was corrected and the lesson. Read them when you want the detail behind a chapter.
- **ADRs** ([`../adr/`](../adr/)) record the significant decisions and why they were made.
- Every change is a pull request on [GitHub](https://github.com/DavidMS/biwenger-manager/pulls?q=is%3Apr), so you can see the real diffs and reviews, and track the work on the [project board](https://github.com/users/DavidMS/projects/8).

Prompts are always quoted exactly as they were typed, typos included.

## Chapters

| # | Chapter | Phase | Status |
|---|---|---|---|
| 00 | [Planning with AI](00-planning-with-ai.md) | Before Phase 0 | Written |
| 01 | [Setting up Claude Code](01-setting-up-claude-code.md) | 0 — AI-ready foundation | Written |
| 02 | [Scaffolding a project with AI](02-scaffolding-with-ai.md) | 1 — Project skeleton | Written |
| 03 | [Reverse-engineering an API with AI](03-reverse-engineering-an-api.md) | 2 — Biwenger API client | Written |
| 04 | Letting AI design a schema | 3 — Domain, persistence and history | Planned |
| 05 | Building UI with AI | 4 — Read-only UI | Planned |
| 06 | Designing deterministic algorithms with AI | 5 — Advisor engine | Planned |
| 07 | Safety when software acts on your behalf | 6 — Actions with confirmation | Planned |
| 08 | Retrospective | 7 — Advanced and polish | Planned |

## Journal

New entries start from [`journal/TEMPLATE.md`](journal/TEMPLATE.md).

| Issue | Entry |
|---|---|
| #2 | [Root CLAUDE.md](journal/0002-root-claude-md.md) |
| #3 | [Shared Claude Code settings: permissions and hooks](journal/0003-claude-settings.md) |
| #4 | [Project skills](journal/0004-project-skills.md) |
| #5 | [Project subagents](journal/0005-project-agents.md) |
| #6 | [Documentation skeleton](journal/0006-docs-skeleton.md) |
| #7 | [GitHub scaffolding](journal/0007-github-scaffolding.md) |
| #8 | [Backend skeleton](journal/0008-backend-skeleton.md) |
| #9 | [Frontend skeleton](journal/0009-frontend-skeleton.md) |
| #10 | [Docker Compose](journal/0010-docker-compose.md) |
| #11 | [CI with GitHub Actions](journal/0011-ci-github-actions.md) |
| #12 | [Module conventions: backend and frontend CLAUDE.md](journal/0012-module-conventions.md) |
| #13 | [Tutorial chapter 02](journal/0013-tutorial-chapter-02.md) |
| #14 | [Spike: capture and document the Biwenger read endpoints](journal/0014-biwenger-read-endpoints.md) |
| #15 | [Biwenger authentication and league context](journal/0015-biwenger-auth.md) |
| #16 | [Typed models and client methods for the read endpoints](journal/0016-typed-models.md) |
| #17 | [CDN endpoints: price history and match reports](journal/0017-cdn-endpoints.md) |
| #18 | [Client resilience: rate limiting, retries, timeouts, errors](journal/0018-client-resilience.md) |
| #19 | [Spike: discover the Biwenger write endpoints](journal/0019-write-endpoints.md) |
| #20 | [Tutorial chapter 03](journal/0020-tutorial-chapter-03.md) |
