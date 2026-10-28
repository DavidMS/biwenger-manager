import { SectionCard } from '@/components/layout/SectionCard'
import { BiwengerErrorState } from '@/components/state/BiwengerErrorState'
import { LoadingState } from '@/components/state/LoadingState'
import { useConnectedAccount } from './hooks'

export function AccountCard() {
  const account = useConnectedAccount()
  return (
    <SectionCard
      title="Biwenger account"
      description="The account and league this app works for."
    >
      {account.isPending && <LoadingState />}
      {account.isError && (
        // This is the Settings page, so the fix is in .env, not here.
        <BiwengerErrorState
          error={account.error}
          notConnectedHint="Set or check BIWENGER_EMAIL and BIWENGER_PASSWORD in .env, then restart the backend."
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
