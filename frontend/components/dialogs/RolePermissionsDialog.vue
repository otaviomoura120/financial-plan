<script setup lang="ts">
interface RoleEndpointPermissionResponse {
  id: number
  version: number
  endpointPermissionId: number
  name: string
  endpoint: string
  type: 'API' | 'FRONT_PAGE'
  group: string
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

const groupedPermissions = computed(() => {
  const map = new Map<string, RoleEndpointPermissionResponse[]>()

  for (const perm of permissions.value) {
    const list = map.get(perm.group) ?? []

    list.push(perm)
    map.set(perm.group, list)
  }

  return map
})

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
    await $fetch(`/api/roles/${props.roleId}/permissions/${perm.endpointPermissionId}`, {
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

const typeLabel: Record<'API' | 'FRONT_PAGE', string> = {
  API: 'API',
  FRONT_PAGE: 'Página',
}
</script>

<template>
  <VDialog
    :width="$vuetify.display.smAndDown ? 'auto' : 960"
    :model-value="props.isDialogVisible"
    scrollable
    @update:model-value="onClose"
  >
    <DialogCloseBtn @click="onClose" />

    <VCard>
      <VCardItem class="pa-6 pb-4">
        <VCardTitle class="text-h5 text-center">
          Permissões da Role
        </VCardTitle>
        <VCardSubtitle class="text-center mt-1">
          {{ props.roleName }}
        </VCardSubtitle>
      </VCardItem>

      <VDivider />

      <VCardText class="pa-0 permissions-scroll">
        <ApiErrorAlert
          v-if="error"
          :error="error"
          class="ma-4"
        />

        <div
          v-if="isLoading"
          class="d-flex justify-center py-12"
        >
          <VProgressCircular indeterminate />
        </div>

        <VTable
          v-else-if="groupedPermissions.size > 0"
          fixed-header
          class="permissions-table"
        >
          <colgroup>
            <col style="inline-size: 28%">
            <col style="inline-size: 80px">
            <col>
            <col style="inline-size: 150px">
          </colgroup>

          <thead>
            <tr>
              <th class="text-left ps-4">
                Nome
              </th>
              <th class="text-left">
                Tipo
              </th>
              <th class="text-left">
                Endpoint
              </th>
              <th class="text-center">
                Acesso
              </th>
            </tr>
          </thead>

          <tbody>
            <template
              v-for="[group, items] in groupedPermissions"
              :key="group"
            >
              <tr class="group-header-row">
                <td colspan="4" class="ps-4">
                  <span class="group-label text-primary">{{ group }}</span>
                </td>
              </tr>

              <tr
                v-for="perm in items"
                :key="perm.id"
                class="permission-row"
              >
                <td class="ps-4 py-3">
                  <span class="text-body-2 font-weight-medium">{{ perm.name }}</span>
                </td>
                <td class="py-3">
                  <VChip
                    :color="perm.type === 'API' ? 'primary' : 'secondary'"
                    size="x-small"
                    variant="tonal"
                    label
                  >
                    {{ typeLabel[perm.type] }}
                  </VChip>
                </td>
                <td class="py-3">
                  <code class="text-caption text-disabled">{{ perm.endpoint }}</code>
                </td>
                <td class="py-3">
                  <div class="d-flex align-center justify-center gap-2">
                    <VSwitch
                      :model-value="perm.permission === 'ALLOW'"
                      :loading="updatingId === perm.id"
                      :disabled="updatingId !== null"
                      :aria-label="`Acesso para ${perm.name}`"
                      color="success"
                      hide-details
                      density="compact"
                      @update:model-value="togglePermission(perm)"
                    />
                    <span
                      class="text-caption access-label"
                      :class="perm.permission === 'ALLOW' ? 'text-success' : 'text-disabled'"
                    >
                      {{ perm.permission === 'ALLOW' ? 'Permitido' : 'Negado' }}
                    </span>
                  </div>
                </td>
              </tr>
            </template>
          </tbody>
        </VTable>

        <p
          v-else-if="!error"
          class="text-center text-disabled py-10"
        >
          Nenhuma permissão encontrada.
        </p>
      </VCardText>

      <VDivider />

      <VCardActions class="pa-4 justify-end">
        <VBtn
          color="secondary"
          variant="tonal"
          @click="onClose"
        >
          Fechar
        </VBtn>
      </VCardActions>
    </VCard>
  </VDialog>
</template>

<style scoped>
.permissions-scroll {
  max-block-size: 65vh;
  overflow-y: auto;
}

.permissions-table :deep(table) {
  border-collapse: collapse;
}

.permissions-table :deep(.v-table__wrapper) {
  overflow: visible;
}

.permissions-table :deep(thead th) {
  background-color: rgb(var(--v-theme-surface));
}

.group-header-row td {
  padding-block: 10px;
  border-block-end: none;
}


.group-label {
  font-size: 0.6875rem;
  font-weight: 700;
  letter-spacing: 0.09em;
  text-transform: uppercase;
}

.group-header-row:not(:first-child) td {
  padding-top: 24px;
}

.permission-row td {
  border-block-end: 1px solid rgba(var(--v-border-color), var(--v-border-opacity));
}

.permission-row:last-child td {
  border-block-end: none;
}

.access-label {
  min-inline-size: 52px;
}
</style>
