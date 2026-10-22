# #16 — Typed models and client methods for the read endpoints

- **Issue:** [#16 [2.3] Typed models and client methods for read endpoints](https://github.com/DavidMS/biwenger-manager/issues/16)
- **Phase:** 2 — Biwenger API client (read-only)
- **Date:** 2026-10-22

## Prompt

> merged, continue with #16

## What Claude did

1. Moved #15 to *Done* and #16 to *In progress*, branched, and read the six endpoint pages from #14.
2. Measured the samples before designing anything: a script listed, for every object type, which fields are sometimes missing or `null`. That gave the list of optional fields, for example lineup `captain` (5 of 6), sale `extended` (6 of 19), player `statusInfo` (6 of 39). It also showed that `lineupMaxClubPlayers` is a number, where pybiwenger types it as a boolean.
3. Checked two library facts by running code instead of recalling them:
   - **Jackson 3 ignores unknown fields by default,** unlike Jackson 2.
   - **Jackson 3 rejects a `null` primitive.**
4. Wrote 28 records in `client.model` that mirror the JSON:
   - names as in the JSON, money in whole euros, times as Unix seconds
   - optional values as `@Nullable` (JSpecify), absent lists as empty
   - `Fitness` reads Biwenger's mixed list of points, status strings and `null`
5. Added one method per endpoint to `BiwengerClient`: `account()`, `league()`, `squad()`, `market()`, `catalog()`, `roundStandings()`, `board()`.
   - Each unwraps the `{status, data}` envelope.
   - The client installs **its own JSON mapper**, so "unknown fields are ignored" holds whatever mapper the app configures.
   - The `league()` name was taken by #15's method, which became `leagueContext()`. The context also gained the league's `scoreId`, which the catalog needs.
6. Wrote 16 endpoint tests against the samples, using real values: maximum bid 57,005,000, 12 of 19 sales are free players, the clause buy of player 17937, Pablo Marín's fitness `["sanctioned", -4, null, "doubt", "doubt"]`. Two of the tests were broken on purpose to see them fail:
   - With the client's own mapper removed, the test with a strict caller mapper fails.
   - With the `lineup` check removed, a standing without a lineup slips through as `null`.
7. Recorded the rules for client records in `backend/CLAUDE.md`, and added the client's methods to the `biwenger-api` skill and the API README.

## Review

`java-reviewer`: **changes requested.** Claude checked each claim against the code and the samples before applying it:

- **major: "primitive = required" only covered primitives.** A missing `lineup`, `settings` or `name` would have arrived as a silent `null`, the very problem the rule was meant to prevent. **Fixed:** required objects and strings now get `Objects.requireNonNull` in the compact constructor, which Jackson reports through the same "doesn't match docs/biwenger-api/" error. The rule in `backend/CLAUDE.md` now says "required unless `@Nullable`".
- **minor: `Map.copyOf` iterates in a different order on each JVM run.** Anything built by looping over the catalog would have broken golden rule 5 (deterministic results). **Fixed:** the maps are sorted by id, with a test.
- **minor: optional fields tested in only one state.** **Added** tests for captain and coach present, `pointsLastSeason` null, and `isExtended()` false.
- **minor, rejected:** "`altPositions` doesn't appear in `catalog.json`." It does, on 12 of 39 players. Claude kept the field and added a test for it.
- **nits, fixed:** `Lineup` now copies its lists before wrapping them, and `Fitness` accepts integers only.

## Corrections

- **Claude assumed Jackson 3 tolerates missing fields. It doesn't:** a missing primitive fails too. The #15 tests used hand-written, minimal account JSON, and they broke. Claude kept the strictness and made it the rule, and rewrote those tests to build their account variants from the real sample.
- **The new error message found the next bug on its own.** The market test failed with "doesn't match docs/biwenger-api/", and the reference chain in the cause pointed at `Sale.extended`. That field is only sent when it is true, so it became a `@Nullable Boolean` with an `isExtended()` accessor.
- **An edit script missed one record shape** (methods but no compact constructor) and stopped halfway. Claude reverted the folder and reran it with that case covered.

Human review may add more corrections. Record them here before merging.

## Decisions made along the way

- **Fail on a dropped field, ignore an added one.** An added field never matters to us. A dropped field we rely on could make the app show a balance of 0 and warn wrongly, so it must fail loudly, with a message that names the docs.
- **Records mirror Biwenger, not our domain.** Unix seconds, Biwenger's names and codes. Mapping to domain types happens in `app.biwenger` in Phase 3 (ADR 0003).
- **Only the fields a feature needs.** The catalog's upcoming games, difficulty ratings and the market's `offers` are left out until a feature asks for them.

## Lesson

Measure the data before you model it. A five-line script over the samples showed which fields are optional, and that one fact drove most of the design. Then let the reviewer test the rule you wrote, not only the code: "primitive = required" sounded complete until the reviewer asked what happens to a missing object.
