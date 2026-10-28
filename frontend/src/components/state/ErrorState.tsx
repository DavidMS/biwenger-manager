import { Alert, AlertDescription, AlertTitle } from '@/components/ui/alert'
import { ApiError } from '@/lib/api'

type ErrorStateProps = {
  error: Error
  /** Overrides the title; by default the problem's own title, from the backend. */
  title?: string
  /** What the user can do about it, shown after the reason. */
  hint?: string
}

/** What a data view shows when its data failed: the reason, and what to do if we know. */
export function ErrorState({ error, title, hint }: ErrorStateProps) {
  const heading =
    title ?? (error instanceof ApiError ? error.title : 'Something went wrong')
  return (
    <Alert variant="destructive">
      <AlertTitle>{heading}</AlertTitle>
      <AlertDescription>
        <p>{error.message}</p>
        {hint && <p>{hint}</p>}
      </AlertDescription>
    </Alert>
  )
}
