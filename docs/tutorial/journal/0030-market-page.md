# #30 — Market page: filters, sorting and the seller

- **Issue:** [#30 [4.4] Market page](https://github.com/DavidMS/biwenger-manager/issues/30)
- **Phase:** 4 — Read-only UI
- **Date:** 2026-10-28

## Prompt

> continue with #30

## What Claude did

1. **Checked the live state of the previous PR (#87) before starting.** It was merged, so Claude branched from an up-to-date `main`. This is the habit #28 and #86 asked for.
2. **Asked two data questions before writing code** (golden rule 1):
   - **The listings:** the daily sync stores the market once a day, but managers list players at any time. The human chose **hybrid, like the squad**: the listings are read live (one ~5 KB call, kept for a minute), and status, trend and points come from the database.
   - **Team names for the team filter:** they weren't stored anywhere, only `team_id`. Claude offered three options:
     - store the clubs in the sync;
     - read the 230 KB catalog live and cache it;
     - filter by id with no names.

     The human chose to **store them**: a `team` table (Flyway V5) that the daily sync fills from the catalog it already downloads.
3. **Backend:**
   - `GET /api/market`: `MarketService` keeps the listings for a minute and joins them with `PlayerReader.read(ids, from, 0)`. Asking for 0 matches now skips the reports query.
   - Points per million is computed in the backend: a `BigDecimal` of points × 1,000,000 / asking price, to two decimals, rounded half up. It is exact and the same on every run, and the advisor (Phase 5) can reuse the definition.
   - `DailyPrice.change(...)` now holds the trend change, so the squad and the market share it.
   - `DailySyncTest` checks that the 20 clubs are stored, and that there are still 20 after the next day's sync.
4. **Frontend:**
   - **Shared pieces first.** The market needed the sparkline, the status badge and the sortable header from the squad page. Rule: "a feature doesn't import another feature's internals". So they moved to `components/player/` and `components/table/`, and the player types and labels to `lib/player.ts`. The squad's tests passed unchanged after the move.
   - `features/market`:
     - `marketView.ts`: `parseView` and `applyView`, pure and unit-tested;
     - `MarketFilters`: native selects and number inputs from shadcn, every one with a label;
     - `MarketTable`, with the seller ("Free agent" or the manager) and the closing time in Madrid time;
     - `MarketList`, which keeps every filter and the sort in the URL.
   - "Nobody is on sale" and "No player matches these filters" are different empty states.
   - Claude flipped the team filter on purpose: three tests failed. Then it put it back.
5. **Size:** about 1,900 changed lines in 44 files, again far over 400. The reasons:
   - the endpoint, the migration and the page belong together;
   - three shadcn components were generated;
   - about 150 lines of the moves are unchanged code.

## Review

Both reviewers approved with nits.

`react-reviewer`'s one real finding:

- **A stale link contradicted itself.** With `?team=999`, the list was filtered by a club that doesn't exist, showing "No player matches these filters", while the Team select showed "Any team". **Fixed:** a team that isn't a club is ignored, and the selects now show the checked view, not the raw URL. That also removed an unchecked cast. A test covers it.
- Also applied:
  - the filters are hidden when nobody is on sale;
  - a max price below the min is marked `aria-invalid`, with a message;
  - ids come from `useId`;
  - tests find controls by role (`combobox`, `spinbutton`, `row`).

`java-reviewer`:

- **Relegated clubs would stay in the team filter forever:** the sync only upserts. **Fixed now rather than in a follow-up,** since it was one query: the filter lists only the clubs the latest sync saw (`max(updated_at)`). A relegated club's row stays, so older players keep their club.
- The sync tests now check the clubs on a same-day re-run and after a failed write, which must roll them back.
- The response's `Seller` DTO is renamed `SellerItem`, so the domain `Seller` no longer needs its full package name. An unused accessor is gone.
- Not changed: the players and the clubs are read in two transactions. At worst the club list is one sync newer than the players, which is harmless.

## Corrections

- **The first lint run failed:** `MarketFilters.tsx` exported a constant next to a component, which breaks React Fast Refresh. The filter names moved to `marketView.ts`.
- **An edit script missed a line the formatter had joined**, as in #27 and #28. Spotless had put a test assertion on one line, so a replace by exact text found nothing. Claude re-read the file and edited the current text.
- **A type import pointed to the old place.** `SortOrder` moved to `SortableHead` during the refactor, and the build's type check caught the stale import. Vitest doesn't type-check, so all the tests had passed.

Human review may add more corrections. Record them here before merging.

## Decisions made along the way

- **Hybrid market:** listings live, history from the database, as for the squad.
- **Clubs stored by the sync** (V5), not read live: one more table, no extra Biwenger call.
- **Points per million computed in the backend**, from the asking price, because that is what you would pay.
- **Default sort: highest price first.** Every filter and the sort live in the URL.

## Lesson

**A second page is when shared components appear.** The squad page was built alone, so its sparkline and badge lived inside it. The market page needed both, and the project's rule pointed to the right home before anything was copied. Moving them in the same PR, with the squad's tests unchanged as a check, was cheaper than copying them now and merging them later.

**`npm test` passing isn't the build passing.** Vitest strips types without checking them, so a broken type import only showed up in `npm run build`. Run all four commands, as `frontend/CLAUDE.md` says.
