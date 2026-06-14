import { defineStore } from 'pinia'
import { ref } from 'vue'

export interface DbUser {
  id: number
  name: string
  email?: string
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

  function setActiveSpace(s: Space) {
    activeSpace.value = s
  }

  function setAvailableSpaces(list: Space[]) {
    availableSpaces.value = list
  }

  function reset() {
    dbUser.value = null
    activeSpace.value = null
    availableSpaces.value = []
  }

  return { dbUser, activeSpace, availableSpaces, setDbUser, setActiveSpace, setAvailableSpaces, reset }
})
