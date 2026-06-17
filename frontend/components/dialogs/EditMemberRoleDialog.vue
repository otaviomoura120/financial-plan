<script setup lang="ts">
interface SpaceMemberResponse {
  memberId: number
  version: number
  userId: number
  userName: string
  userEmail: string
  roleId: number
  roleName: string
  joinedAt: string
}

interface RoleOption {
  id: number
  name: string
}

interface Props {
  isDialogVisible: boolean
  member?: SpaceMemberResponse | null
}

interface Emit {
  (e: 'update:isDialogVisible', value: boolean): void
  (e: 'saved', member: SpaceMemberResponse): void
}

const props = withDefaults(defineProps<Props>(), {
  member: null,
})

const emit = defineEmits<Emit>()

const spaceStore = useSpaceStore()
const { error, setError, clearError } = useApiError()

const selectedRoleId = shallowRef<number | null>(null)
const roles = ref<RoleOption[]>([])
const isLoadingRoles = shallowRef(false)
const isSaving = shallowRef(false)

const roleRules = [(v: number | null) => !!v || 'Role é obrigatória']

watch(
  () => props.isDialogVisible,
  async visible => {
    if (visible && props.member) {
      selectedRoleId.value = props.member.roleId
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

async function onSave() {
  if (!selectedRoleId.value || !props.member || !spaceStore.activeSpace)
    return

  isSaving.value = true
  clearError()

  try {
    const updated = await $fetch<SpaceMemberResponse>(
      `/api/spaces/${spaceStore.activeSpace.id}/members/${props.member.userId}`,
      {
        method: 'PUT',
        body: { version: props.member.version, roleId: selectedRoleId.value },
      },
    )

    emit('saved', updated)
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
    :width="$vuetify.display.smAndDown ? 'auto' : 500"
    :model-value="props.isDialogVisible"
    @update:model-value="onClose"
  >
    <DialogCloseBtn @click="onClose" />

    <VCard class="pa-sm-10 pa-4">
      <VCardText>
        <h4 class="text-h4 text-center mb-2">
          Alterar Role
        </h4>

        <p class="text-body-1 text-center font-weight-medium mb-0">
          {{ props.member?.userName }}
        </p>

        <p class="text-body-2 text-center text-disabled mb-6">
          {{ props.member?.userEmail }}
        </p>

        <ApiErrorAlert
          v-if="error"
          :error="error"
          class="mb-4"
        />

        <div class="d-flex flex-column gap-4">
          <AppSelect
            v-model="selectedRoleId"
            label="Role"
            :items="roles"
            item-title="name"
            item-value="id"
            :loading="isLoadingRoles"
            :rules="roleRules"
          />
        </div>

        <div class="d-flex align-center justify-center gap-4 mt-6">
          <VBtn
            :loading="isSaving"
            :disabled="isLoadingRoles"
            @click="onSave"
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
      </VCardText>
    </VCard>
  </VDialog>
</template>
