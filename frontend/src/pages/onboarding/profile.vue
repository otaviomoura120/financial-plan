<script setup lang="ts">
import { useAuth0 } from '@auth0/auth0-vue'
import { useRouter } from 'vue-router'
import { $api } from '@/utils/api'
import { useSpaceStore } from '@/stores/space'

definePage({
  meta: {
    layout: 'onboarding',
  },
})

const { user: auth0User } = useAuth0()
const spaceStore = useSpaceStore()
const router = useRouter()

const isSubmitting = ref(false)
const errorMessage = ref<string | null>(null)

const form = ref({
  name: auth0User.value?.name ?? '',
  email: auth0User.value?.email ?? '',
  nickname: auth0User.value?.nickname ?? '',
  phoneNumber: '',
  birthdate: '',
  genre: '',
  maritalStatus: '',
})

const genreOptions = ['Male', 'Female', 'Non-binary', 'Prefer not to say']
const maritalStatusOptions = ['Single', 'Married', 'Divorced', 'Widowed', 'Common-law']

async function submit() {
  if (!form.value.name || !form.value.email)
    return

  isSubmitting.value = true
  errorMessage.value = null

  try {
    const payload: Record<string, unknown> = {
      auth0Sub: auth0User.value?.sub,
      name: form.value.name,
      email: form.value.email,
      nickname: form.value.nickname || null,
      phoneNumber: form.value.phoneNumber || null,
      birthdate: form.value.birthdate ? new Date(form.value.birthdate).toISOString() : null,
      genre: form.value.genre || null,
      maritalStatus: form.value.maritalStatus || null,
    }

    const response = await $api<{ id: number; name: string }>('/users', {
      method: 'POST',
      body: payload,
    })

    spaceStore.setDbUser({ id: response.id, name: response.name, email: form.value.email })
    await router.push('/onboarding/space')
  }
  catch {
    errorMessage.value = 'Failed to save your profile. Please try again.'
  }
  finally {
    isSubmitting.value = false
  }
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
        Complete your profile
      </h4>
      <p class="text-body-1 text-medium-emphasis mb-0">
        Tell us a bit about yourself to get started.
      </p>
    </VCardText>

    <VCardText>
      <VForm @submit.prevent="submit">
        <VRow>
          <VCol cols="12">
            <AppTextField
              v-model="form.name"
              label="Full name"
              placeholder="John Smith"
              :rules="[(v: string) => !!v || 'Name is required']"
              required
            />
          </VCol>

          <VCol cols="12">
            <AppTextField
              v-model="form.email"
              label="Email"
              type="email"
              placeholder="john@example.com"
              :rules="[(v: string) => !!v || 'Email is required']"
              required
            />
          </VCol>

          <VCol cols="12">
            <AppTextField
              v-model="form.nickname"
              label="Nickname (optional)"
              placeholder="Johnny"
            />
          </VCol>

          <VCol
            cols="12"
            md="6"
          >
            <AppTextField
              v-model="form.phoneNumber"
              label="Phone (optional)"
              placeholder="+1 555 000 0000"
            />
          </VCol>

          <VCol
            cols="12"
            md="6"
          >
            <AppTextField
              v-model="form.birthdate"
              label="Birth date (optional)"
              type="date"
            />
          </VCol>

          <VCol
            cols="12"
            md="6"
          >
            <AppSelect
              v-model="form.genre"
              label="Gender (optional)"
              :items="genreOptions"
              clearable
            />
          </VCol>

          <VCol
            cols="12"
            md="6"
          >
            <AppSelect
              v-model="form.maritalStatus"
              label="Marital status (optional)"
              :items="maritalStatusOptions"
              clearable
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
              Continue
            </VBtn>
          </VCol>
        </VRow>
      </VForm>
    </VCardText>
  </VCard>
</template>
