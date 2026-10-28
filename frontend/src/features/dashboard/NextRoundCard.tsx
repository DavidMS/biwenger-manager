import { useEffect, useState } from 'react'
import { SectionCard } from '@/components/layout/SectionCard'
import { EmptyState } from '@/components/state/EmptyState'
import { formatDateTime, formatTimeLeft } from '@/lib/format'
import type { NextRound } from './types'

/** The countdown shows minutes, so a tick every few seconds keeps it right without much work. */
export const TICK_MS = 10_000

type NextRoundCardProps = {
  nextRound?: NextRound
  /** Called once when the countdown reaches the start, so the caller can ask for the next round. */
  onStarted?: () => void
}

/** The next round and the time left before it starts and lineups lock. */
export function NextRoundCard({ nextRound, onStarted }: NextRoundCardProps) {
  return (
    <SectionCard title="Next round">
      {nextRound ? (
        <div className="space-y-1">
          <p className="font-medium">{nextRound.name}</p>
          <TimeLeft startsAt={nextRound.startsAt} onStarted={onStarted} />
          <p className="text-muted-foreground">
            {formatDateTime(nextRound.startsAt)}
          </p>
        </div>
      ) : (
        <EmptyState>No upcoming round has a start time yet.</EmptyState>
      )}
    </SectionCard>
  )
}

type TimeLeftProps = {
  startsAt: string
  onStarted?: () => void
}

function TimeLeft({ startsAt, onStarted }: TimeLeftProps) {
  const now = useNow(TICK_MS)
  const left = new Date(startsAt).getTime() - now
  const started = left <= 0
  useEffect(() => {
    if (started) {
      onStarted?.()
    }
  }, [started, onStarted])
  return (
    <p className="font-heading text-2xl">
      {started ? 'Started' : `Starts in ${formatTimeLeft(left)}`}
    </p>
  )
}

function useNow(intervalMs: number): number {
  const [now, setNow] = useState(() => Date.now())
  useEffect(() => {
    const id = setInterval(() => setNow(Date.now()), intervalMs)
    return () => clearInterval(id)
  }, [intervalMs])
  return now
}
