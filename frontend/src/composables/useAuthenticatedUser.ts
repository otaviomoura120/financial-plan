import { computed, ref, watchEffect } from 'vue'
import { useAuth0 } from '@auth0/auth0-vue'

export function useAuthenticatedUser() {
    const { user, isAuthenticated, isLoading, getAccessTokenSilently } = useAuth0()
    const accessToken = ref<string | null>(null)
    const cookieToken = useCookie('accessToken')

    watchEffect(async () => {
        if (isAuthenticated.value && !isLoading.value) {
            try {
                const token = await getAccessTokenSilently()
                accessToken.value = token
                cookieToken.value = token
            } catch (error) {
                console.error('Failed to get access token:', error)
            }
        }
    })

    watchEffect(() => {
        console.log({
            loading: isLoading.value,
            authenticated: isAuthenticated.value
        })
    })

    return {
        user: computed(() => user.value),
        accessToken: computed(() => accessToken.value),
        isAuthenticated: computed(() => isAuthenticated.value),
        isLoading: computed(() => isLoading.value)
    }
}