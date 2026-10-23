import { screen, waitFor, within } from '@testing-library/react'
import { beforeEach, describe, expect, it, vi } from 'vitest'
import { ApiError } from '@/lib/api'
import { SettingsPage } from '@/pages/SettingsPage'
import { renderWithProviders } from '@/test/render'
import { fetchAccount, fetchSyncStatus, startSync } from './api'
import { POLL_MS, pollWhileRunning } from './hooks'
import type { SyncStatus } from './types'

vi.mock('./api')

const ACCOUNT = {
  accountName: 'Account Owner',
  teamName: 'Manager 1',
  leagueName: 'Example League',
}

const SUCCEEDED: SyncStatus = {
  running: false,
  lastRun: {
    trigger: 'SCHEDULED',
    result: 'SUCCEEDED',
    startedAt: '2026-10-22T05:30:00Z',
    finishedAt: '2026-10-22T05:30:04Z',
    day: '2026-10-22',
    players: 551,
    marketListings: 19,
    standings: 6,
  },
}

const RUNNING: SyncStatus = {
  running: true,
  lastRun: {
    trigger: 'MANUAL',
    result: 'RUNNING',
    startedAt: '2026-10-22T08:00:00Z',
  },
}

function card(name: string) {
  return screen
    .getByText(name, { selector: '[data-slot=card-title]' })
    .closest('[data-slot=card]')!
}

describe('SettingsPage', () => {
  beforeEach(() => {
    vi.mocked(fetchAccount).mockResolvedValue(ACCOUNT)
    vi.mocked(fetchSyncStatus).mockResolvedValue(SUCCEEDED)
  })

  it('shows the connected account and league', async () => {
    renderWithProviders(<SettingsPage />)

    const account = within(card('Biwenger account') as HTMLElement)
    expect(await account.findByText('Account Owner')).toBeInTheDocument()
    expect(account.getByText('Manager 1')).toBeInTheDocument()
    expect(account.getByText('Example League')).toBeInTheDocument()
  })

  it('says how to connect when no login is set', async () => {
    vi.mocked(fetchAccount).mockRejectedValue(
      new ApiError(
        503,
        'Biwenger not connected',
        'Biwenger credentials are not set',
      ),
    )

    renderWithProviders(<SettingsPage />)

    expect(
      await screen.findByText('Not connected to Biwenger'),
    ).toBeInTheDocument()
    expect(
      screen.getByText(/Set BIWENGER_EMAIL and BIWENGER_PASSWORD in .env/),
    ).toBeInTheDocument()
  })

  it('shows why Biwenger is unavailable', async () => {
    vi.mocked(fetchAccount).mockRejectedValue(
      new ApiError(
        502,
        'Biwenger unavailable',
        'Biwenger answered HTTP 503 to GET /api/v2/account',
      ),
    )

    renderWithProviders(<SettingsPage />)

    expect(await screen.findByText('Biwenger unavailable')).toBeInTheDocument()
    expect(
      screen.getByText('Biwenger answered HTTP 503 to GET /api/v2/account'),
    ).toBeInTheDocument()
  })

  it('shows the last sync in Madrid time, with its counts', async () => {
    renderWithProviders(<SettingsPage />)

    const sync = within(card('Data sync') as HTMLElement)
    expect(await sync.findByText('Succeeded')).toBeInTheDocument()
    expect(sync.getByText('22 Oct 2026, 07:30')).toBeInTheDocument()
    expect(
      sync.getByText(/551 players, 19 market listings, 6 standings/),
    ).toBeInTheDocument()
    expect(sync.getByText('(scheduled)')).toBeInTheDocument()
  })

  it('shows why the last sync failed', async () => {
    vi.mocked(fetchSyncStatus).mockResolvedValue({
      running: false,
      lastRun: {
        trigger: 'MANUAL',
        result: 'FAILED',
        startedAt: '2026-10-22T08:00:00Z',
        finishedAt: '2026-10-22T08:00:01Z',
        error: 'Biwenger answered HTTP 503 to GET /api/v2/market',
      },
    })

    renderWithProviders(<SettingsPage />)

    expect(await screen.findByText('Failed')).toBeInTheDocument()
    expect(
      screen.getByText('Biwenger answered HTTP 503 to GET /api/v2/market'),
    ).toBeInTheDocument()
  })

  it('says when no sync has run yet', async () => {
    vi.mocked(fetchSyncStatus).mockResolvedValue({ running: false })

    renderWithProviders(<SettingsPage />)

    expect(await screen.findByText('No sync has run yet.')).toBeInTheDocument()
  })

  it('starts a sync and shows it running', async () => {
    vi.mocked(startSync).mockResolvedValue(RUNNING)
    // As the backend does: the last run before the click, then the one it started.
    vi.mocked(fetchSyncStatus)
      .mockResolvedValueOnce(SUCCEEDED)
      .mockResolvedValue(RUNNING)
    const { user } = renderWithProviders(<SettingsPage />)

    await user.click(await screen.findByRole('button', { name: 'Sync now' }))

    expect(startSync).toHaveBeenCalledOnce()
    const button = await screen.findByRole('button', { name: 'Syncing…' })
    expect(button).toBeDisabled()
    expect(screen.getByText('Running')).toBeInTheDocument()
  })

  it('says so when a sync is already running', async () => {
    vi.mocked(startSync).mockRejectedValue(
      new ApiError(409, 'Sync already running', 'A sync is already running'),
    )
    const { user } = renderWithProviders(<SettingsPage />)

    await user.click(await screen.findByRole('button', { name: 'Sync now' }))

    expect(
      await screen.findByText('A sync is already running.'),
    ).toBeInTheDocument()
  })

  it('polls the status only while a sync runs', () => {
    expect(pollWhileRunning(RUNNING)).toBe(POLL_MS)
    expect(pollWhileRunning(SUCCEEDED)).toBe(false)
    expect(pollWhileRunning(undefined)).toBe(false)
  })

  it('shows the result once the running sync finishes', async () => {
    vi.mocked(fetchSyncStatus)
      .mockResolvedValueOnce(RUNNING)
      .mockResolvedValue(SUCCEEDED)

    renderWithProviders(<SettingsPage />)

    expect(await screen.findByText('Running')).toBeInTheDocument()
    await waitFor(
      () => expect(screen.getByText('Succeeded')).toBeInTheDocument(),
      {
        timeout: POLL_MS + 1_000,
      },
    )
  })
})
