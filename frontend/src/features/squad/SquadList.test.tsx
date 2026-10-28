import { screen, within } from '@testing-library/react'
import { beforeEach, describe, expect, it, vi } from 'vitest'
import { ApiError } from '@/lib/api'
import { renderApp } from '@/test/render'
import { fetchSquad } from './api'
import type { Squad, SquadPlayer } from './types'

vi.mock('@/features/squad/api')

const goalkeeper: SquadPlayer = {
  id: 2602,
  name: 'Keeper',
  slug: 'keeper',
  teamId: 1,
  position: 'GOALKEEPER',
  status: 'AVAILABLE',
  price: 9_000_000,
  trendChange: 250_000,
  trend: [
    { day: '2026-10-27', price: 8_750_000 },
    { day: '2026-10-28', price: 9_000_000 },
  ],
  points: 12,
  form: [
    { played: true, points: 6 },
    { played: false, reason: 'AVAILABLE' },
  ],
}

const forward: SquadPlayer = {
  id: 2001,
  name: 'Ayoze',
  slug: 'ayoze-perez',
  teamId: 19,
  position: 'FORWARD',
  status: 'INJURED',
  price: 4_780_000,
  trendChange: -120_000,
  trend: [
    { day: '2026-10-27', price: 4_900_000 },
    { day: '2026-10-28', price: 4_780_000 },
  ],
  points: 40,
  form: [
    { played: false, reason: 'INJURED' },
    { played: true, points: -2 },
  ],
}

const coach: SquadPlayer = {
  ...goalkeeper,
  id: 41411,
  name: 'Mister',
  position: 'COACH',
  status: 'DOUBTFUL',
  price: 1_000_000,
  trendChange: 0,
  trend: [],
  points: 20,
  form: [],
}

function squad(overrides: Partial<Squad> = {}): Squad {
  return {
    players: [forward, coach, goalkeeper],
    missingPlayerIds: [],
    ...overrides,
  }
}

function playerNames() {
  const table = within(screen.getByRole('region', { name: 'Your players' }))
  return table
    .getAllByRole('row')
    .slice(1)
    .map((row) => within(row).getAllByRole('cell')[0].textContent)
}

describe('SquadList', () => {
  beforeEach(() => {
    vi.mocked(fetchSquad).mockReset()
  })

  it('shows a loading state', () => {
    vi.mocked(fetchSquad).mockReturnValue(new Promise(() => {}))

    renderApp('/squad')

    expect(
      screen.getByRole('heading', { level: 1, name: 'Squad' }),
    ).toBeInTheDocument()
    expect(screen.getByRole('status')).toHaveTextContent('Loading…')
  })

  it('says when Biwenger is not connected', async () => {
    vi.mocked(fetchSquad).mockRejectedValue(
      new ApiError(503, 'Biwenger not configured', 'No login is set.'),
    )

    renderApp('/squad')

    expect(
      await screen.findByText('Not connected to Biwenger'),
    ).toBeInTheDocument()
    expect(
      screen.getByText('Check the Biwenger account on the Settings page.'),
    ).toBeInTheDocument()
  })

  it("shows the backend's problem for other errors", async () => {
    vi.mocked(fetchSquad).mockRejectedValue(
      new ApiError(502, 'Biwenger unavailable', 'HTTP 503 from Biwenger'),
    )

    renderApp('/squad')

    expect(await screen.findByText('Biwenger unavailable')).toBeInTheDocument()
  })

  it('says when there are no players', async () => {
    vi.mocked(fetchSquad).mockResolvedValue(squad({ players: [] }))

    renderApp('/squad')

    expect(await screen.findByText(/No players to show/)).toBeInTheDocument()
    expect(screen.queryByRole('table')).not.toBeInTheDocument()
  })

  it('shows each player with value, trend, status, points and form', async () => {
    vi.mocked(fetchSquad).mockResolvedValue(squad())

    renderApp('/squad')

    const row = (await screen.findByRole('cell', { name: 'Ayoze' })).closest(
      'tr',
    )!
    const cells = within(row)
    expect(cells.getByText('Forward')).toBeInTheDocument()
    expect(cells.getByText('Injured')).toBeInTheDocument()
    expect(cells.getByText('€4,780,000')).toBeInTheDocument()
    expect(
      cells.getByRole('img', { name: 'Value over 2 days' }),
    ).toBeInTheDocument()
    expect(cells.getByText('-€120,000')).toBeInTheDocument()
    expect(cells.getByText('40')).toBeInTheDocument()
    const form = cells.getByRole('list', { name: 'Recent form, latest first' })
    expect(
      within(form)
        .getAllByRole('listitem')
        .map((m) => m.textContent),
    ).toEqual(['Iinjured', '-2'])

    const coachRow = screen.getByRole('cell', { name: 'Mister' }).closest('tr')!
    expect(within(coachRow).getByText('Doubtful')).toBeInTheDocument()
    expect(within(coachRow).getByText('No history')).toBeInTheDocument()
    expect(within(coachRow).getByText('No matches')).toBeInTheDocument()
  })

  it('sorts by position first, and by another column when its header is clicked', async () => {
    vi.mocked(fetchSquad).mockResolvedValue(squad())

    const { user } = renderApp('/squad')

    await screen.findByRole('table')
    expect(playerNames()).toEqual(['Keeper', 'Ayoze', 'Mister'])

    await user.click(screen.getByRole('button', { name: 'Points' }))
    expect(playerNames()).toEqual(['Ayoze', 'Mister', 'Keeper'])
    expect(
      screen.getByRole('columnheader', { name: 'Points' }),
    ).toHaveAttribute('aria-sort', 'descending')

    await user.click(screen.getByRole('button', { name: 'Points' }))
    expect(playerNames()).toEqual(['Keeper', 'Mister', 'Ayoze'])

    await user.click(screen.getByRole('button', { name: 'Trend (14 days)' }))
    expect(playerNames()).toEqual(['Keeper', 'Mister', 'Ayoze'])

    await user.click(screen.getByRole('button', { name: 'Value' }))
    expect(playerNames()).toEqual(['Keeper', 'Ayoze', 'Mister'])
  })

  it('reads the sort from the URL', async () => {
    vi.mocked(fetchSquad).mockResolvedValue(squad())

    renderApp('/squad?sort=trend&order=asc')

    await screen.findByRole('table')
    expect(playerNames()).toEqual(['Ayoze', 'Mister', 'Keeper'])
    expect(
      screen.getByRole('columnheader', { name: 'Trend (14 days)' }),
    ).toHaveAttribute('aria-sort', 'ascending')
  })

  it('names the owned players it cannot show', async () => {
    vi.mocked(fetchSquad).mockResolvedValue(
      squad({ missingPlayerIds: [33691, 34181] }),
    )

    renderApp('/squad')

    expect(
      await screen.findByText(/player ids 33691, 34181/),
    ).toBeInTheDocument()
  })
})
