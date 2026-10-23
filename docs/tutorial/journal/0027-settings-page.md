# #27 — Settings page: connected account, league and last sync

- **Issue:** [#27 [4.1] Settings page: connected account, league and last sync](https://github.com/DavidMS/biwenger-manager/issues/27)
- **Phase:** 4 — Read-only UI
- **Date:** 2026-10-23

## Prompt

> merged, move phase 4 to ready and go with the first issue

## What Claude did

1. Moved #26 to *Done*, #27 to *In progress* and #28–#33 to *Ready*, and branched. This is the first issue with both a backend and a frontend part, and the first frontend feature with real data, so its patterns are what #28–#32 will copy.
2. **Backend:**
   - `GET /api/account` returns the account, team and league names. They aren't stored, so the backend reads them from Biwenger and keeps them for 10 minutes; opening the page doesn't cost a request each time.
   - A missing or rejected login answers **503**, and a Biwenger failure **502**, both as `ProblemDetail`.
3. **Frontend:**
   - `lib/api.ts`, the one place that calls the backend. Problem details become an `ApiError`.
   - Madrid-time formatting in `lib/format.ts`.
   - shadcn's card, badge and alert, added with its CLI.
   - The settings feature, the page, its route and its menu entry.
   - "Sync now" calls `POST /api/sync`, then polls the status every 2 seconds while the sync runs.
4. **The frontend got its own safety net,** like the backend's from #78: `fetch` fails in every test unless the feature's `api.ts` is mocked.
5. **Ran the real app** in Docker with no Biwenger login, and took headless-Chrome screenshots of the page:
   - "Not connected to Biwenger", with the fix to apply.
   - After "Sync now", the failed run with its reason and the time in Madrid time.
   - At 500px, the cards stack.

## Review

Both reviewers ran. Claude checked their claims and applied everything.

`react-reviewer`: **changes requested.** None of it was a bug in today's flow, but each finding would have been copied into five more pages.

- **Shared loading, error and empty states,** which `frontend/CLAUDE.md` said the first data view should create, were missing. **Added** in `components/state/`, together with `SectionCard`, a card that is a labelled region with a real heading.
- **Tests found cards with `data-slot` selectors and casts.** They now use `getByRole('region', { name })`, and are split per component.
- **Several states were untested,** and the polling test waited 2 seconds in real time. **Fixed:** every state has a test, and polling uses fake timers. The suite went from 3.4 to 1.5 seconds.
- **Smaller fixes:**
  - Stale messages are cleared when a sync finishes.
  - The status area is announced to screen readers (`role="status"`).
  - The button keeps focus while busy (`aria-disabled`).
  - `lib/api.ts` keeps all request headers, handles 204, and can post JSON for Phase 6.
  - "Not connected" also covers a rejected login.
- **Forward-looking:** when a sync finishes, every other view is now refreshed, so #28–#32 get it for free.

`java-reviewer`: **approve with nits.**

- The 502 and 503 handlers now log once, as `backend/CLAUDE.md` asks.
- The "two account reads on a cold start" Javadoc is now honest.
- A login without a token is a 502 (Biwenger misbehaving), not "check your credentials".
- The gateway got a WireMock test.

## Corrections

- **`@DirtiesContext` stopped the shared test container.** To isolate the new gateway test, Claude marked it `@DirtiesContext`. Closing that context made Spring Boot stop the PostgreSQL container that every test class shares. Every later test then failed after 30 seconds with "connection refused", and two builds ran past 10 minutes. The first fix, `@Bean(destroyMethod = "")`, didn't help, because Boot stops Testcontainers beans through its own lifecycle. The real fix was to give the gateway test **its own class**, and so its own context. `backend/CLAUDE.md` now says never to use `@DirtiesContext` here.
- **Claude's first sync test expected the wrong sequence.** After "Sync now", the mocked status still said "succeeded", while the real backend says "running". The test now follows the real sequence.
- **Edit scripts stopped on files the formatter had re-wrapped,** twice. Claude rewrote those files whole instead.

Human review may add more corrections. Record them here before merging.

## Decisions made along the way

- **Cache the account for 10 minutes, don't store it:** the names change rarely, and the sync never needed them.
- **Poll while running, stop when done;** no WebSocket for a sync that runs once a day.
- **503 for "not connected", 502 for "Biwenger failed",** so the page can say exactly what to do.

## Lesson

The first instance of a pattern is a template, whether you mean it to be or not. The reviewer's "changes requested" was about what the next five pages would copy, not about bugs. Spending the extra round on the first page (shared states, labelled regions, tests by role, fake timers) is cheaper than fixing five copies later.
