import { SectionCard } from '@/components/layout/SectionCard'
import { formatMoney, formatMoneyChange } from '@/lib/format'
import { cn } from '@/lib/utils'
import type { Dashboard } from './types'

type TeamCardProps = {
  dashboard: Dashboard
}

/** Balance, team value, place in the league and season points. */
export function TeamCard({ dashboard }: TeamCardProps) {
  return (
    <SectionCard title="Your team">
      <dl className="grid grid-cols-[max-content_1fr] gap-x-6 gap-y-2">
        <dt className="text-muted-foreground">Balance</dt>
        <dd
          className={cn(
            'font-medium',
            dashboard.balanceNegative && 'text-destructive',
          )}
        >
          {formatMoney(dashboard.balance)}
        </dd>
        <dt className="text-muted-foreground">Team value</dt>
        <dd>
          {formatMoney(dashboard.teamValue)}{' '}
          <span className="text-muted-foreground">
            ({formatMoneyChange(dashboard.teamValueChange)} today)
          </span>
        </dd>
        <dt className="text-muted-foreground">League position</dt>
        <dd>
          {dashboard.position} of {dashboard.managers}
        </dd>
        <dt className="text-muted-foreground">Points</dt>
        <dd>{dashboard.points}</dd>
      </dl>
    </SectionCard>
  )
}
