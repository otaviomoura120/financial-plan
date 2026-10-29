export default defineEventHandler(async event => {
  const { accessToken } = await useAuth0(event).getAccessToken()
  const config = useRuntimeConfig(event)

  const body = await readBody<Record<string, unknown>>(event)

  return backendFetch('/users', {
    baseURL: config.apiBaseUrl,
    method: 'POST',
    headers: buildBackendHeaders(event, accessToken),
    body,
  })
})
