# #35 — Buy recommendations

- **Issue:** [#35 [5.2] Buy recommendations](https://github.com/DavidMS/biwenger-manager/issues/35)
- **Phase:** 5 — Advisor engine (deterministic)
- **Date:** 2026-11-22

## Prompt

> ok, the pr is merged. Create the issue for the next match venue and then go to the #35

Two tasks in one prompt. The first was a follow-up Claude had suggested at the end of #34. Claude filed it as #97 (sync each club's next match venue), with acceptance criteria and an *Out of scope* section, and put it in the board's Backlog before starting #35.

## What Claude did

1. **Read the issue and the services #35 could build on:** market (listings and profiles), squad (your ids, live), dashboard (balance, live) and `PlayerReader` (stored history).
2. **Asked three product questions before writing code**, each with a recommended default: what "budget" means, what makes a position "weak", and what ranks the list. The human took all three recommendations: cash balance only, gain over your weakest starter, and gain first with gain per million as the tie-breaker.
3. **Built a pure `BuyAdvisor`.** Your starters per position are your best N by expected points, with N from a reference formation in config. A candidate's gain is his expected points minus your weakest starter's, or minus 0 for an empty place. The candidates are filtered by balance, ownership and positive gain, then ranked, and each result gets an explanation built from its numbers in a fixed locale.
4. **Wrote `BuyAdvisorService`** to gather the data and score market and squad players with the #34 model, and added `GET /api/advisor/buys`, ready for the advisor page (#40).
5. **Recorded the design in [ADR 0007](../../adr/0007-buy-recommendations.md).**
6. **Ran `java-reviewer` and `./mvnw verify`.**

## Corrections

- **A domain record ended up inside the service.** Claude first nested `BuyRecommendations` in `BuyAdvisorService`, noticed it broke the "services return domain types" layering, and moved it to `advisor.domain` before running anything.
- **A test helper gave a doubtful player a multiplier of 1,** which made the explanation test read "× 1". The helper now matches the status.
- **`java-reviewer` found an off-by-one-day trend.** The service read prices from today, but the scorer counts back from the latest stored price. Before the morning sync that's yesterday's, so "7 days" covered 6. The service now reads one day more, and a test pins it.
- **Players the database doesn't know were silently dropped.** A missing squad player made his position look empty and inflated every candidate's gain there. The response now lists missing squad and market ids, as the market and squad endpoints do.
- **"Points per million" in the explanation was ambiguous** next to the ranking's gain per million of asking price. It now says "season points per million of value", and shows the gain per million too.
- **Reading the balance through `dashboard()`** could pull the 230 KB catalog just to get one number. `DashboardService` now has a `team()` method that shares the same one-minute cache.
- **Test gaps:** a player with no stored history, your own player listed on the market, a Biwenger failure, the 503 when no login is set, and `team()` not touching the schedule.

Human review may add more corrections. Record them here before merging.

## Decisions made along the way

- **The endpoint lives in this issue,** not in the advisor page issue (#40). That keeps #40 frontend only.
- **Zero-price listings are documented, not handled.** They would rank last among equal gains, but Biwenger doesn't list players at 0 €.
- **The formation is fixed in config for now.** The lineup optimizer (5.5) could later supply "your starters", so the buy and lineup advisors agree.

## Lesson

A filter that drops what it can't use should say so. The first version quietly skipped players missing from the database, and the result looked plausible: bigger gains in a line that only looked empty. When an algorithm's input is incomplete, return what was left out next to the answer, so the person reading the recommendation can judge it.
