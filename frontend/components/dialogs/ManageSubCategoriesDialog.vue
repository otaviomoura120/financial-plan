<script setup lang="ts">
interface SubCategoryResponse {
  id: number
  version: number
  categoryId: number
  name: string
  active: boolean
}

interface Props {
  isDialogVisible: boolean
  categoryId: number | null
  categoryName: string
  subCategories: SubCategoryResponse[]
}

interface Emit {
  (e: 'update:isDialogVisible', value: boolean): void
  (e: 'updated', subCategories: SubCategoryResponse[]): void
}

const props = defineProps<Props>()
const emit = defineEmits<Emit>()

const { error, setError, clearError } = useApiError()

const localSubCategories = ref<SubCategoryResponse[]>([])
const newName = shallowRef('')
const isCreating = shallowRef(false)

const editingId = shallowRef<number | null>(null)
const editingName = shallowRef('')
const isSavingEdit = shallowRef(false)

const isDeleteDialogVisible = shallowRef(false)
const selectedSubCategory = shallowRef<SubCategoryResponse | null>(null)
const isDeleting = shallowRef(false)

const isStatusDialogVisible = shallowRef(false)
const isTogglingStatus = shallowRef(false)

const sortedSubCategories = computed(() =>
  [...localSubCategories.value].sort((a, b) => a.name.localeCompare(b.name)),
)

watch(
  () => props.isDialogVisible,
  visible => {
    if (visible) {
      localSubCategories.value = [...props.subCategories]
      newName.value = ''
      editingId.value = null
      clearError()
    }
  },
)

function emitUpdated() {
  emit('updated', localSubCategories.value)
}

async function createSubCategory() {
  if (!newName.value.trim() || props.categoryId === null)
    return

  isCreating.value = true
  clearError()

  try {
    const saved = await $fetch<SubCategoryResponse>('/api/categories/subcategories', {
      method: 'POST',
      body: {
        categoryId: props.categoryId,
        name: newName.value,
      },
    })

    localSubCategories.value = [saved, ...localSubCategories.value]
    newName.value = ''
    emitUpdated()
  }
  catch (e) {
    setError(e)
  }
  finally {
    isCreating.value = false
  }
}

function startEdit(subCategory: SubCategoryResponse) {
  editingId.value = subCategory.id
  editingName.value = subCategory.name
}

function cancelEdit() {
  editingId.value = null
  editingName.value = ''
}

async function saveEdit(subCategory: SubCategoryResponse) {
  if (!editingName.value.trim())
    return

  isSavingEdit.value = true
  clearError()

  try {
    const updated = await $fetch<SubCategoryResponse>(`/api/categories/subcategories/${subCategory.id}`, {
      method: 'PUT',
      body: {
        version: subCategory.version,
        name: editingName.value,
      },
    })

    const idx = localSubCategories.value.findIndex(sc => sc.id === subCategory.id)

    if (idx >= 0)
      localSubCategories.value[idx] = updated

    editingId.value = null
    emitUpdated()
  }
  catch (e) {
    setError(e)
  }
  finally {
    isSavingEdit.value = false
  }
}

function openDelete(subCategory: SubCategoryResponse) {
  selectedSubCategory.value = subCategory
  isDeleteDialogVisible.value = true
}

async function onDeleteConfirm(confirmed: boolean) {
  if (!confirmed || !selectedSubCategory.value)
    return

  isDeleting.value = true
  clearError()

  try {
    await $fetch(`/api/categories/subcategories/${selectedSubCategory.value.id}`, { method: 'DELETE' })

    localSubCategories.value = localSubCategories.value.filter(sc => sc.id !== selectedSubCategory.value!.id)

    emitUpdated()
  }
  catch (e) {
    setError(e)
  }
  finally {
    isDeleting.value = false
    selectedSubCategory.value = null
  }
}

function openToggleStatus(subCategory: SubCategoryResponse) {
  selectedSubCategory.value = subCategory
  isStatusDialogVisible.value = true
}

async function onToggleStatusConfirm(confirmed: boolean) {
  if (!confirmed || !selectedSubCategory.value)
    return

  const target = selectedSubCategory.value
  const nextActive = !target.active

  isTogglingStatus.value = true
  clearError()

  try {
    const updated = await $fetch<SubCategoryResponse>(`/api/categories/subcategories/${target.id}/status`, {
      method: 'PATCH',
      body: { active: nextActive },
    })

    const idx = localSubCategories.value.findIndex(sc => sc.id === target.id)

    if (idx >= 0)
      localSubCategories.value[idx] = updated

    emitUpdated()
  }
  catch (e) {
    setError(e)
  }
  finally {
    isTogglingStatus.value = false
    selectedSubCategory.value = null
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
    scrollable
    @update:model-value="onClose"
  >
    <DialogCloseBtn @click="onClose" />

    <VCard>
      <VCardItem class="pa-6 pb-4">
        <VCardTitle class="text-h5 text-center">
          Subcategorias
        </VCardTitle>
        <VCardSubtitle class="text-center mt-1">
          {{ props.categoryName }}
        </VCardSubtitle>
      </VCardItem>

      <VDivider />

      <VCardText class="pa-4">
        <ApiErrorAlert
          v-if="error"
          :error="error"
          class="mb-4"
        />

        <div class="d-flex align-center gap-2 mb-4">
          <AppTextField
            v-model="newName"
            placeholder="Nome da nova subcategoria"
            density="compact"
            hide-details
            class="flex-grow-1"
            @keyup.enter="createSubCategory"
          />
          <VBtn
            :loading="isCreating"
            :disabled="!newName.trim()"
            @click="createSubCategory"
          >
            Adicionar
          </VBtn>
        </div>

        <VList
          v-if="localSubCategories.length > 0"
          class="pa-0"
        >
          <VListItem
            v-for="subCategory in sortedSubCategories"
            :key="subCategory.id"
            class="px-0"
          >
            <div
              v-if="editingId === subCategory.id"
              class="d-flex align-center gap-2 w-100"
            >
              <AppTextField
                v-model="editingName"
                density="compact"
                hide-details
                class="flex-grow-1"
                autofocus
                @keyup.enter="saveEdit(subCategory)"
                @keyup.esc="cancelEdit"
              />
              <VBtn
                icon
                variant="text"
                size="small"
                color="success"
                :loading="isSavingEdit"
                @click="saveEdit(subCategory)"
              >
                <VIcon icon="tabler-check" />
              </VBtn>
              <VBtn
                icon
                variant="text"
                size="small"
                color="default"
                :disabled="isSavingEdit"
                @click="cancelEdit"
              >
                <VIcon icon="tabler-x" />
              </VBtn>
            </div>

            <div
              v-else
              class="d-flex align-center gap-2 w-100"
            >
              <span class="flex-grow-1 text-body-2">{{ subCategory.name }}</span>

              <VChip
                :color="subCategory.active ? 'success' : 'secondary'"
                size="x-small"
                variant="tonal"
              >
                {{ subCategory.active ? 'Ativa' : 'Inativa' }}
              </VChip>

              <VBtn
                icon
                variant="text"
                size="small"
                color="default"
                @click="startEdit(subCategory)"
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
                :color="subCategory.active ? 'secondary' : 'success'"
                @click="openToggleStatus(subCategory)"
              >
                <VIcon :icon="subCategory.active ? 'tabler-toggle-right' : 'tabler-toggle-left'" />
                <VTooltip activator="parent">
                  {{ subCategory.active ? 'Inativar' : 'Ativar' }}
                </VTooltip>
              </VBtn>

              <VBtn
                icon
                variant="text"
                size="small"
                color="error"
                @click="openDelete(subCategory)"
              >
                <VIcon icon="tabler-trash" />
                <VTooltip activator="parent">
                  Excluir definitivamente
                </VTooltip>
              </VBtn>
            </div>
          </VListItem>
        </VList>

        <p
          v-else
          class="text-center text-disabled py-8"
        >
          Nenhuma subcategoria cadastrada.
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

    <ConfirmDialog
      v-model:is-dialog-visible="isDeleteDialogVisible"
      :auto-result="false"
      confirm-color="error"
      confirmation-question="Tem certeza que deseja excluir definitivamente esta subcategoria? Esta ação não pode ser desfeita."
      cancel-title="Ação cancelada"
      cancel-msg="A subcategoria não foi excluída."
      @confirm="onDeleteConfirm"
    />

    <ConfirmDialog
      v-model:is-dialog-visible="isStatusDialogVisible"
      :auto-result="false"
      :confirmation-question="selectedSubCategory?.active
        ? 'Tem certeza que deseja inativar esta subcategoria?'
        : 'Tem certeza que deseja ativar esta subcategoria?'"
      cancel-title="Ação cancelada"
      cancel-msg="O status da subcategoria não foi alterado."
      @confirm="onToggleStatusConfirm"
    />
  </VDialog>
</template>
