import type { H3Event } from 'h3'

const SPACE_HEADER = 'X-Space-Id'

// Prefers the Space sent by the tab (plugins/space-header.client.ts) over the cookie, which is
// shared across tabs and only remains as a fallback for requests issued during SSR.
function resolveSpaceId(event: H3Event): string | undefined {
  const spaceId = getHeader(event, SPACE_HEADER) ?? getCookie(event, 'activeSpaceId')

  return spaceId && /^\d+$/.test(spaceId) ? spaceId : undefined
}

export function buildBackendHeaders(event: H3Event, accessToken: string): Record<string, string> {
  const headers: Record<string, string> = { Authorization: `Bearer ${accessToken}` }
  const spaceId = resolveSpaceId(event)

  if (spaceId) {
    headers[SPACE_HEADER] = spaceId
  }

  return headers
}
