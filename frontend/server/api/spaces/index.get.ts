export default defineEventHandler(async event => {
  const { accessToken } = await useAuth0(event).getAccessToken()
  const config = useRuntimeConfig(event)
  const { userId } = getQuery(event)

  // The query value is decoded and goes into the backend path, so only a numeric id may pass
  if (typeof userId !== 'string' || !/^\d+$/.test(userId)) {
    throw createError({ statusCode: 400, statusMessage: 'Invalid userId' })
  }

  return backendFetch(`/spaces/user/${userId}`, {
    baseURL: config.apiBaseUrl,
    headers: buildBackendHeaders(event, accessToken),
  })
})
