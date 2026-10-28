import { ApiError } from '@/lib/api'
import { ErrorState } from './ErrorState'

type BiwengerErrorStateProps = {
  error: Error
  /** What to do when the app isn't connected; by default, go to the Settings page. */
  notConnectedHint?: string
}

/**
 * `ErrorState` for data read from Biwenger. A 503 means the login is missing or Biwenger rejected
 * it, so it says so and how to fix it; any other error shows the backend's problem.
 */
export function BiwengerErrorState({
  error,
  notConnectedHint = 'Check the Biwenger account on the Settings page.',
}: BiwengerErrorStateProps) {
  if (error instanceof ApiError && error.status === 503) {
    return (
      <ErrorState
        error={error}
        title="Not connected to Biwenger"
        hint={notConnectedHint}
      />
    )
  }
  return <ErrorState error={error} />
}
