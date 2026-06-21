<script setup lang="ts">
interface GroupMenuChildrenResponse {
  id: number
  version: number
  groupMenuId: number
  name: string
  endpoint: string
  icon: string
  createdAt: string
  updatedAt: string
}

interface GroupMenuResponse {
  id: number
  version: number
  name: string
  icon: string
  children: GroupMenuChildrenResponse[]
  createdAt: string
  updatedAt: string
}

const { error, setError, clearError } = useApiError()
const { isVisible: snackbarVisible, message: snackbarMessage, color: snackbarColor, icon: snackbarIcon, showSuccess, showError } = useSnackbar()

const groupMenus = ref<GroupMenuResponse[]>([])
const search = shallowRef('')
const page = shallowRef(1)
const itemsPerPage = shallowRef(10)
const isLoading = shallowRef(false)
const isDeleting = shallowRef(false)

const isAddEditDialogVisible = shallowRef(false)
const isChildrenDialogVisible = shallowRef(false)
const isDeleteDialogVisible = shallowRef(false)

const selectedGroupMenu = shallowRef<GroupMenuResponse | null>(null)

const filteredGroupMenus = computed(() =>
  groupMenus.value.filter(g =>
    g.name.toLowerCase().includes(search.value.toLowerCase()),
  ),
)

const paginatedGroupMenus = computed(() => {
  const start = (page.value - 1) * itemsPerPage.value

  return filteredGroupMenus.value.slice(start, start + itemsPerPage.value)
})

watch(search, () => {
  page.value = 1
})

onMounted(fetchGroupMenus)

async function fetchGroupMenus() {
  isLoading.value = true
  clearError()

  try {
    groupMenus.value = await $fetch<GroupMenuResponse[]>('/api/group-menus')
  }
  catch (e) {
    setError(e)
  }
  finally {
    isLoading.value = false
  }
}

function openCreate() {
  selectedGroupMenu.value = null
  isAddEditDialogVisible.value = true
}

function openEdit(groupMenu: GroupMenuResponse) {
  selectedGroupMenu.value = groupMenu
  isAddEditDialogVisible.value = true
}

function openChildren(groupMenu: GroupMenuResponse) {
  selectedGroupMenu.value = groupMenu
  isChildrenDialogVisible.value = true
}

function openDelete(groupMenu: GroupMenuResponse) {
  selectedGroupMenu.value = groupMenu
  isDeleteDialogVisible.value = true
}

async function onDeleteConfirm(confirmed: boolean) {
  if (!confirmed || !selectedGroupMenu.value) {
    return
  }

  isDeleting.value = true
  clearError()

  try {
    await $fetch(`/api/group-menus/${selectedGroupMenu.value.id}`, { method: 'DELETE' })
    groupMenus.value = groupMenus.value.filter(g => g.id !== selectedGroupMenu.value!.id)
    showSuccess('Group menu excluído com sucesso.')
  }
  catch (e) {
    showError(e)
  }
  finally {
    isDeleting.value = false
    selectedGroupMenu.value = null
  }
}

function onGroupMenuSaved(saved: GroupMenuResponse) {
  const idx = groupMenus.value.findIndex(g => g.id === saved.id)

  if (idx >= 0) {
    groupMenus.value[idx] = { ...saved, children: groupMenus.value[idx].children ?? [] }
  }
  else {
    groupMenus.value = [{ ...saved, children: [] }, ...groupMenus.value]
  }

  if (selectedGroupMenu.value?.id === saved.id) {
    selectedGroupMenu.value = { ...saved, children: selectedGroupMenu.value.children ?? [] }
  }
}

function onChildrenUpdated(updated: GroupMenuResponse) {
  const idx = groupMenus.value.findIndex(g => g.id === updated.id)

  if (idx >= 0) {
    groupMenus.value[idx] = updated
  }

  selectedGroupMenu.value = updated
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
          Group Menus
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
          Adicionar Group Menu
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
            <th style="min-width: 200px">Nome</th>
            <th style="min-width: 200px">Ícone</th>
            <th>Itens</th>
            <th>Criado em</th>
            <th class="text-center">
              Ações
            </th>
          </tr>
        </thead>
        <tbody>
          <tr
            v-for="groupMenu in paginatedGroupMenus"
            :key="groupMenu.id"
          >
            <td class="font-weight-medium">
              {{ groupMenu.name }}
            </td>
            <td>
              <div class="d-flex align-center gap-2">
                <VIcon
                  :icon="groupMenu.icon"
                  size="18"
                />
                <span class="text-disabled text-body-2">{{ groupMenu.icon }}</span>
              </div>
            </td>
            <td>
              <VChip
                size="small"
                variant="tonal"
                color="primary"
              >
                {{ groupMenu.children.length }} {{ groupMenu.children.length === 1 ? 'item' : 'itens' }}
              </VChip>
            </td>
            <td>{{ formatDate(groupMenu.createdAt) }}</td>
            <td class="text-center" style="white-space: nowrap">
              <VBtn
                icon
                variant="text"
                size="small"
                color="default"
                @click="openChildren(groupMenu)"
              >
                <VIcon icon="tabler-list-details" />
                <VTooltip activator="parent">
                  Gerenciar itens
                </VTooltip>
              </VBtn>

              <VBtn
                icon
                variant="text"
                size="small"
                color="default"
                @click="openEdit(groupMenu)"
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
                @click="openDelete(groupMenu)"
              >
                <VIcon icon="tabler-trash" />
                <VTooltip activator="parent">
                  Excluir
                </VTooltip>
              </VBtn>
            </td>
          </tr>

          <tr v-if="!isLoading && filteredGroupMenus.length === 0">
            <td
              colspan="5"
              class="text-center text-disabled py-8"
            >
              {{ search ? 'Nenhum group menu encontrado para a busca.' : 'Nenhum group menu cadastrado.' }}
            </td>
          </tr>
        </tbody>
        </VTable>
      </div>

      <TablePagination
        v-if="filteredGroupMenus.length > 0"
        :page="page"
        :items-per-page="itemsPerPage"
        :total-items="filteredGroupMenus.length"
        @update:page="page = $event"
      />
    </VCard>

    <AddEditGroupMenuDialog
      v-model:is-dialog-visible="isAddEditDialogVisible"
      :group-menu="selectedGroupMenu"
      @saved="onGroupMenuSaved"
    />

    <ManageGroupMenuChildrenDialog
      v-model:is-dialog-visible="isChildrenDialogVisible"
      :group-menu="selectedGroupMenu"
      @updated="onChildrenUpdated"
    />

    <ConfirmDialog
      v-model:is-dialog-visible="isDeleteDialogVisible"
      :auto-result="false"
      confirmation-question="Tem certeza que deseja excluir este group menu? Todos os itens vinculados também serão removidos."
      cancel-title="Ação cancelada"
      cancel-msg="O group menu não foi excluído."
      @confirm="onDeleteConfirm"
    />
  </div>
</template>
