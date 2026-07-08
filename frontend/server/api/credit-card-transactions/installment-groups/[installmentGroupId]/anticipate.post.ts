export default defineEventHandler(async event => {
  const { accessToken } = await useAuth0(event).getAccessToken()
  const config = useRuntimeConfig(event)
  const installmentGroupId = getRouterParam(event, 'installmentGroupId')
  const body = await readBody(event)

  try {
    return await $fetch(`/credit-card-transactions/installment-groups/${installmentGroupId}/anticipate`, {
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
