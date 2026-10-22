# #11 — CI with GitHub Actions

- **Issue:** [#11 [1.4] CI with GitHub Actions](https://github.com/DavidMS/biwenger-manager/issues/11)
- **Phase:** 1 — Project skeleton
- **Date:** 2026-10-22

## Prompt

> merged, continue with #11

## What Claude did

1. Moved #10 to *Done* and #11 to *In progress*, branched, and ran the `implement-issue` skill.
2. Looked up the current major versions of the actions (`checkout@v7`, `setup-java@v6`, `setup-node@v7`) and read the `setup-java` README before writing anything. v6 reads both the version and the vendor from `.sdkmanrc`, so CI uses the same file as local development.
3. Wrote two workflows, one per folder:
   - **`.github/workflows/backend.yml`:** JDK from `backend/.sdkmanrc`, Maven cache, then `./mvnw verify`, which runs the enforcer, the tests and the Spotless check.
   - **`.github/workflows/frontend.yml`:** Node from `frontend/.nvmrc`, npm cache, then `npm ci`, lint, format check, tests and the build (which includes the type check).
   - Both run on PRs and on pushes to `main`, only when their folder (or their own workflow file) changes. They have read-only permissions and a timeout, and a new push to a PR cancels the previous run.
4. Ran every step locally first, on JDK 25 and Node 24, then pushed and opened the PR as a draft so the checks would run on GitHub.
5. Added CI badges to the README and a line about the checks to the workflow in `CLAUDE.md`.

## Verification

A workflow can only be tested by running it on GitHub, so the checks happened there.

| Check | Result |
|---|---|
| PR #62 (both folders' workflows changed) | `backend` and `frontend` both pass |
| JDK used by CI | Temurin 25.0.4, from `backend/.sdkmanrc` (run log) |
| Node used by CI | 24.22.0, from `frontend/.nvmrc` (run log) |
| Throwaway PR #63, docs only | No checks run |
| Throwaway PR #64, one file under `frontend/` | Only `frontend` runs, and passes |

The two throwaway PRs targeted the #11 branch, so their diff contained only the probe file. Both were closed without merging and their branches deleted.

## Review

Both reviewer agents ran in parallel, one per workflow.

`react-reviewer` (frontend.yml): **approve with nits.**
- **minor:** path filters clash with required status checks. If `frontend` is ever made required in branch protection, a PR that doesn't touch `frontend/` never reports it and waits forever. Documented in a comment in both workflows. Making the checks required needs a different design (see *Decisions*).
- **nit:** `cancel-in-progress: true` also cancelled runs on `main`. Now it cancels only on PRs.
- **nit:** steps had no names. Now they do: Install, Lint, Format check, Test, Build.

`java-reviewer` (backend.yml): **changes requested,** with one blocker that turned out to be wrong.
- **"blocker": `setup-java` needs a `distribution` input.** The reviewer said this from memory and noted it hadn't checked the v6 docs. The first CI run had already passed, and its log says `Using distribution 'temurin' from backend/.sdkmanrc`. Rejected, and the comment in the workflow now explains where the vendor comes from.
- **nit: add `-ntp`.** Also rejected: the log shows `setup-java` v6 already adds it.
- Path filters and `cancel-in-progress`: same findings as above, fixed the same way.

## Corrections

- **A reviewer was confidently wrong.** The backend reviewer called a missing input a blocker, based on how older versions of the action worked. Running the workflow settled it in seconds. Review findings are claims to check, not orders; here the evidence was already in the run log.

Human review may add more corrections. Record them here before merging.

## Decisions made along the way

- **Two workflows with `paths` filters,** not one workflow with a path-filter action. It is GitHub's built-in mechanism and the simplest to read. The cost: a check that doesn't run can't be *required*. If branch protection is added later, run the workflows on every PR and skip the job inside instead (for example with `dorny/paths-filter`), so the check always reports.
- **CI reads the same version files as developers** (`.sdkmanrc`, `.nvmrc`), so changing a version is a one-file change.
- **Actions are pinned to major tags** (`@v7`), not commit SHAs. They are GitHub's own actions, and tags keep the files readable for a tutorial. Pinning by SHA, plus Dependabot to update them, would be a separate issue.

## Lesson

Infrastructure code is tested by running it where it runs. Two throwaway PRs answered "do the path filters work?" more reliably than any amount of reading, and the first real run answered the reviewer's blocker before it was raised.
