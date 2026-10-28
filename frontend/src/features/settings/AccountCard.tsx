import { SectionCard } from '@/components/layout/SectionCard'
import { ErrorState } from '@/components/state/ErrorState'
import { LoadingState } from '@/components/state/LoadingState'
import { ApiError } from '@/lib/api'
import { useConnectedAccount } from './hooks'

export function AccountCard() {
  const account = useConnectedAccount()
  // 503: the login is missing or Biwenger rejected it. Either way, the fix is in .env.
  const notConnected =
    account.error instanceof ApiError && account.error.status === 503

  return (
    <SectionCard
      title="Biwenger account"
      description="The account and league this app works for."
    >
      {account.isPending && <LoadingState />}
      {account.isError && (
        <ErrorState
          error={account.error}
          title={notConnected ? 'Not connected to Biwenger' : undefined}
          hint={
            notConnected
              ? 'Set or check BIWENGER_EMAIL and BIWENGER_PASSWORD in .env, then restart the backend.'
              : undefined
          }
        />
      )}
      {account.isSuccess && (
        <dl className="grid grid-cols-[max-content_1fr] gap-x-6 gap-y-2">
          <dt className="text-muted-foreground">Account</dt>
          <dd>{account.data.accountName}</dd>
          <dt className="text-muted-foreground">Team</dt>
          <dd>{account.data.teamName}</dd>
          <dt className="text-muted-foreground">League</dt>
          <dd>{account.data.leagueName}</dd>
        </dl>
      )}
    </SectionCard>
  )
}
