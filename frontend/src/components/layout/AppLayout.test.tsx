import { render, screen, within } from '@testing-library/react'
import userEvent from '@testing-library/user-event'
import { RouterProvider, createMemoryRouter } from 'react-router'
import { describe, expect, it } from 'vitest'
import { routes } from '@/app/routes'

function renderAt(path: string) {
  const router = createMemoryRouter(routes, { initialEntries: [path] })
  render(<RouterProvider router={router} />)
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

  it('navigates to a section when its link is clicked', async () => {
    renderAt('/')

    await userEvent.click(screen.getByRole('link', { name: 'Market' }))

    expect(
      screen.getByRole('heading', { level: 1, name: 'Market' }),
    ).toBeInTheDocument()
    expect(screen.getByRole('link', { name: 'Market' })).toHaveAttribute(
      'aria-current',
      'page',
    )
    expect(screen.getByRole('link', { name: 'Dashboard' })).not.toHaveAttribute(
      'aria-current',
    )
  })

  it('shows a not found page for unknown addresses', () => {
    renderAt('/nowhere')

    expect(
      screen.getByRole('heading', { level: 1, name: 'Page not found' }),
    ).toBeInTheDocument()
  })
})
