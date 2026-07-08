export default defineEventHandler(async event => {
  const { accessToken } = await useAuth0(event).getAccessToken()
  const config = useRuntimeConfig(event)
  const installmentGroupId = getRouterParam(event, 'installmentGroupId')

  return $fetch(`/credit-card-transactions/installment-groups/${installmentGroupId}`, {
    baseURL: config.public.apiBaseUrl,
    method: 'GET',
    headers: buildBackendHeaders(event, accessToken),
  })
})
