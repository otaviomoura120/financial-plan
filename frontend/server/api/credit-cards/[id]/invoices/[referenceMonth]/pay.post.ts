export default defineEventHandler(async event => {
  const { accessToken } = await useAuth0(event).getAccessToken()
  const config = useRuntimeConfig(event)
  const id = getRouterParam(event, 'id')
  const referenceMonth = getRouterParam(event, 'referenceMonth')
  const body = await readBody(event)

  try {
    return await $fetch(`/credit-cards/${id}/invoices/${referenceMonth}/pay`, {
      baseURL: config.public.apiBaseUrl,
      method: 'POST',
      headers: buildBackendHeaders(event, accessToken),
      body,
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
