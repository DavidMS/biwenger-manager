import { QueryClient, QueryClientProvider } from '@tanstack/react-query'
import { render } from '@testing-library/react'
import userEvent from '@testing-library/user-event'
import type { ReactElement } from 'react'
import { RouterProvider, createMemoryRouter } from 'react-router'
import { routes } from '@/app/routes'

type UserEventOptions = Parameters<typeof userEvent.setup>[0]

/** A fresh client per test: no shared cache, and no retries to wait for. */
function testQueryClient() {
  return new QueryClient({
    defaultOptions: { queries: { retry: false }, mutations: { retry: false } },
  })
}

/**
 * Renders one component with what data-fetching components need. With fake timers, pass
 * `{ advanceTimers: vi.advanceTimersByTime }` so user-event's own waits move with them.
 */
export function renderWithProviders(
  ui: ReactElement,
  userOptions?: UserEventOptions,
) {
  const user = userEvent.setup(userOptions)
  const queryClient = testQueryClient()
  render(<QueryClientProvider client={queryClient}>{ui}</QueryClientProvider>)
  return { user, queryClient }
}

/** Renders the whole app at `path`, routes included. */
export function renderApp(path: string) {
  const user = userEvent.setup()
  const router = createMemoryRouter(routes, { initialEntries: [path] })
  render(
    <QueryClientProvider client={testQueryClient()}>
      <RouterProvider router={router} />
    </QueryClientProvider>,
  )
  return { user }
}
