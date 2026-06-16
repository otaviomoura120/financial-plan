export default defineEventHandler(async (event) => {
  const { accessToken } = await useAuth0(event).getAccessToken()
  const config = useRuntimeConfig(event)
  const id = getRouterParam(event, 'id')
  const userId = getRouterParam(event, 'userId')
  const body = await readBody(event)

  return $fetch(`/spaces/${id}/members/${userId}`, {
    baseURL: config.public.apiBaseUrl,
    method: 'PUT',
    headers: { Authorization: `Bearer ${accessToken}` },
    body,
  })
})
