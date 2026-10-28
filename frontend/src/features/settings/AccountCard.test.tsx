import { screen, within } from '@testing-library/react'
import { describe, expect, it, vi } from 'vitest'
import { ApiError } from '@/lib/api'
import { renderWithProviders } from '@/test/render'
import { AccountCard } from './AccountCard'
import { fetchAccount } from './api'

vi.mock('./api')

function card() {
  return within(screen.getByRole('region', { name: 'Biwenger account' }))
}

describe('AccountCard', () => {
  it('shows that it is loading', () => {
    vi.mocked(fetchAccount).mockReturnValue(new Promise(() => {}))

    renderWithProviders(<AccountCard />)

    expect(card().getByRole('status')).toHaveTextContent('Loading…')
  })

  it('shows the connected account, team and league', async () => {
    vi.mocked(fetchAccount).mockResolvedValue({
      accountName: 'Account Owner',
      teamName: 'Manager 1',
      leagueName: 'Example League',
    })

    renderWithProviders(<AccountCard />)

    expect(await card().findByText('Account Owner')).toBeInTheDocument()
    expect(card().getByText('Manager 1')).toBeInTheDocument()
    expect(card().getByText('Example League')).toBeInTheDocument()
  })

  it('says why it is not connected and how to fix it', async () => {
    // 503: the login is missing, or Biwenger rejected it.
    vi.mocked(fetchAccount).mockRejectedValue(
      new ApiError(
        503,
        'Biwenger not connected',
        'Biwenger rejected the login (HTTP 401)',
      ),
    )

    renderWithProviders(<AccountCard />)

    expect(
      await card().findByText('Not connected to Biwenger'),
    ).toBeInTheDocument()
    expect(
      card().getByText('Biwenger rejected the login (HTTP 401)'),
    ).toBeInTheDocument()
    expect(
      card().getByText(/Set or check BIWENGER_EMAIL and BIWENGER_PASSWORD/),
    ).toBeInTheDocument()
  })

  it('shows any other failure with the backend title', async () => {
    vi.mocked(fetchAccount).mockRejectedValue(
      new ApiError(
        502,
        'Biwenger unavailable',
        'Biwenger answered HTTP 503 to GET /api/v2/account',
      ),
    )

    renderWithProviders(<AccountCard />)

    expect(await card().findByText('Biwenger unavailable')).toBeInTheDocument()
    expect(card().queryByText(/BIWENGER_EMAIL/)).not.toBeInTheDocument()
  })
})
