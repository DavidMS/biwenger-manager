# #92 — The market table fits at laptop width

- **Issue:** [#92 [4.x] Market table: the Closes column is hidden at laptop width with no hint it scrolls](https://github.com/DavidMS/biwenger-manager/issues/92)
- **Phase:** 4 — Read-only UI
- **Date:** 2026-11-06

## Prompt

> continue with #92 and #93

## What Claude did

1. **Reproduced the problem with harder data.** The demo server from [#93](0093-player-link-affordance.md) has long names on purpose. With them the table was 1447px wide in a 1088px card, so both Seller and Closes were out of view, not just Closes.
2. **Merged the columns nothing sorts by.** Team and position moved under the player's name, as a small second line. That frees the most width and costs no feature, because neither column had a sort button.
3. **Shortened the closing time** to `30 Oct, 11:15` with a new `formatShortDateTime`. The full date stays in the `title` of a `<time>` element. Long seller names now wrap, down to a minimum width.
4. **Added a scroll shadow for every table.** About ten lines in `index.css` target shadcn's `data-slot='table-container'`, so the generated `ui/table.tsx` didn't change and the squad table got the same fix. It is pure CSS: the shadows stay in place, and card-coloured covers scroll with the content and hide the shadow at the end you have reached.
5. **Measured instead of guessing.** The script from #93 read each table's `clientWidth` and `scrollWidth` in headless Chrome, and could lay the page out at a real 390px, under the window minimum chapter 05 warned about.

| Page | 1280px | 1024px | 500px |
|---|---|---|---|
| Market, before | 1447 in 1088 | not measured | not measured |
| Market, after | fits | 1056 in 960, scrolls | 1056 in 436, scrolls |
| Squad (no layout change) | fits | 970 in 960, scrolls | 970 in 436, scrolls |

6. **Looked at the screenshots** at [1280px](../images/92/market-1280.png) and [500px](../images/92/market-500.png), and at the table scrolled to the middle and to the end, to see the shadow move.
7. **Ran `react-reviewer`,** lint, the format check, tests and the build.

## Corrections

- **Short sellers wrapped at narrow widths.** Once a table scrolls it shrinks to its minimum, so "Free agent" and "Manager 4" took two lines. The screenshot showed it. `min-w-28` on the seller cell fixed it, and the table still fits at 1280px.
- **The measuring loop printed nothing useful at first.** zsh doesn't split `$s` into words as bash does, so the script got one argument. The numbers above come from the rerun.

Human review may add more corrections. Record them here before merging.

## Decisions made along the way

- **No column is hidden.** On a phone, hiding a column would lose data. Closes is the column you most need, so scrolling with a hint is better.
- **The hint lives in `index.css`, not in the generated `Table`.** Every table gets it, and a later `shadcn add` can't overwrite it.
- **Tests say what moved.** Closes shows the short format and carries the full one in `title`. Team and position stay as separate text in the row, so those checks didn't change. The name helper now reads the link, because the first cell also holds the team.

## Lesson

Test layout with the longest data you can imagine, not the sample you have. The bug was reported as one hidden column; long names showed two. jsdom can't see any of this, so a small script that reads `scrollWidth` from a real browser gives the number a unit test can't.
