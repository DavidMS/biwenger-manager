import { screen } from '@testing-library/react'
import { describe, expect, it, vi } from 'vitest'
import { renderWithProviders } from '@/test/render'
import { DashboardPage } from './DashboardPage'

vi.mock('@/features/dashboard/api', () => ({
  fetchDashboard: () => new Promise(() => {}),
}))

describe('DashboardPage', () => {
  it('shows its heading and loads the dashboard', () => {
    renderWithProviders(<DashboardPage />)

    expect(
      screen.getByRole('heading', { level: 1, name: 'Dashboard' }),
    ).toBeInTheDocument()
    expect(screen.getByRole('status')).toHaveTextContent('Loading…')
  })
})
