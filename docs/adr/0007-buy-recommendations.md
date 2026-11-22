# 0007. Buy recommendations rank by gain to your XI

- **Status:** Accepted
- **Date:** 2026-11-22
- **Issue:** #35

## Context

The buy advisor (F7) ranks market players. Issue #35 asks for three things: filter by the available budget, boost positions where your squad is weak, and explain each result from its numbers. The player scoring model ([ADR 0006](0006-player-scoring-model.md)) gives each player expected points, value trend and points per million, and leaves it to each advisor to combine them.

Three choices were open: what "budget" means, what makes a position "weak", and what ranks the list.

## Options considered

Budget:

1. **Cash balance (chosen).** Asking price ≤ current balance. It never suggests going negative, which scores 0 if it lasts until the round starts.
2. **Balance + allowed debt.** More options, but it assumes you'll sell in time, and it needs the league's debt rule.
3. **Balance + a margin from config.**

Weak position:

1. **Gain over your weakest starter (chosen).** Per position, your starters are your best N players by expected points, with N from a reference formation. A candidate's gain is his expected points minus your weakest starter's there, or minus 0 when you have fewer than N. A weak line gives big gains, so it rises with no separate boost factor.
2. **Shortage of fit players.** A multiplier for positions with fewer fit players than the formation needs. It ignores how good the starters are, and adds a weight to tune.

Ranking:

1. **Gain, then gain per million (chosen).** Highest gain first; ties by gain per million of asking price, then player id.
2. **Gain per million first.** Value for money, but it puts cheap players with small gains above a star who would transform the XI.

## Decision

We will rank market players by **gain to your XI** (`advisor.domain.BuyAdvisor`):

- A player is a candidate if you don't own him (your own sales are skipped), his asking price is at most your balance, his position has starters in the reference formation, and his gain is positive.
- The reference formation defaults to 1 goalkeeper, 4 defenders, 4 midfielders, 2 forwards and no coach.
- Order: gain descending, then gain per million of asking price descending, then player id ascending. The first `limit` (10) are returned.
- Each result carries the player's full score, the starter he would replace (or an empty slot), the gain, the gain per million and the balance left. It also carries an English sentence built from those numbers in a fixed locale, for example: "Expected 5.00 points (form 7.33, season average 6.00). Would replace Pedri (2.10) in your midfield: +2.90 (0.97 per million of asking price). Value up 5.77% in 7 days. 6.00 season points per million of value. Leaves 1,200,000 €."
- Settings live under `biwenger.advisor.buy.*` (`starters.goalkeeper` … `starters.coach`, `limit`), checked at startup.
- `GET /api/advisor/buys` computes the list on each request and stores nothing. It also lists the squad and market players it couldn't score because the daily sync never stored them, since a squad player left out can make a gain look bigger than it is. Your balance comes from the dashboard's team status only, so the advisor never reads the 230 KB catalog.

## Consequences

- The reference formation is fixed in config, while the lineup optimizer (5.5) will choose the best formation. When it exists, "your starters" could come from the optimizer instead, so the two advisors agree. Until then the gain can differ a little from what the optimizer would say.
- Asking price is the listing's minimum. Real bids are usually higher, so the balance left is a best case. The bid advisor (5.4) suggests the bid itself.
- Value trend and points per million show up in the explanation but don't affect the order. Someone buying to resell would want them to; that would be a separate ranking.
- Until #97 stores fixtures, expected points ignore the next venue (ADR 0006).
- Multi-position players are judged in their main position only.
