---
name: frontend-feature
description: How to add a page or feature to the React + TypeScript + Vite frontend in frontend/ - API function, TanStack Query hook, components, route and Vitest tests. Use when writing or changing code under frontend/.
---

# Add a frontend feature

Read `frontend/CLAUDE.md` first. It holds the conventions: folder structure, components, data fetching, TypeScript, styling and tests. This skill is the order of work. When the two disagree, `frontend/CLAUDE.md` wins.

## Order of work

The feature lives in `src/features/<feature>/`.

1. **Types** (`types.ts`). Types for the backend's JSON, matching its response DTOs exactly. Check them against the backend's DTO records or a real response.
2. **API functions** (`api.ts`). Plain async functions that call `/api/<feature>` and return typed data, throwing on a non-2xx response.
3. **Hooks** (`hooks.ts`). A query-key object (`<feature>Keys`) and `use<Thing>()` hooks around `useQuery` or `useMutation`. Mutations invalidate what they change.
4. **Components** (`<Component>.tsx` + `<Component>.test.tsx`). shadcn/ui and Tailwind tokens. Handle loading, error and empty states. Tests mock `api.ts` and query by role and name. Add missing shadcn components with `npx shadcn add <name>`.
5. **Page and route.** The page in `src/pages/` stays thin: `PageHeader` plus the feature's components. Register it in `app/routes.tsx`, and in `app/navigation.ts` if it belongs in the menu.
6. **Check:** `npm run lint && npm run format:check && npm test && npm run build`. Then open it with `npm run dev` (backend running) and look at it once, at desktop and phone width.

Skip the steps a feature doesn't need, but keep the order.

## Before the PR

- Every new component and helper is tested, directly or through its parent. Make each new test fail once to see that it can.
- No `fetch` in components, no `any`, no raw colours, no new CSS files.
- Any button that writes to Biwenger (bid, sell, accept offer, save lineup) opens a confirmation dialog and goes through the backend's propose → confirm endpoints.
- Recommendations are shown, not computed, in the frontend.
- If the change adds a convention, update `frontend/CLAUDE.md` in the same PR.
