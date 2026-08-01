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
  endDate: string | null
  installments: number | null
  active: boolean
  createdDate: string
}

interface SubCategoryOption {
  id: number
  categoryId: number
  name: string
  active: boolean
  system: boolean
}

interface CategoryOption {
  id: number
  name: string
  active: boolean
  system: boolean
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
  if (!spaceStore.activeSpace) {
    return
  }

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

function openDelete(billRecurring: BillResponse) {
  selectedBillRecurring.value = billRecurring
  isDeleteDialogVisible.value = true
}

function onEdited() {
  fetchBillRecurrings()
  showSuccess('Recorrência atualizada com sucesso.')
}

async function onDeleteConfirm(confirmed: boolean) {
  if (!confirmed || !selectedBillRecurring.value) {
    return
  }

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
  if (id === null) {
    return '—'
  }

  return categoriesById.value.get(id)?.name ?? '—'
}

function formatDate(isoDate: string) {
  const [year, month, day] = isoDate.split('-')

  return `${day}/${month}/${year}`
}

function formatRecurrenceEnd(billRecurring: BillResponse) {
  if (billRecurring.installments != null) {
    return `${billRecurring.installments} parcelas`
  }

  if (billRecurring.endDate != null) {
    const [year, month] = billRecurring.endDate.split('-')

    return `Até ${month}/${year}`
  }

  return 'Sem término'
}

function onClose() {
  emit('update:isDialogVisible', false)
}
</script>

<template>
  <VDialog
    :model-value="props.isDialogVisible"
    :fullscreen="$vuetify.display.smAndDown"
    max-width="900"
    scrollable
    @update:model-value="onClose"
  >
    <DialogCloseBtn
      v-if="!$vuetify.display.smAndDown"
      @click="onClose"
    />

    <VCard
      class="d-flex flex-column pa-4 pa-sm-8"
      style="block-size: 100%"
    >
      <VCardText
        class="d-flex flex-column flex-grow-1 pa-0"
        style="overflow: hidden; min-height: 0;"
      >
        <div class="d-flex align-start gap-2 mb-1">
          <h5 class="text-h5">
            Configurações de Recorrência
          </h5>

          <VSpacer />

          <IconBtn
            v-if="$vuetify.display.smAndDown"
            @click="onClose"
          >
            <VIcon icon="tabler-x" />
          </IconBtn>
        </div>

        <p class="text-body-2 text-disabled mb-6">
          Contas que se repetem todo mês.
          <span class="d-inline-flex align-center cursor-pointer">
            <VIcon
              icon="tabler-info-circle"
              size="16"
            />
            <VTooltip
              activator="parent"
              location="bottom"
              max-width="320"
            >
              Alterações afetam as contas pendentes do mês atual em diante; as de meses anteriores ou já pagas não são alteradas.
            </VTooltip>
          </span>
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
          class="flex-grow-1"
          style="overflow: auto; min-height: 0;"
        >
          <VTable
            fixed-header
            class="bill-recurrings-table"
          >
            <thead style="white-space: nowrap">
              <tr>
                <th>Nome</th>
                <th>Categoria</th>
                <th>Valor padrão</th>
                <th>Dia de vencimento</th>
                <th>Término</th>
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
                <td class="text-no-wrap tabular-nums font-weight-medium">
                  {{ currencyFormatter.format(billRecurring.defaultAmount) }}
                </td>
                <td class="text-disabled text-no-wrap tabular-nums">
                  {{ formatDate(billRecurring.startDate) }}
                </td>
                <td class="text-disabled text-no-wrap">
                  {{ formatRecurrenceEnd(billRecurring) }}
                </td>
                <td class="text-center text-no-wrap">
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
                  colspan="6"
                  class="text-center text-disabled py-8"
                >
                  Nenhuma conta recorrente cadastrada.
                </td>
              </tr>
            </tbody>
          </VTable>
        </div>

        <div class="d-flex justify-end mt-6">
          <VBtn
            color="secondary"
            variant="tonal"
            :block="$vuetify.display.xs"
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

<style scoped>
/* The wrapper below delegates scrolling to the parent, so the table needs a floor width —
   otherwise narrow viewports squeeze the columns instead of scrolling horizontally. */
.bill-recurrings-table :deep(table) {
  border-collapse: collapse;
  min-inline-size: 820px;
}

.bill-recurrings-table :deep(.v-table__wrapper) {
  overflow: visible;
}

.bill-recurrings-table :deep(thead th) {
  background-color: rgb(var(--v-theme-surface));
}
</style>
