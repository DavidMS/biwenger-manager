import { screen, waitFor, within } from '@testing-library/react'
import { beforeEach, describe, expect, it, vi } from 'vitest'
import { ApiError } from '@/lib/api'
import { renderApp } from '@/test/render'
import { fetchPlayer } from './api'
import type { PlayerDetail } from './types'

vi.mock('@/features/player/api')

const ayoze: PlayerDetail = {
  id: 2001,
  name: 'Ayoze',
  teamId: 19,
  teamName: 'Villarreal',
  position: 'FORWARD',
  status: 'DOUBTFUL',
  value: 4_780_000,
  points: 40,
  prices: [
    { day: '2026-10-26', price: 4_900_000 },
    { day: '2026-10-27', price: 4_700_000 },
    { day: '2026-10-28', price: 4_780_000 },
  ],
  matches: [
    {
      matchId: 1,
      roundId: 4906,
      date: '2026-10-18T14:00:00Z',
      home: true,
      played: true,
      points: -2,
      minutes: 90,
    },
    {
      matchId: 2,
      roundId: 4907,
      date: '2026-10-25T16:00:00Z',
      home: false,
      played: false,
      reason: 'INJURED',
    },
  ],
}

function region(name: string) {
  return within(screen.getByRole('region', { name }))
}

describe('PlayerDetailView', () => {
  beforeEach(() => {
    vi.mocked(fetchPlayer).mockReset()
  })

  it('shows a heading while it loads', async () => {
    vi.mocked(fetchPlayer).mockReturnValue(new Promise(() => {}))

    renderApp('/players/2001')

    // The lazy page's fallback shows the same heading, so wait for the page itself.
    await waitFor(() => expect(fetchPlayer).toHaveBeenCalledWith(2001))
    expect(
      screen.getByRole('heading', { level: 1, name: 'Player' }),
    ).toBeInTheDocument()
    expect(screen.getByRole('status')).toHaveTextContent('Loading…')
  })

  it('says how to fix a player the sync has never stored', async () => {
    vi.mocked(fetchPlayer).mockRejectedValue(
      new ApiError(404, 'Player not found', 'No player 99 has been synced'),
    )

    renderApp('/players/99')

    expect(await screen.findByText('Player not found')).toBeInTheDocument()
    expect(
      screen.getByText(/run a sync on the Settings page/),
    ).toBeInTheDocument()
  })

  it('rejects a link that names no player, without asking the backend', async () => {
    renderApp('/players/abc')

    expect(
      await screen.findByRole('heading', {
        level: 1,
        name: 'Player not found',
      }),
    ).toBeInTheDocument()
    expect(fetchPlayer).not.toHaveBeenCalled()
  })

  it('shows status, value, points, the value history and every match', async () => {
    vi.mocked(fetchPlayer).mockResolvedValue(ayoze)

    renderApp('/players/2001')

    expect(
      await screen.findByRole('heading', { level: 1, name: 'Ayoze' }),
    ).toBeInTheDocument()
    expect(screen.getByText('Villarreal · Forward')).toBeInTheDocument()
    const status = region('Status')
    expect(status.getByText('Doubtful')).toBeInTheDocument()
    expect(status.getByText('€4,780,000')).toBeInTheDocument()
    expect(status.getByText('40')).toBeInTheDocument()

    const history = region('Value history')
    expect(
      history.getByText(
        'Value from €4,900,000 on 26 Oct 2026 to €4,780,000 on 28 Oct 2026; lowest €4,700,000, highest €4,900,000.',
      ),
    ).toBeInTheDocument()
    // The table view lists every day, latest first.
    expect(
      history
        .getAllByRole('row')
        .slice(1)
        .map((row) => within(row).getAllByRole('cell')[1].textContent),
    ).toEqual(['€4,780,000', '€4,700,000', '€4,900,000'])

    const matches = region('Points per match')
      .getAllByRole('row')
      .slice(1)
      .map((row) =>
        within(row)
          .getAllByRole('cell')
          .map((cell) => cell.textContent),
      )
    // Latest first; 16:00 UTC is 17:00 in Madrid.
    expect(matches).toEqual([
      ['25 Oct 2026, 17:00', 'Away', 'Injured'],
      ['18 Oct 2026, 16:00', 'Home', '-2', '90'],
    ])
  })

  it('says when there is no history and no match yet', async () => {
    vi.mocked(fetchPlayer).mockResolvedValue({
      ...ayoze,
      teamName: undefined,
      prices: [{ day: '2026-10-28', price: 4_780_000 }],
      matches: [],
    })

    renderApp('/players/2001')

    expect(
      await screen.findByText('Not enough value history yet.'),
    ).toBeInTheDocument()
    expect(screen.getByText('No matches yet.')).toBeInTheDocument()
    expect(screen.getByText('Forward')).toBeInTheDocument()
  })
})
