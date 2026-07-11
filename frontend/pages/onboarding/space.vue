<script setup lang="ts">
import { useSpaceStore } from '@/stores/space'

definePageMeta({ layout: 'onboarding' })

const spaceStore = useSpaceStore()

const isSubmitting = ref(false)
const { error, setError, clearError } = useApiError()

const form = ref({
  name: '',
  description: '',
})

async function submit() {
  if (!form.value.name)
    return

  isSubmitting.value = true
  clearError()

  try {
    const space = await $fetch<{ id: number; name: string; description?: string }>('/api/spaces', {
      method: 'POST',
      body: {
        name: form.value.name,
        description: form.value.description || null,
        creatorId: spaceStore.dbUser?.id,
      },
    })

    spaceStore.setActiveSpace({ id: space.id, name: space.name, description: space.description })
    await navigateTo('/dashboard')
  }
  catch {
    setError('Falha ao criar seu Espaço. Por favor, tente novamente.')
  }
  finally {
    isSubmitting.value = false
  }
}
</script>

<template>
  <VCard
    :max-width="480"
    width="100%"
    class="pa-2"
  >
    <VCardText>
      <h4 class="text-h4 mb-1">
        Crie seu Espaço
      </h4>
      <p class="text-body-1 text-medium-emphasis mb-0">
        Um Espaço organiza todas as suas informações financeiras. Você pode convidar outras pessoas depois.
      </p>
    </VCardText>

    <VCardText>
      <VForm @submit.prevent="submit">
        <VRow>
          <VCol cols="12">
            <AppTextField
              v-model="form.name"
              label="Nome do Espaço"
              placeholder="Orçamento Familiar"
              :rules="[(v: string) => !!v || 'Nome do Espaço é obrigatório']"
              required
            />
          </VCol>

          <VCol cols="12">
            <AppTextField
              v-model="form.description"
              label="Descrição (opcional)"
              placeholder="Nosso espaço financeiro compartilhado"
            />
          </VCol>

          <VCol
            v-if="error"
            cols="12"
          >
            <ApiErrorAlert :error="error" />
          </VCol>

          <VCol cols="12">
            <VBtn
              block
              type="submit"
              :loading="isSubmitting"
            >
              Criar Espaço
            </VBtn>
          </VCol>
        </VRow>
      </VForm>
    </VCardText>
  </VCard>
</template>
