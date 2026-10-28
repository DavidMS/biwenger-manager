import { PageHeader } from '@/components/layout/PageHeader'
import { DashboardSummary } from '@/features/dashboard/DashboardSummary'

export function DashboardPage() {
  return (
    <div className="space-y-6">
      <PageHeader
        title="Dashboard"
        description="Balance, team value, league position and the next round."
      />
      <DashboardSummary />
    </div>
  )
}
