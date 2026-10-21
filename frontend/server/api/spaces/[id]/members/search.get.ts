export default defineEventHandler(async event => {
  const { accessToken } = await useAuth0(event).getAccessToken()
  const config = useRuntimeConfig(event)
  const id = getRouterParam(event, 'id')
  const query = getQuery(event)

  return backendFetch(`/spaces/${id}/members/search`, {
    baseURL: config.public.apiBaseUrl,
    headers: buildBackendHeaders(event, accessToken),
    query: { email: query.email },
  })
})
