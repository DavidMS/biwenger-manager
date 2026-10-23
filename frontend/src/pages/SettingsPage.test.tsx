import { screen } from '@testing-library/react'
import { describe, expect, it, vi } from 'vitest'
import { renderWithProviders } from '@/test/render'
import { SettingsPage } from './SettingsPage'

vi.mock('@/features/settings/api', () => ({
  fetchAccount: () => new Promise(() => {}),
  fetchSyncStatus: () => new Promise(() => {}),
  startSync: () => new Promise(() => {}),
}))

describe('SettingsPage', () => {
  it('shows the account and the sync, each as a labelled region', () => {
    renderWithProviders(<SettingsPage />)

    expect(
      screen.getByRole('heading', { level: 1, name: 'Settings' }),
    ).toBeInTheDocument()
    expect(
      screen.getByRole('region', { name: 'Biwenger account' }),
    ).toBeInTheDocument()
    expect(
      screen.getByRole('region', { name: 'Data sync' }),
    ).toBeInTheDocument()
  })
})
