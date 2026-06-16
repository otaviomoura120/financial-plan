<script setup lang="ts">
interface OwnProfileResponse {
  id: number
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

const isLoadingProfile = shallowRef(false)
const isSaving = shallowRef(false)
const profileId = shallowRef<number | null>(null)

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
    if (visible)
      await loadProfile()
  },
)

async function loadProfile() {
  isLoadingProfile.value = true
  clearError()

  try {
    const profile = await $fetch<OwnProfileResponse>('/api/users/me')

    profileId.value = profile.id
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
  if (!form.value.name || !profileId.value)
    return

  isSaving.value = true
  clearError()

  try {
    const updated = await $fetch<{ id: number; name: string }>(`/api/users/${profileId.value}`, {
      method: 'PUT',
      body: {
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

    emit('saved', {
      id: updated.id,
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
    :width="$vuetify.display.smAndDown ? 'auto' : 600"
    :model-value="props.isDialogVisible"
    @update:model-value="onClose"
  >
    <DialogCloseBtn @click="onClose" />

    <VCard class="pa-sm-10 pa-4">
      <VCardText>
        <h4 class="text-h4 text-center mb-2">
          Editar meus dados
        </h4>

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

        <VForm
          v-else
          @submit.prevent="onSave"
        >
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

          <div class="d-flex align-center justify-center gap-4 mt-6">
            <VBtn
              type="submit"
              :loading="isSaving"
            >
              Salvar
            </VBtn>

            <VBtn
              color="secondary"
              variant="tonal"
              :disabled="isSaving"
              @click="onClose"
            >
              Cancelar
            </VBtn>
          </div>
        </VForm>
      </VCardText>
    </VCard>
  </VDialog>
</template>
