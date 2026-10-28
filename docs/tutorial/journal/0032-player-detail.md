# #32 — Player detail page

- **Issue:** [#32 [4.6] Player detail page](https://github.com/DavidMS/biwenger-manager/issues/32)
- **Phase:** 4 — Read-only UI
- **Date:** 2026-10-28

## Prompt

> continue with #32

## What Claude did

1. **Asked how to draw the chart** (golden rule 1). The app had only drawn a tiny hand-made SVG sparkline so far. Claude offered two options:
   - a hand-made SVG line, with no new dependency but axes, tooltip and resizing to write by hand;
   - shadcn's `chart` component, which wraps Recharts and follows the app's theme.

   The human chose **the shadcn chart**. Claude loaded the `dataviz` skill before writing any chart code.
2. **Backend:** everything comes from the database, with no Biwenger call.
   - `PlayerReader.detail(id)` reads the profile, every daily price (oldest first), the season points and every match report (by date, then match id) in one read-only transaction.
   - `PlayerReader.team(id)` finds the club among all stored teams, so a player of a relegated club still gets a club name. When no club is stored, the JSON leaves the name out.
   - `GET /api/players/{playerId}` answers 404 "Player not found" for a player that was never synced.
3. **Frontend:**
   - `features/player`: a page header with the name, club and position, then cards for the status, the value history and the points per match.
   - **The chart follows the dataviz rules:**
     - one series, so there is no legend (the card title names it);
     - a 2px line with no dots and a crosshair tooltip;
     - a text summary in the `figcaption` ("Value from €4.8m on 1 Aug to €5.2m on 22 Oct; lowest …");
     - a "Show as a table" view with every value, latest first.

     The chart itself is `aria-hidden`, because the summary and the table say the same thing in text.
   - **The line uses `--primary`, not `--chart-1`.** The theme's chart colours are light greys that almost vanish as a 2px line on white.
   - The matches table shows the date, home or away, points and minutes. When the player didn't play, it shows the reason ("Not picked", "Injured", …) instead.
   - A new `PlayerLink` makes player names link to `/players/:id` on the squad, market and standings pages. Each page has a test for the link.
   - An id in the URL that isn't a number shows "Player not found" without calling the API.
4. **Kept the chart out of the first download.** Adding Recharts grew the main bundle from 130 KB to 235 KB gzipped. The player page is now loaded only when it's opened (`React.lazy`), and the main bundle is back to 134 KB. The page's own chunk is 102 KB.
5. **Mutation check:** Claude removed the line that puts the latest match first. The order test failed, then Claude put the line back.
6. **Documented the chart rules** in `frontend/CLAUDE.md` (a new Charts section), so the next chart starts from them.

## Review

`react-reviewer` asked for changes, and Claude applied all of them:

- **A test failed on the reviewer's machine and passed on Claude's.** `formatMoneyCompact` used `Intl`'s compact notation, and whether `en-GB` ends `€4.8m` or `€4.8M` depends on the ICU data in the Node build. CI could have gone either way. **Fixed:** the function now builds the string by hand, with tests for millions, thousands, small and negative amounts.
- **The chart was hidden from screen readers but still a Tab stop.** Recharts 3 turns on keyboard support by default, which gives the SVG `tabIndex=0` inside an `aria-hidden` figure. **Fixed** with `accessibilityLayer={false}`, and the rule added to the Charts section of `frontend/CLAUDE.md`.
- **While the page's code downloaded, there was no heading.** The lazy load's fallback is now the same "Player" header and loading state. The loading test then found that fallback before the page had loaded, so it now waits for the API call.
- The tooltip's date is narrowed to a string before formatting, instead of turning a missing value into `"undefined"`.

`java-reviewer` approved with nits. It judged the controller's two reads (the player, then the club name) fine without a service around them.

- **The order test didn't show that the date comes before the match id.** Both matches were on the same day. **Fixed:** an earlier match with a higher id now comes first. A test for a player with no prices or matches was added too.
- The old, untied `findByIdPlayerIdOrderByMatchDateAsc` was left unused, so a later caller could pick it by mistake. **Removed**, and its repository test now uses the tied version.
- **Not done:** the current price is called `value` here and `price` on the squad. The page calls it "value" throughout, so the name stayed.

## Corrections

- **The lint check failed on the lazy page.** `react-refresh` wants a file to export only components, and the `lazy()` call sat in `routes.tsx` beside the route table. Claude moved it to its own file, `pages/LazyPlayerPage.tsx`.
- **Recharts needs `ResizeObserver`, which jsdom doesn't have.** Claude stubbed it in `src/test/setup.ts`. Since jsdom can't lay out a chart either, the tests check the summary and the table view instead of the SVG.
- **Nobody has looked at the chart in a browser yet.** It needs a synced database, and the sync needs the Biwenger credentials in `.env`, which Claude doesn't read. The human should open a player page before merging.

Human review may add more corrections. Record them here before merging.

## Decisions made along the way

- **shadcn chart (Recharts) for charts,** loaded only with the pages that draw them.
- **`--primary` for a single line,** because the theme's chart colours are too light.
- **Every chart has a text summary and a table view,** so no value is reachable only by hovering.

## Lesson

**A new dependency has a size, so measure it.** The chart took one command to add and every test passed, but the first page anyone opened became nearly twice as heavy. Reading the build output caught it, and a lazy load fixed it without changing the page. Also, **a test that passes on your machine can depend on your machine.** The money format test relied on the ICU data in Node, and only a second run elsewhere showed it. And **say what wasn't checked:** passing tests don't show that a chart looks right.
