export default defineEventHandler(async event => {
  const { accessToken } = await useAuth0(event).getAccessToken()
  const config = useRuntimeConfig(event)
  const id = getRouterParam(event, 'id')

  return $fetch(`/roles/${id}/permissions`, {
    baseURL: config.public.apiBaseUrl,
    method: 'GET',
    headers: buildBackendHeaders(event, accessToken),
  })
})
