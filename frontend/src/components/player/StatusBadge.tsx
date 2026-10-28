import { Badge } from '@/components/ui/badge'
import { statusLabels, type PlayerStatus } from '@/lib/player'

type StatusBadgeProps = {
  status: PlayerStatus
}

const variants: Record<
  Exclude<PlayerStatus, 'AVAILABLE'>,
  'destructive' | 'secondary' | 'outline'
> = {
  INJURED: 'destructive',
  SUSPENDED: 'destructive',
  DOUBTFUL: 'secondary',
  UNKNOWN: 'outline',
}

/** Why a player may not play. An available player gets no badge, so the others stand out. */
export function StatusBadge({ status }: StatusBadgeProps) {
  if (status === 'AVAILABLE') {
    return (
      <span className="text-muted-foreground">{statusLabels.AVAILABLE}</span>
    )
  }
  return <Badge variant={variants[status]}>{statusLabels[status]}</Badge>
}
