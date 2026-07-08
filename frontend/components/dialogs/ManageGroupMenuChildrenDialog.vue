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

interface Props {
  isDialogVisible: boolean
  groupMenu: GroupMenuResponse | null
}

interface Emit {
  (e: 'update:isDialogVisible', value: boolean): void
  (e: 'updated', groupMenu: GroupMenuResponse): void
}

const props = defineProps<Props>()
const emit = defineEmits<Emit>()

const { error, setError, clearError } = useApiError()
const { isVisible: snackbarVisible, message: snackbarMessage, color: snackbarColor, icon: snackbarIcon, showSuccess, showError } = useSnackbar()

const children = ref<GroupMenuChildrenResponse[]>([])
const search = shallowRef('')
const page = shallowRef(1)
const itemsPerPage = shallowRef(10)
const isDeleting = shallowRef(false)

const isAddEditChildDialogVisible = shallowRef(false)
const isDeleteChildDialogVisible = shallowRef(false)
const selectedChild = shallowRef<GroupMenuChildrenResponse | null>(null)

const filteredChildren = computed(() =>
  children.value.filter(c =>
    c.name.toLowerCase().includes(search.value.toLowerCase())
    || c.endpoint.toLowerCase().includes(search.value.toLowerCase()),
  ),
)

const paginatedChildren = computed(() => {
  const start = (page.value - 1) * itemsPerPage.value

  return filteredChildren.value.slice(start, start + itemsPerPage.value)
})

watch(
  () => props.isDialogVisible,
  visible => {
    if (visible && props.groupMenu) {
      children.value = [...(props.groupMenu.children ?? [])]
      search.value = ''
      page.value = 1
      clearError()
    }
  },
)

watch(search, () => {
  page.value = 1
})

function openCreate() {
  selectedChild.value = null
  isAddEditChildDialogVisible.value = true
}

function openEdit(child: GroupMenuChildrenResponse) {
  selectedChild.value = child
  isAddEditChildDialogVisible.value = true
}

function openDelete(child: GroupMenuChildrenResponse) {
  selectedChild.value = child
  isDeleteChildDialogVisible.value = true
}

async function onDeleteConfirm(confirmed: boolean) {
  if (!confirmed || !selectedChild.value)
    return

  isDeleting.value = true
  clearError()

  try {
    await $fetch(`/api/group-menus/children/${selectedChild.value.id}`, { method: 'DELETE' })
    children.value = children.value.filter(c => c.id !== selectedChild.value!.id)
    emitUpdated()
    showSuccess('Item excluído com sucesso.')
  }
  catch (e) {
    showError(e)
  }
  finally {
    isDeleting.value = false
    selectedChild.value = null
  }
}

function onChildSaved(saved: GroupMenuChildrenResponse) {
  const idx = children.value.findIndex(c => c.id === saved.id)

  if (idx >= 0)
    children.value[idx] = saved

  else
    children.value = [saved, ...children.value]

  emitUpdated()
}

function emitUpdated() {
  if (!props.groupMenu)
    return

  emit('updated', { ...props.groupMenu, children: [...children.value] })
}

function onClose() {
  emit('update:isDialogVisible', false)
}
</script>

<template>
  <VDialog
    :width="$vuetify.display.smAndDown ? 'auto' : 800"
    :model-value="props.isDialogVisible"
    @update:model-value="onClose"
  >
    <DialogCloseBtn @click="onClose" />

    <VCard>
      <VCardText class="d-flex align-center flex-wrap gap-4 pa-6">
        <div>
          <h4 class="text-h5">
            Itens do Menu
          </h4>
          <p class="text-body-2 text-disabled mb-0">
            {{ props.groupMenu?.name }}
          </p>
        </div>

        <VSpacer />

        <VTextField
          v-model="search"
          placeholder="Buscar por nome ou endpoint..."
          density="compact"
          prepend-inner-icon="tabler-search"
          style="max-inline-size: 260px"
          hide-details
        />

        <VBtn
          prepend-icon="tabler-plus"
          size="small"
          @click="openCreate"
        >
          Adicionar
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

      <VTable>
        <thead>
          <tr>
            <th>Nome</th>
            <th>Endpoint</th>
            <th>Ícone</th>
            <th class="text-center">
              Ações
            </th>
          </tr>
        </thead>
        <tbody>
          <tr
            v-for="child in paginatedChildren"
            :key="child.id"
          >
            <td class="font-weight-medium">
              {{ child.name }}
            </td>
            <td class="text-disabled">
              {{ child.endpoint }}
            </td>
            <td>
              <VIcon
                :icon="child.icon"
                size="18"
              />
            </td>
            <td class="text-center">
              <VBtn
                icon
                variant="text"
                size="small"
                color="default"
                @click="openEdit(child)"
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
                @click="openDelete(child)"
              >
                <VIcon icon="tabler-trash" />
                <VTooltip activator="parent">
                  Excluir
                </VTooltip>
              </VBtn>
            </td>
          </tr>

          <tr v-if="filteredChildren.length === 0">
            <td
              colspan="4"
              class="text-center text-disabled py-8"
            >
              {{ search ? 'Nenhum item encontrado para a busca.' : 'Nenhum item cadastrado neste group menu.' }}
            </td>
          </tr>
        </tbody>
      </VTable>

      <TablePagination
        v-if="filteredChildren.length > 0"
        :page="page"
        :items-per-page="itemsPerPage"
        :total-items="filteredChildren.length"
        @update:page="page = $event"
      />
    </VCard>

    <AddEditGroupMenuChildrenDialog
      v-if="props.groupMenu"
      v-model:is-dialog-visible="isAddEditChildDialogVisible"
      :group-menu-id="props.groupMenu.id"
      :child="selectedChild"
      @saved="onChildSaved"
    />

    <ConfirmDialog
      v-model:is-dialog-visible="isDeleteChildDialogVisible"
      :auto-result="false"
      confirmation-question="Tem certeza que deseja excluir este item do menu?"
      cancel-title="Ação cancelada"
      cancel-msg="O item não foi excluído."
      @confirm="onDeleteConfirm"
    />
  </VDialog>
</template>
