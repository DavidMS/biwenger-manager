# 0006. Player scoring model

- **Status:** Accepted
- **Date:** 2026-11-22
- **Issue:** #34

## Context

Every advisor in Phase 5 (buys, sells, bid range, lineup, what-if) needs the same per-player numbers: how many points he's likely to score next, whether his value is rising, and how much he returns per euro. Golden rule 5 says recommendations are deterministic and explained: the same data and configuration give the same output, ties are broken by player id, and every result shows the numbers that produced it.

What the database holds per player: match reports (points, minutes, home or away, why he missed a match), daily prices, his current status and season points. It does **not** store his club's next fixture: Biwenger's catalog has `teams[].nextGames`, but the sync doesn't save it yet.

## Options considered

1. **Separate components (chosen).** Expected points, value trend and points per million, each with its inputs. Buy, sell and lineup combine them in their own way. Pro: each advisor weighs what matters to it (a lineup doesn't care about value trend). Con: no single "score" to sort by.
2. **One composite score.** A configurable weighted sum used by every advisor. Pro: one number. Con: one set of weights has to suit very different questions, and the sum is harder to explain.

For home/away: take the next venue as an **input** (`HOME`, `AWAY`, `UNKNOWN`), sync the fixtures in this issue too, or leave the venue out. We take it as an input. The model is complete now, and syncing fixtures becomes a separate small issue.

For a fit player who wasn't picked: count the match as **0 points** (rotation risk is a real signal) or leave it out. We count it as 0.

## Decision

We will compute a `PlayerScore` in `advisor.domain.PlayerScorer`, a pure function with no I/O, no clock and no floating point (`BigDecimal`, half-up rounding):

1. **Recent form**: weighted average of the last `formMatches` (5) matches he played or was fit and left out of (0 points), newest weighing 5, then 4 … 1. Matches missed for any other reason (injury, doubt, suspension or a status we don't know) are skipped, because the status multiplier covers his availability.
2. **Season average**: points per match played, from his match reports.
3. **Base**: `formWeight` (0.6) × form + 0.4 × season average.
4. **Venue factor**: `(season average + venueWeight (0.5) × (venue average − season average)) ÷ season average`, computed with one division and never below 0. It is 1 when the venue is unknown, he has fewer than `venueMinGames` (3) at home or away, or his season average isn't positive.
5. **Expected points**: base × venue factor × status multiplier (available 1, doubtful 0.5, injured 0, suspended 0, unknown 0.5).
6. **Value trend**: latest price minus the oldest one in the last `trendDays` (7) days, the latest included (the market page counts its trend the same way), in euros and as a percentage.
7. **Points per million**: season points per million of his current value. It's the market page's formula, but the market divides by the asking price, so the two differ for a player on sale.

The score reports every input of every step, including the home and away games and the venue average, and each step works on those rounded numbers (two decimals, four for the venue factor), so a score can be recomputed by hand from what the UI shows. Inputs are sorted inside the model, so the order the caller passes doesn't change the result. Ranking by expected points breaks ties by player id (`PlayerScore.BY_EXPECTED_POINTS`).

Every weight is set under `biwenger.advisor.scoring.*` (`ScoringProperties`). A value out of range stops the app at startup.

## Consequences

- Advisors 5.2 to 5.6 build on `PlayerScore` and pick the components they need.
- Until next fixtures are synced, callers pass `Venue.UNKNOWN`, so the venue factor is always 1. Storing `nextGames` is a follow-up issue.
- Season average comes from match reports, while points per million uses the snapshot's season points. They agree as long as the reports are complete, which the post-round points refresh (ADR 0005) keeps true.
- The weights are a first guess. Tuning them changes recommendations without a code change, but there's no back-test yet to tell whether a change is better. Season analytics (7.4) could add one.
