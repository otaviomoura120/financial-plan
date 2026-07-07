<script setup lang="ts">
interface SubCategoryResponse {
  id: number
  version: number
  categoryId: number
  name: string
  active: boolean
}

interface CategoryResponse {
  id: number
  version: number
  name: string
  active: boolean
  subCategories: SubCategoryResponse[]
}

const spaceStore = useSpaceStore()
const { error, setError, clearError } = useApiError()
const { isVisible: snackbarVisible, message: snackbarMessage, color: snackbarColor, icon: snackbarIcon, showSuccess, showError } = useSnackbar()

const categories = ref<CategoryResponse[]>([])
const search = shallowRef('')
const page = shallowRef(1)
const itemsPerPage = shallowRef(10)
const isLoading = shallowRef(false)
const isDeleting = shallowRef(false)
const searchVisible = shallowRef(false)

const isAddEditDialogVisible = shallowRef(false)
const isSubCategoriesDialogVisible = shallowRef(false)
const isDeleteDialogVisible = shallowRef(false)
const isStatusDialogVisible = shallowRef(false)
const isTogglingStatus = shallowRef(false)

const selectedCategory = shallowRef<CategoryResponse | null>(null)
const subCategoriesTargetId = shallowRef<number | null>(null)
const subCategoriesTargetName = shallowRef('')
const subCategoriesTargetList = shallowRef<SubCategoryResponse[]>([])

const filteredCategories = computed(() =>
  categories.value.filter(c =>
    c.name.toLowerCase().includes(search.value.toLowerCase()),
  ),
)

const paginatedCategories = computed(() => {
  const start = (page.value - 1) * itemsPerPage.value

  return filteredCategories.value.slice(start, start + itemsPerPage.value)
})

watch(
  () => spaceStore.activeSpace,
  async space => {
    if (space)
      await fetchCategories()

    else
      categories.value = []
  },
  { immediate: true },
)

watch(search, () => {
  page.value = 1
})

async function fetchCategories() {
  if (!spaceStore.activeSpace)
    return

  isLoading.value = true
  clearError()

  try {
    categories.value = await $fetch<CategoryResponse[]>('/api/categories', {
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
  selectedCategory.value = null
  isAddEditDialogVisible.value = true
}

function openEdit(category: CategoryResponse) {
  selectedCategory.value = category
  isAddEditDialogVisible.value = true
}

function openSubCategories(category: CategoryResponse) {
  subCategoriesTargetId.value = category.id
  subCategoriesTargetName.value = category.name
  subCategoriesTargetList.value = category.subCategories
  isSubCategoriesDialogVisible.value = true
}

function onSubCategoriesUpdated(subCategories: SubCategoryResponse[]) {
  const idx = categories.value.findIndex(c => c.id === subCategoriesTargetId.value)

  if (idx >= 0)
    categories.value[idx] = { ...categories.value[idx], subCategories }
}

function openDelete(category: CategoryResponse) {
  selectedCategory.value = category
  isDeleteDialogVisible.value = true
}

async function onDeleteConfirm(confirmed: boolean) {
  if (!confirmed || !selectedCategory.value)
    return

  isDeleting.value = true
  clearError()

  try {
    await $fetch(`/api/categories/${selectedCategory.value.id}`, { method: 'DELETE' })

    categories.value = categories.value.filter(c => c.id !== selectedCategory.value!.id)

    showSuccess('Categoria excluída com sucesso.')
  }
  catch (e) {
    showError(e)
  }
  finally {
    isDeleting.value = false
    selectedCategory.value = null
  }
}

function openToggleStatus(category: CategoryResponse) {
  selectedCategory.value = category
  isStatusDialogVisible.value = true
}

async function onToggleStatusConfirm(confirmed: boolean) {
  if (!confirmed || !selectedCategory.value)
    return

  const target = selectedCategory.value
  const nextActive = !target.active

  isTogglingStatus.value = true
  clearError()

  try {
    // A resposta sempre traz subCategories: [] (UpdateCategoryStatusService não popula esse
    // campo, mesma limitação documentada para o PUT em AddEditCategoryDialog.vue) — preserva
    // a lista local de subcategorias em vez de sobrescrever com o array vazio do backend.
    const updated = await $fetch<CategoryResponse>(`/api/categories/${target.id}/status`, {
      method: 'PATCH',
      body: { active: nextActive },
    })

    const idx = categories.value.findIndex(c => c.id === target.id)

    if (idx >= 0)
      categories.value[idx] = { ...updated, subCategories: categories.value[idx].subCategories }

    showSuccess(nextActive ? 'Categoria ativada com sucesso.' : 'Categoria inativada com sucesso.')
  }
  catch (e) {
    showError(e)
  }
  finally {
    isTogglingStatus.value = false
    selectedCategory.value = null
  }
}

function onCategorySaved(saved: CategoryResponse) {
  const idx = categories.value.findIndex(c => c.id === saved.id)

  if (idx >= 0)
    categories.value[idx] = saved
  else
    categories.value = [{ ...saved, subCategories: saved.subCategories ?? [] }, ...categories.value]

  if (selectedCategory.value?.id === saved.id)
    selectedCategory.value = saved
}

function toggleSearch() {
  searchVisible.value = !searchVisible.value
  if (!searchVisible.value)
    search.value = ''
}

function activeSubCategoriesCount(category: CategoryResponse) {
  return category.subCategories.filter(sc => sc.active).length
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
          Categorias
        </h5>

        <VSpacer />

        <VTextField
          v-model="search"
          placeholder="Buscar por nome..."
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
          <span class="d-none d-sm-inline">Adicionar Categoria</span>
        </VBtn>

        <VTextField
          v-if="searchVisible"
          v-model="search"
          placeholder="Buscar por nome..."
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
              <th>Nome</th>
              <th>Subcategorias</th>
              <th>Status</th>
              <th class="text-center">
                Ações
              </th>
            </tr>
          </thead>
          <tbody>
            <tr
              v-for="category in paginatedCategories"
              :key="category.id"
            >
              <td>
                <span class="font-weight-medium">{{ category.name }}</span>
              </td>
              <td class="text-disabled">
                {{ activeSubCategoriesCount(category) }}
              </td>
              <td>
                <VChip
                  :color="category.active ? 'success' : 'secondary'"
                  size="small"
                  variant="tonal"
                >
                  {{ category.active ? 'Ativo' : 'Inativo' }}
                </VChip>
              </td>
              <td
                class="text-center"
                style="white-space: nowrap"
              >
                <VBtn
                  icon
                  variant="text"
                  size="small"
                  color="default"
                  @click="openSubCategories(category)"
                >
                  <VIcon icon="tabler-list-details" />
                  <VTooltip activator="parent">
                    Gerenciar subcategorias
                  </VTooltip>
                </VBtn>

                <VBtn
                  icon
                  variant="text"
                  size="small"
                  color="default"
                  @click="openEdit(category)"
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
                  :color="category.active ? 'secondary' : 'success'"
                  @click="openToggleStatus(category)"
                >
                  <VIcon :icon="category.active ? 'tabler-toggle-right' : 'tabler-toggle-left'" />
                  <VTooltip activator="parent">
                    {{ category.active ? 'Inativar' : 'Ativar' }}
                  </VTooltip>
                </VBtn>

                <VBtn
                  icon
                  variant="text"
                  size="small"
                  color="error"
                  @click="openDelete(category)"
                >
                  <VIcon icon="tabler-trash" />
                  <VTooltip activator="parent">
                    Excluir definitivamente
                  </VTooltip>
                </VBtn>
              </td>
            </tr>

            <tr v-if="!isLoading && filteredCategories.length === 0">
              <td
                colspan="4"
                class="text-center text-disabled py-8"
              >
                {{ search ? 'Nenhuma categoria encontrada para a busca.' : 'Nenhuma categoria cadastrada.' }}
              </td>
            </tr>
          </tbody>
        </VTable>
      </div>

      <TablePagination
        v-if="filteredCategories.length > 0"
        :page="page"
        :items-per-page="itemsPerPage"
        :total-items="filteredCategories.length"
        @update:page="page = $event"
      />
    </VCard>

    <AddEditCategoryDialog
      v-model:is-dialog-visible="isAddEditDialogVisible"
      :category="selectedCategory"
      @saved="onCategorySaved"
    />

    <ManageSubCategoriesDialog
      v-model:is-dialog-visible="isSubCategoriesDialogVisible"
      :category-id="subCategoriesTargetId"
      :category-name="subCategoriesTargetName"
      :sub-categories="subCategoriesTargetList"
      @updated="onSubCategoriesUpdated"
    />

    <ConfirmDialog
      v-model:is-dialog-visible="isDeleteDialogVisible"
      :auto-result="false"
      confirm-color="error"
      confirmation-question="Tem certeza que deseja excluir definitivamente esta categoria? Esta ação não pode ser desfeita."
      cancel-title="Ação cancelada"
      cancel-msg="A categoria não foi excluída."
      @confirm="onDeleteConfirm"
    />

    <ConfirmDialog
      v-model:is-dialog-visible="isStatusDialogVisible"
      :auto-result="false"
      :confirmation-question="selectedCategory?.active
        ? 'Tem certeza que deseja inativar esta categoria?'
        : 'Tem certeza que deseja ativar esta categoria?'"
      cancel-title="Ação cancelada"
      cancel-msg="O status da categoria não foi alterado."
      @confirm="onToggleStatusConfirm"
    />
  </div>
</template>
