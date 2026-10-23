type LoadingStateProps = {
  label?: string
}

/** What a data view shows while its data loads. */
export function LoadingState({ label = 'Loading…' }: LoadingStateProps) {
  return (
    <p role="status" className="text-muted-foreground">
      {label}
    </p>
  )
}
