<script setup lang="ts">
interface OwnProfileResponse {
  id: number
  version: number
  name: string
  email: string
  nickname: string | null
  phoneNumber: string | null
  birthdate: string | null
  genre: string | null
  maritalStatus: string | null
}

interface Props {
  isDialogVisible: boolean
}

interface Emit {
  (e: 'update:isDialogVisible', value: boolean): void
  (e: 'saved', profile: OwnProfileResponse): void
}

const props = defineProps<Props>()

const emit = defineEmits<Emit>()

const { error, setError, clearError } = useApiError()

const auth0User = useUser()
const isPasswordUser = computed(() => auth0User.value?.sub?.startsWith('auth0|') ?? false)

const isRequestingReset = shallowRef(false)
const resetEmailSent = shallowRef(false)

async function requestPasswordReset() {
  isRequestingReset.value = true
  clearError()

  try {
    await $fetch('/api/auth/change-password', { method: 'POST' })
    resetEmailSent.value = true
  }
  catch (e) {
    setError(e)
  }
  finally {
    isRequestingReset.value = false
  }
}

const isLoadingProfile = shallowRef(false)
const isSaving = shallowRef(false)
const profileId = shallowRef<number | null>(null)
const profileVersion = shallowRef<number | null>(null)

const form = ref({
  name: '',
  email: '',
  nickname: '',
  phoneNumber: '',
  birthdate: '',
  genre: '',
  maritalStatus: '',
})

const genreOptions = ['Masculino', 'Feminino']
const maritalStatusOptions = ['Solteiro(a)', 'Casado(a)', 'Divorciado(a)', 'Viúvo(a)', 'União estável']

const nameRules = [(v: string) => !!v || 'Nome é obrigatório']

watch(
  () => props.isDialogVisible,
  async visible => {
    if (visible) {
      await loadProfile()
    }
  },
)

async function loadProfile() {
  isLoadingProfile.value = true
  clearError()

  try {
    const profile = await $fetch<OwnProfileResponse>('/api/users/me')

    profileId.value = profile.id
    profileVersion.value = profile.version
    form.value = {
      name: profile.name,
      email: profile.email,
      nickname: profile.nickname ?? '',
      phoneNumber: profile.phoneNumber ?? '',
      birthdate: profile.birthdate ? profile.birthdate.slice(0, 10) : '',
      genre: profile.genre ?? '',
      maritalStatus: profile.maritalStatus ?? '',
    }
  }
  catch (e) {
    setError(e)
  }
  finally {
    isLoadingProfile.value = false
  }
}

async function onSave() {
  if (!form.value.name || !profileId.value) {
    return
  }

  isSaving.value = true
  clearError()

  try {
    const updated = await $fetch<{ id: number; version: number; name: string }>(`/api/users/${profileId.value}`, {
      method: 'PUT',
      body: {
        version: profileVersion.value,
        name: form.value.name,
        nickname: form.value.nickname || null,
        phoneNumber: form.value.phoneNumber || null,
        birthdate: form.value.birthdate
          ? new Date(form.value.birthdate).toISOString()
          : null,
        genre: form.value.genre || null,
        maritalStatus: form.value.maritalStatus || null,
      },
    })

    profileVersion.value = updated.version

    emit('saved', {
      id: updated.id,
      version: updated.version,
      name: updated.name,
      email: form.value.email,
      nickname: form.value.nickname || null,
      phoneNumber: form.value.phoneNumber || null,
      birthdate: form.value.birthdate
        ? new Date(form.value.birthdate).toISOString()
        : null,
      genre: form.value.genre || null,
      maritalStatus: form.value.maritalStatus || null,
    })
    emit('update:isDialogVisible', false)
  }
  catch (e) {
    setError(e)
  }
  finally {
    isSaving.value = false
  }
}

function onClose() {
  emit('update:isDialogVisible', false)
}
</script>

<template>
  <VDialog
    :model-value="props.isDialogVisible"
    :fullscreen="$vuetify.display.smAndDown"
    max-width="600"
    scrollable
    @update:model-value="onClose"
  >
    <DialogCloseBtn
      v-if="!$vuetify.display.smAndDown"
      @click="onClose"
    />

    <VCard class="d-flex flex-column">
      <VCardItem class="px-5 px-sm-8 pt-5 pt-sm-8">
        <VCardTitle class="text-h5 text-wrap">
          Editar meus dados
        </VCardTitle>
        <VCardSubtitle class="text-wrap">
          Atualize suas informações pessoais.
        </VCardSubtitle>

        <template #append>
          <IconBtn
            v-if="$vuetify.display.smAndDown"
            @click="onClose"
          >
            <VIcon icon="tabler-x" />
          </IconBtn>
        </template>
      </VCardItem>

      <VCardText class="px-5 px-sm-8">
        <ApiErrorAlert
          v-if="error"
          :error="error"
          class="mb-4"
        />

        <div
          v-if="isLoadingProfile"
          class="d-flex justify-center py-10"
        >
          <VProgressCircular indeterminate />
        </div>

        <VForm v-else>
          <VRow>
            <VCol cols="12">
              <AppTextField
                v-model="form.name"
                label="Nome completo"
                placeholder="João Silva"
                :rules="nameRules"
                required
              />
            </VCol>

            <VCol cols="12">
              <AppTextField
                v-model="form.email"
                label="E-mail"
                disabled
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
          </VRow>
        </VForm>

        <template v-if="isPasswordUser">
          <VDivider class="my-6" />

          <VAlert
            v-if="resetEmailSent"
            type="success"
            variant="tonal"
            density="compact"
            class="mb-3"
          >
            E-mail de redefinição de senha enviado. Verifique sua caixa de entrada.
          </VAlert>

          <VBtn
            color="warning"
            variant="tonal"
            :loading="isRequestingReset"
            :disabled="resetEmailSent"
            prepend-icon="tabler-lock-password"
            :block="$vuetify.display.xs"
            @click="requestPasswordReset"
          >
            Alterar senha
          </VBtn>
        </template>
      </VCardText>

      <VDivider />

      <VCardActions class="px-5 px-sm-8 py-4">
        <div class="d-flex flex-column flex-sm-row justify-sm-end gap-3 w-100">
          <VBtn
            color="primary"
            variant="elevated"
            :slim="false"
            :loading="isSaving"
            :block="$vuetify.display.xs"
            @click="onSave"
          >
            Salvar
          </VBtn>

          <VBtn
            color="secondary"
            variant="tonal"
            :slim="false"
            :disabled="isSaving"
            :block="$vuetify.display.xs"
            @click="onClose"
          >
            Cancelar
          </VBtn>
        </div>
      </VCardActions>
    </VCard>
  </VDialog>
</template>
