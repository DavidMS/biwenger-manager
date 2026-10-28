import { Link } from 'react-router'

type PlayerLinkProps = {
  id: number
  name: string
}

/** A player's name that opens the player's detail page. */
export function PlayerLink({ id, name }: PlayerLinkProps) {
  return (
    <Link to={`/players/${id}`} className="underline-offset-4 hover:underline">
      {name}
    </Link>
  )
}
