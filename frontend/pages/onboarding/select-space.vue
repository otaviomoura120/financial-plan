<script setup lang="ts">
import { type Space, useSpaceStore } from '@/stores/space'

definePageMeta({ layout: 'onboarding' })

const spaceStore = useSpaceStore()

function selectSpace(space: Space) {
  spaceStore.setActiveSpace(space)
  navigateTo('/dashboard')
}
</script>

<template>
  <VCard
    :max-width="560"
    width="100%"
    class="pa-2"
  >
    <VCardText>
      <h4 class="text-h4 mb-1">
        Escolha um Espaço
      </h4>
      <p class="text-body-1 text-medium-emphasis mb-0">
        Você pertence a múltiplos Espaços. Selecione um para continuar.
      </p>
    </VCardText>

    <VCardText>
      <VRow>
        <VCol
          v-for="space in spaceStore.availableSpaces"
          :key="space.id"
          cols="12"
        >
          <VCard
            border
            hover
            class="cursor-pointer"
            @click="selectSpace(space)"
          >
            <VCardText class="d-flex align-center gap-4">
              <VAvatar
                color="primary"
                variant="tonal"
                size="40"
              >
                <VIcon icon="tabler-building" />
              </VAvatar>
              <div>
                <div class="text-body-1 font-weight-medium">
                  {{ space.name }}
                </div>
                <div
                  v-if="space.description"
                  class="text-body-2 text-medium-emphasis"
                >
                  {{ space.description }}
                </div>
              </div>
              <VSpacer />
              <VIcon
                icon="tabler-chevron-right"
                class="text-disabled"
              />
            </VCardText>
          </VCard>
        </VCol>
      </VRow>
    </VCardText>
  </VCard>
</template>
