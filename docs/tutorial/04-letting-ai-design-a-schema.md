# 04 — Letting AI design a schema

Phase 3 gives the app a memory. Biwenger only shows the present: today's prices, today's market, today's table. This phase adds:

- a domain model
- a PostgreSQL schema
- a daily sync that keeps a snapshot of each day
- a backfill of the season so far
- a refresh of match points after each round
- endpoints to start a sync and see how it went

Claude Code designed and wrote all of it. This chapter covers which decisions it handed back to the human, which rules it put into the database itself, and what reviews and real runs corrected. It also covers the phase's one serious mistake: a test that called the real API.

Issues: [#21](https://github.com/DavidMS/biwenger-manager/issues/21) domain model, [#22](https://github.com/DavidMS/biwenger-manager/issues/22) schema, [#23](https://github.com/DavidMS/biwenger-manager/issues/23) daily sync, [#24](https://github.com/DavidMS/biwenger-manager/issues/24) backfill, [#25](https://github.com/DavidMS/biwenger-manager/issues/25) manual sync and status, [#78](https://github.com/DavidMS/biwenger-manager/issues/78) points refresh (a follow-up raised in #24's review) and [#26](https://github.com/DavidMS/biwenger-manager/issues/26) this chapter.

## Goal

Every later feature reads from these tables:

- the dashboard's team value
- a player's price chart
- the advisor's form and points per match

So the schema had to be right in a specific way: **safe to write twice, impossible to fill with contradictions, and owned by one writer per row.** A schema that looks tidy but lets two jobs fight over a row, or stores half a record, fails one morning when nobody is watching.

## Concepts

### The human answers the schema questions

Some schema choices are product decisions. Claude asked them with a question tool before writing a migration, and the journal quotes each answer. These are the human's real answers:

| Issue | Question | Answer |
|---|---|---|
| [#22](journal/0022-flyway-schema.md) | Keep the sellers' ownership data (clause, price paid, clause lock) in the market snapshot? | > Yes, store it (Recommended) |
| [#22](journal/0022-flyway-schema.md) | Match points in all 7 scoring systems, or the league's? | > League's system only (Recommended) |
| [#24](journal/0024-history-backfill.md) | How to start the one-off backfill? | > Setting in .env (Recommended) |
| [#78](journal/0078-points-refresh.md) | Refresh every player's points, or only the ones that matter? | > All catalog players (Recommended) |
| [#78](journal/0078-points-refresh.md) | Run the refresh on its own schedule, or inside the daily sync? | > Inside the daily sync (Recommended) |

Each option said what it costs: "about 550 requests, roughly 5 minutes", or "can't be fetched for past days later". The human was choosing between consequences, not between names. Claude decided the rest itself and wrote it down in [ADR 0005](../adr/0005-history-schema.md), so the human could overrule it in review.

### Natural keys make the sync safe to repeat

Every snapshot row is keyed by what it describes and the day: `player_id + snapshot_date`, `snapshot_date + manager_id`. A second sync on the same day updates the row instead of adding one. The day is the **calendar day in Madrid**, where La Liga is played, computed from an injected `Clock` so tests can fix it. One test checks that 23:30 UTC on the 22nd is already the 23rd.

The alternative, one row per sync run, is simpler to write but harder for every reader, because each would have to pick "the" snapshot of a day. Making the write side careful once keeps the read side simple forever.

### Let the database enforce what the code promises

The domain records already refused bad data, for example a sale by a manager without ownership data. The schema repeats each rule as a `CHECK`, and a test inserts the bad row with plain SQL to prove the database rejects it. The reason is that the code isn't the only thing that will ever write here: a future migration, a manual fix or a bug can too.

The first draft got this wrong in an instructive way. It checked one column of a group (`clause` must come with a seller) instead of the group. A row with a seller and no purchase price would have been accepted, and the app would then have crashed reading it back. The fix checks all six columns together: `num_nulls(seller…, clause, purchase_price, …) IN (0, 6)`.

### One owner per row

Two writers that are each correct can still collide:

- The **daily sync** writes today's prices. The **backfill** writes a year of past prices, and that history ends with today.
- If both wrote today's row at the same moment, the sync's whole day would roll back, losing a market and a table that can't be fetched later.

The fix was a rule rather than a lock: **today belongs to the daily sync, and the backfill stops at yesterday** ([#24](journal/0024-history-backfill.md)). The same thinking produced the other boundaries:

- **The sync fetches first and writes after,** in one transaction, so no transaction waits on the network and a failure stores nothing.
- **Bulk writes use batched `INSERT … ON CONFLICT`** instead of JPA's `save()`, which runs a `SELECT` per row: about 200,000 rows for the backfill.
- **The backfill never overwrites a day the sync stored** (`DO NOTHING`).

### Prove it on the real stack

Unit and integration tests ran against a real PostgreSQL in Testcontainers, with the same image as Docker Compose. Twice, Claude also started the whole app in a separate Compose project with a scratch env file, so the real `.env` was never touched:

- **[#22](journal/0022-flyway-schema.md):** Flyway migrated an empty database at startup, and a restart reported "up to date".
- **[#25](journal/0025-manual-sync.md):** the endpoints worked through nginx, with **no Biwenger login**, so nothing reached Biwenger. That run found a small race the tests had missed: right after `POST /api/sync`, the status said "running" but didn't show the run yet.

## How it went

| Issue | Prompt | Result |
|---|---|---|
| [#21](journal/0021-domain-model.md) | > merged, move phase 3 to ready and go with #21 | Domain types, a `Money` type, sealed variants, the mapper from Biwenger's records |
| [#22](journal/0022-flyway-schema.md) | > merged, continue with #22 | Five tables in `V1`, entities, repositories, ADR 0005 |
| [#23](journal/0023-daily-sync.md) | > merged, continue with #23 | The 07:30 daily sync, idempotent, all-or-nothing |
| [#24](journal/0024-history-backfill.md) | > merged, continue with #24 | Resumable backfill of the season's prices and match points |
| [#25](journal/0025-manual-sync.md) | > merged, start with #25 | `POST /api/sync`, `GET /api/sync/status`, the first `ProblemDetail` errors |
| [#78](journal/0078-points-refresh.md) | > go on with #78 | Match points refreshed after each finished round |

Two prompts in this phase changed how Claude works rather than what it builds. The first approved the follow-up issue and handed over the push:

> yes, a follow-up issue should keep match points up to date after the one-off-backfill... also you can push and create the PR if you haven't already

The second made that a standing rule, which Claude saved as a memory for future sessions:

> push it and open the PR when the review is done

## What went wrong

The phase's reviews found more real bugs than any phase before. Most were about time and concurrency, which a read-through rarely catches.

- **A test called the real Biwenger API** ([#78](journal/0078-points-refresh.md)). `DailySyncTest`, written in #23, pointed only the API at WireMock. When #78 made the sync also call the CDN, one test run sent **39 real requests to `cf.biwenger.com`**. They were public, read-only and without a login, but they broke golden rule 3. Claude stopped, told the human, and added a **default that fails closed**: in tests, both Biwenger URLs now point at a port where nothing listens, unless a test points them at WireMock. The test had been safe when written; it became unsafe when the code under it started calling something new.
- **Half a row was allowed** ([#22](journal/0022-flyway-schema.md)), as described above. A `CHECK` on one column of a group doesn't check the group.
- **Two writers on one row** ([#24](journal/0024-history-backfill.md)): the backfill's "today" against the sync's.
- **One bad player would block every round forever** ([#78](journal/0078-points-refresh.md)). Rounds were marked as refreshed only if every player succeeded, so one page that always answers 404 would have meant about 550 requests every day for nothing. The client now says whether a failure is **transient**, and only transient ones keep a round pending.
- **A startup cleanup failed fresh runs** ([#25](journal/0025-manual-sync.md)). The web server and the scheduler start *before* Spring's "ready" event, so a sync started in that window was marked "interrupted" by the cleanup meant for crashed runs.
- **A failure could leave no trace** ([#25](journal/0025-manual-sync.md)). Recording a failure ran before logging it, so with the database down, nothing was logged at all. The order is now log first, then record.
- **Tests passing by luck** ([#23](journal/0023-daily-sync.md)). A test class left rows behind for other classes, and only its method order hid it. Three attempts to reproduce the failure didn't work. It was fixed anyway, by cleaning up after every test.
- **Proofs that proved nothing.** Breaking code on purpose only counts when it fails for the expected reason:
  - In #25, replacing a cutoff with `Instant.MAX` broke the app's startup instead of the rule.
  - In #24, a rate-limit test checked each gap between requests as they *arrived* at WireMock, which network jitter shortens. The test now checks the total span instead.
- **The test database ran out of connections:** about 13 cached Spring contexts × 10 connections each, against PostgreSQL's limit of 100.

## Results

- 6 PRs ([#74](https://github.com/DavidMS/biwenger-manager/pull/74)–[#77](https://github.com/DavidMS/biwenger-manager/pull/77), [#79](https://github.com/DavidMS/biwenger-manager/pull/79), [#80](https://github.com/DavidMS/biwenger-manager/pull/80)) for 6 issues, plus this chapter, and 1 issue (#78) added by a review.
- About 2,600 lines of code and 2,300 of tests. 151 Java tests in total, most against a real PostgreSQL.
- 4 Flyway migrations (`V1`–`V4`) and 1 ADR ([0005](../adr/0005-history-schema.md)).
- 5 schema and product questions answered by the human.
- **Requests that reached the real Biwenger from tests: 39.** All were public CDN reads, and now blocked by the fail-closed default. Writes sent by code: 0.

## Takeaways

- **Hand the schema's product questions to the human, with their costs,** and write down the rest in an ADR they can overrule.
- **Key snapshots by what they describe and the day,** so the job that fills them is safe to repeat.
- **Put each rule in the database as well as the code,** check the whole group of columns, and test that a bad row is rejected.
- **Give every row one owner.** When two jobs could write it, decide whose it is instead of reaching for a lock.
- **Make the safe outcome the default.** A test URL that points nowhere unless set protects every future test. A rule in a document only protects the tests whose authors remember it.
