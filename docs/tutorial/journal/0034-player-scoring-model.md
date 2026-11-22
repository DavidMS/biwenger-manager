# #34 — Player scoring model

- **Issue:** [#34 [5.1] Player scoring model](https://github.com/DavidMS/biwenger-manager/issues/34)
- **Phase:** 5 — Advisor engine (deterministic)
- **Date:** 2026-11-22

## Prompt

> check the current status of the proyect and tell me whats next?

> yes go on

The first prompt only asked for a status report. Claude checked the open issues and PRs and recommended #34, because every other Phase 5 issue builds on it. It also listed the formula choices the issue left open. "yes go on" approved both the issue and the plan to propose a formula before writing any code.

## What Claude did

1. **Read the issue, plan §5 and the data the database already holds:** match reports with home/away and the reason for missing a match, daily prices, status and season points. It found one gap: the next fixture's venue isn't stored. Biwenger's catalog has `nextGames`, but the sync drops it.
2. **Asked four questions before writing code** (golden rule 1), each with a recommended default: how to handle home/away without fixtures, how to count a fit player left out, whether to have one composite score or separate components, and the default weights. The human took every recommendation.
3. **Built `advisor.domain.PlayerScorer`,** a pure function from `ScoringInput` and `ScoringWeights` to a `PlayerScore`. The score carries every intermediate number. The math uses `BigDecimal` with half-up rounding, and there's no clock or I/O. The weights bind from `biwenger.advisor.scoring.*`, and a value out of range stops the app at startup.
4. **Wrote the formula down in [ADR 0006](../../adr/0006-player-scoring-model.md).**
5. **Wrote tests whose expected values are worked out by hand in comments,** so the tests also show how the formula works. The weights binding is tested with an `ApplicationContextRunner`, not a full context.
6. **Ran `java-reviewer` and `./mvnw verify`.**

## Corrections

- **The first `verify` failed with 70 errors,** none of them in the new code: Docker Desktop wasn't running, so Testcontainers couldn't start PostgreSQL. Claude started Docker and ran it again.
- **`java-reviewer` found the score didn't contain all its own inputs.** When the venue applies, the factor depends on the venue average and the home/away game counts, and neither was reported. That broke the "every result includes the numbers that produced it" criterion. Callers pass `UNKNOWN` today, so no test noticed. Both are now in `PlayerScore`.
- **The venue factor was rounded twice** (the ratio first, then the factor), so it could be off by 0.0001 from the ADR's formula. Example: 7 vs 6 gives 1.0834 instead of 1.0833. It's now one division, with a test on that exact case.
- **"7 trend days" meant 8 prices.** The market page counts "N days, today included" and the scorer counted N days back. They now agree.
- **"Same rule as the market page" for points per million was only half true:** the market divides by the asking price, the scorer by the player's value. The comment and the ADR now say so.
- **Test gaps the reviewer listed:** negative expectations, a non-positive season average with a known venue, every missed reason, an oldest price of zero, an exact half-up case, the order test covering the venue path, and duplicate price days (now rejected).

Human review may add more corrections. Record them here before merging.

## Decisions made along the way

- **The venue is an input, not synced here.** Callers pass `Venue.UNKNOWN` until a follow-up issue stores `nextGames`. That keeps this PR to the pure model the issue asks for.
- **Each step uses the rounded numbers it reports,** so the UI can show a score that adds up by hand. The cost is a little precision.
- **The scoring variables aren't in `docker-compose.yml` yet.** The defaults work, and adding ten variables to compose and `.env.example` is better done once the advisor page makes tuning them useful.

## Lesson

Run the reviewer even when the tests pass. Every test here was green, yet the model broke one of its own acceptance criteria, and only on a path no caller uses yet. A reviewer reading the code against the criteria catches what tests written by the same author miss: you test the cases you thought of.
