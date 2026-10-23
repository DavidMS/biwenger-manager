import { render, screen } from '@testing-library/react'
import { describe, expect, it } from 'vitest'
import { ApiError } from '@/lib/api'
import { ErrorState } from './ErrorState'

describe('ErrorState', () => {
  it("uses the backend problem's title and shows its detail", () => {
    render(
      <ErrorState
        error={
          new ApiError(502, 'Biwenger unavailable', 'HTTP 503 from Biwenger')
        }
      />,
    )

    expect(screen.getByText('Biwenger unavailable')).toBeInTheDocument()
    expect(screen.getByText('HTTP 503 from Biwenger')).toBeInTheDocument()
  })

  it('takes a title and a hint when the view knows better', () => {
    render(
      <ErrorState
        error={new Error('Failed to fetch')}
        title="Offline"
        hint="Is the backend running?"
      />,
    )

    expect(screen.getByText('Offline')).toBeInTheDocument()
    expect(screen.getByText('Is the backend running?')).toBeInTheDocument()
  })

  it('has a generic title for errors that are not from the backend', () => {
    render(<ErrorState error={new Error('Failed to fetch')} />)

    expect(screen.getByText('Something went wrong')).toBeInTheDocument()
  })
})
