import { computed, ref, watchEffect } from 'vue'
import { useAuth0 } from '@auth0/auth0-vue'

export function useAuthenticatedUser() {
    const { user, isAuthenticated, isLoading, getAccessTokenSilently } = useAuth0()
    const accessToken = ref<string | null>(null)

    watchEffect(async () => {
        if (isAuthenticated.value && !isLoading.value) {
            try {
                accessToken.value = await getAccessTokenSilently()
            } catch (error) {
                console.error('Failed to get access token:', error)
            }
        }
    })

    return {
        user: computed(() => user.value),
        accessToken: computed(() => accessToken.value),
        isAuthenticated: computed(() => isAuthenticated.value),
        isLoading: computed(() => isLoading.value)
    }
}