import { useEffect, useState } from 'react'
import { SectionCard } from '@/components/layout/SectionCard'
import { EmptyState } from '@/components/state/EmptyState'
import { formatDateTime, formatTimeLeft } from '@/lib/format'
import type { NextRound } from './types'

/** The countdown shows minutes, so a tick every few seconds keeps it right without much work. */
export const TICK_MS = 10_000

type NextRoundCardProps = {
  nextRound?: NextRound
}

/** The next round and the time left before it starts and lineups lock. */
export function NextRoundCard({ nextRound }: NextRoundCardProps) {
  return (
    <SectionCard title="Next round">
      {nextRound ? (
        <div className="space-y-1">
          <p className="font-medium">{nextRound.name}</p>
          <TimeLeft startsAt={nextRound.startsAt} />
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

function TimeLeft({ startsAt }: { startsAt: string }) {
  const now = useNow(TICK_MS)
  const left = new Date(startsAt).getTime() - now
  return (
    <p className="font-heading text-2xl">
      {left > 0 ? `Starts in ${formatTimeLeft(left)}` : 'Started'}
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
