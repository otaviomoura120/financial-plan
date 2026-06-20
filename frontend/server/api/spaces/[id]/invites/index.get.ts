export default defineEventHandler(async (event) => {
  const { accessToken } = await useAuth0(event).getAccessToken()
  const config = useRuntimeConfig(event)
  const id = getRouterParam(event, 'id')

  return $fetch(`/spaces/${id}/invites`, {
    baseURL: config.public.apiBaseUrl,
    headers: buildBackendHeaders(event, accessToken),
  })
})
