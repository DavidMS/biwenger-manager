import {
  Table,
  TableBody,
  TableCell,
  TableHead,
  TableHeader,
  TableRow,
} from '@/components/ui/table'
import { formatDateTime } from '@/lib/format'
import type { PlayerStatus } from '@/lib/player'
import { cn } from '@/lib/utils'
import type { PlayerMatch } from './types'

type MatchesTableProps = {
  /** In the order they were played; shown latest first. */
  matches: PlayerMatch[]
}

const missedReasons: Record<PlayerStatus, string> = {
  AVAILABLE: 'Not picked',
  DOUBTFUL: 'Doubtful',
  INJURED: 'Injured',
  SUSPENDED: 'Suspended',
  UNKNOWN: 'Missed',
}

/** Points per match: what he scored and played, or why he missed it. */
export function MatchesTable({ matches }: MatchesTableProps) {
  return (
    <Table>
      <TableHeader>
        <TableRow>
          <TableHead>Date</TableHead>
          <TableHead>Venue</TableHead>
          <TableHead>Points</TableHead>
          <TableHead>Minutes</TableHead>
        </TableRow>
      </TableHeader>
      <TableBody>
        {matches.toReversed().map((match) => (
          <TableRow key={match.matchId}>
            <TableCell className="whitespace-nowrap">
              {formatDateTime(match.date)}
            </TableCell>
            <TableCell>{match.home ? 'Home' : 'Away'}</TableCell>
            {match.played ? (
              <>
                <TableCell
                  className={cn(
                    'tabular-nums',
                    match.points < 0 && 'text-destructive',
                  )}
                >
                  {match.points}
                </TableCell>
                <TableCell className="tabular-nums">{match.minutes}</TableCell>
              </>
            ) : (
              <TableCell colSpan={2} className="text-muted-foreground">
                {missedReasons[match.reason]}
              </TableCell>
            )}
          </TableRow>
        ))}
      </TableBody>
    </Table>
  )
}
