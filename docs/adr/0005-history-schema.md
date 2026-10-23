# 0005. History schema: daily snapshots keyed by the Madrid day

- **Status:** Accepted
- **Date:** 2026-10-23
- **Issue:** #22

## Context

Biwenger only shows the present: today's prices, today's market, today's table. Its price history goes back about a year for one player at a time, and nothing keeps the market or the standings over time. Phase 3 stores that history in PostgreSQL (`V1__history_schema.sql`). The daily sync (#23) must be safe to run twice, and the backfill (#24) fills older days from the price history.

## Options considered

1. **One row per sync run,** keyed by a timestamp. Simple to write, but two runs on the same day double the data, and every reader has to pick "the" snapshot of a day.
2. **One row per natural key and day** (`player_id + snapshot_date`, `snapshot_date + player_id`, `snapshot_date + manager_id`). A second run the same day updates the row.
3. **Change-only rows** (store a price only when it changes). Smaller, but harder to query ("the value on day X") and to make idempotent.

## Decision

We will use **option 2**, with these rules:

- **A day is the calendar day in `Europe/Madrid`**, the zone La Liga and Biwenger run on (`backend/CLAUDE.md`). The market closes at 07:00 Madrid time, so a sync after the close belongs to the day it ran.
- **Snapshots have no foreign key to `player`.** A player who leaves La Liga drops out of the catalog (seen in #14), but his history must stay.
- **`player_price_snapshot` has nullable `price_change`, `status` and `points`, and a `source` column** (`DAILY_SYNC` or `BACKFILL`). The backfill only knows the price, so its rows leave those columns empty rather than guessing.
- **`market_snapshot` keeps the seller's ownership data** (clause, price paid, acquired, clause unlocks) for players listed by managers. The human chose this in #22, because it can't be fetched for past days later. It feeds clause watch (F13) and the rival-balance estimate (F4).
- **`player_points_report` keeps points in one scoring system, the league's, and stores which one** (`score_id`). The human chose this over all seven systems, because nothing planned needs the others.
- **The database checks consistency itself:** points only for a played match, no seller without ownership data, no negative prices. A bug in the code then fails loudly instead of storing a contradiction.
- **Money is `BIGINT` euros,** and times are `TIMESTAMPTZ` in UTC (`hibernate.jdbc.time_zone=UTC`).

Flyway owns the schema; Hibernate only validates it (`ddl-auto=validate`).

## Consequences

- A sync can be re-run any time on a day without cleanup, and "the value on day X" is a primary-key lookup.
- At most one snapshot per day: changes within a day (a price that moves twice) are not kept. Biwenger updates prices once a day, so nothing is lost today.
- Readers must handle backfilled days without status or points.
- If the league ever changes its scoring system, older reports keep the old `score_id`; comparing across systems would need a re-fetch.
- Tests and the app need PostgreSQL. Tests use Testcontainers with the same image as Docker Compose.
