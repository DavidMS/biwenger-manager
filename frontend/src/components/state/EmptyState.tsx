import type { ReactNode } from 'react'

type EmptyStateProps = {
  children: ReactNode
}

/** What a data view shows when there is nothing yet. */
export function EmptyState({ children }: EmptyStateProps) {
  return <p className="text-muted-foreground">{children}</p>
}
