export default defineEventHandler(async (event) => {
  const { accessToken } = await useAuth0(event).getAccessToken()
  const config = useRuntimeConfig(event)

  const body = await readBody(event)

  return $fetch('/spaces', {
    baseURL: config.public.apiBaseUrl,
    method: 'POST',
    headers: { Authorization: `Bearer ${accessToken}` },
    body,
  })
})
