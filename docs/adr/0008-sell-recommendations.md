# 0008. Sell recommendations by signals, ranked by loss to your XI

- **Status:** Accepted
- **Date:** 2026-11-22
- **Issue:** #36

## Context

The sell advisor (F9) suggests players to let go. The issue names four reasons (falling value, long-term injury, low points per million, position surplus) and one hard rule: never go below a minimum squad size or leave a position empty. It builds on the player scoring model ([ADR 0006](0006-player-scoring-model.md)) and mirrors the buy advisor ([ADR 0007](0007-buy-recommendations.md)).

Two reasons can't be read straight from the data:

- **Injury length.** We know a player's current status and why he missed each match, not how long an injury will last.
- **Low points per million.** Season points grow every round, so any fixed threshold would drift over the season.

## Options considered

Ranking:

1. **Least loss to your XI (chosen).** Sell first whoever you'd miss least, the mirror of buying by gain.
2. **Most signals first.** Simple, but it ignores what the player gives your XI.

What the squad keeps:

1. **Formation + minimum 15 (chosen).** Each position keeps its formation starters (and at least one player), and the squad keeps at least 15.
2. **Formation + minimum 11.** Just one XI.
3. **One per position + minimum 11.** The literal criterion, but it could leave you unable to field a full XI.

Long-term injury:

1. **Injured, and missed his latest 2+ matches in a row through injury (chosen).** A fresh knock doesn't count.
2. **Any injured status.** Flags short knocks too.

## Decision

We will suggest sales in `advisor.domain.SellAdvisor`:

- **Signals** (a player needs at least one):
  - **Falling value:** down at least `fallingPercent` (3) % over the scoring trend.
  - **Long injury:** injured now, and missed at least `injuryMatches` (2) of his latest matches in a row through injury.
  - **Low value for money:** season points per million under `lowValueShare` (0.5) of the squad's median. Being relative, it keeps working as points accumulate.
  - **Surplus:** deeper in his position than the formation's starters plus `backups` (1).
- **XI loss:** for a starter, his expected points minus the best player outside the XI in his position, who would start instead. For anyone else, 0. A starter nobody could replace is never suggested, because selling him would always leave the formation short.
- **Order:** lowest XI loss, then more signals, then steepest value fall, then lowest player id.
- **Squad rules:** taken in that order, a sale is suggested only if, together with the ones already suggested, the squad keeps at least `minSquadSize` (15) players and each position keeps `max(formation starters, 1)`. The list also stays consistent about covers: a starter is skipped if the player named to start instead is already sold or covering another suggested sale, and a player named as a cover isn't sold. Players the sync never stored count toward the squad size. The first `limit` (10) are returned.
- **Each result** carries the player's full score, his signals with their numbers, who would cover him and the XI loss. It also carries an English sentence built from those numbers in a fixed locale.
- **Settings** live under `biwenger.advisor.sell.*` and are checked at startup. `GET /api/advisor/sells` computes the list on each request.

The reference formation moves out of the buy settings into its own `ReferenceFormation`, set from `biwenger.advisor.formation.*`, so both advisors reason about the same XI. This replaces ADR 0007's `biwenger.advisor.buy.starters.*` settings; nothing else in that decision changes.

## Consequences

- Each XI loss is measured against today's squad. Because no two suggested sales share a cover, and no cover is sold, the losses of the whole list add up.
- Keeping covers consistent can hold back a second good sale in the same line. Run the advisor again after selling to see it.
- The sale price shown is the player's value. Selling to a manager can fetch more; offers aren't considered.
- Until #97 stores fixtures, expected points ignore the next venue (ADR 0006).
