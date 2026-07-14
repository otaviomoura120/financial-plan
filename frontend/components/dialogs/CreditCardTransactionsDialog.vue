<script setup lang="ts">
interface CreditCardResponse {
  id: number
  version: number
  spaceId: number
  name: string
  limit: number
  closingDay: number
  dueDay: number
  active: boolean
  createdDate: string
}

interface CreditCardTransactionResponse {
  id: number
  version: number
  creditCardId: number
  userId: number
  categoryId: number | null
  subCategoryId: number | null
  amount: number
  purchaseDate: string
  description?: string | null
  referenceMonth: string
  competenceMonth: string
  installmentGroupId: string
  installmentNumber: number
  totalInstallments: number
  anticipated: boolean
  originalReferenceMonth: string | null
  createdDate: string
  totalAmount: number
}

interface SubCategoryResponse {
  id: number
  categoryId: number
  name: string
  active: boolean
}

interface CategoryResponse {
  id: number
  name: string
  active: boolean
  subCategories: SubCategoryResponse[]
}

interface Props {
  isDialogVisible: boolean
  creditCardId: number | null
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

function toLocalDateString(date: Date) {
  const year = date.getFullYear()
  const month = String(date.getMonth() + 1).padStart(2, '0')
  const day = String(date.getDate()).padStart(2, '0')

  return `${year}-${month}-${day}`
}

function currentOpenReferenceMonth(closingDay: number) {
  const now = new Date()
  const daysInMonth = new Date(now.getFullYear(), now.getMonth() + 1, 0).getDate()
  const clampedClosingDay = Math.min(closingDay, daysInMonth)

  const referenceMonthDate = now.getDate() <= clampedClosingDay
    ? new Date(now.getFullYear(), now.getMonth(), 1)
    : new Date(now.getFullYear(), now.getMonth() + 1, 1)

  return toLocalDateString(referenceMonthDate)
}

const creditCard = ref<CreditCardResponse | null>(null)
const transactions = ref<CreditCardTransactionResponse[]>([])
const categories = ref<CategoryResponse[]>([])

const selectedMonth = shallowRef(currentMonthValue())

const page = shallowRef(1)
const itemsPerPage = shallowRef(10)
const isLoading = shallowRef(false)
const isDeleting = shallowRef(false)

const isAddEditDialogVisible = shallowRef(false)
const isDeleteDialogVisible = shallowRef(false)
const isDeleteInstallmentDialogVisible = shallowRef(false)
const isAnticipateDialogVisible = shallowRef(false)

const selectedTransaction = shallowRef<CreditCardTransactionResponse | null>(null)

const targetReferenceMonth = computed(() =>
  creditCard.value ? currentOpenReferenceMonth(creditCard.value.closingDay) : null,
)

const categoriesById = computed(() => new Map(categories.value.map(c => [c.id, c])))

const subCategoriesById = computed(() => {
  const map = new Map<number, SubCategoryResponse>()

  for (const category of categories.value) {
    for (const subCategory of category.subCategories)
      map.set(subCategory.id, subCategory)
  }

  return map
})

const sortedTransactions = computed(() =>
  [...transactions.value].sort((a, b) => b.purchaseDate.localeCompare(a.purchaseDate)),
)

const paginatedTransactions = computed(() => {
  const start = (page.value - 1) * itemsPerPage.value

  return sortedTransactions.value.slice(start, start + itemsPerPage.value)
})

watch(
  () => props.isDialogVisible,
  visible => {
    if (visible && props.creditCardId) {
      fetchAll()
    }
    else {
      creditCard.value = null
      transactions.value = []
      categories.value = []
      clearError()
    }
  },
)

watch(
  () => spaceStore.activeSpace,
  async space => {
    if (props.isDialogVisible && space)
      await fetchAll()
  },
)

watch(selectedMonth, () => {
  if (props.isDialogVisible)
    fetchTransactions()
})

async function fetchAll() {
  await Promise.all([fetchCreditCard(), fetchCategories(), fetchTransactions()])
}

async function fetchCreditCard() {
  if (!spaceStore.activeSpace || !props.creditCardId)
    return

  const cards = await $fetch<CreditCardResponse[]>('/api/credit-cards', {
    query: { spaceId: spaceStore.activeSpace.id },
  })

  creditCard.value = cards.find(c => c.id === props.creditCardId) ?? null
}

async function fetchCategories() {
  if (!spaceStore.activeSpace)
    return

  categories.value = await $fetch<CategoryResponse[]>('/api/categories', {
    query: { spaceId: spaceStore.activeSpace.id },
  })
}

async function fetchTransactions() {
  if (!spaceStore.activeSpace || !props.creditCardId)
    return

  isLoading.value = true
  clearError()

  try {
    transactions.value = await $fetch<CreditCardTransactionResponse[]>('/api/credit-card-transactions', {
      query: {
        spaceId: spaceStore.activeSpace.id,
        creditCardId: props.creditCardId,
        competenceMonth: selectedMonth.value,
      },
    })
    page.value = 1
  }
  catch (e) {
    setError(e)
  }
  finally {
    isLoading.value = false
  }
}

function openCreate() {
  selectedTransaction.value = null
  isAddEditDialogVisible.value = true
}

function openEdit(transaction: CreditCardTransactionResponse) {
  selectedTransaction.value = transaction
  isAddEditDialogVisible.value = true
}

function openDelete(transaction: CreditCardTransactionResponse) {
  selectedTransaction.value = transaction

  if (transaction.totalInstallments > 1)
    isDeleteInstallmentDialogVisible.value = true
  else
    isDeleteDialogVisible.value = true
}

function openAnticipate(transaction: CreditCardTransactionResponse) {
  selectedTransaction.value = transaction
  isAnticipateDialogVisible.value = true
}

async function onDeleteConfirm(confirmed: boolean) {
  if (!confirmed || !selectedTransaction.value)
    return

  await deleteSelectedTransaction(false)
}

async function onDeleteInstallmentConfirm(result: 'single' | 'future' | null) {
  if (!result || !selectedTransaction.value)
    return

  await deleteSelectedTransaction(result === 'future')
}

async function deleteSelectedTransaction(includeFuture: boolean) {
  if (!selectedTransaction.value)
    return

  const transaction = selectedTransaction.value

  isDeleting.value = true
  clearError()

  try {
    await $fetch(`/api/credit-card-transactions/${transaction.id}`, {
      method: 'DELETE',
      query: { includeFuture },
    })

    if (includeFuture) {
      transactions.value = transactions.value.filter(t =>
        !(t.installmentGroupId === transaction.installmentGroupId && t.installmentNumber >= transaction.installmentNumber),
      )
    }
    else {
      transactions.value = transactions.value.filter(t => t.id !== transaction.id)
    }

    showSuccess('Lançamento excluído com sucesso.')
  }
  catch (e) {
    showError(e)
  }
  finally {
    isDeleting.value = false
    selectedTransaction.value = null
  }
}

function onTransactionSaved(saved: CreditCardTransactionResponse) {
  const idx = transactions.value.findIndex(t => t.id === saved.id)

  if (idx >= 0)
    transactions.value[idx] = saved
  else if (saved.competenceMonth === selectedMonth.value)
    transactions.value = [saved, ...transactions.value]
}

function onAnticipated(updated: CreditCardTransactionResponse[]) {
  for (const updatedTransaction of updated) {
    const idx = transactions.value.findIndex(t => t.id === updatedTransaction.id)

    if (idx >= 0)
      transactions.value[idx] = updatedTransaction
  }

  showSuccess('Parcelas antecipadas com sucesso.')
}

function categoryName(id: number | null) {
  if (id === null)
    return '—'

  return categoriesById.value.get(id)?.name ?? '—'
}

function subCategoryName(id: number | null) {
  if (id === null)
    return null

  return subCategoriesById.value.get(id)?.name ?? null
}

function formatDate(isoDate: string) {
  const [year, month, day] = isoDate.split('-')

  return `${day}/${month}/${year}`
}

function formatReferenceMonth(isoDate: string) {
  const [year, month] = isoDate.split('-')

  return `${month}/${year}`
}

function onClose() {
  emit('update:isDialogVisible', false)
}
</script>

<template>
  <VDialog
    :model-value="props.isDialogVisible"
    :width="$vuetify.display.smAndDown ? '100%' : '95%'"
    max-width="1600"
    scrollable
    @update:model-value="onClose"
  >
    <DialogCloseBtn @click="onClose" />

    <div>
      <VCard>
        <VCardText
          class="pa-5 v-application"
          style="display: block;"
        >
          <div>
            <VCard
              class="d-flex flex-column"
              style="block-size: 100%"
            >
              <VCardText class="d-flex align-center flex-wrap gap-4">
                <h5
                  class="text-h5 text-truncate"
                  style="min-inline-size: 0"
                >
                  Lançamentos {{ creditCard ? `— ${creditCard.name}` : '' }}
                </h5>

                <VSpacer />

                <div
                  class="d-flex flex-wrap align-center gap-2"
                  style="flex-grow: 1; justify-content: flex-end;"
                >
                  <MonthYearSelect
                    v-model="selectedMonth"
                    label="Mês de Referência"
                  />

                  <VBtn
                    prepend-icon="tabler-plus"
                    style="align-self: flex-end"
                    @click="openCreate"
                  >
                    <span class="d-sm-inline">Adicionar</span>
                  </VBtn>
                </div>
              </VCardText>

              <VDivider />

              <VCardText
                class="flex-grow-1"
                style="overflow-y: auto"
              >
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
                        <th>Data da Compra</th>
                        <th style="min-width: 200px">
                          Categoria
                        </th>
                        <th>Descrição</th>
                        <th>Mês de Referência</th>
                        <th>Parcela</th>
                        <th class="text-right">
                          Valor
                        </th>
                        <th class="text-center">
                          Ações
                        </th>
                      </tr>
                    </thead>
                    <tbody>
                      <tr
                        v-for="transaction in paginatedTransactions"
                        :key="transaction.id"
                      >
                        <td>{{ formatDate(transaction.purchaseDate) }}</td>
                        <td>
                          {{ categoryName(transaction.categoryId) }}
                          <span
                            v-if="subCategoryName(transaction.subCategoryId)"
                            class="text-disabled"
                          >
                            / {{ subCategoryName(transaction.subCategoryId) }}
                          </span>
                        </td>
                        <td class="text-disabled">
                          {{ transaction.description || '—' }}
                        </td>
                        <td>
                          {{ formatReferenceMonth(transaction.competenceMonth) }}
                          <div class="text-caption text-disabled">
                            Fatura: {{ formatReferenceMonth(transaction.referenceMonth) }}
                          </div>
                        </td>
                        <td>
                          <VChip
                            v-if="transaction.totalInstallments > 1"
                            size="small"
                            variant="tonal"
                            color="info"
                          >
                            {{ transaction.installmentNumber }}/{{ transaction.totalInstallments }}
                          </VChip>
                          <VChip
                            v-else
                            size="small"
                            variant="tonal"
                          >
                            À vista
                          </VChip>
                          <VChip
                            v-if="transaction.anticipated"
                            size="small"
                            variant="tonal"
                            color="warning"
                            class="ms-1"
                          >
                            Antecipada
                          </VChip>
                        </td>
                        <td class="text-right">
                          {{ currencyFormatter.format(transaction.amount) }}
                          <div
                            v-if="transaction.totalInstallments > 1"
                            class="text-caption text-disabled"
                          >
                            Total: {{ currencyFormatter.format(transaction.totalAmount) }}
                          </div>
                        </td>
                        <td
                          class="text-center"
                          style="white-space: nowrap"
                        >
                          <VBtn
                            v-if="transaction.totalInstallments > 1 && transaction.installmentNumber < transaction.totalInstallments"
                            icon
                            variant="text"
                            size="small"
                            color="default"
                            @click="openAnticipate(transaction)"
                          >
                            <VIcon icon="tabler-clock-forward" />
                            <VTooltip activator="parent">
                              Antecipar parcelas
                            </VTooltip>
                          </VBtn>

                          <VBtn
                            icon
                            variant="text"
                            size="small"
                            color="default"
                            @click="openEdit(transaction)"
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
                            @click="openDelete(transaction)"
                          >
                            <VIcon icon="tabler-trash" />
                            <VTooltip activator="parent">
                              Excluir
                            </VTooltip>
                          </VBtn>
                        </td>
                      </tr>

                      <tr v-if="!isLoading && transactions.length === 0">
                        <td
                          colspan="7"
                          class="text-center text-disabled py-8"
                        >
                          Nenhum lançamento encontrado para o período selecionado.
                        </td>
                      </tr>
                    </tbody>
                  </VTable>
                </div>

                <TablePagination
                  v-if="transactions.length > 0"
                  :page="page"
                  :items-per-page="itemsPerPage"
                  :total-items="transactions.length"
                  @update:page="page = $event"
                />
              </VCardText>
            </VCard>
          </div>
        </VCardText>
      </VCard>
    </div>

    <AddEditCreditCardTransactionDialog
      v-model:is-dialog-visible="isAddEditDialogVisible"
      :credit-card-id="props.creditCardId"
      :transaction="selectedTransaction"
      :categories="categories"
      @saved="onTransactionSaved"
    />

    <AnticipateInstallmentsDialog
      v-model:is-dialog-visible="isAnticipateDialogVisible"
      :installment-group-id="selectedTransaction?.installmentGroupId ?? null"
      :target-reference-month="targetReferenceMonth"
      @anticipated="onAnticipated"
    />

    <ConfirmDialog
      v-model:is-dialog-visible="isDeleteDialogVisible"
      :auto-result="false"
      confirm-color="error"
      confirmation-question="Tem certeza que deseja excluir este lançamento?"
      cancel-title="Ação cancelada"
      cancel-msg="O lançamento não foi excluído."
      @confirm="onDeleteConfirm"
    />

    <DeleteInstallmentDialog
      v-model:is-dialog-visible="isDeleteInstallmentDialogVisible"
      @confirm="onDeleteInstallmentConfirm"
    />
  </VDialog>
</template>
