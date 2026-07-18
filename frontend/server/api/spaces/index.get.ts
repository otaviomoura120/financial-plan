export default defineEventHandler(async event => {
  const { accessToken } = await useAuth0(event).getAccessToken()
  const config = useRuntimeConfig(event)
  const { userId } = getQuery(event)

  return backendFetch(`/spaces/user/${userId}`, {
    baseURL: config.public.apiBaseUrl,
    headers: buildBackendHeaders(event, accessToken),
  })
})
