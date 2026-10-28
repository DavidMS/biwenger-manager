import { formatMoneyChange } from '@/lib/format'
import { cn } from '@/lib/utils'
import type { PricePoint } from './types'

type SparklineProps = {
  trend: PricePoint[]
  change: number
}

const WIDTH = 80
const HEIGHT = 24

/** A player's value over the trend's days as a small line, coloured by the overall change. */
export function Sparkline({ trend, change }: SparklineProps) {
  if (trend.length < 2) {
    return <span className="text-muted-foreground">No history</span>
  }
  const prices = trend.map((point) => point.price)
  const min = Math.min(...prices)
  const range = Math.max(...prices) - min
  const points = prices
    .map((price, index) => {
      const x = (index / (prices.length - 1)) * WIDTH
      // A flat line sits in the middle; otherwise the highest value touches the top.
      const y =
        range === 0 ? HEIGHT / 2 : HEIGHT - ((price - min) / range) * HEIGHT
      return `${x.toFixed(1)},${y.toFixed(1)}`
    })
    .join(' ')
  return (
    <span className="inline-flex items-center gap-2">
      <svg
        role="img"
        aria-label={`Value over ${trend.length} days`}
        width={WIDTH}
        height={HEIGHT}
        viewBox={`-1 -1 ${WIDTH + 2} ${HEIGHT + 2}`}
        className={cn(
          'shrink-0',
          change < 0 ? 'text-destructive' : 'text-foreground',
          change === 0 && 'text-muted-foreground',
        )}
      >
        <polyline
          points={points}
          fill="none"
          stroke="currentColor"
          strokeWidth={1.5}
          strokeLinejoin="round"
        />
      </svg>
      <span
        className={cn(
          'text-sm whitespace-nowrap',
          change < 0 ? 'text-destructive' : 'text-muted-foreground',
        )}
      >
        {formatMoneyChange(change)}
      </span>
    </span>
  )
}
