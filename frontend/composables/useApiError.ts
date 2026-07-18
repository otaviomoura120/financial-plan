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
      const fetchErr = e as { data?: unknown; statusMessage?: string }
      const data = fetchErr.data

      if (typeof data === 'string' && data) {
        error.value = data

        return
      }

      const nestedData = (data as { data?: unknown } | undefined)?.data

      if (typeof nestedData === 'string' && nestedData) {
        error.value = nestedData

        return
      }

      const dataMessage = (data as { message?: string } | undefined)?.message

      error.value = dataMessage ?? fetchErr.statusMessage ?? e.message ?? FALLBACK

      return
    }

    error.value = FALLBACK
  }

  function clearError() {
    error.value = null
  }

  return { error: readonly(error), setError, clearError }
}
