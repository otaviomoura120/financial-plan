<script setup lang="ts">
definePageMeta({ middleware: 'auth' })

interface SpaceResponse {
  id: number
  version: number
  name: string
  description?: string
  createdDate: string
  currentUserRoleName: string | null
}

const spaceStore = useSpaceStore()
const { error, setError, clearError } = useApiError()
const { isVisible: snackbarVisible, message: snackbarMessage, color: snackbarColor, icon: snackbarIcon, showSuccess, showError } = useSnackbar()

const spaces = ref<SpaceResponse[]>([])
const search = shallowRef('')
const page = shallowRef(1)
const itemsPerPage = shallowRef(10)
const isLoading = shallowRef(false)
const isDeleting = shallowRef(false)
const searchVisible = shallowRef(false)

const isAddEditDialogVisible = shallowRef(false)
const isDeleteDialogVisible = shallowRef(false)
const selectedSpace = shallowRef<SpaceResponse | null>(null)

const filteredSpaces = computed(() =>
  spaces.value.filter(s =>
    s.name.toLowerCase().includes(search.value.toLowerCase())
    || (s.description ?? '').toLowerCase().includes(search.value.toLowerCase()),
  ),
)

const paginatedSpaces = computed(() => {
  const start = (page.value - 1) * itemsPerPage.value

  return filteredSpaces.value.slice(start, start + itemsPerPage.value)
})

watch(search, () => {
  page.value = 1
})

watch(() => spaceStore.dbUser,
  async () => {
    console.log('active space changed')
    await fetchSpaces()
  }, { immediate: true },
)

async function fetchSpaces() {
  if (!spaceStore.dbUser)
    return

  isLoading.value = true
  clearError()

  try {
    spaces.value = await $fetch<SpaceResponse[]>('/api/spaces', {
      query: { userId: spaceStore.dbUser.id },
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
  selectedSpace.value = null
  isAddEditDialogVisible.value = true
}

function openEdit(space: SpaceResponse) {
  selectedSpace.value = space
  isAddEditDialogVisible.value = true
}

function openDelete(space: SpaceResponse) {
  selectedSpace.value = space
  isDeleteDialogVisible.value = true
}

function setActive(space: SpaceResponse) {
  spaceStore.setActiveSpace({ id: space.id, name: space.name, description: space.description })
}

async function onDeleteConfirm(confirmed: boolean) {
  if (!confirmed || !selectedSpace.value)
    return

  isDeleting.value = true
  clearError()

  const deletedId = selectedSpace.value.id

  try {
    await $fetch(`/api/spaces/${deletedId}`, { method: 'DELETE' })
    spaces.value = spaces.value.filter(s => s.id !== deletedId)
    spaceStore.setAvailableSpaces(spaces.value.map(s => ({ id: s.id, name: s.name, description: s.description })))

    if (spaceStore.activeSpace?.id === deletedId && spaces.value.length > 0)
      spaceStore.setActiveSpace(spaces.value[0])

    showSuccess('Espaço excluído com sucesso.')
  }
  catch (e) {
    showError(e)
  }
  finally {
    isDeleting.value = false
    selectedSpace.value = null
  }
}

function onSpaceSaved(saved: SpaceResponse) {
  const idx = spaces.value.findIndex(s => s.id === saved.id)

  if (idx >= 0)
    spaces.value[idx] = saved
  else
    spaces.value = [saved, ...spaces.value]

  if (selectedSpace.value?.id === saved.id)
    selectedSpace.value = saved

  spaceStore.setAvailableSpaces(spaces.value.map(s => ({ id: s.id, name: s.name, description: s.description })))

  if (spaceStore.activeSpace?.id === saved.id)
    spaceStore.setActiveSpace({ id: saved.id, name: saved.name, description: saved.description })
}

function toggleSearch() {
  searchVisible.value = !searchVisible.value
  if (!searchVisible.value)
    search.value = ''
}

function formatDate(iso: string) {
  return new Date(iso).toLocaleDateString('pt-BR')
}
</script>

<template>
  <div>
    <VCard>
      <VCardText class="d-flex align-center flex-wrap gap-4">
        <h5
          class="text-h5 text-truncate"
          style="min-inline-size: 0"
        >
          Espaços
        </h5>

        <VSpacer />

        <VTextField
          v-model="search"
          placeholder="Buscar por nome ou descrição..."
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
          <span class="d-none d-sm-inline">Novo Espaço</span>
        </VBtn>

        <VTextField
          v-if="searchVisible"
          v-model="search"
          placeholder="Buscar por nome ou descrição..."
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

      <div
        v-else
        style="overflow-x: auto"
      >
        <VTable>
          <thead style="white-space: nowrap">
            <tr>
              <th style="min-width: 300px">
                Nome
              </th>
              <th style="min-width: 200px">
                Descrição
              </th>
              <th>Sua role</th>
              <th>Criado em</th>
              <th class="text-center">
                Ações
              </th>
            </tr>
          </thead>
          <tbody>
            <tr
              v-for="space in paginatedSpaces"
              :key="space.id"
            >
              <td>
                <div class="d-flex align-center gap-2">
                  <span class="font-weight-medium">{{ space.name }}</span>
                  <VChip
                    v-if="spaceStore.activeSpace?.id === space.id"
                    color="success"
                    size="x-small"
                    variant="tonal"
                  >
                    Ativo
                  </VChip>
                </div>
              </td>
              <td class="text-disabled">
                {{ space.description ?? '—' }}
              </td>
              <td>
                <VChip
                  v-if="space.currentUserRoleName"
                  :color="space.currentUserRoleName === 'OWNER' ? 'warning' : 'primary'"
                  size="small"
                  variant="tonal"
                >
                  {{ space.currentUserRoleName }}
                </VChip>
                <span
                  v-else
                  class="text-disabled"
                >—</span>
              </td>
              <td>{{ formatDate(space.createdDate) }}</td>
              <td
                class="text-center"
                style="white-space: nowrap"
              >
                <VBtn
                  icon
                  variant="text"
                  size="small"
                  color="default"
                  :disabled="spaceStore.activeSpace?.id === space.id"
                  @click="setActive(space)"
                >
                  <VIcon icon="tabler-circle-check" />
                  <VTooltip activator="parent">
                    {{ spaceStore.activeSpace?.id === space.id ? 'Espaço já ativo' : 'Definir como ativo' }}
                  </VTooltip>
                </VBtn>

                <VBtn
                  icon
                  variant="text"
                  size="small"
                  color="default"
                  :disabled="space.currentUserRoleName !== 'OWNER'"
                  @click="openEdit(space)"
                >
                  <VIcon icon="tabler-pencil" />
                  <VTooltip activator="parent">
                    {{ space.currentUserRoleName !== 'OWNER' ? 'Apenas o OWNER pode editar' : 'Editar espaço' }}
                  </VTooltip>
                </VBtn>

                <VBtn
                  icon
                  variant="text"
                  size="small"
                  color="error"
                  :disabled="space.currentUserRoleName !== 'OWNER'"
                  @click="openDelete(space)"
                >
                  <VIcon icon="tabler-trash" />
                  <VTooltip activator="parent">
                    {{ space.currentUserRoleName !== 'OWNER' ? 'Apenas o OWNER pode excluir' : 'Excluir espaço' }}
                  </VTooltip>
                </VBtn>
              </td>
            </tr>

            <tr v-if="!isLoading && filteredSpaces.length === 0">
              <td
                colspan="5"
                class="text-center text-disabled py-8"
              >
                {{ search ? 'Nenhum espaço encontrado para a busca.' : 'Nenhum espaço cadastrado.' }}
              </td>
            </tr>
          </tbody>
        </VTable>
      </div>

      <TablePagination
        v-if="filteredSpaces.length > 0"
        :page="page"
        :items-per-page="itemsPerPage"
        :total-items="filteredSpaces.length"
        @update:page="page = $event"
      />
    </VCard>

    <AddEditSpaceDialog
      v-model:is-dialog-visible="isAddEditDialogVisible"
      :space="selectedSpace"
      @saved="onSpaceSaved"
    />

    <ConfirmDialog
      v-model:is-dialog-visible="isDeleteDialogVisible"
      :auto-result="false"
      confirmation-question="Tem certeza que deseja excluir este espaço? Todos os membros, roles e dados associados serão removidos permanentemente."
      cancel-title="Ação cancelada"
      cancel-msg="O espaço não foi excluído."
      @confirm="onDeleteConfirm"
    />
  </div>
</template>
