# #12 — Module conventions: backend and frontend CLAUDE.md

- **Issue:** [#12 [1.5] Module conventions: backend/CLAUDE.md and frontend/CLAUDE.md](https://github.com/DavidMS/biwenger-manager/issues/12)
- **Phase:** 1 — Project skeleton
- **Date:** 2026-10-22

## Prompt

> merged, continue with #12

## What Claude did

1. Moved #11 to *Done* and #12 to *In progress*, branched, and ran the `implement-issue` skill.
2. Read the real skeletons (POMs, the enforcer rules, the health test, every frontend source file, `components.json`, `tsconfig`) and ADRs 0003 and 0004, so the conventions describe what exists rather than what the plan imagined.
3. Wrote two nested `CLAUDE.md` files. Claude Code loads a nested one automatically when it works on a file in that folder.
   - **`backend/CLAUDE.md`:**
     - the two modules and who may call whom
     - packages by feature, with `domain`, `persistence`, `service` and `web` inside each
     - three kinds of types: domain records, entities and DTOs, each staying in its own package
     - errors as `ProblemDetail` through one advice
     - which test type to use for each layer
   - **`frontend/CLAUDE.md`:**
     - `app/`, `pages/` and `features/<feature>/` (with `api.ts`, `types.ts` and `hooks.ts`), `components/`, `lib/`
     - named-export components, data only through TanStack Query, query-key objects
     - styling with theme tokens and shadcn/ui
     - tests that query by role and mock the feature's `api.ts`
4. Shortened the `backend-feature` and `frontend-feature` skills to the order of work and a pre-PR checklist. The rules now live in one place, the module `CLAUDE.md`, and the skills point there. Before this change they repeated the rules, and had already drifted (for example, `src/api/` in the skill versus `features/` now).
5. Updated the root `CLAUDE.md` and tutorial chapter 01, which still called these files "planned" or "provisional".
6. **Checked that the nested file really loads.** A fresh `claude -p` session read only the backend's main class and was asked how API errors must be returned. It answered "`ProblemDetail` through one `@RestControllerAdvice`" and named `backend/CLAUDE.md` as the source.

## Review

Both reviewer agents read the conventions against the code and the library versions. Both gave **approve with nits**. Claude checked the factual claims (package versions, the `cn` import, line contents) before applying them, and all held up. 17 findings, all applied. The most useful:

`java-reviewer`:
- **Our own example broke golden rule 5.** A sample test name, `suggestsTheCheapestPlayerWhenScoresTie()`, broke ties by price, but the rule is by player id. Now `breaksScoreTiesByPlayerId()`. An example in a conventions file gets copied, so it has to follow the rules too.
- **"Errors as `ProblemDetail`" wouldn't have covered Spring's own errors** (failed `@Valid`, bad JSON) unless the advice extends `ResponseEntityExceptionHandler`. Added, plus: an `IllegalArgumentException` from bad Biwenger data is a 500, not the user's 400.
- **A configuration record holding the password would print it** in its generated `toString()`. Such records now mask it.
- **It didn't say who calls the Biwenger client.** Now only `app.biwenger` does, so no feature service sees Biwenger's records.
- Also added: `@Transactional` on services only, `open-in-view=false`, `long` for money sums, Testcontainers through `@ServiceConnection`, and that Spring doesn't read `.env` itself.

`react-reviewer`:
- **React Router wasn't pinned down.** Agents tend to write `react-router-dom` and route loaders, which would give the app two data layers. Now: import from `react-router`, no `loader` or `action`, TanStack Query is the one data layer.
- **Two rules contradicted each other.** "Don't import another feature's internals" against "mutations invalidate the queries they change", when a market bid must refresh the squad. The query-key object is now the feature's public API.
- **The layout test will break** as soon as a page calls a query hook, because it renders every route without a `QueryClientProvider`. Noted where the render helper is described.
- Also fixed:
  - `npx shadcn add` instead of `@latest`, which could pull a different major
  - `border-border` as the colour token (plain `border` is a width)
  - the `hooks/` folder from `components.json`
  - "seen to fail once" moved from a checklist gate to advice

## Corrections

- **The format hook had been silently doing nothing for frontend files.** Writing `frontend/CLAUDE.md` showed that CI's `format:check` also checks Markdown, which the hook didn't format. Fixing that turned up a bigger problem: Prettier loads its plugins from the current directory. From the repo root it failed with "Cannot find package 'prettier-plugin-tailwindcss'", and the hook hides errors by design. It only worked when the session happened to be in `frontend/`, which is how the #9 check passed. The hook now runs Prettier from `frontend/` and also covers `.md`. Tested from the root on a `.md` file and on a `.tsx` file, and the Tailwind classes came out sorted.

**Human review:** the human accepted every convention listed under *Decisions*, and the package name `io.github.davidms` left open since #8:

> I accept all the conventions. PR is merged continue with #13

The PR was merged unchanged. This note was added afterwards, in #13.

## Decisions made along the way

These are conventions the human should confirm in review:

- **Backend packages by feature,** with layer subpackages inside, and one `app.biwenger` anti-corruption package.
- **Hand-written mapping** (`toDomain()`, `from(...)`), with no mapping library.
- **Frontend `features/<feature>/` folders** with thin pages. This replaces the `src/api/` folder the provisional skill had.
- **The time zone is `Europe/Madrid`** for the clock and for dates shown to the user, because La Liga and Biwenger run on Spanish time.
- **Rules live in the module `CLAUDE.md`, workflow in the skill.** One source for each, so they can't drift apart again.

## Lesson

A conventions file is code that runs in the AI's head, so test it like code. Run it: a fresh session showed the nested file loads and is used. Review it against the real code and the real library versions: two reviewers found a rule that broke a golden rule, two rules that contradicted each other, and a secret that would have been logged. And a hook that hides its own errors should be tested from the directory it really runs in.
