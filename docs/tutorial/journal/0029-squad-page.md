# #29 — My squad page: value trend, status badges and sorting

- **Issue:** [#29 [4.3] My squad page](https://github.com/DavidMS/biwenger-manager/issues/29)
- **Phase:** 4 — Read-only UI
- **Date:** 2026-10-28

## Prompt

> continue with the next issue I think is #29

## What Claude did

1. **Checked first that the previous PR (#84) had been merged**, with `gh pr list`, instead of trusting the git status from the start of the session (#28's lesson). Then it branched from an up-to-date `main`.
2. **Found that the issue, labelled frontend only, needed a backend endpoint**, and that the database didn't record which players are yours. The daily sync stores every player's value, but not the squad. Claude asked two questions before writing code:
   - **Data source.** Claude offered three options:
     - *hybrid*: the squad ids live, everything else from the database;
     - storing the squad in the sync (a new migration, stale after a buy);
     - fully live (the 230 KB catalog).

     The human chose **hybrid**.
   - **Trend window:** 7, 14 or 30 days. The human chose **14 days**.
3. **Backend:**
   - `BiwengerGateway.squadPlayerIds()`: one small `/user` call, ids sorted.
   - `PlayerReader` in `player/service`. A feature may not use another feature's persistence, so the squad reads the player tables through this service. It returns profiles, the values since a day, season points (the latest snapshot that has them, since a backfilled day has none) and the last matches, latest first, with ties broken by match id. The season points use a `DISTINCT ON` native query.
   - `GET /api/squad`: `SquadService` keeps the ids for a minute, and reads the database on every request. The response lists the players Biwenger says you own but the sync has never stored (`missingPlayerIds`), so they aren't silently dropped. Three of the 13 in the sample are like that.
4. **Frontend:**
   - `features/squad`: a `SquadList` container, `SquadTable`, `Sparkline` (an inline SVG, red when the value fell), `StatusBadge` and `RecentForm`. The shadcn `Table` was added with `npx shadcn add table`.
   - Sorting by position, value, points and trend is kept in the URL (`?sort=points&order=desc`). Position sorts in lineup order, and ties go to the lower player id in either direction (golden rule 5).
   - Tests cover loading, both error kinds, empty, the populated rows, every status badge, sorting by clicks and from the URL, and the missing players. Claude reversed the tie-break once on purpose to see the sort test fail.
5. **Size:** about 1,800 changed lines in 34 files, far over the 400-line guideline, as in #28. Most of it is tests and the generated table. The endpoint and the page only make sense together.

## Review

Both reviewers approved with nits. Claude applied everything except the two noted below.

`java-reviewer`:

- **The four database reads each had their own transaction.** A sync committing between them could mix two days in one answer: today's price with yesterday's trend. **Fixed:** one `PlayerReader.read(...)` with a single read-only transaction, returning `StoredPlayers`. The Biwenger call stays outside it, because holding a connection for up to ~80 s of retries would be worse.
- Missing tests for empty ids and for the match-date tie-break: **added**. A negative match count is now rejected.
- Not changed: a player with no season points yet shows 0, not "–". It is rare (only backfilled days) and the tests make it explicit.

`react-reviewer`:

- **The Suspended badge, named in the acceptance criteria, was never tested.** **Fixed** with a test for every status.
- The sparkline's accessible name now gives the start and end values, played matches read "6 points", and only the sorted column has `aria-sort`. `parseSort` uses a type guard instead of casts.
- Not changed here: the "not connected to Biwenger" error handling is now copied in three features. A follow-up issue is suggested in the PR.

## Corrections

- **`gh` and the JDK weren't on the agent's `PATH`.** Claude found `gh` in `/opt/homebrew/bin` and pointed `JAVA_HOME` at the SDKMAN JDK 25 named in `.sdkmanrc`.
- **A test expected "Ayoze Pérez"; the anonymised sample says "Ayoze".** Claude fixed the test data.
- **Two cells were named "Coach"**: the coach player and his position. Claude renamed the test player.

Human review may add more corrections. Record them here before merging.

## Decisions made along the way

- **Hybrid data source:** which players you own changes during the day (a won bid, a sale), so it is read live. Values, status and form change once a day, so they come from the database.
- **A 14-day trend**, computed in the backend (`Squad.TREND_DAYS`). The frontend only draws it and sorts by it.
- **Players the sync never stored are named, not hidden.**

## Lesson

**The issue's labels are a guess made when it was written.** #29 said "frontend", but the data it needed had never been stored. Reading the schema before the components showed this in a few minutes, and the data-source question then decided the shape of the whole change, as it did in #28.

The reviewers again found what the tests couldn't: a race between transactions, and an acceptance criterion (the Suspended badge) the code met but no test proved.
