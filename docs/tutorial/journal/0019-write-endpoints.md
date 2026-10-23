# #19 — Spike: discover the Biwenger write endpoints

- **Issue:** [#19 [2.6] Spike: discover Biwenger write endpoints](https://github.com/DavidMS/biwenger-manager/issues/19)
- **Phase:** 2 — Biwenger API client (read-only)
- **Date:** 2026-10-23

## Prompt

> Merged, go ahead with #19

## The problem

Every capture is a **real action on the human's account:** a bid, a listing, a lineup change. Claude must never send one (golden rule 2), and the browser's recording of it (a HAR file) holds the session token, so Claude must not read it either.

Claude asked two questions. The human chose:

- **A HAR export plus a script** they run, over copying each request from DevTools by hand.
- **Three actions:** bid then cancel, list a player then withdraw, re-save the lineup. Accepting or rejecting an offer was left out: accepting sells the player.

## What Claude did

1. Wrote `scripts/biwenger_har_extract.py`, reusing the anonymiser from #14:
   - It keeps only non-`GET` calls to the API and skips the login, whose body holds the password.
   - It removes `Authorization` and cookies, and keeps only a short allowlist of other headers.
   - It anonymises using the account call in the HAR, leak-checks every file, and prints no bodies.
   - Tests use a made-up HAR, and one was broken on purpose to see it fail.
2. Gave the human step-by-step DevTools instructions, then improved the script after each attempt (see Corrections).
3. Reviewed the six samples field by hand, before committing anything. **That found a leak** (see Corrections).
4. Wrote [`write-endpoints.md`](../../biwenger-api/write-endpoints.md):
   - the five actions, with what the server requires
   - what is still unknown
   - a table of the rules the server enforces, each marked known, inferred or unknown
5. Checked its own claims against the read samples. "The lineup lists players by position" was wrong: the slots follow the formation, and a player can fill a slot of one of his `altPositions`.
6. Commented on #42 and #43 with what changes for Phase 6.

## What the browser showed

- **A bid is an offer:** `POST /api/v2/offers` with `type: "purchase"` and `to: null` for a free player. It is cancelled with `DELETE /offers/{id}`, and its `until` is the market close.
- **Listing and withdrawing** are `POST /market` and `DELETE /market?player=`. pybiwenger had the second one right.
- **A lineup save sends the whole lineup:** 11 slots in formation order and 4 reserves, `null` for empty.
- **Several writes answer `204` with no body,** and the web app sends an `X-Version` header equal to the account's `version`.

## Corrections

- **A real Google profile photo URL nearly reached the repo.** Claude's field-by-field review of the samples found it in both lineup answers. The cause was a bug in the #14 anonymiser: it recognised managers by Python object identity, which only worked because the capture script anonymised the same objects it had scanned. The HAR extractor parses bodies a second time, so the check never matched, and the leak check didn't catch it because it only searches for values it already knows. **Fixed twice:**
  - managers are now recognised by their id
  - the leak check refuses any personal avatar host (Google, Facebook, Gravatar), with no list of known values needed

  The regression tests fail on the old code. The public read samples were scanned and are clean; nothing had been committed.
- **The first HAR was saved inside the repo,** in `scripts/`, with no `.har` extension, so `.gitignore` didn't cover it. Claude stopped, checked with `git status` that it was untracked and had never been committed, and asked for it to be moved. The script now refuses any HAR inside the repo, and Claude stages files by name until such a file is gone.
- **"No account answer," twice, for two different reasons:**
  - The second recording really had no account call: the page wasn't reloaded. The script now takes several HARs from one session, so one file's account call anonymises the other.
  - The first one had the answer, but **base64-encoded**, which the script read as plain JSON and treated as missing. It now decodes base64 and says "can't be read" rather than "missing".

  A new `--list` mode, which masks every number and prints no bodies, made this diagnosable without anyone looking at the data.
- **Claude committed with a failing test again,** as in #14: `| tail -1` hid the exit code. From then on, commits ran only inside `if tests pass; then … fi`.
- **The real offer id** was replaced with a fake one by hand. It identifies nobody, but it is real data the samples don't need.

Human review may add more corrections. Record them here before merging.

## Decisions made along the way

- **Claude never reads the HAR,** only the script's anonymised output and its masked `--list`.
- **No reviewer agent for this PR.** It contains no Java or React code. The safety check was the field-by-field review of the output, which is what caught the leak.
- **Leave accept/reject offer uncaptured** rather than ask for an irreversible action. #42 can implement the five known actions first.

## Lesson

An automatic leak check only finds what it knows to look for. Here it knew every name, id, token and cookie, and still missed a photo URL that no list contained. Two habits closed the gap: reading every field of the output before committing it, and adding a check that needs no list (any personal avatar host fails). When the data is someone's account, a human-style review of the output is part of the pipeline, not an optional extra.
