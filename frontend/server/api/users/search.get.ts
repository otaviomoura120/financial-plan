export default defineEventHandler(async (event) => {
  const { accessToken } = await useAuth0(event).getAccessToken()
  const config = useRuntimeConfig(event)
  const query = getQuery(event)

  return $fetch('/users/search', {
    baseURL: config.public.apiBaseUrl,
    headers: { Authorization: `Bearer ${accessToken}` },
    query,
  })
})
