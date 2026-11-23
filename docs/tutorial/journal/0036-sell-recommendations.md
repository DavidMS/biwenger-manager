# #36 — Sell recommendations

- **Issue:** [#36 [5.3] Sell recommendations](https://github.com/DavidMS/biwenger-manager/issues/36)
- **Phase:** 5 — Advisor engine (deterministic)
- **Date:** 2026-11-22 to 2026-11-23

## Prompt

> ok, merged. go with #36

The same message arrived twice while the work was under way. Claude treated the second as a repeat, not a restart. A day later the session resumed with:

> can you resume what you were doing?

Claude picked up from the state on disk (branch, files, the reviewer's findings) and carried on without redoing anything.

## What Claude did

1. **Read the issue and the data available.** Two of the four signals need a definition. Injury length isn't known (only the current status and why each match was missed). And any fixed points-per-million threshold drifts, because season points keep growing.
2. **Asked four questions, each with a recommended default:** ranking, what the squad must keep, when an injury is long-term, and the thresholds. The human took all four: least loss to your XI first; keep the formation's starters and at least 15 players; injured and out for 2+ matches in a row; down 3 %, under half the squad's median, beyond starters plus one backup.
3. **Refactored first, in its own commit.** The reference formation moved out of the buy settings into a shared `ReferenceFormation` (`biwenger.advisor.formation.*`), so buy and sell reason about the same XI. ADR 0007 got an "Amended by" line, not a rewrite.
4. **Built a pure `SellAdvisor`:**
   - signals as a sealed type, each carrying its numbers
   - who would start instead and the loss to your XI
   - a ranking
   - a greedy pass that applies the squad rules to all suggested sales together

   Then `SellAdvisorService` and `GET /api/advisor/sells`. Scoring from stored history moved into one helper shared by both services.
5. **Recorded the design in [ADR 0008](../../adr/0008-sell-recommendations.md).**
6. **Ran `java-reviewer` and `./mvnw verify`.**

## Corrections

- **The first draft returned `null`** for a player with no signal. The convention is `Optional`, and Claude changed it before running anything.
- **A whole case turned out to be impossible.** "Nobody can replace this starter" can never get through the keep-the-formation rule, because his line has no one to spare. While planning the tests, Claude removed the `NoCover` variant instead of writing code and tests for a state that can't occur.
- **One test expectation was wrong, not the code.** An injured player has 0 expected points, so he also sinks to the bottom of his line and counts as surplus. The test expected one signal; the advisor correctly gave two.
- **`java-reviewer` found the list could contradict itself.** Two suggested starters could name the same teammate as "would start instead", and a player named as a cover could be sold too. Each line read fine on its own. The selection step now keeps covers consistent, with tests for both cases.
- **"missed his last 1 matches"** if someone sets the injury threshold to 1. Fixed.
- **Test gaps:** covered starters ranked by loss, two coaches with one surplus, and the singular wording.

Human review may add more corrections. Record them here before merging.

## Decisions made along the way

- **Refactor, then feature, in separate commits.** The reviewer could confirm the buy advisor's behaviour didn't change before reading the new code.
- **Points-per-million threshold relative to the squad median.** Any absolute number would be wrong by midseason.
- **Consistency over coverage.** When two good sales compete for the same cover, the list offers one. Running the advisor again after selling shows the next.

## Lesson

Check recommendations as a set, not one by one. Each suggestion was correct alone, but together two of them told the user the same player would replace two different starters. When an algorithm returns a list the user may act on all at once, test the list's promises together, not only each item's.
