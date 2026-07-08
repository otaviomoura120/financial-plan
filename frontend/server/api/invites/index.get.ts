export default defineEventHandler(async event => {
  const { accessToken } = await useAuth0(event).getAccessToken()
  const config = useRuntimeConfig(event)

  return $fetch('/invites', {
    baseURL: config.public.apiBaseUrl,
    headers: buildBackendHeaders(event, accessToken),
  })
})
