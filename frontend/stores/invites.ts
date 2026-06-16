import { defineStore } from 'pinia'
import { ref } from 'vue'

export interface MyInvite {
  token: string
  spaceId: number
  spaceName: string
  roleId: number
  roleName: string
  expiresAt: string
}

export const useInviteStore = defineStore('invites', () => {
  const pendingInvites = ref<MyInvite[]>([])
  const isLoading = ref(false)

  async function fetchPendingInvites() {
    isLoading.value = true

    try {
      const result = await $fetch<MyInvite[]>('/api/invites')

      pendingInvites.value = Array.isArray(result) ? result : []
    }
    catch {
      // bell degrades to "no invites" on failure; the /invites page surfaces its own error
    }
    finally {
      isLoading.value = false
    }
  }

  function removeInvite(token: string) {
    pendingInvites.value = pendingInvites.value.filter(i => i.token !== token)
  }

  function reset() {
    pendingInvites.value = []
  }

  return { pendingInvites, isLoading, fetchPendingInvites, removeInvite, reset }
})
