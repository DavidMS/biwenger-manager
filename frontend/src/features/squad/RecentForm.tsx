import { cn } from '@/lib/utils'
import type { Match, PlayerStatus } from './types'

type RecentFormProps = {
  form: Match[]
}

const missed: Record<PlayerStatus, { short: string; long: string }> = {
  AVAILABLE: { short: '–', long: 'not picked' },
  DOUBTFUL: { short: 'D', long: 'doubtful' },
  INJURED: { short: 'I', long: 'injured' },
  SUSPENDED: { short: 'S', long: 'suspended' },
  UNKNOWN: { short: '?', long: 'missed' },
}

/** The last matches, latest first: points when he played, a letter for why he missed it. */
export function RecentForm({ form }: RecentFormProps) {
  if (form.length === 0) {
    return <span className="text-muted-foreground">No matches</span>
  }
  return (
    <ol aria-label="Recent form, latest first" className="flex gap-1">
      {form.map((match, index) => (
        <li
          key={index}
          className={cn(
            'min-w-7 rounded-md px-1 text-center text-sm tabular-nums',
            match.played
              ? match.points < 0
                ? 'bg-destructive/10 text-destructive'
                : 'bg-secondary text-secondary-foreground'
              : 'text-muted-foreground',
          )}
        >
          {match.played ? (
            match.points
          ) : (
            <>
              <span aria-hidden="true">{missed[match.reason].short}</span>
              <span className="sr-only">{missed[match.reason].long}</span>
            </>
          )}
        </li>
      ))}
    </ol>
  )
}
