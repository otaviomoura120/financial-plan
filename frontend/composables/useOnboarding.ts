import { ref } from 'vue'
import { useSpaceStore } from '@/stores/space'
import type { OnboardingCheckResult } from '@/server/api/onboarding/check.get'

export function useOnboarding() {
  const spaceStore = useSpaceStore()
  const isChecking = ref(false)
  const { error, setError, clearError } = useApiError()

  async function checkAndRedirect(): Promise<void> {
    if (isChecking.value || spaceStore.activeSpace)
      return

    isChecking.value = true
    clearError()

    try {
      const result = await $fetch<OnboardingCheckResult>('/api/onboarding/check')

      switch (result.status) {
        case 'new_user':
          await navigateTo('/onboarding/profile')
          break
        case 'no_spaces':
          spaceStore.setDbUser(result.user)
          await navigateTo('/onboarding/space')
          break
        case 'one_space':
          spaceStore.setDbUser(result.user)
          spaceStore.setActiveSpace(result.spaces[0])
          break
        case 'multiple_spaces':
          spaceStore.setDbUser(result.user)
          spaceStore.setAvailableSpaces(result.spaces)
          await navigateTo('/onboarding/select-space')
          break
      }
    }
    catch (e) {
      setError(e)
    }
    finally {
      isChecking.value = false
    }
  }

  return { checkAndRedirect, isChecking, error, clearError }
}
