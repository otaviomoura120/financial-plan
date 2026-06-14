import { defineStore } from 'pinia'
import { computed, ref } from 'vue'

export interface GroupMenuChild {
  name: string
  endpoint: string
  icon: string
}

export interface GroupMenu {
  name: string
  icon: string
  children: GroupMenuChild[]
}

export const useMenuStore = defineStore('menu', () => {
  const items = ref<GroupMenu[]>([])
  const loading = ref(false)

  const navItems = computed(() =>
    items.value.map(group => ({
      title: group.name,
      icon: { icon: group.icon },
      children: group.children.map(child => ({
        title: child.name,
        to: { path: child.endpoint },
        icon: { icon: child.icon },
      })),
    })),
  )

  async function fetchMenuStructure() {
    loading.value = true
    try {
      items.value = await $fetch<GroupMenu[]>('/api/menu-structure')
    }
    finally {
      loading.value = false
    }
  }

  function reset() {
    items.value = []
    loading.value = false
  }

  return { items, loading, navItems, fetchMenuStructure, reset }
})
