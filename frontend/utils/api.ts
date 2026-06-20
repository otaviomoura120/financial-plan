export const $api = $fetch.create({

  // Request interceptor
  async onRequest({ options }) {
    // Set baseUrl for all API calls
    options.baseURL = useRuntimeConfig().public.apiBaseUrl || '/api'

    const accessToken = useCookie('accessToken').value
    if (accessToken) {
      options.headers = {
        ...options.headers,
        Authorization: `Bearer ${accessToken}`,
      }
    }

    const spaceId = useCookie('activeSpaceId').value
    if (spaceId) {
      options.headers = {
        ...options.headers,
        'X-Space-Id': String(spaceId),
      }
    }
  },
})
