import { Link } from 'react-router'

type PlayerLinkProps = {
  id: number
  name: string
}

/**
 * A player's name that opens the player's detail page. The underline is always there, because a
 * touch screen has no hover to reveal it.
 */
export function PlayerLink({ id, name }: PlayerLinkProps) {
  return (
    <Link
      to={`/players/${id}`}
      className="rounded-sm underline decoration-muted-foreground underline-offset-4 transition-colors hover:decoration-foreground focus-visible:ring-3 focus-visible:ring-muted-foreground focus-visible:outline-hidden"
    >
      {name}
    </Link>
  )
}
