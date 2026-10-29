export default defineEventHandler(async event => {
  const { accessToken } = await useAuth0(event).getAccessToken()
  const config = useRuntimeConfig(event)

  return backendFetch('/invites', {
    baseURL: config.apiBaseUrl,
    headers: buildBackendHeaders(event, accessToken),
  })
})
