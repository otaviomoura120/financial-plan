export default defineEventHandler(async event => {
  const { accessToken } = await useAuth0(event).getAccessToken()
  const config = useRuntimeConfig(event)
  const query = getQuery(event)

  return backendFetch('/transactions', {
    baseURL: config.apiBaseUrl,
    method: 'GET',
    headers: buildBackendHeaders(event, accessToken),
    query,
  })
})
