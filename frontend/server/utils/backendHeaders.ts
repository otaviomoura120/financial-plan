import type { H3Event } from 'h3'

export function buildBackendHeaders(event: H3Event, accessToken: string): Record<string, string> {
  const headers: Record<string, string> = { Authorization: `Bearer ${accessToken}` }
  const spaceId = getCookie(event, 'activeSpaceId')

  if (spaceId) {
    headers['X-Space-Id'] = spaceId
  }

  return headers
}
