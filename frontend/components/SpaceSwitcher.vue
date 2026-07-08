<script lang="ts" setup>
import { type Space, useSpaceStore } from '@/stores/space'

const spaceStore = useSpaceStore()

const hasMultipleSpaces = computed(() => spaceStore.availableSpaces.length > 1)

function switchSpace(space: Space) {
  spaceStore.setActiveSpace(space)
}
</script>

<template>
  <div
    v-if="spaceStore.activeSpace"
    class="space-switcher px-4 py-3"
  >
    <div class="text-xs text-uppercase text-disabled font-weight-medium mb-1 letter-spacing-1">
      Espaço ativo
    </div>

    <template v-if="hasMultipleSpaces">
      <VMenu location="bottom start">
        <template #activator="{ props: menuProps }">
          <div
            v-bind="menuProps"
            class="d-flex align-center gap-2 cursor-pointer space-switcher-trigger rounded pa-1"
          >
            <VAvatar
              color="primary"
              variant="tonal"
              size="28"
            >
              <VIcon
                icon="tabler-building"
                size="16"
              />
            </VAvatar>
            <span class="text-body-2 font-weight-medium text-truncate flex-1-1">
              {{ spaceStore.activeSpace.name }}
            </span>
            <VIcon
              icon="tabler-selector"
              size="16"
              class="text-disabled"
            />
          </div>
        </template>

        <VList density="compact">
          <VListSubheader>Switch Space</VListSubheader>
          <VListItem
            v-for="space in spaceStore.availableSpaces"
            :key="space.id"
            :active="space.id === spaceStore.activeSpace?.id"
            color="primary"
            @click="switchSpace(space)"
          >
            <template #prepend>
              <VAvatar
                color="primary"
                variant="tonal"
                size="24"
                class="me-2"
              >
                <VIcon
                  icon="tabler-building"
                  size="14"
                />
              </VAvatar>
            </template>
            <VListItemTitle>{{ space.name }}</VListItemTitle>
          </VListItem>
        </VList>
      </VMenu>
    </template>

    <template v-else>
      <div class="d-flex align-center gap-2 pa-1">
        <VAvatar
          color="primary"
          variant="tonal"
          size="28"
        >
          <VIcon
            icon="tabler-building"
            size="16"
          />
        </VAvatar>
        <span class="text-body-2 font-weight-medium text-truncate">
          {{ spaceStore.activeSpace.name }}
        </span>
      </div>
    </template>
  </div>
</template>

<style scoped>
.space-switcher-trigger:hover {
  background-color: rgb(var(--v-theme-on-surface) / 6%);
}

.letter-spacing-1 {
  letter-spacing: 0.08em;
}
</style>
