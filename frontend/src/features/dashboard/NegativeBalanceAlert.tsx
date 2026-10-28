import { Alert, AlertDescription, AlertTitle } from '@/components/ui/alert'
import { formatDateTime, formatMoney } from '@/lib/format'
import type { NextRound } from './types'

type NegativeBalanceAlertProps = {
  balance: number
  nextRound?: NextRound
}

/** The game's rule: a team whose balance is still negative when a round starts scores 0 points. */
export function NegativeBalanceAlert({
  balance,
  nextRound,
}: NegativeBalanceAlertProps) {
  const deadline = nextRound
    ? `before ${nextRound.name} starts (${formatDateTime(nextRound.startsAt)})`
    : 'before the next round starts'
  return (
    <Alert variant="destructive" className="border-destructive">
      <AlertTitle>Your balance is negative: {formatMoney(balance)}</AlertTitle>
      <AlertDescription>
        <p>
          If it is still negative when a round starts, your team scores 0 points
          in that round. Sell players or accept offers {deadline}.
        </p>
      </AlertDescription>
    </Alert>
  )
}
