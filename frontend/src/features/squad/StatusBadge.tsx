import { Badge } from '@/components/ui/badge'
import type { PlayerStatus } from './types'

type StatusBadgeProps = {
  status: PlayerStatus
}

const badges: Record<
  Exclude<PlayerStatus, 'AVAILABLE'>,
  { label: string; variant: 'destructive' | 'secondary' | 'outline' }
> = {
  INJURED: { label: 'Injured', variant: 'destructive' },
  SUSPENDED: { label: 'Suspended', variant: 'destructive' },
  DOUBTFUL: { label: 'Doubtful', variant: 'secondary' },
  UNKNOWN: { label: 'Unknown status', variant: 'outline' },
}

/** Why a player may not play. An available player gets no badge, so the others stand out. */
export function StatusBadge({ status }: StatusBadgeProps) {
  if (status === 'AVAILABLE') {
    return <span className="text-muted-foreground">Available</span>
  }
  const { label, variant } = badges[status]
  return <Badge variant={variant}>{label}</Badge>
}
