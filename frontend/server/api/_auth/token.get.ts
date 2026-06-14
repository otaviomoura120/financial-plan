export default defineEventHandler(async (event) => {
  const { accessToken } = await useAuth0(event).getAccessToken()

  return { accessToken }
})
