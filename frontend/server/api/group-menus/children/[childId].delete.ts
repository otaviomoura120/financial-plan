export default defineEventHandler(async event => {
  const { accessToken } = await useAuth0(event).getAccessToken()
  const config = useRuntimeConfig(event)
  const childId = getRouterParam(event, 'childId')

  return $fetch(`/group-menus/children/${childId}`, {
    baseURL: config.public.apiBaseUrl,
    method: 'DELETE',
    headers: buildBackendHeaders(event, accessToken),
  })
})
