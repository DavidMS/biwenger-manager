# #21 — Domain model and API-to-domain mapping

- **Issue:** [#21 [3.1] Domain model and API-to-domain mapping](https://github.com/DavidMS/biwenger-manager/issues/21)
- **Phase:** 3 — Domain, persistence & history
- **Date:** 2026-10-23

## Prompt

> merged, move phase 3 to ready and go with #21

## What Claude did

1. Moved #20 to *Done*, #21 to *In progress* and #22–#26 to *Ready*, then branched.
2. Wrote the domain in `app`, by feature, as `backend/CLAUDE.md` asks:
   - `player.domain`: `Player`, `Team`, `Position`, `PlayerStatus`, and `RoundForm` for one round of recent form
   - `league.domain`: `League`, `Manager`, `Round`, `RoundStatus`
   - `market.domain`: `MarketListing`, `Seller`
   - `common`: `Money`
3. **`Money` is a type of its own,** a record holding `long` euros with exact arithmetic. A price and an id are both `long`, so without it `new Player(price, id)` would compile and quietly be wrong.
4. **Variants are sealed types, not nulls.**
   - A round of form is `Played(points)`, `Absent(reason)` or `NotPicked`.
   - A seller is `Game` or `ByManager(manager)`.
   - Biwenger's mixed `fitness` list (`["sanctioned", -4, null, …]`) becomes something a `switch` can check for completeness.
5. Wrote `BiwengerMapper` in `app.biwenger`, the only package that sees the client's records. It translates Biwenger's codes (position `1`–`5`, status strings) and turns Unix seconds into `Instant` and euros into `Money`. Lists come out ordered by id, so the output is the same on every run.
6. Tested the mapper against the anonymised samples: Ayoze's form `6, 2, 9, 6, 0`, Pablo Marín's absences, an injury note, a defender allowed in midfield, a postponed round, and the market's 12 free players. Breaking the `"doubt"` translation on purpose made the form test fail.

## Review

`java-reviewer`: **approve with nits.** It confirmed the null policy and the package split, and that snapshot dates belong in #22–#23, not in these types.

- **An unknown round status would have broken the whole catalog,** while an unknown player status maps to `UNKNOWN`. **Fixed:** both map to `UNKNOWN` now. Only an unknown *position* still fails, because a player in the wrong lineup slot is unsafe.
- **The advisor will need percentages,** such as "price + 10 %" or "25 % of team value", and without them someone would reach for `double`. **Added** `times`, `percent(pct, RoundingMode)` and `sum`, all exact. The test reproduces the market's maximum bid: 51,420,000 + 25 % of 22,340,000 = 57,005,000.
- **The listing drops the seller's ownership data** (clause, price paid, clause lock). **Deferred to #22:** whether `market_snapshot` stores it is a schema decision.
- **Smaller fixes:**
  - more tests: `NotPicked` from `"ok"`, the invariants, overflow
  - the test mapper states the client's JSON setting explicitly
  - one visibility rule in the mapper

## Corrections

- **A determinism slip, caught before the first build.** `Player.otherPositions` used `Set.copyOf`, which iterates in a different order between JVM runs. It's the same trap the reviewer found in the catalog maps in #16. It is now an `EnumSet`, which always iterates in declaration order, with a test.
- **A stray unused helper** was left in the mapper and removed before the first build.

Human review may add more corrections. Record them here before merging.

## Decisions made along the way

- **Only the fields features need:** squad, market and standings. Last season's points, upcoming games and the clause data wait until a feature asks for them.
- **Fail only where it's unsafe.** An unknown position fails. Unknown statuses become `UNKNOWN`, which the advisor will treat as "not available".
- **A static, pure mapper.** It needs nothing from Spring. A component that calls the client and uses it will come with the sync job (#23).

## Lesson

Give values their own types when they share a primitive. Prices, ids and timestamps were all `long` in the client, faithfully, because that is the JSON. In the domain, `Money`, `Instant` and sealed variants make the wrong call fail to compile instead of failing in a recommendation.
