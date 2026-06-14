import { readonly, shallowRef } from 'vue'

const FALLBACK = 'Ocorreu um erro inesperado. Por favor, tente novamente.'

export function useApiError() {
  const error = shallowRef<string | null>(null)

  function setError(e: unknown) {
    if (typeof e === 'string') {
      error.value = e || FALLBACK

      return
    }

    if (e instanceof Error) {
      const fetchErr = e as { data?: { message?: string }; statusMessage?: string }

      error.value = fetchErr.data?.message ?? fetchErr.statusMessage ?? e.message ?? FALLBACK

      return
    }

    error.value = FALLBACK
  }

  function clearError() {
    error.value = null
  }

  return { error: readonly(error), setError, clearError }
}
