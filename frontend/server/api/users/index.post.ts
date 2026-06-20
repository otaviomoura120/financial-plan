export default defineEventHandler(async event => {
  const { accessToken } = await useAuth0(event).getAccessToken()
  const session = await useAuth0(event).getSession()
  const config = useRuntimeConfig(event)

  const body = await readBody<Record<string, unknown>>(event)

  return $fetch('/users', {
    baseURL: config.public.apiBaseUrl,
    method: 'POST',
    headers: buildBackendHeaders(event, accessToken),
    body: {
      ...body,

      // Override any client-provided value with the verified identity from the session
      auth0Sub: session?.user?.sub,
    },
  })
})
