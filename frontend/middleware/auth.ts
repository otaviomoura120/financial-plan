export default defineNuxtRouteMiddleware(to => {
  const user = useUser()

  if (!user.value)
    return navigateTo(`/auth/login?returnTo=${encodeURIComponent(to.fullPath)}`)
})
