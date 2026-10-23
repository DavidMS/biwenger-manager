import { describe, expect, it, vi } from 'vitest'
import { ApiError, getJson, postJson } from './api'

function answer(
  status: number,
  body: unknown,
  contentType = 'application/json',
) {
  vi.mocked(fetch).mockResolvedValueOnce(
    new Response(typeof body === 'string' ? body : JSON.stringify(body), {
      status,
      headers: { 'Content-Type': contentType },
    }),
  )
}

function sentHeaders(call = 0) {
  return new Headers(vi.mocked(fetch).mock.calls[call][1]?.headers)
}

describe('api', () => {
  it('returns the JSON of a successful answer', async () => {
    answer(200, { running: false })

    await expect(getJson('/api/sync/status')).resolves.toEqual({
      running: false,
    })
    expect(sentHeaders().get('Accept')).toBe('application/json')
  })

  it('posts JSON with a JSON content type, and keeps the Accept header', async () => {
    answer(200, { ok: true })

    await postJson('/api/actions', { player: 2001 })

    expect(sentHeaders().get('Content-Type')).toBe('application/json')
    expect(sentHeaders().get('Accept')).toBe('application/json')
    expect(vi.mocked(fetch).mock.calls[0][1]?.body).toBe('{"player":2001}')
  })

  it('returns nothing for 204 No Content', async () => {
    vi.mocked(fetch).mockResolvedValueOnce(new Response(null, { status: 204 }))

    await expect(postJson('/api/whatever')).resolves.toBeUndefined()
  })

  it('turns a problem detail into an ApiError', async () => {
    answer(
      409,
      {
        title: 'Sync already running',
        detail: 'A sync is already running',
        status: 409,
      },
      'application/problem+json',
    )

    const error = await postJson('/api/sync').catch((e: unknown) => e)

    expect(error).toBeInstanceOf(ApiError)
    expect(error).toMatchObject({
      status: 409,
      title: 'Sync already running',
      message: 'A sync is already running',
    })
  })

  it('copes with an error page that is not JSON', async () => {
    answer(502, '<html>Bad Gateway</html>', 'text/html')

    const error = await getJson('/api/account').catch((e: unknown) => e)

    expect(error).toMatchObject({
      status: 502,
      message: 'The server answered 502.',
    })
  })
})
