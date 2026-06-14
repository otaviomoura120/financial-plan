<script setup lang="ts">
import { useRouter } from 'vue-router'
import { $api } from '@/utils/api'
import { useSpaceStore } from '@/stores/space'

definePage({
  meta: {
    layout: 'onboarding',
  },
})

const spaceStore = useSpaceStore()
const router = useRouter()

const isSubmitting = ref(false)
const errorMessage = ref<string | null>(null)

const form = ref({
  name: '',
  description: '',
})

async function submit() {
  if (!form.value.name)
    return

  isSubmitting.value = true
  errorMessage.value = null

  try {
    const space = await $api<{ id: number; name: string; description?: string }>('/spaces', {
      method: 'POST',
      body: {
        name: form.value.name,
        description: form.value.description || null,
        creatorId: spaceStore.dbUser?.id,
      },
    })

    spaceStore.setActiveSpace({ id: space.id, name: space.name, description: space.description })
    await router.push('/')
  }
  catch {
    errorMessage.value = 'Failed to create your space. Please try again.'
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
        Create your Space
      </h4>
      <p class="text-body-1 text-medium-emphasis mb-0">
        A Space organises all your financial information. You can invite others later.
      </p>
    </VCardText>

    <VCardText>
      <VForm @submit.prevent="submit">
        <VRow>
          <VCol cols="12">
            <AppTextField
              v-model="form.name"
              label="Space name"
              placeholder="Family Budget"
              :rules="[(v: string) => !!v || 'Space name is required']"
              required
            />
          </VCol>

          <VCol cols="12">
            <AppTextField
              v-model="form.description"
              label="Description (optional)"
              placeholder="Our shared financial space"
            />
          </VCol>

          <VCol
            v-if="errorMessage"
            cols="12"
          >
            <VAlert
              type="error"
              variant="tonal"
              :text="errorMessage"
            />
          </VCol>

          <VCol cols="12">
            <VBtn
              block
              type="submit"
              :loading="isSubmitting"
            >
              Create Space
            </VBtn>
          </VCol>
        </VRow>
      </VForm>
    </VCardText>
  </VCard>
</template>
