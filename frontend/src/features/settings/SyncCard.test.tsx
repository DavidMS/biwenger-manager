import { act, screen, within } from '@testing-library/react'
import { afterEach, beforeEach, describe, expect, it, vi } from 'vitest'
import { ApiError } from '@/lib/api'
import { renderWithProviders } from '@/test/render'
import { SyncCard } from './SyncCard'
import { fetchSyncStatus, startSync } from './api'
import { POLL_MS, pollWhileRunning } from './hooks'
import type { SyncStatus } from './types'

vi.mock('./api')

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

function card() {
  return within(screen.getByRole('region', { name: 'Data sync' }))
}

describe('SyncCard', () => {
  beforeEach(() => {
    vi.mocked(fetchSyncStatus).mockResolvedValue(SUCCEEDED)
  })

  it('shows the last sync in Madrid time, with its counts', async () => {
    renderWithProviders(<SyncCard />)

    expect(await card().findByText('Succeeded')).toBeInTheDocument()
    expect(card().getByText('22 Oct 2026, 07:30')).toBeInTheDocument()
    expect(
      card().getByText(/551 players, 19 market listings, 6 standings/),
    ).toBeInTheDocument()
    expect(card().getByText('(scheduled)')).toBeInTheDocument()
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

    renderWithProviders(<SyncCard />)

    expect(await card().findByText('Failed')).toBeInTheDocument()
    expect(
      card().getByText('Biwenger answered HTTP 503 to GET /api/v2/market'),
    ).toBeInTheDocument()
  })

  it('says when no sync has run yet', async () => {
    vi.mocked(fetchSyncStatus).mockResolvedValue({ running: false })

    renderWithProviders(<SyncCard />)

    expect(await card().findByText('No sync has run yet.')).toBeInTheDocument()
  })

  it('shows that the status is loading, then why it is unavailable', async () => {
    vi.mocked(fetchSyncStatus).mockRejectedValue(
      new ApiError(500, 'Internal Server Error', 'Boom'),
    )

    renderWithProviders(<SyncCard />)

    expect(card().getByText('Loading…')).toBeInTheDocument()
    expect(
      await card().findByText('Sync status unavailable'),
    ).toBeInTheDocument()
  })

  it('starts a sync and shows it running, without losing focus', async () => {
    vi.mocked(startSync).mockResolvedValue(RUNNING)
    // As the backend does: the last run before the click, then the one it started.
    vi.mocked(fetchSyncStatus)
      .mockResolvedValueOnce(SUCCEEDED)
      .mockResolvedValue(RUNNING)
    const { user } = renderWithProviders(<SyncCard />)

    await user.click(await card().findByRole('button', { name: 'Sync now' }))

    expect(startSync).toHaveBeenCalledOnce()
    const button = await card().findByRole('button', { name: 'Syncing…' })
    expect(button).toHaveAttribute('aria-disabled', 'true')
    expect(button).toHaveFocus()
    expect(card().getByRole('status')).toHaveTextContent('Running')
    // A second click while it runs does nothing.
    await user.click(button)
    expect(startSync).toHaveBeenCalledOnce()
  })

  it('says why a sync could not start', async () => {
    vi.mocked(startSync).mockRejectedValue(
      new ApiError(503, 'Biwenger not connected', 'No login'),
    )
    const { user } = renderWithProviders(<SyncCard />)

    await user.click(await card().findByRole('button', { name: 'Sync now' }))

    expect(await card().findByText('Sync not started')).toBeInTheDocument()
    expect(card().getByText('No login')).toBeInTheDocument()
  })

  it('polls the status only while a sync runs', () => {
    expect(pollWhileRunning(RUNNING)).toBe(POLL_MS)
    expect(pollWhileRunning(SUCCEEDED)).toBe(false)
    expect(pollWhileRunning(undefined)).toBe(false)
  })

  describe('while a sync runs', () => {
    beforeEach(() => {
      vi.useFakeTimers({ shouldAdvanceTime: true })
    })

    afterEach(() => {
      vi.useRealTimers()
    })

    it('polls until it finishes, then stops and refreshes the other views', async () => {
      vi.mocked(fetchSyncStatus)
        .mockResolvedValueOnce(RUNNING)
        .mockResolvedValue(SUCCEEDED)
      const { queryClient } = renderWithProviders(<SyncCard />)
      const invalidate = vi.spyOn(queryClient, 'invalidateQueries')

      expect(await card().findByText('Running')).toBeInTheDocument()
      await act(() => vi.advanceTimersByTimeAsync(POLL_MS))
      expect(await card().findByText('Succeeded')).toBeInTheDocument()

      // The sync changed the stored data: every other view refreshes.
      expect(invalidate).toHaveBeenCalledWith(
        expect.objectContaining({ predicate: expect.any(Function) }),
      )
      const calls = vi.mocked(fetchSyncStatus).mock.calls.length
      await act(() => vi.advanceTimersByTimeAsync(3 * POLL_MS))
      expect(fetchSyncStatus).toHaveBeenCalledTimes(calls)
    })

    it('shows "already running" only while it runs', async () => {
      vi.mocked(fetchSyncStatus)
        .mockResolvedValueOnce(SUCCEEDED)
        .mockResolvedValueOnce(RUNNING)
        .mockResolvedValue(SUCCEEDED)
      vi.mocked(startSync).mockRejectedValue(
        new ApiError(409, 'Sync already running', 'A sync is already running'),
      )
      const { user } = renderWithProviders(<SyncCard />, {
        advanceTimers: vi.advanceTimersByTime,
      })

      await user.click(await card().findByRole('button', { name: 'Sync now' }))

      expect(
        await card().findByText('A sync is already running.'),
      ).toBeInTheDocument()
      await act(() => vi.advanceTimersByTimeAsync(POLL_MS))
      expect(await card().findByText('Succeeded')).toBeInTheDocument()
      expect(
        card().queryByText('A sync is already running.'),
      ).not.toBeInTheDocument()
    })
  })
})
