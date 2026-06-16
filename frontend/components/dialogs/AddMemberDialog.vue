<script setup lang="ts">
interface UserSearchResponse {
  id: number
  name: string
  email: string
}

interface RoleOption {
  id: number
  name: string
}

interface Props {
  isDialogVisible: boolean
}

interface Emit {
  (e: 'update:isDialogVisible', value: boolean): void
  (e: 'inviteSent'): void
}

const props = defineProps<Props>()
const emit = defineEmits<Emit>()

const spaceStore = useSpaceStore()
const { error, setError, clearError } = useApiError()

type Step = 'search' | 'found' | 'not-found'

const step = shallowRef<Step>('search')
const email = shallowRef('')
const foundUser = shallowRef<UserSearchResponse | null>(null)
const selectedRoleId = shallowRef<number | null>(null)
const roles = ref<RoleOption[]>([])
const isSearching = shallowRef(false)
const isSubmitting = shallowRef(false)
const isLoadingRoles = shallowRef(false)

watch(
  () => props.isDialogVisible,
  async visible => {
    if (visible) {
      step.value = 'search'
      email.value = ''
      foundUser.value = null
      selectedRoleId.value = null
      clearError()
      await loadRoles()
    }
  },
)

async function loadRoles() {
  if (!spaceStore.activeSpace)
    return

  isLoadingRoles.value = true

  try {
    roles.value = await $fetch<RoleOption[]>('/api/roles', {
      query: { spaceId: spaceStore.activeSpace.id },
    })
  }
  catch (e) {
    setError(e)
  }
  finally {
    isLoadingRoles.value = false
  }
}

async function searchUser() {
  if (!email.value.trim())
    return

  isSearching.value = true
  clearError()

  try {
    foundUser.value = await $fetch<UserSearchResponse>('/api/users/search', {
      query: { email: email.value.trim() },
    })
    step.value = 'found'
  }
  catch (e: unknown) {
    const err = e as { status?: number }

    if (err?.status === 404) {
      foundUser.value = null
      step.value = 'not-found'
    }
    else {
      setError(e)
    }
  }
  finally {
    isSearching.value = false
  }
}

async function sendInvite() {
  if (!selectedRoleId.value || !spaceStore.activeSpace)
    return

  isSubmitting.value = true
  clearError()

  try {
    await $fetch(`/api/spaces/${spaceStore.activeSpace.id}/invites`, {
      method: 'POST',
      body: { email: email.value.trim(), roleId: selectedRoleId.value },
    })

    emit('inviteSent')
    emit('update:isDialogVisible', false)
  }
  catch (e) {
    setError(e)
  }
  finally {
    isSubmitting.value = false
  }
}

function backToSearch() {
  step.value = 'search'
  foundUser.value = null
  selectedRoleId.value = null
  clearError()
}

function onClose() {
  emit('update:isDialogVisible', false)
}
</script>

<template>
  <VDialog
    :width="$vuetify.display.smAndDown ? 'auto' : 560"
    :model-value="props.isDialogVisible"
    @update:model-value="onClose"
  >
    <DialogCloseBtn @click="onClose" />

    <VCard class="pa-sm-10 pa-4">
      <VCardText>
        <h4 class="text-h4 text-center mb-2">
          Adicionar Usuário
        </h4>

        <p class="text-body-1 text-center text-disabled mb-6">
          Busque por e-mail para adicionar ao espaço atual.
        </p>

        <ApiErrorAlert
          v-if="error"
          :error="error"
          class="mb-4"
        />

        <!-- Step: search -->
        <div v-if="step === 'search'">
          <div class="d-flex gap-3">
            <AppTextField
              v-model="email"
              label="E-mail"
              placeholder="usuario@exemplo.com"
              class="flex-grow-1"
              @keyup.enter="searchUser"
            />

            <VBtn
              :loading="isSearching"
              :disabled="!email.trim()"
              style="margin-block-start: 26px"
              @click="searchUser"
            >
              Buscar
            </VBtn>
          </div>
        </div>

        <!-- Step: found -->
        <div v-else-if="step === 'found'">
          <VCard
            variant="tonal"
            color="primary"
            class="mb-4 pa-3"
          >
            <div class="d-flex align-center gap-3">
              <VIcon icon="tabler-user-check" />
              <div>
                <p class="text-body-1 font-weight-medium mb-0">
                  {{ foundUser?.name }}
                </p>
                <p class="text-body-2 text-disabled mb-0">
                  {{ foundUser?.email }}
                </p>
              </div>
            </div>
          </VCard>

          <AppSelect
            v-model="selectedRoleId"
            label="Role"
            :items="roles"
            item-title="name"
            item-value="id"
            :loading="isLoadingRoles"
            class="mb-4"
          />

          <div class="d-flex align-center justify-center gap-4 mt-2">
            <VBtn
              :loading="isSubmitting"
              :disabled="!selectedRoleId"
              prepend-icon="tabler-mail"
              @click="sendInvite"
            >
              Enviar Convite
            </VBtn>

            <VBtn
              color="secondary"
              variant="tonal"
              :disabled="isSubmitting"
              @click="backToSearch"
            >
              Voltar
            </VBtn>
          </div>
        </div>

        <!-- Step: not-found -->
        <div v-else-if="step === 'not-found'">
          <VCard
            variant="tonal"
            color="warning"
            class="mb-4 pa-3"
          >
            <div class="d-flex align-center gap-3">
              <VIcon icon="tabler-user-question" />
              <p class="text-body-2 mb-0">
                Nenhum usuário encontrado com o e-mail <strong>{{ email }}</strong>. Deseja enviar um convite?
              </p>
            </div>
          </VCard>

          <AppSelect
            v-model="selectedRoleId"
            label="Role que será atribuída ao aceitar o convite"
            :items="roles"
            item-title="name"
            item-value="id"
            :loading="isLoadingRoles"
            class="mb-4"
          />

          <div class="d-flex align-center justify-center gap-4 mt-2">
            <VBtn
              :loading="isSubmitting"
              :disabled="!selectedRoleId"
              prepend-icon="tabler-mail"
              @click="sendInvite"
            >
              Enviar Convite
            </VBtn>

            <VBtn
              color="secondary"
              variant="tonal"
              :disabled="isSubmitting"
              @click="backToSearch"
            >
              Voltar
            </VBtn>
          </div>
        </div>
      </VCardText>
    </VCard>
  </VDialog>
</template>
