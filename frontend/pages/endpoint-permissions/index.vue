<script setup lang="ts">
interface EndpointPermissionResponse {
  id: number
  version: number
  endpoint: string
  name: string
  icon?: string
  sequence?: number
  type: 'API' | 'FRONT_PAGE'
  permittedMethods?: string
  group: string
  createdAt: string
  updatedAt: string
}

definePageMeta({ middleware: 'auth' })

const { error, setError, clearError } = useApiError()
const { isVisible: snackbarVisible, message: snackbarMessage, color: snackbarColor, icon: snackbarIcon, showSuccess, showError } = useSnackbar()

const items = ref<EndpointPermissionResponse[]>([])
const search = shallowRef('')
const page = shallowRef(1)
const itemsPerPage = shallowRef(10)
const isLoading = shallowRef(false)
const isDeleting = shallowRef(false)
const searchVisible = shallowRef(false)

const isAddEditDialogVisible = shallowRef(false)
const isDeleteDialogVisible = shallowRef(false)

const selectedItem = shallowRef<EndpointPermissionResponse | null>(null)

const filteredItems = computed(() => {
  const q = search.value.toLowerCase()

  return items.value.filter(i =>
    i.name.toLowerCase().includes(q) || i.group.toLowerCase().includes(q),
  )
})

const paginatedItems = computed(() => {
  const start = (page.value - 1) * itemsPerPage.value

  return filteredItems.value.slice(start, start + itemsPerPage.value)
})

watch(search, () => {
  page.value = 1
})

onMounted(fetchItems)

async function fetchItems() {
  isLoading.value = true
  clearError()

  try {
    items.value = await $fetch<EndpointPermissionResponse[]>('/api/endpoint-permissions')
  }
  catch (e) {
    setError(e)
  }
  finally {
    isLoading.value = false
  }
}

function openCreate() {
  selectedItem.value = null
  isAddEditDialogVisible.value = true
}

function openEdit(item: EndpointPermissionResponse) {
  selectedItem.value = item
  isAddEditDialogVisible.value = true
}

function openDelete(item: EndpointPermissionResponse) {
  selectedItem.value = item
  isDeleteDialogVisible.value = true
}

async function onDeleteConfirm(confirmed: boolean) {
  if (!confirmed || !selectedItem.value)
    return

  isDeleting.value = true
  clearError()

  try {
    await $fetch(`/api/endpoint-permissions/${selectedItem.value.id}`, { method: 'DELETE' })
    items.value = items.value.filter(i => i.id !== selectedItem.value!.id)
    showSuccess('Permissão excluída com sucesso.')
  }
  catch (e) {
    showError(e)
  }
  finally {
    isDeleting.value = false
    selectedItem.value = null
  }
}

function onItemSaved(saved: EndpointPermissionResponse) {
  const idx = items.value.findIndex(i => i.id === saved.id)

  if (idx >= 0)
    items.value[idx] = saved
  else
    items.value = [saved, ...items.value]

  if (selectedItem.value?.id === saved.id)
    selectedItem.value = saved
}

function toggleSearch() {
  searchVisible.value = !searchVisible.value
  if (!searchVisible.value)
    search.value = ''
}
</script>

<template>
  <div>
    <VCard>
      <VCardText class="d-flex align-center flex-wrap gap-4">
        <h5 class="text-h5">
          Permissões de Endpoint
        </h5>

        <VSpacer />

        <VTextField
          v-model="search"
          placeholder="Buscar por nome ou grupo..."
          density="compact"
          prepend-inner-icon="tabler-search"
          class="d-none d-md-block flex-grow-1"
          style="max-inline-size: 280px"
          hide-details
        />

        <VBtn
          class="d-md-none"
          icon
          variant="text"
          size="small"
          color="default"
          @click="toggleSearch"
        >
          <VIcon :icon="searchVisible ? 'tabler-x' : 'tabler-search'" />
        </VBtn>

        <VBtn
          prepend-icon="tabler-plus"
          @click="openCreate"
        >
          Adicionar Permissão
        </VBtn>

        <VTextField
          v-if="searchVisible"
          v-model="search"
          placeholder="Buscar por nome ou grupo..."
          density="compact"
          prepend-inner-icon="tabler-search"
          class="d-md-none w-100"
          hide-details
          autofocus
        />
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
            <th style="min-width: 200px">Endpoint</th>
            <th style="min-width: 200px">Tipo</th>
            <th style="min-width: 200px">Grupo</th>
            <th style="min-width: 200px">Métodos</th>
            <th>Sequência</th>
            <th class="text-center">
              Ações
            </th>
          </tr>
        </thead>
        <tbody>
          <tr
            v-for="item in paginatedItems"
            :key="item.id"
          >
            <td class="font-weight-medium">
              {{ item.name }}
            </td>
            <td class="text-disabled">
              <code>{{ item.endpoint }}</code>
            </td>
            <td>
              <VChip
                :color="item.type === 'API' ? 'info' : 'success'"
                size="small"
                variant="tonal"
              >
                {{ item.type === 'FRONT_PAGE' ? 'Front Page' : 'API' }}
              </VChip>
            </td>
            <td>{{ item.group }}</td>
            <td class="text-disabled">
              {{ item.permittedMethods ?? '—' }}
            </td>
            <td class="text-disabled">
              {{ item.sequence ?? '—' }}
            </td>
            <td class="text-center" style="white-space: nowrap">
              <VBtn
                icon
                variant="text"
                size="small"
                color="default"
                @click="openEdit(item)"
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
                @click="openDelete(item)"
              >
                <VIcon icon="tabler-trash" />
                <VTooltip activator="parent">
                  Excluir
                </VTooltip>
              </VBtn>
            </td>
          </tr>

          <tr v-if="!isLoading && filteredItems.length === 0">
            <td
              colspan="7"
              class="text-center text-disabled py-8"
            >
              {{ search ? 'Nenhuma permissão encontrada para a busca.' : 'Nenhuma permissão cadastrada.' }}
            </td>
          </tr>
        </tbody>
        </VTable>
      </div>

      <TablePagination
        v-if="filteredItems.length > 0"
        :page="page"
        :items-per-page="itemsPerPage"
        :total-items="filteredItems.length"
        @update:page="page = $event"
      />
    </VCard>

    <AddEditEndpointPermissionDialog
      v-model:is-dialog-visible="isAddEditDialogVisible"
      :item="selectedItem"
      @saved="onItemSaved"
    />

    <ConfirmDialog
      v-model:is-dialog-visible="isDeleteDialogVisible"
      :auto-result="false"
      confirmation-question="Tem certeza que deseja excluir esta permissão? Todos os vínculos de role com esta permissão serão removidos."
      cancel-title="Ação cancelada"
      cancel-msg="A permissão não foi excluída."
      @confirm="onDeleteConfirm"
    />
  </div>
</template>
