import { QueryClient, QueryClientProvider } from '@tanstack/react-query'
import { render } from '@testing-library/react'
import userEvent from '@testing-library/user-event'
import type { ReactElement } from 'react'
import { RouterProvider, createMemoryRouter } from 'react-router'
import { routes } from '@/app/routes'

/** A fresh client per test: no shared cache, and no retries to wait for. */
function testQueryClient() {
  return new QueryClient({
    defaultOptions: { queries: { retry: false }, mutations: { retry: false } },
  })
}

/** Renders one component with what data-fetching components need. */
export function renderWithProviders(ui: ReactElement) {
  const user = userEvent.setup()
  render(
    <QueryClientProvider client={testQueryClient()}>{ui}</QueryClientProvider>,
  )
  return { user }
}

/** Renders the whole app at {@code path}, routes included. */
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
