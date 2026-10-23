# #22 — Database schema with Flyway

- **Issue:** [#22 [3.2] Database schema with Flyway](https://github.com/DavidMS/biwenger-manager/issues/22)
- **Phase:** 3 — Domain, persistence & history
- **Date:** 2026-10-23

## Prompt

> merged, continue with #22

## What Claude did

1. Moved #21 to *Done* and #22 to *In progress*, and branched.
2. **Asked the two schema questions that were the human's to answer.** The first was left open from #21. The human chose:

   > Yes, store it (Recommended)

   > League's system only (Recommended)

   So the market snapshot keeps sellers' ownership data (clause, price paid, acquired, clause lock), and match reports keep points in the league's scoring system, along with which system that is.
3. Wrote `V1__history_schema.sql` with five tables, keyed by natural key and day: `player_id + snapshot_date`, and so on. A second sync on the same day then updates instead of duplicating, which #23 needs. The day is the Madrid calendar day, and snapshots have no foreign key to `player`, so a player who leaves La Liga keeps his history.
4. Added `Ownership` and `Standing` to the domain, and the mapper for both. Wrote JPA entities with `from(domain)` and `toDomain()`, composite keys as `@Embeddable` records, and Spring Data repositories.
5. Tested against a real PostgreSQL in Testcontainers, with one shared container for the whole run:
   - round trips of the real samples through the mapper and the database
   - a second save on the same day updating the row
   - the history queries' ordering
   - the database's own checks rejecting bad rows

   Removing a check on purpose made its test fail. With `ddl-auto=validate`, the app wouldn't even start if an entity didn't match the migration.
6. **Ran the real thing.** The full Docker Compose stack, in a separate project with a scratch env file: Flyway migrated an empty database at startup, a restart reported "up to date", and the stack and volume were removed afterwards.
7. Wrote [ADR 0005](../../adr/0005-history-schema.md), and updated the run commands. The backend now needs the database, and the tests need Docker.

## Review

`java-reviewer`: **changes requested.** Claude checked each point against the schema and applied all of them before the migration reached `main`, so V1 could still be edited:

- **major:** only `clause` was tied to the seller. A sale with a seller but no purchase price would be accepted, and then crash `toDomain()`. **Fixed:** all six seller and ownership columns must come together (`num_nulls(…) IN (0, 6)`). A test inserts half the data, and it fails without the new check.
- **major:** a missed match could have no reason, which the entity never writes. **Fixed:** a reason exactly when he didn't play, and 0 minutes then.
- **minor:** status columns had no allowed values. **Added.**
- **minor, for #24:** the backfill would overwrite a daily row and null its status and points, and `save()` runs a `SELECT` per row, which is too slow for about 200,000 rows. **Written into ADR 0005:** batches of `INSERT … ON CONFLICT DO NOTHING`.
- **minor:** nothing recorded when a row was last written. **Added** `synced_at` to every snapshot.
- **The idempotency tests only counted rows.** They now change a value between the two saves and check that the row was updated.

## Corrections

- **A misleading comment in the migration.** It said "read per player, newest first" above indexes that are by date and by round. Fixed before the first commit: per-player reads use the primary keys.
- **The first constraints were too weak,** as the review shows. A check on one column of a group doesn't check the group. That's worth remembering for every "these fields come together" rule.

Human review may add more corrections. Record them here before merging.

## Decisions made along the way

See [ADR 0005](../../adr/0005-history-schema.md): one row per key and Madrid day; no foreign keys from snapshots; nullable backfill columns plus `source`; ownership kept; points in one scoring system.

## Lesson

Let the database enforce what the code promises. The domain rules ("a manager's sale has ownership data") were already in the records, but a schema that allows half a row will one day hold one, from a bug, a manual fix or a future migration. A `CHECK` per rule, and a test that inserts the bad row, make the promise hold where the data lives.
