import { PageHeader } from '@/components/layout/PageHeader'
import { StandingsView } from '@/features/standings/StandingsView'

export function StandingsPage() {
  return (
    <div className="space-y-6">
      <PageHeader
        title="Standings"
        description="The league table, and each manager's squad and team value."
      />
      <StandingsView />
    </div>
  )
}
