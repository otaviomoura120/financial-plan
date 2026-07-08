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
      if (result.status === 'new_user') {
        await navigateTo('/onboarding/profile')
      }
      else if (result.status === 'no_spaces') {
        spaceStore.setDbUser(result.user)
        await navigateTo('/onboarding/space')
      }
      else if (result.status === 'one_space') {
        spaceStore.setDbUser(result.user)
        spaceStore.setActiveSpace(result.spaces[0])
      }
      else if (result.status === 'multiple_spaces') {
        spaceStore.setDbUser(result.user)
        spaceStore.setAvailableSpaces(result.spaces)

        const savedSpaceId = useCookie<number | null>('activeSpaceId').value
        const savedSpace = result.spaces.find(s => s.id === savedSpaceId)

        if (savedSpace)
          spaceStore.setActiveSpace(savedSpace)
        else
          await navigateTo('/onboarding/select-space')
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
