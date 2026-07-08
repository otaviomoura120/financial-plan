export default defineEventHandler(async event => {
  const { accessToken } = await useAuth0(event).getAccessToken()
  const config = useRuntimeConfig(event)
  const token = getRouterParam(event, 'token')

  try {
    return await backendFetch(`/invites/${token}/decline`, {
      baseURL: config.public.apiBaseUrl,
      method: 'POST',
      headers: buildBackendHeaders(event, accessToken),
    })
  }
  catch (e) {
    const fetchError = e as { statusCode?: number; data?: unknown }

    throw createError({
      statusCode: fetchError.statusCode ?? 500,
      statusMessage: typeof fetchError.data === 'string' ? fetchError.data : 'unknown',
      data: fetchError.data,
    })
  }
})
