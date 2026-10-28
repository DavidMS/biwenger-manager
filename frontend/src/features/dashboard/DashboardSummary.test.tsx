import { act, screen, within } from '@testing-library/react'
import { afterEach, describe, expect, it, vi } from 'vitest'
import { ApiError } from '@/lib/api'
import { renderWithProviders } from '@/test/render'
import { fetchDashboard } from './api'
import { DashboardSummary } from './DashboardSummary'
import { TICK_MS } from './NextRoundCard'
import type { Dashboard } from './types'

vi.mock('./api')

const DASHBOARD: Dashboard = {
  balance: 51_420_000,
  balanceNegative: false,
  points: 220,
  position: 6,
  managers: 6,
  teamValue: 22_340_000,
  teamValueChange: -70_000,
  nextRound: { name: 'Jornada 10', startsAt: '2026-10-30T19:00:00Z' },
}

function teamCard() {
  return within(screen.getByRole('region', { name: 'Your team' }))
}

describe('DashboardSummary', () => {
  afterEach(() => {
    vi.useRealTimers()
  })

  it('shows that it is loading', () => {
    vi.mocked(fetchDashboard).mockReturnValue(new Promise(() => {}))

    renderWithProviders(<DashboardSummary />)

    expect(screen.getByRole('status')).toHaveTextContent('Loading…')
  })

  it('shows the balance, team value, position and points', async () => {
    vi.mocked(fetchDashboard).mockResolvedValue(DASHBOARD)

    renderWithProviders(<DashboardSummary />)

    await screen.findByRole('region', { name: 'Your team' })
    expect(teamCard().getByText('€51,420,000')).toBeInTheDocument()
    expect(teamCard().getByText('€22,340,000')).toBeInTheDocument()
    expect(teamCard().getByText('(-€70,000 today)')).toBeInTheDocument()
    expect(teamCard().getByText('6 of 6')).toBeInTheDocument()
    expect(teamCard().getByText('220')).toBeInTheDocument()
    expect(
      screen.getByRole('region', { name: 'Next round' }),
    ).toHaveTextContent('Jornada 10')
    expect(screen.queryByRole('alert')).not.toBeInTheDocument()
  })

  it('warns in red when the balance is negative', async () => {
    vi.mocked(fetchDashboard).mockResolvedValue({
      ...DASHBOARD,
      balance: -1_250_000,
      balanceNegative: true,
    })

    renderWithProviders(<DashboardSummary />)

    const alert = await screen.findByRole('alert')
    expect(alert).toHaveTextContent('Your balance is negative: -€1,250,000')
    expect(alert).toHaveTextContent('your team scores 0 points')
    // The deadline is the next round's start, in Madrid time.
    expect(alert).toHaveTextContent(
      'before Jornada 10 starts (30 Oct 2026, 20:00)',
    )
    expect(alert).toHaveClass('text-destructive')
    expect(teamCard().getByText('-€1,250,000')).toHaveClass('text-destructive')
  })

  it('still warns when no round is scheduled', async () => {
    vi.mocked(fetchDashboard).mockResolvedValue({
      ...DASHBOARD,
      balance: -1,
      balanceNegative: true,
      nextRound: undefined,
    })

    renderWithProviders(<DashboardSummary />)

    expect(await screen.findByRole('alert')).toHaveTextContent(
      'before the next round starts',
    )
  })

  it('says how to fix a missing connection', async () => {
    vi.mocked(fetchDashboard).mockRejectedValue(
      new ApiError(
        503,
        'Biwenger not connected',
        'Biwenger rejected the login (HTTP 401)',
      ),
    )

    renderWithProviders(<DashboardSummary />)

    expect(
      await screen.findByText('Not connected to Biwenger'),
    ).toBeInTheDocument()
    expect(
      screen.getByText('Biwenger rejected the login (HTTP 401)'),
    ).toBeInTheDocument()
    expect(screen.getByText(/Settings page/)).toBeInTheDocument()
  })

  it('shows any other failure with the backend title', async () => {
    vi.mocked(fetchDashboard).mockRejectedValue(
      new ApiError(
        502,
        'Biwenger unavailable',
        'Biwenger answered HTTP 503 to GET /api/v2/account',
      ),
    )

    renderWithProviders(<DashboardSummary />)

    expect(await screen.findByText('Biwenger unavailable')).toBeInTheDocument()
    expect(
      screen.getByText('Biwenger answered HTTP 503 to GET /api/v2/account'),
    ).toBeInTheDocument()
  })

  it('asks for the following round once the next one starts', async () => {
    vi.useFakeTimers({ shouldAdvanceTime: true })
    vi.setSystemTime(new Date('2026-10-30T18:59:55Z'))
    vi.mocked(fetchDashboard)
      .mockResolvedValueOnce(DASHBOARD)
      .mockResolvedValueOnce({
        ...DASHBOARD,
        nextRound: { name: 'Jornada 11', startsAt: '2026-11-06T20:00:00Z' },
      })

    renderWithProviders(<DashboardSummary />)
    const card = await screen.findByRole('region', { name: 'Next round' })
    expect(card).toHaveTextContent('Jornada 10')

    await act(() => vi.advanceTimersByTimeAsync(TICK_MS))

    expect(await within(card).findByText('Jornada 11')).toBeInTheDocument()
    expect(fetchDashboard).toHaveBeenCalledTimes(2)
  })
})
