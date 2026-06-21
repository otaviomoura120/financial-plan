<script setup lang="ts">
interface RoleResponse {
  id: number
  version: number
  spaceId: number
  name: string
  description?: string
  createdAt: string
  updatedAt: string
}

const spaceStore = useSpaceStore()
const { error, setError, clearError } = useApiError()
const { isVisible: snackbarVisible, message: snackbarMessage, color: snackbarColor, icon: snackbarIcon, showSuccess, showError } = useSnackbar()

const roles = ref<RoleResponse[]>([])
const search = shallowRef('')
const page = shallowRef(1)
const itemsPerPage = shallowRef(10)
const isLoading = shallowRef(false)
const isDeleting = shallowRef(false)

const isAddEditDialogVisible = shallowRef(false)
const isPermissionsDialogVisible = shallowRef(false)
const isDeleteDialogVisible = shallowRef(false)

const selectedRole = shallowRef<RoleResponse | null>(null)
const permissionsRoleId = shallowRef<number | null>(null)
const permissionsRoleName = shallowRef('')

const filteredRoles = computed(() =>
  roles.value.filter(r =>
    r.name.toLowerCase().includes(search.value.toLowerCase()),
  ),
)

const paginatedRoles = computed(() => {
  const start = (page.value - 1) * itemsPerPage.value

  return filteredRoles.value.slice(start, start + itemsPerPage.value)
})

watch(
  () => spaceStore.activeSpace,
  async (space) => {
    if (space) {
      await fetchRoles()
    }
    else {
      roles.value = []
    }
  },
  { immediate: true },
)

watch(search, () => {
  page.value = 1
})

async function fetchRoles() {
  if (!spaceStore.activeSpace)
    return

  isLoading.value = true
  clearError()

  try {
    roles.value = await $fetch<RoleResponse[]>('/api/roles', {
      query: { spaceId: spaceStore.activeSpace.id },
    })
  }
  catch (e) {
    setError(e)
  }
  finally {
    isLoading.value = false
  }
}

function openCreate() {
  selectedRole.value = null
  isAddEditDialogVisible.value = true
}

function openEdit(role: RoleResponse) {
  selectedRole.value = role
  isAddEditDialogVisible.value = true
}

function openPermissions(role: RoleResponse) {
  permissionsRoleId.value = role.id
  permissionsRoleName.value = role.name
  isPermissionsDialogVisible.value = true
}

function openDelete(role: RoleResponse) {
  selectedRole.value = role
  isDeleteDialogVisible.value = true
}

async function onDeleteConfirm(confirmed: boolean) {
  if (!confirmed || !selectedRole.value)
    return

  isDeleting.value = true
  clearError()

  try {
    await $fetch(`/api/roles/${selectedRole.value.id}`, { method: 'DELETE' })
    roles.value = roles.value.filter(r => r.id !== selectedRole.value!.id)
    showSuccess('Role excluída com sucesso.')
  }
  catch (e) {
    showError(e)
  }
  finally {
    isDeleting.value = false
    selectedRole.value = null
  }
}

function onRoleSaved(saved: RoleResponse) {
  const idx = roles.value.findIndex(r => r.id === saved.id)

  if (idx >= 0)
    roles.value[idx] = saved
  else
    roles.value = [saved, ...roles.value]

  if (selectedRole.value?.id === saved.id)
    selectedRole.value = saved
}

function formatDate(iso: string) {
  return new Date(iso).toLocaleDateString('pt-BR')
}
</script>

<template>
  <div>
    <VCard>
      <VCardText class="d-flex align-center flex-wrap gap-4">
        <h5 class="text-h5">
          Roles
        </h5>

        <VSpacer />

        <VTextField
          v-model="search"
          placeholder="Buscar por nome..."
          density="compact"
          prepend-inner-icon="tabler-search"
          class="flex-grow-1"
          style="max-inline-size: 280px"
          hide-details
        />

        <VBtn
          prepend-icon="tabler-plus"
          @click="openCreate"
        >
          Adicionar Role
        </VBtn>
      </VCardText>

      <VDivider />

      <ApiErrorAlert
        v-if="error"
        :error="error"
        class="ma-4"
      />

      <VSnackbar
        v-model="snackbarVisible"
        :color="snackbarColor"
        :timeout="3000"
      >
        <div class="d-flex align-center gap-2">
          <VIcon :icon="snackbarIcon" />
          {{ snackbarMessage }}
        </div>
      </VSnackbar>

      <div
        v-if="isLoading"
        class="d-flex justify-center py-10"
      >
        <VProgressCircular indeterminate />
      </div>

      <div v-else style="overflow-x: auto">
        <VTable>
        <thead style="white-space: nowrap">
          <tr>
            <th>Nome</th>
            <th style="min-width: 200px">Descrição</th>
            <th>Criado em</th>
            <th class="text-center">
              Ações
            </th>
          </tr>
        </thead>
        <tbody>
          <tr
            v-for="role in paginatedRoles"
            :key="role.id"
          >
            <td>
              <div class="d-flex align-center gap-2">
                <span class="font-weight-medium">{{ role.name }}</span>
                <VChip
                  v-if="role.name === 'OWNER'"
                  color="warning"
                  size="x-small"
                  variant="tonal"
                >
                  Especial
                </VChip>
              </div>
            </td>
            <td class="text-disabled">
              {{ role.description ?? '—' }}
            </td>
            <td>{{ formatDate(role.createdAt) }}</td>
            <td class="text-center" style="white-space: nowrap">
              <VBtn
                icon
                variant="text"
                size="small"
                color="default"
                @click="openPermissions(role)"
              >
                <VIcon icon="tabler-shield-check" />
                <VTooltip activator="parent">
                  Gerenciar permissões
                </VTooltip>
              </VBtn>

              <VBtn
                icon
                variant="text"
                size="small"
                color="default"
                @click="openEdit(role)"
              >
                <VIcon icon="tabler-pencil" />
                <VTooltip activator="parent">
                  Editar
                </VTooltip>
              </VBtn>

              <VBtn
                icon
                variant="text"
                size="small"
                color="error"
                :disabled="role.name === 'OWNER'"
                @click="openDelete(role)"
              >
                <VIcon icon="tabler-trash" />
                <VTooltip activator="parent">
                  {{ role.name === 'OWNER' ? 'Role especial não pode ser excluída' : 'Excluir' }}
                </VTooltip>
              </VBtn>
            </td>
          </tr>

          <tr v-if="!isLoading && filteredRoles.length === 0">
            <td
              colspan="4"
              class="text-center text-disabled py-8"
            >
              {{ search ? 'Nenhuma role encontrada para a busca.' : 'Nenhuma role cadastrada.' }}
            </td>
          </tr>
        </tbody>
        </VTable>
      </div>

      <TablePagination
        v-if="filteredRoles.length > 0"
        :page="page"
        :items-per-page="itemsPerPage"
        :total-items="filteredRoles.length"
        @update:page="page = $event"
      />
    </VCard>

    <AddEditRoleDialog
      v-model:is-dialog-visible="isAddEditDialogVisible"
      :role="selectedRole"
      @saved="onRoleSaved"
    />

    <RolePermissionsDialog
      v-model:is-dialog-visible="isPermissionsDialogVisible"
      :role-id="permissionsRoleId"
      :role-name="permissionsRoleName"
    />

    <ConfirmDialog
      v-model:is-dialog-visible="isDeleteDialogVisible"
      :auto-result="false"
      confirmation-question="Tem certeza que deseja excluir esta role? Todos os usuários com esta role perderão o acesso associado."
      cancel-title="Ação cancelada"
      cancel-msg="A role não foi excluída."
      @confirm="onDeleteConfirm"
    />
  </div>
</template>
