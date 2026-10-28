import { PageHeader } from '@/components/layout/PageHeader'
import { SquadList } from '@/features/squad/SquadList'

export function SquadPage() {
  return (
    <div className="space-y-6">
      <PageHeader
        title="Squad"
        description="Your players with value trend, status, points and form."
      />
      <SquadList />
    </div>
  )
}
