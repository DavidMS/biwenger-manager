import { CartesianGrid, Line, LineChart, XAxis, YAxis } from 'recharts'
import {
  ChartContainer,
  ChartTooltip,
  ChartTooltipContent,
  type ChartConfig,
} from '@/components/ui/chart'
import {
  Table,
  TableBody,
  TableCell,
  TableHead,
  TableHeader,
  TableRow,
} from '@/components/ui/table'
import {
  formatDay,
  formatMoney,
  formatMoneyCompact,
  formatShortDay,
} from '@/lib/format'
import type { PricePoint } from '@/lib/player'

type PriceChartProps = {
  prices: PricePoint[]
}

// One series, so no legend: the card's title names it. `--primary` keeps the line strong in both
// themes; the theme's chart greys are too light for a 2px line on white.
const config = {
  price: { label: 'Value', color: 'var(--primary)' },
} satisfies ChartConfig

/**
 * The value per day as a line, with a crosshair tooltip. Screen readers get a summary, and every
 * value is also in a table view.
 */
export function PriceChart({ prices }: PriceChartProps) {
  const values = prices.map((point) => point.price)
  const first = prices[0]
  const last = prices[prices.length - 1]
  const summary = `Value from ${formatMoney(first.price)} on ${formatDay(first.day)} to ${formatMoney(last.price)} on ${formatDay(last.day)}; lowest ${formatMoney(Math.min(...values))}, highest ${formatMoney(Math.max(...values))}.`
  return (
    <figure className="space-y-3">
      <ChartContainer
        config={config}
        className="aspect-auto h-64 w-full"
        aria-hidden="true"
      >
        <LineChart
          data={prices}
          margin={{ left: 4, right: 12 }}
          // The chart is aria-hidden: without this, Recharts makes it a Tab stop.
          accessibilityLayer={false}
        >
          <CartesianGrid vertical={false} />
          <XAxis
            dataKey="day"
            tickLine={false}
            axisLine={false}
            minTickGap={32}
            tickFormatter={formatShortDay}
          />
          <YAxis
            tickLine={false}
            axisLine={false}
            width={56}
            domain={['auto', 'auto']}
            tickFormatter={formatMoneyCompact}
          />
          <ChartTooltip
            content={
              <ChartTooltipContent
                labelFormatter={(_, payload) => {
                  const day: unknown = payload[0]?.payload?.day
                  return typeof day === 'string' ? formatDay(day) : ''
                }}
                formatter={(value) => formatMoney(Number(value))}
              />
            }
          />
          <Line
            dataKey="price"
            type="linear"
            stroke="var(--color-price)"
            strokeWidth={2}
            strokeLinecap="round"
            strokeLinejoin="round"
            dot={false}
            isAnimationActive={false}
          />
        </LineChart>
      </ChartContainer>
      <figcaption className="text-sm text-muted-foreground">
        {summary}
      </figcaption>
      <details>
        <summary className="cursor-pointer text-sm">Show as a table</summary>
        <Table>
          <TableHeader>
            <TableRow>
              <TableHead>Day</TableHead>
              <TableHead>Value</TableHead>
            </TableRow>
          </TableHeader>
          <TableBody>
            {prices.toReversed().map((point) => (
              <TableRow key={point.day}>
                <TableCell>{formatDay(point.day)}</TableCell>
                <TableCell className="tabular-nums">
                  {formatMoney(point.price)}
                </TableCell>
              </TableRow>
            ))}
          </TableBody>
        </Table>
      </details>
    </figure>
  )
}
