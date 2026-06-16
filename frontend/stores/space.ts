import { defineStore } from 'pinia'
import { ref } from 'vue'

export interface DbUser {
  id: number
  name: string
  email?: string
  nickname?: string | null
  phoneNumber?: string | null
  birthdate?: string | null
  genre?: string | null
  maritalStatus?: string | null
}

export interface Space {
  id: number
  name: string
  description?: string
}

export const useSpaceStore = defineStore('space', () => {
  const dbUser = ref<DbUser | null>(null)
  const activeSpace = ref<Space | null>(null)
  const availableSpaces = ref<Space[]>([])

  function setDbUser(u: DbUser) {
    dbUser.value = u
  }

  function updateDbUser(patch: Partial<DbUser>) {
    if (dbUser.value)
      dbUser.value = { ...dbUser.value, ...patch }
  }

  function setActiveSpace(s: Space) {
    activeSpace.value = s
    useCookie<number | null>('activeSpaceId').value = s.id
  }

  function setAvailableSpaces(list: Space[]) {
    availableSpaces.value = list
  }

  function reset() {
    dbUser.value = null
    activeSpace.value = null
    availableSpaces.value = []
    useCookie<number | null>('activeSpaceId').value = null
  }

  return { dbUser, activeSpace, availableSpaces, setDbUser, updateDbUser, setActiveSpace, setAvailableSpaces, reset }
})
