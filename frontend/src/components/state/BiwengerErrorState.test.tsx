import { render, screen, within } from '@testing-library/react'
import { describe, expect, it } from 'vitest'
import { ApiError } from '@/lib/api'
import { BiwengerErrorState } from './BiwengerErrorState'

const alert = () => within(screen.getByRole('alert'))

describe('BiwengerErrorState', () => {
  const notConfigured = new ApiError(
    503,
    'Biwenger not configured',
    'No login is set.',
  )

  it('says the app is not connected on a 503, and points to Settings', () => {
    render(<BiwengerErrorState error={notConfigured} />)

    expect(alert().getByText('Not connected to Biwenger')).toBeInTheDocument()
    expect(alert().getByText('No login is set.')).toBeInTheDocument()
    expect(
      alert().getByText('Check the Biwenger account on the Settings page.'),
    ).toBeInTheDocument()
  })

  it('takes another hint for the not-connected case', () => {
    render(
      <BiwengerErrorState error={notConfigured} notConnectedHint="Fix .env." />,
    )

    expect(alert().getByText('Fix .env.')).toBeInTheDocument()
  })

  it("shows the backend's problem for other API errors, with no hint", () => {
    render(
      <BiwengerErrorState
        error={
          new ApiError(502, 'Biwenger unavailable', 'HTTP 503 from Biwenger')
        }
        notConnectedHint="Fix .env."
      />,
    )

    expect(alert().getByText('Biwenger unavailable')).toBeInTheDocument()
    expect(alert().getByText('HTTP 503 from Biwenger')).toBeInTheDocument()
    expect(
      alert().queryByText('Not connected to Biwenger'),
    ).not.toBeInTheDocument()
    expect(alert().queryByText('Fix .env.')).not.toBeInTheDocument()
    expect(
      alert().queryByText('Check the Biwenger account on the Settings page.'),
    ).not.toBeInTheDocument()
  })

  it('has a generic title for errors that are not from the backend', () => {
    render(<BiwengerErrorState error={new Error('Failed to fetch')} />)

    expect(alert().getByText('Something went wrong')).toBeInTheDocument()
    expect(alert().getByText('Failed to fetch')).toBeInTheDocument()
  })
})
