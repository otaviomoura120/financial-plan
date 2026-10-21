// Stamps every call to our own Nitro API with the Space this tab is showing.
// The `activeSpaceId` cookie is shared by all tabs, so relying on it alone lets a tab that
// switched Space change which Space another tab reads and writes. The Nitro proxy forwards
// this header to the backend (see server/utils/backendHeaders.ts), which validates membership.
const SPACE_HEADER = 'X-Space-Id'

function isInternalApiRequest(request: RequestInfo): boolean {
  return typeof request === 'string' && request.startsWith('/api/')
}

export default defineNuxtPlugin(() => {
  const spaceStore = useSpaceStore()

  globalThis.$fetch = globalThis.$fetch.create({
    onRequest({ request, options }) {
      const spaceId = spaceStore.activeSpace?.id

      if (!spaceId || !isInternalApiRequest(request)) {
        return
      }

      options.headers.set(SPACE_HEADER, String(spaceId))
    },
  })
})
