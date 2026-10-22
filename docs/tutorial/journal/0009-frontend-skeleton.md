# #9 — Frontend skeleton

- **Issue:** [#9 [1.2] Frontend skeleton: Vite, React, TypeScript, Tailwind, shadcn/ui](https://github.com/DavidMS/biwenger-manager/issues/9)
- **Phase:** 1 — Project skeleton
- **Date:** 2026-10-22

## Prompt

> merged, go with #9

## What Claude did

1. Moved #8 to *Done* and #9 to *In progress*, branched, and ran the `implement-issue` skill.
2. Scaffolded with the **official `create-vite` react-ts template** instead of writing configs from memory. The template had moved on since the plan was written:
   - it ships `oxlint` instead of ESLint, which Claude swapped for ESLint because the issue asks for it
   - it pins TypeScript 6.0, not the new 7.0, which typescript-eslint doesn't support yet
3. Added the stack:
   - Tailwind 4 through its Vite plugin
   - shadcn/ui through its own CLI (`radix` base, `nova` preset)
   - TanStack Query, with one shared `QueryClient`
   - React Router, with a layout route, the five pages and a "not found" page
   - ESLint 10 + Prettier, with the Tailwind class-sorting plugin
   - Vitest 5 + Testing Library + jsdom
4. Wrote layout tests that query by role and accessible name: the nav links, the default page, the unknown-address page and, after review, navigating from every nav link. There are 8 tests in total. To check the tests can fail, Claude renamed a nav label, saw them fail, and put it back.
5. Checked the rest by running it:
   - **the format hook:** a badly formatted `.tsx` file was reformatted on save, Tailwind classes sorted
   - **`npm run dev`:** serves `/squad`
   - **the production build:** screenshots in headless Chrome show the layout styled, with the active link highlighted
6. Updated `CLAUDE.md` with the frontend commands and the Node requirement.

## Corrections

- **The first install was silently out of date.** On the machine's Node 23, npm installed ESLint 9 (flagged deprecated), Vitest 3 and jsdom 27, without an error. Their newer majors support Node 22, 24 and 26+ but not odd-numbered 23, which never gets long-term support. Claude noticed that the resolved versions didn't match `npm view`, traced it to the `engines` fields, then:
  - installed Node 24 LTS with nvm and kept the user's default Node unchanged
  - reinstalled everything on Node 24
  - added `.nvmrc`, an `engines` range and `engine-strict=true`, so npm on Node 23 now stops with `EBADENGINE` instead of quietly downgrading

  Tests, lint and build still run on Node 23. Only installing is blocked.
- **shadcn's CLI needed two tries** to run without prompts. The preset name is `nova`, not `radix-nova`.
- **A "mobile" screenshot was misleading.** Headless Chrome wouldn't render narrower than about 500px, so the 390px shot was a crop, not a real phone layout. The nav wraps (`flex-wrap`), but a real small-screen check is still to do.

Human review may add more corrections. Record them here before merging.

## Review

This was the first real run of the `react-reviewer` agent (#5). Its verdict was **approve with nits**: no blockers, five findings, all fixed.

- **minor: import cycle.** `routes.tsx` imported `AppLayout`, which imported `navItems` from `routes.tsx`. It worked by luck: using `navItems` at module level would crash, and the cycle can break Vite's Fast Refresh. `navItems` moved to `src/app/navigation.ts`.
- **minor: paths written twice.** Each path appeared in the nav list and in the routes, so they could drift apart and a nav link could silently lead to "not found". A new test clicks *every* nav link and checks that the right page opens and that it is the only one marked current. Changing `/squad` to `/squads` in the nav list now fails that test.
- **nit:** `strict` is TypeScript 6's default but wasn't written in `tsconfig`. It's now explicit, because readers of a tutorial repo shouldn't have to know the default.
- **nit:** the tests now use `userEvent.setup()`, the recommended pattern in user-event 14.
- **nit:** a `cn()` call that merged nothing was removed.

## Decisions made along the way

- **Follow the official scaffolder, then adjust to the issue.** It gives current versions and conventions. Where it disagrees with the issue (oxlint vs ESLint), the issue wins.
- **TypeScript 6.0, not 7.0,** until typescript-eslint supports 7.
- **Fail loudly on the wrong Node** (`engine-strict`) rather than letting npm pick older packages.
- **shadcn/ui on Radix**, its longest-standing base and the one with the most examples to learn from. The new `cn` dependency was checked: it is shadcn's own package, a replacement for `clsx` + `tailwind-merge`.
- **Navigation uses shadcn's `buttonVariants`** on React Router's `NavLink`. The active page gets `aria-current="page"` and the secondary style.

## Lesson

Read what the package manager actually installed, not what you asked for. npm resolved older majors without an error because of an `engines` mismatch, and the only sign was a version number that didn't match `npm view`. Make wrong environments fail loudly (`engine-strict` here, the JDK enforcer rule in #8) so the next person doesn't have to notice.
