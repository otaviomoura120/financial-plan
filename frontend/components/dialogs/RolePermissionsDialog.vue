<script setup lang="ts">
interface RoleEndpointPermissionResponse {
  id: number
  version: number
  endpointPermissionId: number
  name: string
  endpoint: string
  type: 'API' | 'FRONT_PAGE'
  permission: 'ALLOW' | 'DENY'
}

interface Props {
  isDialogVisible: boolean
  roleId: number | null
  roleName: string
}

interface Emit {
  (e: 'update:isDialogVisible', value: boolean): void
}

const props = defineProps<Props>()
const emit = defineEmits<Emit>()

const { error, setError, clearError } = useApiError()

const permissions = ref<RoleEndpointPermissionResponse[]>([])
const isLoading = shallowRef(false)
const updatingId = shallowRef<number | null>(null)

watch(
  () => props.isDialogVisible,
  async (visible) => {
    if (visible && props.roleId !== null) {
      await fetchPermissions()
    }
    else {
      permissions.value = []
      clearError()
    }
  },
)

async function fetchPermissions() {
  isLoading.value = true
  clearError()

  try {
    permissions.value = await $fetch<RoleEndpointPermissionResponse[]>(
      `/api/roles/${props.roleId}/permissions`,
    )
  }
  catch (e) {
    setError(e)
  }
  finally {
    isLoading.value = false
  }
}

async function togglePermission(perm: RoleEndpointPermissionResponse) {
  const newAccess = perm.permission === 'ALLOW' ? 'DENY' : 'ALLOW'

  updatingId.value = perm.id

  try {
    await $fetch(`/api/roles/${props.roleId}/permissions/${perm.id}`, {
      method: 'PATCH',
      body: { version: perm.version, access: newAccess },
    })

    const target = permissions.value.find(p => p.id === perm.id)

    if (target) {
      target.permission = newAccess
      target.version += 1
    }
  }
  catch (e) {
    setError(e)
  }
  finally {
    updatingId.value = null
  }
}

function onClose() {
  emit('update:isDialogVisible', false)
}

const typeLabel: Record<string, string> = {
  API: 'API',
  FRONT_PAGE: 'Página',
}
</script>

<template>
  <VDialog
    :width="$vuetify.display.smAndDown ? 'auto' : 900"
    :model-value="props.isDialogVisible"
    @update:model-value="onClose"
  >
    <DialogCloseBtn @click="onClose" />

    <VCard class="pa-sm-10 pa-4">
      <VCardText>
        <h4 class="text-h4 text-center mb-2">
          Permissões da Role
        </h4>
        <p class="text-body-1 text-center mb-6">
          {{ props.roleName }}
        </p>

        <ApiErrorAlert
          v-if="error"
          :error="error"
          class="mb-4"
        />

        <div
          v-if="isLoading"
          class="d-flex justify-center py-8"
        >
          <VProgressCircular indeterminate />
        </div>

        <VTable
          v-else
          class="text-no-wrap"
        >
          <thead>
            <tr>
              <th>Nome</th>
              <th>Tipo</th>
              <th>Endpoint</th>
              <th class="text-center">
                Acesso
              </th>
            </tr>
          </thead>
          <tbody>
            <tr
              v-for="perm in permissions"
              :key="perm.id"
            >
              <td>{{ perm.name }}</td>
              <td>
                <VChip
                  :color="perm.type === 'API' ? 'primary' : 'secondary'"
                  size="small"
                  variant="tonal"
                >
                  {{ typeLabel[perm.type] ?? perm.type }}
                </VChip>
              </td>
              <td class="text-body-2 text-disabled">
                {{ perm.endpoint }}
              </td>
              <td class="text-center">
                <div class="d-flex align-center justify-center gap-2">
                  <VSwitch
                    :model-value="perm.permission === 'ALLOW'"
                    :loading="updatingId === perm.id"
                    :disabled="updatingId !== null"
                    color="success"
                    hide-details
                    @update:model-value="togglePermission(perm)"
                  />
                  <span :class="perm.permission === 'ALLOW' ? 'text-success' : 'text-disabled'">
                    {{ perm.permission === 'ALLOW' ? 'Permitido' : 'Negado' }}
                  </span>
                </div>
              </td>
            </tr>
            <tr v-if="!isLoading && permissions.length === 0">
              <td
                colspan="4"
                class="text-center text-disabled py-6"
              >
                Nenhuma permissão encontrada.
              </td>
            </tr>
          </tbody>
        </VTable>

        <div class="d-flex justify-end mt-6">
          <VBtn
            color="secondary"
            variant="tonal"
            @click="onClose"
          >
            Fechar
          </VBtn>
        </div>
      </VCardText>
    </VCard>
  </VDialog>
</template>
