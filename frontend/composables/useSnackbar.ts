import { shallowRef } from 'vue'

const FALLBACK = 'Ocorreu um erro inesperado. Por favor, tente novamente.'

function extractMessage(e: unknown): string {
  if (typeof e === 'string')
    return e || FALLBACK

  if (e instanceof Error) {
    const fetchErr = e as { data?: { message?: string }; statusMessage?: string }

    return fetchErr.data?.message ?? fetchErr.statusMessage ?? e.message ?? FALLBACK
  }

  return FALLBACK
}

export function useSnackbar() {
  const isVisible = shallowRef(false)
  const message = shallowRef('')
  const color = shallowRef<'success' | 'error'>('success')
  const icon = shallowRef('tabler-circle-check')

  function showSuccess(msg: string) {
    message.value = msg
    color.value = 'success'
    icon.value = 'tabler-circle-check'
    isVisible.value = true
  }

  function showError(e: unknown) {
    message.value = extractMessage(e)
    color.value = 'error'
    icon.value = 'tabler-circle-x'
    isVisible.value = true
  }

  return { isVisible, message, color, icon, showSuccess, showError }
}
