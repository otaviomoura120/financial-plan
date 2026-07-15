<script setup lang="ts">
interface BillResponse {
  id: number
  version: number
  spaceId: number
  name: string
  categoryId: number | null
  subCategoryId: number | null
  defaultAmount: number
  startDate: string
  active: boolean
  createdDate: string
}

interface SubCategoryOption {
  id: number
  categoryId: number
  name: string
  active: boolean
}

interface CategoryOption {
  id: number
  name: string
  active: boolean
  subCategories: SubCategoryOption[]
}

interface Props {
  isDialogVisible: boolean
  categories: CategoryOption[]
}

interface Emit {
  (e: 'update:isDialogVisible', value: boolean): void
}

const props = defineProps<Props>()
const emit = defineEmits<Emit>()

const spaceStore = useSpaceStore()
const { error, setError, clearError } = useApiError()
const { isVisible: snackbarVisible, message: snackbarMessage, color: snackbarColor, icon: snackbarIcon, showSuccess, showError } = useSnackbar()

const currencyFormatter = new Intl.NumberFormat('pt-BR', { style: 'currency', currency: 'BRL' })

const billRecurrings = ref<BillResponse[]>([])
const isLoading = shallowRef(false)
const isDeleting = shallowRef(false)

const isEditDialogVisible = shallowRef(false)
const isScheduleDialogVisible = shallowRef(false)
const isDeleteDialogVisible = shallowRef(false)

const selectedBillRecurring = shallowRef<BillResponse | null>(null)

const categoriesById = computed(() => new Map(props.categories.map(c => [c.id, c])))

watch(
  () => props.isDialogVisible,
  visible => {
    if (visible) {
      clearError()
      fetchBillRecurrings()
    }
  },
)

async function fetchBillRecurrings() {
  if (!spaceStore.activeSpace)
    return

  isLoading.value = true
  clearError()

  try {
    billRecurrings.value = await $fetch<BillResponse[]>('/api/bills', {
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

function openEdit(billRecurring: BillResponse) {
  selectedBillRecurring.value = billRecurring
  isEditDialogVisible.value = true
}

function openSchedule(billRecurring: BillResponse) {
  selectedBillRecurring.value = billRecurring
  isScheduleDialogVisible.value = true
}

function openDelete(billRecurring: BillResponse) {
  selectedBillRecurring.value = billRecurring
  isDeleteDialogVisible.value = true
}

function onEdited() {
  fetchBillRecurrings()
  showSuccess('Recorrência atualizada com sucesso.')
}

function onScheduleSaved(saved: BillResponse) {
  const idx = billRecurrings.value.findIndex(b => b.id === saved.id)

  if (idx >= 0)
    billRecurrings.value[idx] = saved

  showSuccess('Agenda atualizada com sucesso.')
}

async function onDeleteConfirm(confirmed: boolean) {
  if (!confirmed || !selectedBillRecurring.value)
    return

  isDeleting.value = true
  clearError()

  try {
    await $fetch(`/api/bills/${selectedBillRecurring.value.id}`, { method: 'DELETE' })

    billRecurrings.value = billRecurrings.value.filter(b => b.id !== selectedBillRecurring.value!.id)

    showSuccess('Recorrência excluída com sucesso.')
  }
  catch (e) {
    showError(e)
  }
  finally {
    isDeleting.value = false
    selectedBillRecurring.value = null
  }
}

function categoryName(id: number | null) {
  if (id === null)
    return '—'

  return categoriesById.value.get(id)?.name ?? '—'
}

function formatDate(isoDate: string) {
  const [year, month, day] = isoDate.split('-')

  return `${day}/${month}/${year}`
}

function onClose() {
  emit('update:isDialogVisible', false)
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
          Configurações de Recorrência
        </h4>
        <p class="text-body-1 text-center mb-6">
          Contas que se repetem todo mês. Alterações aqui afetam as contas pendentes do mês atual em diante (já geradas ou não); as de meses anteriores ou já pagas não são alteradas.
        </p>

        <ApiErrorAlert
          v-if="error"
          :error="error"
          class="mb-4"
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
                <th>Categoria</th>
                <th>Valor padrão</th>
                <th>Dia de vencimento</th>
                <th class="text-center">
                  Ações
                </th>
              </tr>
            </thead>
            <tbody>
              <tr
                v-for="billRecurring in billRecurrings"
                :key="billRecurring.id"
              >
                <td>
                  <span class="font-weight-medium">{{ billRecurring.name }}</span>
                </td>
                <td class="text-disabled">
                  {{ categoryName(billRecurring.categoryId) }}
                </td>
                <td>
                  {{ currencyFormatter.format(billRecurring.defaultAmount) }}
                </td>
                <td class="text-disabled">
                  {{ formatDate(billRecurring.startDate) }}
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
                    @click="openEdit(billRecurring)"
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
                    color="default"
                    @click="openSchedule(billRecurring)"
                  >
                    <VIcon icon="tabler-calendar-cog" />
                    <VTooltip activator="parent">
                      Editar Agenda
                    </VTooltip>
                  </VBtn>

                  <VBtn
                    icon
                    variant="text"
                    size="small"
                    color="error"
                    @click="openDelete(billRecurring)"
                  >
                    <VIcon icon="tabler-trash" />
                    <VTooltip activator="parent">
                      Excluir definitivamente
                    </VTooltip>
                  </VBtn>
                </td>
              </tr>

              <tr v-if="!isLoading && billRecurrings.length === 0">
                <td
                  colspan="5"
                  class="text-center text-disabled py-8"
                >
                  Nenhuma conta recorrente cadastrada.
                </td>
              </tr>
            </tbody>
          </VTable>
        </div>

        <div class="d-flex align-center justify-center mt-6">
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

    <AddEditBillDialog
      v-model:is-dialog-visible="isEditDialogVisible"
      :bill="selectedBillRecurring"
      :categories="categories"
      @saved="onEdited"
    />

    <UpdateBillScheduleDialog
      v-model:is-dialog-visible="isScheduleDialogVisible"
      :bill="selectedBillRecurring"
      @saved="onScheduleSaved"
    />

    <ConfirmDialog
      v-model:is-dialog-visible="isDeleteDialogVisible"
      :auto-result="false"
      confirm-color="error"
      confirmation-question="Tem certeza que deseja excluir definitivamente esta recorrência? As contas pendentes do mês atual em diante serão excluídas; as de meses anteriores ou já pagas continuam disponíveis, mas deixam de ficar vinculadas a ela."
      cancel-title="Ação cancelada"
      cancel-msg="A recorrência não foi excluída."
      @confirm="onDeleteConfirm"
    />
  </VDialog>
</template>
