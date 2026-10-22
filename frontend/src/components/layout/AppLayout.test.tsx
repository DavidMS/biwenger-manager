import { render, screen, within } from '@testing-library/react'
import userEvent from '@testing-library/user-event'
import { RouterProvider, createMemoryRouter } from 'react-router'
import { describe, expect, it } from 'vitest'
import { navItems } from '@/app/navigation'
import { routes } from '@/app/routes'

function renderAt(path: string) {
  const user = userEvent.setup()
  const router = createMemoryRouter(routes, { initialEntries: [path] })
  render(<RouterProvider router={router} />)
  return { user }
}

describe('AppLayout', () => {
  it('shows a link to every section in the main navigation', () => {
    renderAt('/')

    const nav = screen.getByRole('navigation', { name: 'Main' })
    const links = within(nav)
      .getAllByRole('link')
      .map((link) => link.textContent)
    expect(links).toEqual([
      'Dashboard',
      'Squad',
      'Market',
      'Standings',
      'Advisor',
    ])
  })

  it('opens the dashboard by default and marks it as the current page', () => {
    renderAt('/')

    expect(
      screen.getByRole('heading', { level: 1, name: 'Dashboard' }),
    ).toBeInTheDocument()
    expect(screen.getByRole('link', { name: 'Dashboard' })).toHaveAttribute(
      'aria-current',
      'page',
    )
  })

  it.each(navItems)(
    'opens the $label page from its navigation link',
    async ({ label }) => {
      // Start somewhere else so every link, including Dashboard, really navigates.
      const { user } = renderAt('/nowhere')

      await user.click(screen.getByRole('link', { name: label }))

      expect(
        screen.getByRole('heading', { level: 1, name: label }),
      ).toBeInTheDocument()
      const nav = screen.getByRole('navigation', { name: 'Main' })
      const current = within(nav)
        .getAllByRole('link')
        .filter((link) => link.getAttribute('aria-current') === 'page')
      expect(current.map((link) => link.textContent)).toEqual([label])
    },
  )

  it('shows a not found page for unknown addresses', () => {
    renderAt('/nowhere')

    expect(
      screen.getByRole('heading', { level: 1, name: 'Page not found' }),
    ).toBeInTheDocument()
  })
})
