export default defineEventHandler(async event => {
  const { accessToken } = await useAuth0(event).getAccessToken()
  const config = useRuntimeConfig(event)
  const id = getRouterParam(event, 'id')
  const body = await readBody(event)

  return $fetch(`/users/${id}`, {
    baseURL: config.public.apiBaseUrl,
    method: 'PUT',
    headers: { Authorization: `Bearer ${accessToken}` },
    body,
  })
})
