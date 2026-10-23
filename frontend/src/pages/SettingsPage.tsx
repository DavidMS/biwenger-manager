import { PageHeader } from '@/components/layout/PageHeader'
import { AccountCard } from '@/features/settings/AccountCard'
import { SyncCard } from '@/features/settings/SyncCard'

export function SettingsPage() {
  return (
    <div className="space-y-6">
      <PageHeader
        title="Settings"
        description="The connected Biwenger account, and the data sync."
      />
      <div className="grid gap-6 md:grid-cols-2">
        <AccountCard />
        <SyncCard />
      </div>
    </div>
  )
}
