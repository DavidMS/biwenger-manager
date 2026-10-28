import { screen, within } from '@testing-library/react'
import { describe, expect, it, vi } from 'vitest'
import { navItems } from '@/app/navigation'
import { renderApp as renderAt } from '@/test/render'

// Pages that fetch data (Dashboard, Settings) get answers that never come: this test is about
// navigation.
vi.mock('@/features/dashboard/api', () => ({
  fetchDashboard: () => new Promise(() => {}),
}))
vi.mock('@/features/settings/api', () => ({
  fetchAccount: () => new Promise(() => {}),
  fetchSyncStatus: () => new Promise(() => {}),
  startSync: () => new Promise(() => {}),
}))

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
      'Settings',
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
