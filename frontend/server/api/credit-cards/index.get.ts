export default defineEventHandler(async event => {
  const { accessToken } = await useAuth0(event).getAccessToken()
  const config = useRuntimeConfig(event)
  const query = getQuery(event)

  return $fetch('/credit-cards', {
    baseURL: config.public.apiBaseUrl,
    method: 'GET',
    headers: buildBackendHeaders(event, accessToken),
    query,
  })
})
