import { PageHeader } from '@/components/layout/PageHeader'
import { MarketList } from '@/features/market/MarketList'

export function MarketPage() {
  return (
    <div className="space-y-6">
      <PageHeader
        title="Market"
        description="Today's market: filter by position, team, status and price, and sort by value for money."
      />
      <MarketList />
    </div>
  )
}
