---
name: frontend-feature
description: How to add a page or feature to the React + TypeScript + Vite frontend in frontend/ - API function, TanStack Query hook, components, route and Vitest tests. Use when writing or changing code under frontend/.
---

# Add a frontend feature

> **Provisional.** The frontend doesn't exist yet. Phase 1 (#12) creates `frontend/CLAUDE.md` with the real conventions and refines this skill. When the two disagree, `frontend/CLAUDE.md` wins.

Read `frontend/CLAUDE.md` first if it exists. Stack: React, TypeScript (strict), Vite, Tailwind CSS, shadcn/ui, TanStack Query, Vitest + React Testing Library.

## Order of work

1. **Types.** TypeScript types for the backend's response DTOs in `src/api/types.ts` (or the feature's folder). Match the backend JSON exactly.
2. **API function.** A plain async function in `src/api/<feature>.ts` that calls the backend (`/api/<feature>`) and returns typed data. It throws on non-2xx responses. Components never call `fetch` directly.
3. **Query hook.** `use<Thing>()` in `src/features/<feature>/hooks.ts`, wrapping `useQuery` or `useMutation`. Keep query keys in one place per feature, e.g. `['market', filters]`. Mutations invalidate the queries they change.
4. **Components.** Small function components in `src/features/<feature>/`. Use shadcn/ui primitives and Tailwind classes, no custom CSS files. Every data view handles loading, error and empty states.
5. **Route.** Add the page to the router and the navigation.
6. **Tests**, then `npm test` and `npm run build`.

## Tests

- Vitest + React Testing Library. Query by role and accessible name (`getByRole('button', { name: /suggest buys/i })`), not by class or test id.
- Mock the API module, not `fetch` internals. Test loading, error, empty and populated states.
- Wrap components in a fresh `QueryClient` per test, with retries off.
- Pure helpers (formatting money, sorting) get plain unit tests.

## Rules

- All UI text is in English.
- Money: Biwenger values are integers in euros. Format them in one shared helper.
- Any button that triggers a Biwenger write (bid, sell, accept offer, save lineup) opens a confirmation dialog that shows the consequences first. It calls the backend's *propose* endpoint, then *confirm* only after the user agrees.
- Recommendations come from the backend. The frontend displays them and their explanations; it doesn't compute them.
- Accessible by default: labels on inputs, buttons with text or `aria-label`, keyboard reachable dialogs.
