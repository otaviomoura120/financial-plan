export default defineEventHandler(async event => {
  const { accessToken } = await useAuth0(event).getAccessToken()
  const config = useRuntimeConfig(event)
  const installmentGroupId = getRouterParam(event, 'installmentGroupId')
  const body = await readBody(event)

  return backendFetch(`/credit-card-transactions/installment-groups/${installmentGroupId}/anticipate`, {
    baseURL: config.public.apiBaseUrl,
    method: 'POST',
    headers: buildBackendHeaders(event, accessToken),
    body,
  })
})
