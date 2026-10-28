import { act, render, screen, within } from '@testing-library/react'
import { afterEach, describe, expect, it, vi } from 'vitest'
import { NextRoundCard, TICK_MS } from './NextRoundCard'

function card() {
  return within(screen.getByRole('region', { name: 'Next round' }))
}

describe('NextRoundCard', () => {
  afterEach(() => {
    vi.useRealTimers()
  })

  it('counts down to the start, in Madrid time', async () => {
    vi.useFakeTimers({ shouldAdvanceTime: true })
    vi.setSystemTime(new Date('2026-10-28T17:58:30Z'))

    render(
      <NextRoundCard
        nextRound={{ name: 'Jornada 10', startsAt: '2026-10-30T19:00:00Z' }}
      />,
    )

    expect(card().getByText('Jornada 10')).toBeInTheDocument()
    expect(card().getByText('Starts in 2d 1h 1m')).toBeInTheDocument()
    expect(card().getByText('30 Oct 2026, 20:00')).toBeInTheDocument()

    await act(() => vi.advanceTimersByTimeAsync(6 * TICK_MS))

    expect(card().getByText('Starts in 2d 1h 0m')).toBeInTheDocument()
  })

  it('says when the round has started', async () => {
    vi.useFakeTimers({ shouldAdvanceTime: true })
    vi.setSystemTime(new Date('2026-10-30T18:59:55Z'))

    render(
      <NextRoundCard
        nextRound={{ name: 'Jornada 10', startsAt: '2026-10-30T19:00:00Z' }}
      />,
    )
    expect(card().getByText('Starts in under a minute')).toBeInTheDocument()

    await act(() => vi.advanceTimersByTimeAsync(TICK_MS))

    expect(card().getByText('Started')).toBeInTheDocument()
  })

  it('says when no round is scheduled', () => {
    render(<NextRoundCard />)

    expect(
      card().getByText('No upcoming round has a start time yet.'),
    ).toBeInTheDocument()
  })
})
