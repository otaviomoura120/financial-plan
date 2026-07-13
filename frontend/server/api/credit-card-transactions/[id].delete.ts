export default defineEventHandler(async event => {
  const { accessToken } = await useAuth0(event).getAccessToken()
  const config = useRuntimeConfig(event)
  const id = getRouterParam(event, 'id')
  const query = getQuery(event)

  try {
    return await backendFetch(`/credit-card-transactions/${id}`, {
      baseURL: config.public.apiBaseUrl,
      method: 'DELETE',
      headers: buildBackendHeaders(event, accessToken),
      query,
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
