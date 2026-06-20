export default defineEventHandler(async (event) => {
  const { accessToken } = await useAuth0(event).getAccessToken()
  const config = useRuntimeConfig(event)
  const id = getRouterParam(event, 'id')

  return $fetch(`/spaces/${id}`, {
    baseURL: config.public.apiBaseUrl,
    method: 'DELETE',
    headers: buildBackendHeaders(event, accessToken),
  })
})
