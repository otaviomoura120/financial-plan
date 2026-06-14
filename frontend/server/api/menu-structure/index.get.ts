export default defineEventHandler(async (event) => {
  const { accessToken } = await useAuth0(event).getAccessToken()
  const config = useRuntimeConfig(event)

  return $fetch('/menu-structure', {
    baseURL: config.public.apiBaseUrl,
    headers: { Authorization: `Bearer ${accessToken}` },
  })
})
