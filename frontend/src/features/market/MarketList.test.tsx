import { screen, within } from '@testing-library/react'
import { beforeEach, describe, expect, it, vi } from 'vitest'
import { ApiError } from '@/lib/api'
import { renderApp } from '@/test/render'
import { fetchMarket } from './api'
import type { Market, MarketPlayer } from './types'

vi.mock('@/features/market/api')

const free: MarketPlayer = {
  id: 16738,
  name: 'Striker',
  teamId: 3,
  position: 'FORWARD',
  status: 'AVAILABLE',
  price: 5_640_000,
  value: 5_630_000,
  trendChange: 120_000,
  trend: [
    { day: '2026-10-27', price: 5_510_000 },
    { day: '2026-10-28', price: 5_630_000 },
  ],
  points: 40,
  pointsPerMillion: 7.09,
  closesAt: '2026-10-29T05:00:00Z',
  seller: { free: true },
}

const fromManager: MarketPlayer = {
  ...free,
  id: 2602,
  name: 'Keeper',
  teamId: 1,
  position: 'GOALKEEPER',
  status: 'INJURED',
  price: 6_930_000,
  trendChange: -50_000,
  points: 12,
  pointsPerMillion: 1.73,
  closesAt: '2026-10-30T10:15:00Z',
  seller: { free: false, managerId: 2_000_003, managerName: 'Manager 3' },
}

const cheap: MarketPlayer = {
  ...free,
  id: 30487,
  name: 'Bargain',
  teamId: 3,
  position: 'MIDFIELDER',
  status: 'DOUBTFUL',
  price: 800_000,
  trendChange: 0,
  trend: [],
  points: 12,
  pointsPerMillion: 15,
}

function market(overrides: Partial<Market> = {}): Market {
  return {
    players: [free, fromManager, cheap],
    teams: [
      { id: 1, name: 'Athletic' },
      { id: 3, name: 'Barcelona' },
    ],
    missingPlayerIds: [],
    ...overrides,
  }
}

function shownNames() {
  return within(screen.getByRole('table'))
    .getAllByRole('row')
    .slice(1)
    .map((row) => within(row).getAllByRole('cell')[0].textContent)
}

describe('MarketList', () => {
  beforeEach(() => {
    vi.mocked(fetchMarket).mockReset()
  })

  it('shows a loading state', () => {
    vi.mocked(fetchMarket).mockReturnValue(new Promise(() => {}))

    renderApp('/market')

    expect(
      screen.getByRole('heading', { level: 1, name: 'Market' }),
    ).toBeInTheDocument()
    expect(screen.getByRole('status')).toHaveTextContent('Loading…')
  })

  it('says when Biwenger is not connected', async () => {
    vi.mocked(fetchMarket).mockRejectedValue(
      new ApiError(503, 'Biwenger not configured', 'No login is set.'),
    )

    renderApp('/market')

    expect(
      await screen.findByText('Not connected to Biwenger'),
    ).toBeInTheDocument()
  })

  it("shows the backend's problem for other errors", async () => {
    vi.mocked(fetchMarket).mockRejectedValue(
      new ApiError(502, 'Biwenger unavailable', 'HTTP 503 from Biwenger'),
    )

    renderApp('/market')

    expect(await screen.findByText('Biwenger unavailable')).toBeInTheDocument()
  })

  it('says when nobody is on sale', async () => {
    vi.mocked(fetchMarket).mockResolvedValue(market({ players: [] }))

    renderApp('/market')

    expect(
      await screen.findByText('Nobody is on sale right now.'),
    ).toBeInTheDocument()
    expect(
      screen.queryByRole('form', { name: 'Market filters' }),
    ).not.toBeInTheDocument()
  })

  it('shows each player with team, price, trend, points, value for money, seller and closing time', async () => {
    vi.mocked(fetchMarket).mockResolvedValue(market())

    renderApp('/market')

    const row = within(await screen.findByRole('row', { name: /^Keeper/ }))
    expect(row.getByText('Athletic')).toBeInTheDocument()
    expect(row.getByText('Goalkeeper')).toBeInTheDocument()
    expect(row.getByText('Injured')).toBeInTheDocument()
    expect(row.getByText('€6,930,000')).toBeInTheDocument()
    expect(row.getByText('-€50,000')).toBeInTheDocument()
    expect(row.getByText('1.73')).toBeInTheDocument()
    expect(row.getByText('Manager 3')).toBeInTheDocument()
    // 10:15 UTC is 11:15 in Madrid.
    expect(row.getByText('30 Oct 2026, 11:15')).toBeInTheDocument()

    const freeRow = within(screen.getByRole('row', { name: /^Striker/ }))
    expect(freeRow.getByText('Free agent')).toBeInTheDocument()
    expect(
      freeRow.getByRole('img', { name: 'Value from €5,510,000 to €5,630,000' }),
    ).toBeInTheDocument()
  })

  it('filters by position, team, status and price, and clears the filters', async () => {
    vi.mocked(fetchMarket).mockResolvedValue(market())

    const { user } = renderApp('/market')

    await screen.findByRole('table')
    expect(shownNames()).toEqual(['Keeper', 'Striker', 'Bargain'])

    await user.selectOptions(
      screen.getByRole('combobox', { name: 'Team' }),
      'Barcelona',
    )
    expect(shownNames()).toEqual(['Striker', 'Bargain'])

    await user.selectOptions(
      screen.getByRole('combobox', { name: 'Position' }),
      'Midfielder',
    )
    expect(shownNames()).toEqual(['Bargain'])

    await user.selectOptions(
      screen.getByRole('combobox', { name: 'Status' }),
      'Injured',
    )
    expect(
      screen.getByText('No player matches these filters.'),
    ).toBeInTheDocument()

    await user.click(screen.getByRole('button', { name: 'Clear filters' }))
    expect(shownNames()).toEqual(['Keeper', 'Striker', 'Bargain'])

    await user.type(
      screen.getByRole('spinbutton', { name: 'Min price (M€)' }),
      '1',
    )
    await user.type(
      screen.getByRole('spinbutton', { name: 'Max price (M€)' }),
      '6',
    )
    expect(shownNames()).toEqual(['Striker'])
    expect(screen.getByText(/^1 of 3 players/)).toBeInTheDocument()
  })

  it('sorts by points per million, points, trend and price', async () => {
    vi.mocked(fetchMarket).mockResolvedValue(market())

    const { user } = renderApp('/market')

    await screen.findByRole('table')
    await user.click(screen.getByRole('button', { name: 'Points per M€' }))
    expect(shownNames()).toEqual(['Bargain', 'Striker', 'Keeper'])
    expect(
      screen.getByRole('columnheader', { name: 'Points per M€' }),
    ).toHaveAttribute('aria-sort', 'descending')

    // Equal points: the lower player id first.
    await user.click(screen.getByRole('button', { name: 'Points' }))
    expect(shownNames()).toEqual(['Striker', 'Keeper', 'Bargain'])

    await user.click(screen.getByRole('button', { name: 'Trend (14 days)' }))
    await user.click(screen.getByRole('button', { name: 'Trend (14 days)' }))
    expect(shownNames()).toEqual(['Keeper', 'Bargain', 'Striker'])

    await user.click(screen.getByRole('button', { name: 'Price' }))
    expect(shownNames()).toEqual(['Keeper', 'Striker', 'Bargain'])
  })

  it('reads the filters and the sort from the URL', async () => {
    vi.mocked(fetchMarket).mockResolvedValue(market())

    renderApp('/market?team=3&sort=price&order=asc')

    await screen.findByRole('table')
    expect(shownNames()).toEqual(['Bargain', 'Striker'])
    expect(screen.getByRole('combobox', { name: 'Team' })).toHaveValue('3')
  })

  it('ignores a team in the URL that is not a club', async () => {
    vi.mocked(fetchMarket).mockResolvedValue(market())

    renderApp('/market?team=999')

    await screen.findByRole('table')
    expect(shownNames()).toEqual(['Keeper', 'Striker', 'Bargain'])
    expect(screen.getByRole('combobox', { name: 'Team' })).toHaveValue('')
  })

  it('says when the max price is below the min price', async () => {
    vi.mocked(fetchMarket).mockResolvedValue(market())

    renderApp('/market?minPrice=6&maxPrice=2')

    const max = await screen.findByRole('spinbutton', {
      name: 'Max price (M€)',
    })
    expect(max).toHaveAttribute('aria-invalid', 'true')
    expect(max).toHaveAccessibleDescription(
      'The max price is below the min price.',
    )
  })

  it('names the players on sale it cannot show', async () => {
    vi.mocked(fetchMarket).mockResolvedValue(
      market({ missingPlayerIds: [99999] }),
    )

    renderApp('/market')

    expect(await screen.findByText(/player\s+ids 99999/)).toBeInTheDocument()
  })
})
