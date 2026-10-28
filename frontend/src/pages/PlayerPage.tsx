import { useParams } from 'react-router'
import { PageHeader } from '@/components/layout/PageHeader'
import { PlayerDetailView } from '@/features/player/PlayerDetailView'

export function PlayerPage() {
  const { playerId } = useParams()
  const id = Number(playerId)
  if (!Number.isInteger(id) || id <= 0) {
    return (
      <PageHeader
        title="Player not found"
        description="This link doesn't name a player."
      />
    )
  }
  // A new key per player, so going from one player to another starts clean.
  return <PlayerDetailView key={id} playerId={id} />
}
