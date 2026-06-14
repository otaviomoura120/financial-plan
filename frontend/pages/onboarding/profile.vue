<script setup lang="ts">
import { useSpaceStore } from '@/stores/space'

definePageMeta({ layout: 'onboarding' })

const auth0User = useUser()
const spaceStore = useSpaceStore()

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

const genreOptions = ['Masculino', 'Feminino']
const maritalStatusOptions = ['Solteiro(a)', 'Casado(a)', 'Divorciado(a)', 'Viúvo(a)', 'União estável']

async function submit() {
  if (!form.value.name || !form.value.email)
    return

  isSubmitting.value = true
  errorMessage.value = null

  try {
    const response = await $fetch<{ id: number; name: string }>('/api/users', {
      method: 'POST',
      body: {
        name: form.value.name,
        email: form.value.email,
        nickname: form.value.nickname || null,
        phoneNumber: form.value.phoneNumber || null,
        birthdate: form.value.birthdate
          ? new Date(form.value.birthdate).toISOString()
          : null,
        genre: form.value.genre || null,
        maritalStatus: form.value.maritalStatus || null,
      },
    })

    spaceStore.setDbUser({ id: response.id, name: response.name, email: form.value.email })
    await navigateTo('/onboarding/space')
  }
  catch {
    errorMessage.value = 'Falha ao salvar seu perfil. Por favor, tente novamente.'
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
        Complete seu perfil
      </h4>
      <p class="text-body-1 text-medium-emphasis mb-0">
        Conte um pouco sobre você para começar.
      </p>
    </VCardText>

    <VCardText>
      <VForm @submit.prevent="submit">
        <VRow>
          <VCol cols="12">
            <AppTextField
              v-model="form.name"
              label="Nome completo"
              placeholder="João Silva"
              :rules="[(v: string) => !!v || 'Nome é obrigatório']"
              required
            />
          </VCol>

          <VCol cols="12">
            <AppTextField
              v-model="form.email"
              label="E-mail"
              type="email"
              placeholder="joao@exemplo.com"
              :rules="[(v: string) => !!v || 'E-mail é obrigatório']"
              required
            />
          </VCol>

          <VCol cols="12">
            <AppTextField
              v-model="form.nickname"
              label="Apelido (opcional)"
              placeholder="Johnny"
            />
          </VCol>

          <VCol
            cols="12"
            md="6"
          >
            <AppTextField
              v-model="form.phoneNumber"
              label="Telefone (opcional)"
              placeholder="+55 11 99999-9999"
            />
          </VCol>

          <VCol
            cols="12"
            md="6"
          >
            <AppTextField
              v-model="form.birthdate"
              label="Data de nascimento (opcional)"
              type="date"
            />
          </VCol>

          <VCol
            cols="12"
            md="6"
          >
            <AppSelect
              v-model="form.genre"
              label="Gênero (opcional)"
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
              label="Estado civil (opcional)"
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
              Continuar
            </VBtn>
          </VCol>
        </VRow>
      </VForm>
    </VCardText>
  </VCard>
</template>
