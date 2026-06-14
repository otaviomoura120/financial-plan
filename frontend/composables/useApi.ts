import { defu } from 'defu'
import type { UseFetchOptions } from 'nuxt/app'

export const useApi: typeof useFetch = <T>(url: MaybeRefOrGetter<string>, options: UseFetchOptions<T> = {}) => {
  const config = useRuntimeConfig()

  const defaults: UseFetchOptions<T> = {
    baseURL: config.public.apiBaseUrl,
    async onRequest({ options: reqOptions }) {
      const { accessToken } = await $fetch<{ accessToken: string }>('/api/_auth/token')

      if (accessToken)
        (reqOptions.headers as Record<string, string>).Authorization = `Bearer ${accessToken}`
    },
  }

  return useFetch(url, defu(options, defaults))
}
