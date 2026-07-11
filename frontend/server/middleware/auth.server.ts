const PUBLIC_PATHS = ['/auth/']

export default defineEventHandler(async event => {
  const url = getRequestURL(event)

  if (url.pathname === '/' || PUBLIC_PATHS.some(p => url.pathname.startsWith(p)))
    return

  const session = await useAuth0(event).getSession()

  if (!session)
    return sendRedirect(event, `/auth/login?returnTo=${encodeURIComponent(url.pathname)}`)
})
