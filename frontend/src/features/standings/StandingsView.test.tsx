import { screen, within } from '@testing-library/react'
import { beforeEach, describe, expect, it, vi } from 'vitest'
import { ApiError } from '@/lib/api'
import { renderApp } from '@/test/render'
import { fetchRivalSquad, fetchStandings } from './api'
import type { RivalSquad, Standings } from './types'

vi.mock('@/features/standings/api')

const standings: Standings = {
  standings: [
    {
      managerId: 2000004,
      name: 'Manager 4',
      position: 1,
      points: 515,
      teamValue: 64_530_000,
      teamValueChange: -70_000,
      you: false,
    },
    {
      managerId: 2000001,
      name: 'Manager 1',
      position: 2,
      points: 220,
      teamValue: 22_340_000,
      teamValueChange: 120_000,
      you: true,
    },
  ],
}

const leadersSquad: RivalSquad = {
  managerId: 2000004,
  name: 'Manager 4',
  position: 1,
  teamValue: 64_530_000,
  lineupSavedAt: '2026-10-26T10:00:00Z',
  players: [
    {
      id: 30487,
      name: 'Striker',
      teamId: 3,
      position: 'FORWARD',
      status: 'SUSPENDED',
      value: 9_000_000,
      points: 61,
    },
    {
      id: 41411,
      name: 'Keeper',
      teamId: 1,
      position: 'GOALKEEPER',
      status: 'AVAILABLE',
      value: 2_000_000,
      points: 30,
    },
  ],
  missingPlayerIds: [33691],
}

function table() {
  return within(screen.getByRole('region', { name: 'League table' }))
}

describe('StandingsView', () => {
  beforeEach(() => {
    vi.mocked(fetchStandings).mockReset()
    vi.mocked(fetchRivalSquad).mockReset()
  })

  it('shows a loading state', () => {
    vi.mocked(fetchStandings).mockReturnValue(new Promise(() => {}))

    renderApp('/standings')

    expect(
      screen.getByRole('heading', { level: 1, name: 'Standings' }),
    ).toBeInTheDocument()
    expect(screen.getByRole('status')).toHaveTextContent('Loading…')
  })

  it('says when Biwenger is not connected', async () => {
    vi.mocked(fetchStandings).mockRejectedValue(
      new ApiError(503, 'Biwenger not configured', 'No login is set.'),
    )

    renderApp('/standings')

    expect(
      await screen.findByText('Not connected to Biwenger'),
    ).toBeInTheDocument()
  })

  it('says when there are no standings', async () => {
    vi.mocked(fetchStandings).mockResolvedValue({ standings: [] })

    renderApp('/standings')

    expect(
      await screen.findByText('The league has no standings yet.'),
    ).toBeInTheDocument()
  })

  it('shows each manager with position, points and team value, and marks you', async () => {
    vi.mocked(fetchStandings).mockResolvedValue(standings)

    renderApp('/standings')

    const leader = within(await screen.findByRole('row', { name: /Manager 4/ }))
    expect(leader.getByRole('cell', { name: '1' })).toBeInTheDocument()
    expect(leader.getByRole('cell', { name: '515' })).toBeInTheDocument()
    expect(leader.getByText('€64,530,000')).toBeInTheDocument()
    expect(leader.getByText('(-€70,000)')).toBeInTheDocument()
    expect(leader.queryByText('You')).not.toBeInTheDocument()
    expect(
      within(table().getByRole('row', { name: /Manager 1/ })).getByText('You'),
    ).toBeInTheDocument()
    expect(fetchRivalSquad).not.toHaveBeenCalled()
  })

  it("shows a manager's squad and team value when the name is clicked", async () => {
    vi.mocked(fetchStandings).mockResolvedValue(standings)
    vi.mocked(fetchRivalSquad).mockResolvedValue(leadersSquad)

    const { user } = renderApp('/standings')

    await user.click(await screen.findByRole('link', { name: 'Manager 4' }))

    const squad = within(
      await screen.findByRole('region', { name: "Manager 4's squad" }),
    )
    expect(fetchRivalSquad).toHaveBeenCalledWith(2000004)
    expect(await squad.findByText(/Team value €64,530,000/)).toBeInTheDocument()
    // 10:00 UTC is 11:00 in Madrid.
    expect(
      squad.getByText(/lineup saved 26 Oct 2026, 11:00/),
    ).toBeInTheDocument()
    // In lineup order: the goalkeeper first.
    expect(
      squad
        .getAllByRole('row')
        .slice(1)
        .map((row) => within(row).getAllByRole('cell')[0].textContent),
    ).toEqual(['Keeper', 'Striker'])
    expect(
      within(squad.getByRole('row', { name: /Striker/ })).getByText(
        'Suspended',
      ),
    ).toBeInTheDocument()
    expect(
      squad.getByText(/1 player in this lineup is not shown/),
    ).toBeInTheDocument()
    expect(screen.getByRole('link', { name: 'Manager 4' })).toHaveAttribute(
      'aria-current',
      'true',
    )
  })

  it("links each squad player's name to the player page", async () => {
    vi.mocked(fetchStandings).mockResolvedValue(standings)
    vi.mocked(fetchRivalSquad).mockResolvedValue(leadersSquad)

    renderApp('/standings?manager=2000004')

    expect(
      await screen.findByRole('link', { name: 'Striker' }),
    ).toHaveAttribute('href', '/players/30487')
  })

  it('opens the squad named in the URL', async () => {
    vi.mocked(fetchStandings).mockResolvedValue(standings)
    vi.mocked(fetchRivalSquad).mockResolvedValue(leadersSquad)

    renderApp('/standings?manager=2000004')

    expect(
      await screen.findByRole('region', { name: "Manager 4's squad" }),
    ).toBeInTheDocument()
  })

  it('ignores a manager in the URL who is not in the table', async () => {
    vi.mocked(fetchStandings).mockResolvedValue(standings)

    renderApp('/standings?manager=42')

    await screen.findByRole('row', { name: /Manager 4/ })
    expect(fetchRivalSquad).not.toHaveBeenCalled()
  })

  it('shows the squad loading inside its titled region', async () => {
    vi.mocked(fetchStandings).mockResolvedValue(standings)
    vi.mocked(fetchRivalSquad).mockReturnValue(new Promise(() => {}))

    renderApp('/standings?manager=2000004')

    const squad = within(
      await screen.findByRole('region', { name: "Manager 4's squad" }),
    )
    expect(squad.getByRole('status')).toHaveTextContent('Loading the squad…')
  })

  it('says when a lineup has no players, and names no missing ones', async () => {
    vi.mocked(fetchStandings).mockResolvedValue(standings)
    vi.mocked(fetchRivalSquad).mockResolvedValue({
      ...leadersSquad,
      players: [],
      missingPlayerIds: [],
    })

    renderApp('/standings?manager=2000004')

    const squad = within(
      await screen.findByRole('region', { name: "Manager 4's squad" }),
    )
    expect(
      await squad.findByText('No players in this lineup.'),
    ).toBeInTheDocument()
    expect(squad.queryByText(/not shown/)).not.toBeInTheDocument()
  })

  it("shows the backend's problem when a squad fails", async () => {
    vi.mocked(fetchStandings).mockResolvedValue(standings)
    vi.mocked(fetchRivalSquad).mockRejectedValue(
      new ApiError(502, 'Biwenger unavailable', 'HTTP 503 from Biwenger'),
    )

    renderApp('/standings?manager=2000004')

    const squad = within(
      await screen.findByRole('region', { name: "Manager 4's squad" }),
    )
    expect(await squad.findByText('Biwenger unavailable')).toBeInTheDocument()
    expect(table().getByRole('row', { name: /Manager 4/ })).toBeInTheDocument()
  })
})
