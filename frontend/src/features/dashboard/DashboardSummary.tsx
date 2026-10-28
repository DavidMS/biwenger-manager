import { useCallback } from 'react'
import { ErrorState } from '@/components/state/ErrorState'
import { LoadingState } from '@/components/state/LoadingState'
import { ApiError } from '@/lib/api'
import { useDashboard } from './hooks'
import { NegativeBalanceAlert } from './NegativeBalanceAlert'
import { NextRoundCard } from './NextRoundCard'
import { TeamCard } from './TeamCard'

/** The dashboard's data: a warning when the balance is negative, your team, and the next round. */
export function DashboardSummary() {
  const dashboard = useDashboard()
  const { refetch } = dashboard
  // The backend picks the next round when asked: once this one starts, ask again, so the card and
  // the warning's deadline move on to the following round.
  const onRoundStarted = useCallback(() => void refetch(), [refetch])

  if (dashboard.isPending) {
    return <LoadingState />
  }
  if (dashboard.isError) {
    // 503: the login is missing or Biwenger rejected it.
    const notConnected =
      dashboard.error instanceof ApiError && dashboard.error.status === 503
    return (
      <ErrorState
        error={dashboard.error}
        title={notConnected ? 'Not connected to Biwenger' : undefined}
        hint={
          notConnected
            ? 'Check the Biwenger account on the Settings page.'
            : undefined
        }
      />
    )
  }
  const data = dashboard.data
  return (
    <div className="space-y-6">
      {data.balanceNegative && (
        <NegativeBalanceAlert
          balance={data.balance}
          nextRound={data.nextRound}
        />
      )}
      <div className="grid gap-6 md:grid-cols-2">
        <TeamCard dashboard={data} />
        <NextRoundCard nextRound={data.nextRound} onStarted={onRoundStarted} />
      </div>
    </div>
  )
}
