import { describe, expect, it } from 'vitest'
import { errorMessage } from './client'

describe('errorMessage', () => {
  it('prefers the server message from the API error body', () => {
    expect(errorMessage({ response: { status: 409, data: { message: 'Room is not available' } } })).toBe('Room is not available')
  })

  it('maps common status codes to friendly text', () => {
    expect(errorMessage({ response: { status: 403, data: {} } })).toMatch(/permission/)
    expect(errorMessage({ response: { status: 404, data: {} } })).toMatch(/not found/)
    expect(errorMessage({ code: 'ERR_NETWORK' })).toMatch(/Could not reach/)
  })

  it('falls back to the error message', () => {
    expect(errorMessage(new Error('boom'))).toBe('boom')
  })
})
