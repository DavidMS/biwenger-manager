import { Alert, AlertDescription, AlertTitle } from '@/components/ui/alert'
import {
  Card,
  CardContent,
  CardDescription,
  CardHeader,
  CardTitle,
} from '@/components/ui/card'
import { ApiError } from '@/lib/api'
import { useConnectedAccount } from './hooks'

export function AccountCard() {
  const account = useConnectedAccount()

  return (
    <Card>
      <CardHeader>
        <CardTitle>Biwenger account</CardTitle>
        <CardDescription>
          The account and league this app works for.
        </CardDescription>
      </CardHeader>
      <CardContent>
        {account.isPending && <p className="text-muted-foreground">Loading…</p>}
        {account.isError && <AccountError error={account.error} />}
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
      </CardContent>
    </Card>
  )
}

type AccountErrorProps = {
  error: Error
}

function AccountError({ error }: AccountErrorProps) {
  const notConnected = error instanceof ApiError && error.status === 503
  return (
    <Alert variant="destructive">
      <AlertTitle>
        {notConnected ? 'Not connected to Biwenger' : 'Biwenger unavailable'}
      </AlertTitle>
      <AlertDescription>
        {notConnected
          ? 'Set BIWENGER_EMAIL and BIWENGER_PASSWORD in .env, then restart the backend.'
          : error.message}
      </AlertDescription>
    </Alert>
  )
}
