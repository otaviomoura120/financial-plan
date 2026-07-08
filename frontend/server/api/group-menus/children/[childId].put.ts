export default defineEventHandler(async event => {
  const { accessToken } = await useAuth0(event).getAccessToken()
  const config = useRuntimeConfig(event)
  const childId = getRouterParam(event, 'childId')
  const body = await readBody(event)

  return $fetch(`/group-menus/children/${childId}`, {
    baseURL: config.public.apiBaseUrl,
    method: 'PUT',
    headers: buildBackendHeaders(event, accessToken),
    body,
  })
})
