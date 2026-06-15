export default defineEventHandler(async (event) => {
  const { accessToken } = await useAuth0(event).getAccessToken()
  const config = useRuntimeConfig(event)
  const query = getQuery(event)

  return $fetch('/roles', {
    baseURL: config.public.apiBaseUrl,
    method: 'GET',
    headers: { Authorization: `Bearer ${accessToken}` },
    query,
  })
})
