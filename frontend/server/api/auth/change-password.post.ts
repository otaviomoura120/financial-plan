export default defineEventHandler(async (event) => {
  const session = await useAuth0(event).getSession()

  if (!session?.user?.email) {
    throw createError({ statusCode: 401, message: 'Não autenticado' })
  }

  const config = useRuntimeConfig()

  await $fetch(`https://${config.auth0.domain}/dbconnections/change_password`, {
    method: 'POST',
    body: {
      client_id: config.auth0.clientId,
      email: session.user.email,
      connection: 'Username-Password-Authentication',
    },
  })
})
