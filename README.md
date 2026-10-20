# Biwenger Manager

A personal web app to manage a [Biwenger](https://biwenger.as.com/) fantasy football account (La Liga, one league). It shows your squad, the market and the standings, suggests buys, sells and the best lineup with **deterministic algorithms**, and will carry out Biwenger actions only after you confirm them.

It is also a **tutorial on building software with AI**. Almost all of it is written by [Claude Code](https://claude.com/claude-code), one GitHub issue at a time, and the whole process is documented: the prompts, what the AI did, what had to be corrected and what we learned.

> **Status:** Phase 0 (AI-ready foundation) is done. There is no application code yet; Phase 1 creates the backend and frontend skeletons.

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

Not yet: Phase 1 adds `docker compose up`. Biwenger credentials will go in a local `.env` file (copied from `.env.example`), which is gitignored and never committed.

## Disclaimer

The Biwenger API is unofficial and reverse-engineered (with thanks to [pybiwenger](https://github.com/pablominue/pybiwenger)). It can change without notice. This project is not affiliated with Biwenger. Use it at your own risk and within Biwenger's terms of service.
