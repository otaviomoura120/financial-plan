import { ref } from 'vue'
import { useAuth0 } from '@auth0/auth0-vue'
import { ofetch } from 'ofetch'
import { useRouter } from 'vue-router'
import { useSpaceStore } from '@/stores/space'
import { $api } from '@/utils/api'

interface UserMeResponse {
  id: number
  name: string
  email: string
}

interface SpaceResponse {
  id: number
  name: string
  description?: string
}

export function useOnboarding() {
  const { getAccessTokenSilently } = useAuth0()
  const spaceStore = useSpaceStore()
  const router = useRouter()
  const baseURL = import.meta.env.VITE_API_BASE_URL || '/api'
  const isChecking = ref(false)

  async function checkAndRedirect(): Promise<void> {
    if (isChecking.value || spaceStore.activeSpace)
      return

    isChecking.value = true

    try {
      const token = await getAccessTokenSilently()
      const headers = { Authorization: `Bearer ${token}` }
      const user = await fetchUser(headers)
      if (!user) {
        await router.push('/onboarding/profile')
        return
      }

      spaceStore.setDbUser(user)

      const spaces = await ofetch<SpaceResponse[]>(`/spaces/user/${user.id}`, { baseURL, headers })

      if (spaces.length === 0) {
        await router.push('/onboarding/space')
      }
      else if (spaces.length === 1) {
        spaceStore.setActiveSpace(spaces[0])
      }
      else {
        spaceStore.setAvailableSpaces(spaces)
        await router.push('/onboarding/select-space')
      }
    }
    finally {
      isChecking.value = false
    }
  }

  async function fetchUser(headers: Record<string, string>): Promise<UserMeResponse | null> {

    // const response = await $api<{ id: number; name: string }>('/users/me', {
    //   method: 'GET'
    // })

    try {
      return await ofetch<UserMeResponse>('/users/me', { baseURL, headers })
    }
    catch (e: unknown) {
      const err = e as { response?: { status?: number } }
      if (err.response?.status === 404)
        return null

      throw e
    }
  }

  return { checkAndRedirect, isChecking }
}
