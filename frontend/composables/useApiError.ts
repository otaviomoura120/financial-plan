import { readonly, shallowRef } from 'vue'

const FALLBACK = 'Ocorreu um erro inesperado. Por favor, tente novamente.'

// The backend rate limit (infrastructure/ratelimit) answers 429 with an English text meant for logs
const TOO_MANY_REQUESTS = 'Muitas requisições em pouco tempo. Aguarde alguns minutos e tente novamente.'

function resolveMessage(e: unknown): string {
  if (typeof e === 'string') {
    return e || FALLBACK
  }

  if (!(e instanceof Error)) {
    return FALLBACK
  }

  const fetchErr = e as { data?: unknown; statusMessage?: string; statusCode?: number }

  if (fetchErr.statusCode === 429) {
    return TOO_MANY_REQUESTS
  }

  const data = fetchErr.data

  if (typeof data === 'string' && data) {
    return data
  }

  const nestedData = (data as { data?: unknown } | undefined)?.data

  if (typeof nestedData === 'string' && nestedData) {
    return nestedData
  }

  const dataMessage = (data as { message?: string } | undefined)?.message

  return dataMessage ?? fetchErr.statusMessage ?? e.message ?? FALLBACK
}

export function useApiError() {
  const error = shallowRef<string | null>(null)

  // `messages` translates backend error codes (e.g. "space_member_limit_reached") into text for the user
  function setError(e: unknown, messages?: Record<string, string>) {
    const message = resolveMessage(e)

    error.value = messages?.[message] ?? message
  }

  function clearError() {
    error.value = null
  }

  return { error: readonly(error), setError, clearError }
}
