# 00 — Planning with AI

Before writing any code, the human and Claude produced an [implementation plan](../IMPLEMENTATION_PLAN.md) and turned it into about 50 GitHub issues. This chapter shows how, with the real prompts. The planning took about an hour and a half on 2026-10-20.

## Goal

Agree on *what* to build before building it: the features, the architecture, the order of work and the rules. A plan written down is something both the human and the AI can check later work against. A plan that lives only in a chat window is forgotten in the next session.

## Concepts

- **Context is everything.** Claude Code starts every session knowing nothing about your project except what is in the repo. Anything decided in conversation has to be written into a file to survive: here, the plan and later `CLAUDE.md`.
- **The human decides, the AI drafts.** Claude researched and proposed. Every product decision (build tool, database, scope, whether the app uses AI) was made by the human.
- **Plans become issues.** Each task in the plan became a GitHub issue with acceptance criteria, so later prompts can be as short as "start #4".

## How it went

### 1. Access

The first prompt checked what Claude could reach:

> do you have access to my github account?

Git over SSH worked, but the GitHub CLI (`gh`) wasn't logged in. The human ran `gh auth login`, which gave Claude access to issues, PRs and CI. Claude also pointed out that the local git identity was a work email, which the human might not want on a personal repo.

### 2. The brief

> ok, I want to create an app in this repo that does the following. It has a simple interface that lets me manage my biwenger account. After that I can ask it to check the market and propose possible buys of players, possible sells, possible squads for the next match... look for information about biwenger and suggest functionalities. Also, to create the api to connect to biwenger you can check this proyect https://github.com/pablominue/pybiwenger?st_source=ai_overview with all of that info I want you to create an implementation plan in this repo with phases. In the first phase we will add all the necessary things to work comfortably with AI (CLAUDE.md, Skills and agents that will help with the development) I want you to document everything. I want this project to serve as a kind of "tutorial" to explain how to work with AI (the whole process). I want the app to have a backend and a frontend (JAVA and REACT). Then create an implementation plan in a markdown file. After that once we have reviewed and validate it we will create a project in github and issues there so that we can go implementing them one by one.

This prompt works because it states the outcome (an app plus a tutorial), the constraints (Java and React, a phase 0 for AI tooling), a starting point for research (pybiwenger) and the process (plan → review → GitHub issues → implement one by one). It doesn't try to design the app itself.

Claude then researched:

- **pybiwenger's source code**: the login flow, the headers every request needs and every endpoint it calls. This became the endpoint table in plan §4.2.
- **Web searches** on Biwenger's game rules (market, bids, clauses, scoring systems, lineups), on other unofficial API clients, and on Claude Code best practices for `CLAUDE.md`, skills and agents.

One finding shaped the whole plan: **no known client implements write operations** (bidding, selling, saving a lineup). pybiwenger and the archived [biwenger-java-api](https://github.com/jbujalance/biwenger-java-api) only read data. So the plan includes a spike to discover those endpoints with the browser's DevTools, and the "act on my behalf" phase waits on it.

The first draft had 9 phases, 16 features and a list of open questions, including an AI chat assistant where you could "ask it" things.

### 3. Review

The human answered the open questions in one line:

> Maven, PostgreSQL, for the time being it will run on my machine, we will deploy it later, the ui and tutorial should be in english, la liga only, why do you need the budget for claude api for? I'd like the algorithm that decides buys, sells and line ups to be a deterministic algorithm... I only play in one league

Claude explained the budget question. The chat assistant would call the Claude API from the app, which is billed per use and separate from a Claude subscription. Claude then asked whether to keep the chat. The answer:

> drop it, I just want a button that suggest sells, buys and line ups but no chat needed for that

That removed a whole phase, the API key and any running cost. It also produced the strongest rule in the project: **the app makes no LLM calls; recommendations are deterministic algorithms**. AI is used to *build* the app, not inside it. This is the most important correction in the planning phase, and it came from the human asking "why?" about something Claude had assumed.

### 4. From plan to GitHub

Asked whether to commit the plan straight to `main` or through a pull request, the human picked option 1, the PR:

> 1

So the plan itself was the first example of the workflow: branch `docs/implementation-plan`, [PR #1](https://github.com/DavidMS/biwenger-manager/pull/1), human merge.

Claude then created, with `gh` and a short Python script:

- 17 labels: `phase:0`–`phase:7`, `area:*` and `type:*`
- 8 milestones, one per phase
- the [project board](https://github.com/users/DavidMS/projects/8) with columns Backlog → Ready → In progress → In review → Done
- 51 issues (#2–#52), one per task, each with context, acceptance criteria, notes for Claude and a definition of done

Two snags along the way:

- **A missing permission.** Managing GitHub Projects needs an extra `gh` permission. The human ran `gh auth refresh -h github.com -s project`; the first attempt without `-h github.com` failed in the non-interactive shell.
- **A CLI bug.** `gh project create` failed with a GraphQL error in `gh` 2.87.2, so Claude created the board through the GitHub GraphQL API directly.

## What went wrong

- **Claude assumed a feature the human didn't want.** "I can ask it" was read as a chat assistant. Asking about the cost exposed the assumption.
- **Tooling friction**, as above. Each case cost a minute because Claude reported the exact error and the next step instead of guessing.

## Results

- [`docs/IMPLEMENTATION_PLAN.md`](../IMPLEMENTATION_PLAN.md): vision, architecture, workflow, API research, 15 features, 8 phases (0–7) and the decisions table.
- 51 issues on the project board, the 6 Phase 0 issues in *Ready*.

## Takeaways

- Give the AI the outcome, the constraints and where to research, then let it draft. Review the draft hard.
- Ask "why?" about anything you didn't request. Assumptions hide there.
- Write decisions into the repo (the plan's §7, later [ADRs](../adr/)) so every future session sees them.
- Detailed issues buy you short prompts later: from #2 on, prompts look like "start #3".
