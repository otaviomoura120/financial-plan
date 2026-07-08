export default defineEventHandler(async event => {
  const { accessToken } = await useAuth0(event).getAccessToken()
  const config = useRuntimeConfig(event)

  return $fetch('/group-menus', {
    baseURL: config.public.apiBaseUrl,
    method: 'GET',
    headers: buildBackendHeaders(event, accessToken),
  })
})
