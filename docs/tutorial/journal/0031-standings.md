# #31 — Standings and rival squads

- **Issue:** [#31 [4.5] Standings and rival squads](https://github.com/DavidMS/biwenger-manager/issues/31)
- **Phase:** 4 — Read-only UI
- **Date:** 2026-10-28

## Prompt

> continue with #31

## What Claude did

1. **Checked that #88 (#30) was merged** before branching from `main`.
2. **Asked where a rival's squad comes from** (golden rule 1). Biwenger has no documented call for another manager's squad. The only source is each manager's saved lineup in `rounds/league`, which our API reference marks as *inferred*. Claude offered two options:
   - infer the squad from the lineups, with no new endpoint and no extra call, but possibly stale;
   - pause for a spike to capture `GET /api/v2/user/{id}?fields=players`, which the human would run.

   The human chose **infer from lineups**. The standings themselves are read live: it is the same small call the dashboard already makes.
3. **Checked the inference against the sample before writing the test:**
   - **The coach is in none of the lineup's lists.** Without adding `lineup.coach`, every squad would miss its coach. The leader's squad is 23 players from the lists plus the coach.
   - **Your own lineup shows 14 players while your live squad has 13.** The extra one is the player the API reference notes was taken by clause after the lineup was saved. This is the staleness the page warns about.
4. **Backend:**
   - `BiwengerMapper.leagueTable` builds a `LeagueEntry` per manager. It takes the starters and reserves (skipping empty slots), the players left out, and the coach, distinct and sorted. It also keeps the time the lineup was saved.
   - `StandingsService` keeps the table for a minute. A squad uses the cached table, so it costs no extra Biwenger call, and gets the players from `PlayerReader`.
   - `GET /api/standings` marks your row with `you`. `GET /api/standings/{managerId}/squad` answers 404 "Manager not found" for a manager not in the league.
5. **Frontend:**
   - `features/standings`: each manager's name is a real link to `?manager=<id>`, so the choice survives a reload and works with Back.
   - The squad card shows the team value, says when the lineup was saved and that later trades aren't reflected, and lists players in lineup order.
   - A manager in the URL who isn't in the table is ignored; #30's review taught this one.
   - It reuses `StatusBadge` and `BiwengerErrorState`, and the player types from `lib/player.ts`, all shared in #86 and #30.
   - Claude reversed the lineup order on purpose: the squad test failed. Then it put the order back.
6. **Size:** about 1,100 changed lines, most of them tests. Still over 400, but the shared pieces from #86 and #30 kept it to half the size of those PRs.

## Review

Both reviewers approved with nits, and Claude applied all of them but two.

`react-reviewer`:

- **While a squad loaded or failed, its message showed with no heading** saying whose squad it was. On a phone, below the table, that was a bare "Biwenger unavailable". **Fixed:** the card now gets the manager's name from the table and is a titled region in every state.
- The squad's loading state, empty state and "no missing players" case are now tested.
- The note about players not shown now says it in words instead of listing raw Biwenger ids. Comments say "their".
- **Not done:** on a phone the squad opens below the whole table, and nothing scrolls to it. Suggested as a follow-up.

`java-reviewer`:

- **The new mapper method had been inserted between `teamStatus`'s Javadoc and `teamStatus`,** so one method had two Javadocs and the other none. **Fixed** by putting the comment back above `teamStatus`.
- Two managers in the same position are now ordered by id (golden rule 5). The gateway's Javadoc now says the call is two on a cold start, and `LeagueEntry` null-checks its list.

## Corrections

- **An edit script missed JSX that Prettier had already reformatted**, the third time this session. The script stopped halfway, and Claude re-read the file and edited the current text.
- **Making the squad card a region in every state broke a test's timing.** The test found the region while the squad was still loading, so its text wasn't there yet. Claude switched to `findByText`, which waits. It also changed the click test, which passed only because the mock answered fast enough.

Human review may add more corrections. Record them here before merging.

## Decisions made along the way

- **Rival squads inferred from saved lineups,** shown with the save time and a warning that they may be stale.
- **Standings live, kept for a minute.** A squad uses the same cached table.
- **The coach counts as a squad player,** as in Biwenger.

## Lesson

**Check an inference against real data before trusting it.** The API reference said "players + reserves + discarded = the squad". Counting the sample showed the coach was missing from all three, which would have dropped every coach without any test noticing. It also showed a stale player in a real lineup, which turned the documented caveat into a message on the page.
