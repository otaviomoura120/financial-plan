export default defineEventHandler(async event => {
  const { accessToken } = await useAuth0(event).getAccessToken()
  const config = useRuntimeConfig(event)
  const id = getRouterParam(event, 'id')
  const permissionId = getRouterParam(event, 'permissionId')
  const body = await readBody(event)

  return backendFetch(`/roles/${id}/permissions/${permissionId}`, {
    baseURL: config.public.apiBaseUrl,
    method: 'PATCH',
    headers: buildBackendHeaders(event, accessToken),
    body,
  })
})
