export default defineEventHandler(async event => {
  const { accessToken } = await useAuth0(event).getAccessToken()
  const config = useRuntimeConfig(event)
  const query = getQuery(event)

  return backendFetch('/menu-structure', {
    baseURL: config.public.apiBaseUrl,
    headers: buildBackendHeaders(event, accessToken),
    query,
  })
})
