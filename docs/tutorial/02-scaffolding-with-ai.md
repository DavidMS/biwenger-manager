# 02 — Scaffolding a project with AI

Phase 1 turns an empty repo into a project that builds, tests, runs and checks itself, with no features yet. This chapter covers how Claude Code scaffolded the backend, frontend, Docker setup and CI, and what had to be corrected along the way. Scaffolding is where AI-generated code most often looks right and isn't.

Issues: [#8](https://github.com/DavidMS/biwenger-manager/issues/8) backend, [#9](https://github.com/DavidMS/biwenger-manager/issues/9) frontend, [#10](https://github.com/DavidMS/biwenger-manager/issues/10) Docker Compose, [#11](https://github.com/DavidMS/biwenger-manager/issues/11) CI, [#12](https://github.com/DavidMS/biwenger-manager/issues/12) module conventions and [#13](https://github.com/DavidMS/biwenger-manager/issues/13) this chapter.

## Goal

Every later phase builds on what this phase leaves behind:

- the build tools
- the folder layout
- the formatter, linter and tests
- `docker compose up`
- the CI checks on every PR
- the conventions Claude follows inside each module

A skeleton's mistakes are copied into every feature that follows, so the goal was a skeleton that works today and is checked by machines, not one that only looks complete.

## Concepts

### One skill drives every issue

Phase 0 wrote the [`implement-issue`](../../.claude/skills/implement-issue/SKILL.md) skill. Phase 1 was its first real use. It holds the whole loop:

1. read the issue
2. branch and move the board card
3. implement and test
4. run the reviewer agents
5. write the journal entry
6. open the PR

Because the steps live in the skill, the prompts didn't need to describe them. These are the prompts, verbatim:

> merged, move phase 1 to ready and go with #8

> merged, go with #9

> merged, go with #10

> merged, continue with #11

> merged, continue with #12

> I accept all the conventions. PR is merged continue with #13

All the know-how is in the repo: the skill, `CLAUDE.md` and the issue's acceptance criteria. The prompt only has to name the issue. The trade-off is that a gap in the skill repeats on every issue, so the skill has to be right.

### Ask the tools for current versions, don't recall them

A model's knowledge of library versions is a snapshot from training. Claude's first step in each scaffolding issue was to ask a live source:

- **Backend ([#8](journal/0008-backend-skeleton.md)):** Spring Initializr gave Spring Boot 4.1.1 and Maven 3.10.0.
- **Frontend ([#9](journal/0009-frontend-skeleton.md)):** the official `create-vite` template and shadcn's own CLI.
- **Docker ([#10](journal/0010-docker-compose.md)):** the base image tags were checked to exist. Checking what was inside them showed the Java runtime image has no `curl`, which changed how the health check works.
- **CI ([#11](journal/0011-ci-github-actions.md)):** the current major of each GitHub Action, and the `setup-java` README.

Where the scaffolder and the issue disagreed (the Vite template ships `oxlint`, the issue asks for ESLint), the issue won.

### Make the wrong environment fail loudly

Two silent failures turned into loud ones:

- **The backend build requires JDK 25.** On another JDK it stops with "Java 25 is required. Run `sdk env` in backend/".
- **`npm install` requires a supported Node** (`engine-strict`). On the machine's Node 23, npm had quietly installed older majors of ESLint, Vitest and jsdom without any error ([#9](journal/0009-frontend-skeleton.md)). Now it stops with `EBADENGINE`.

CI reads the same files as developers do (`backend/.sdkmanrc`, `frontend/.nvmrc`), so changing a version means changing one file.

### Rules that the build enforces

[ADR 0004](../adr/0004-biwenger-client-uses-spring-http-client.md) says the Biwenger client must not depend on Spring Boot, the server side or a database. That isn't left as a sentence in a document: a Maven enforcer rule fails the build if the client pulls one in. It was tested by adding the PostgreSQL driver to the client and watching the build fail. This is the same idea as Phase 0's hooks: **a rule a machine checks beats a rule a model is asked to remember.**

### Nested `CLAUDE.md` files

The root `CLAUDE.md` is loaded in every session. A `CLAUDE.md` inside a folder is loaded only when Claude works on a file in that folder. [#12](journal/0012-module-conventions.md) used this to write the module conventions where they apply:

- **[`backend/CLAUDE.md`](../../backend/CLAUDE.md):** packages by feature, domain vs entity vs DTO, errors as `ProblemDetail`, test types.
- **[`frontend/CLAUDE.md`](../../frontend/CLAUDE.md):** folders, components, data through TanStack Query, styling, tests.

They were written *after* the skeletons existed, so they describe real code rather than a plan. The feature skills were cut down to the order of work and now point to these files, so each rule has one home.

To check that it loads, a fresh session read only the backend's main class and was asked how API errors must be returned. It answered from `backend/CLAUDE.md` and said so.

### Reviewer agents, and reviewing the reviewers

Every PR in this phase was reviewed by `java-reviewer` or `react-reviewer` (or both) running in the background, then by the human. The agents found real problems:

- **An import cycle** that only worked by luck ([#9](journal/0009-frontend-skeleton.md)).
- **A `.dockerignore` gap** that would have baked a local `.env.local` into the frontend image ([#10](journal/0010-docker-compose.md)).
- **nginx resolving the backend's address once**, so it would break after the backend was recreated with a new IP ([#10](journal/0010-docker-compose.md)).
- **In the conventions:** an example test name that broke golden rule 5, a config record that would have logged the password, and two rules that contradicted each other ([#12](journal/0012-module-conventions.md)).

Reviewers can be wrong too. In [#11](journal/0011-ci-github-actions.md), `java-reviewer` called a missing `distribution` input a **blocker**. It was relying on how older versions of the action worked. The CI run had already passed, and its log said `Using distribution 'temurin' from backend/.sdkmanrc`. Claude now checks each finding's claims (package versions, line contents, run logs) before applying it.

## How it went

| Issue | Built | Verified by running |
|---|---|---|
| [#8](journal/0008-backend-skeleton.md) | Maven multi-module (`biwenger-client`, `app`), Spring Boot 4.1, Java 25, Spotless, enforcer rules, a health endpoint | `./mvnw verify`, the app's `/actuator/health`, the build failing on JDK 21 and on a banned dependency |
| [#9](journal/0009-frontend-skeleton.md) | Vite + React 19 + TypeScript, Tailwind 4, shadcn/ui, TanStack Query, React Router, ESLint, Prettier, Vitest; five empty pages | 8 tests, made to fail once, then headless-Chrome screenshots of the built app |
| [#10](journal/0010-docker-compose.md) | Compose with PostgreSQL, backend and frontend (nginx), all health-checked, ports bound to `127.0.0.1` | A separate Compose project: deep links, the proxy, caching, gzip, the non-root user, data surviving a restart |
| [#11](journal/0011-ci-github-actions.md) | GitHub Actions `backend` and `frontend` checks with path filters | Both checks on the PR, then two throwaway PRs: docs-only ran nothing, frontend-only ran only `frontend` |
| [#12](journal/0012-module-conventions.md) | `backend/CLAUDE.md`, `frontend/CLAUDE.md`, slimmer feature skills | A fresh session answering from the nested file |

The pattern in the last column: **every promise was checked with one command.** A `curl`, a `psql` query, a throwaway PR. None of these checks is clever, and together they found most of the bugs in the next section.

## What went wrong

The most instructive corrections, and what changed because of them:

- **A rule read too strictly** ([#8](journal/0008-backend-skeleton.md)). Claude had written "no Spring web deps" for the client, then banned the whole `spring-web` artifact. That artifact also contains Spring's HTTP client, so the ban would have cost hand-written HTTP code and added no real isolation. The human asked:

  > why are you leaving Spring web out of the client for the biwenger client?

  Claude explained the trade-off, and the human decided: "yes, allow spring-web and write ADR 0004". **Changed:** [ADR 0004](../adr/0004-biwenger-client-uses-spring-http-client.md), plus the enforcer rule, plan, skill and reviewer agent. Lesson: before enforcing a rule, ask what it protects.

- **Wrong first guess at a confusing error** ([#8](journal/0008-backend-skeleton.md)). Spotless failed on every Java file. Claude's guess (JDK 25 module access) was wrong, and the flags it added changed nothing. Running the formatter by hand showed a version incompatibility. **Changed:** pinned google-java-format 1.36.1, with a comment saying why. The useless flags were deleted.

- **The documented command didn't work** ([#8](journal/0008-backend-skeleton.md)). `./mvnw -pl app -am spring-boot:run` also tried to run the parent POM. It was found only because Claude ran the command exactly as `CLAUDE.md` wrote it. **Changed:** the parent skips `run`.

- **npm quietly downgraded packages** ([#9](journal/0009-frontend-skeleton.md)). The only sign was a version number that didn't match `npm view`. **Changed:** `.nvmrc`, an `engines` range and `engine-strict`.

- **A check that passed for the wrong reason** ([#10](journal/0010-docker-compose.md)). The test for "nginx follows a backend with a new IP" passed because the backend got the *same* IP back. Claude noticed the unchanged address and redid the test with the old IP held by another container.

- **A zsh quirk produced a false success message** ([#10](journal/0010-docker-compose.md)). A command kept in a variable never ran, but the script printed "removed" anyway. **Changed:** use shell functions, and confirm the result (`docker volume ls`).

- **A Phase 0 hook had silently done nothing** ([#12](journal/0012-module-conventions.md)). The format hook ran Prettier from the repo root, where Prettier can't find its Tailwind plugin, and the hook hides errors by design. It worked only when the session happened to be in `frontend/`, which is how the #9 check passed. Writing a Markdown file that CI checks but the hook skipped led to the discovery. **Changed:** the hook runs Prettier from `frontend/` and covers `.md`.

The last two have the same shape: a script that hides its errors reported success when nothing had happened. Silence is not evidence.

## Results

- 5 issues and 5 PRs ([#59](https://github.com/DavidMS/biwenger-manager/pull/59)–[#62](https://github.com/DavidMS/biwenger-manager/pull/62), [#65](https://github.com/DavidMS/biwenger-manager/pull/65)), plus this chapter.
- About 1,500 lines of code and configuration, plus about 400 of journal and ADR. Another 10,000 lines are generated: the npm lock file and the Maven Wrapper.
- 1 backend test (the health endpoint) and 8 frontend tests (layout and navigation). That's few, because there are no features yet; the point is that the test setup for each layer works.
- 2 CI checks, `backend` and `frontend`, each taking under 40 seconds.
- 1 new ADR ([0004](../adr/0004-biwenger-client-uses-spring-http-client.md)) and 2 nested `CLAUDE.md` files.
- 17 reviewer findings on the conventions alone, all checked and applied. 1 reviewer "blocker" rejected with evidence from the run log.

## Takeaways

- **Get versions and templates from live tools** (Initializr, `create-vite`, release pages), not from the model's memory.
- **Make wrong setups fail loudly:** an enforcer rule, `engine-strict`, a required variable in Compose. A quiet fallback costs more later than a clear error now.
- **Verify each promise with one real command**, the way a user would meet it. And check that the check can fail: a test that passes for the wrong reason, or a script that hides its errors, proves nothing.
- **Write conventions after the code exists,** in nested `CLAUDE.md` files next to that code, and test that a fresh session uses them.
- **Treat review findings, from agents or people, as claims to check.** Most are right. The wrong ones are easy to spot once you look at the evidence.
