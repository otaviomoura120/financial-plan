<script setup lang="ts">
type TransactionType = 'INCOME' | 'EXPENSE' | 'TRANSFER'

interface TransactionResponse {
  id: number
  version: number
  type: TransactionType
  userId: number
  bankAccountId: number
  destinationBankAccountId: number | null
  categoryId: number | null
  subCategoryId: number | null
  paymentMethodId: number | null
  amount: number
  transactionDate: string
  description?: string | null
  createdDate: string
}

interface BankAccountResponse {
  id: number
  name: string
  active: boolean
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

interface PaymentMethodResponse {
  id: number
  name: string
  active: boolean
}

const spaceStore = useSpaceStore()
const { error, setError, clearError } = useApiError()
const { isVisible: snackbarVisible, message: snackbarMessage, color: snackbarColor, icon: snackbarIcon, showSuccess, showError } = useSnackbar()

function toLocalDateString(date: Date) {
  const year = date.getFullYear()
  const month = String(date.getMonth() + 1).padStart(2, '0')
  const day = String(date.getDate()).padStart(2, '0')

  return `${year}-${month}-${day}`
}

function firstDayOfMonth() {
  const now = new Date()

  return toLocalDateString(new Date(now.getFullYear(), now.getMonth(), 1))
}

function lastDayOfMonth() {
  const now = new Date()

  return toLocalDateString(new Date(now.getFullYear(), now.getMonth() + 1, 0))
}

const transactions = ref<TransactionResponse[]>([])
const bankAccounts = ref<BankAccountResponse[]>([])
const categories = ref<CategoryResponse[]>([])
const paymentMethods = ref<PaymentMethodResponse[]>([])

const from = shallowRef(firstDayOfMonth())
const to = shallowRef(lastDayOfMonth())

const page = shallowRef(1)
const itemsPerPage = shallowRef(10)
const isLoading = shallowRef(false)
const isDeleting = shallowRef(false)

const isAddEditDialogVisible = shallowRef(false)
const isDeleteDialogVisible = shallowRef(false)

const selectedTransaction = shallowRef<TransactionResponse | null>(null)

const bankAccountsById = computed(() => new Map(bankAccounts.value.map(ba => [ba.id, ba])))
const categoriesById = computed(() => new Map(categories.value.map(c => [c.id, c])))
const paymentMethodsById = computed(() => new Map(paymentMethods.value.map(pm => [pm.id, pm])))

const subCategoriesById = computed(() => {
  const map = new Map<number, SubCategoryResponse>()

  for (const category of categories.value) {
    for (const subCategory of category.subCategories)
      map.set(subCategory.id, subCategory)
  }

  return map
})

const sortedTransactions = computed(() =>
  [...transactions.value].sort((a, b) => b.transactionDate.localeCompare(a.transactionDate)),
)

const paginatedTransactions = computed(() => {
  const start = (page.value - 1) * itemsPerPage.value

  return sortedTransactions.value.slice(start, start + itemsPerPage.value)
})

const typeLabel: Record<TransactionType, string> = {
  INCOME: 'Receita',
  EXPENSE: 'Despesa',
  TRANSFER: 'Transferência',
}

const typeColor: Record<TransactionType, string> = {
  INCOME: 'success',
  EXPENSE: 'error',
  TRANSFER: 'info',
}

watch(
  () => spaceStore.activeSpace,
  async space => {
    if (space) {
      await fetchAll()
    }
    else {
      transactions.value = []
      bankAccounts.value = []
      categories.value = []
      paymentMethods.value = []
    }
  },
  { immediate: true },
)

async function fetchAll() {
  await Promise.all([fetchTransactions(), fetchDropdownData()])
}

async function fetchDropdownData() {
  if (!spaceStore.activeSpace)
    return

  const spaceId = spaceStore.activeSpace.id

  const [bankAccountsResult, categoriesResult, paymentMethodsResult] = await Promise.all([
    $fetch<BankAccountResponse[]>('/api/bank-accounts', { query: { spaceId } }),
    $fetch<CategoryResponse[]>('/api/categories', { query: { spaceId } }),
    $fetch<PaymentMethodResponse[]>('/api/payment-methods', { query: { spaceId } }),
  ])

  bankAccounts.value = bankAccountsResult
  categories.value = categoriesResult
  paymentMethods.value = paymentMethodsResult
}

async function fetchTransactions() {
  if (!spaceStore.activeSpace)
    return

  isLoading.value = true
  clearError()

  try {
    transactions.value = await $fetch<TransactionResponse[]>('/api/transactions', {
      query: {
        spaceId: spaceStore.activeSpace.id,
        from: from.value,
        to: to.value,
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

function openEdit(transaction: TransactionResponse) {
  selectedTransaction.value = transaction
  isAddEditDialogVisible.value = true
}

function openDelete(transaction: TransactionResponse) {
  selectedTransaction.value = transaction
  isDeleteDialogVisible.value = true
}

async function onDeleteConfirm(confirmed: boolean) {
  if (!confirmed || !selectedTransaction.value)
    return

  isDeleting.value = true
  clearError()

  try {
    await $fetch(`/api/transactions/${selectedTransaction.value.id}`, { method: 'DELETE' })
    transactions.value = transactions.value.filter(t => t.id !== selectedTransaction.value!.id)
    showSuccess('Transação excluída com sucesso.')
  }
  catch (e) {
    showError(e)
  }
  finally {
    isDeleting.value = false
    selectedTransaction.value = null
  }
}

function onTransactionSaved(saved: TransactionResponse) {
  const idx = transactions.value.findIndex(t => t.id === saved.id)

  if (idx >= 0)
    transactions.value[idx] = saved
  else if (saved.transactionDate >= from.value && saved.transactionDate <= to.value)
    transactions.value = [saved, ...transactions.value]
}

function bankAccountName(id: number | null) {
  if (id === null)
    return '—'

  return bankAccountsById.value.get(id)?.name ?? '—'
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

function paymentMethodName(id: number | null) {
  if (id === null)
    return '—'

  return paymentMethodsById.value.get(id)?.name ?? '—'
}

function formatAmount(transaction: TransactionResponse) {
  const formatted = formatCurrency(transaction.amount)

  if (transaction.type === 'INCOME')
    return `+ ${formatted}`

  if (transaction.type === 'EXPENSE')
    return `- ${formatted}`

  return formatted
}

function formatDate(isoDate: string) {
  const [year, month, day] = isoDate.split('-')

  return `${day}/${month}/${year}`
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
          Transações
        </h5>

        <VSpacer />

        <div
          class="d-flex flex-wrap align-center gap-2"
          style="flex-grow: 1; justify-content: flex-end;"
        >
          <VTextField
            v-model="from"
            type="date"
            label="De"
            density="compact"
            hide-details
            style="max-inline-size: 170px"
          />

          <VTextField
            v-model="to"
            type="date"
            label="Até"
            density="compact"
            hide-details
            style="max-inline-size: 170px"
          />

          <VBtn
            variant="tonal"
            @click="fetchTransactions"
          >
            Filtrar
          </VBtn>

          <VBtn
            prepend-icon="tabler-plus"
            @click="openCreate"
          >
            <span class="d-sm-inline">Adicionar</span>
          </VBtn>
        </div>
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
              <th>Data</th>
              <th>Tipo</th>
              <th>Conta</th>
              <th style="min-width: 200px">
                Categoria / Destino
              </th>
              <th>Forma de Pagamento</th>
              <th>Descrição</th>
              <th
                class="text-right"
                style="min-width: 115px"
              >
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
              style="font-size: 0.8rem"
            >
              <td>{{ formatDate(transaction.transactionDate) }}</td>
              <td>
                <VChip
                  :color="typeColor[transaction.type]"
                  size="small"
                  variant="tonal"
                >
                  {{ typeLabel[transaction.type] }}
                </VChip>
              </td>
              <td>{{ bankAccountName(transaction.bankAccountId) }}</td>
              <td>
                <template v-if="transaction.type === 'TRANSFER'">
                  → {{ bankAccountName(transaction.destinationBankAccountId) }}
                </template>
                <template v-else>
                  {{ categoryName(transaction.categoryId) }}
                  <span
                    v-if="subCategoryName(transaction.subCategoryId)"
                    class="text-disabled"
                  >
                    / {{ subCategoryName(transaction.subCategoryId) }}
                  </span>
                </template>
              </td>
              <td>
                {{ transaction.type === 'TRANSFER' ? '—' : paymentMethodName(transaction.paymentMethodId) }}
              </td>
              <td class="text-disabled">
                {{ transaction.description || '—' }}
              </td>
              <td
                class="text-right"
                :class="{
                  'text-success': transaction.type === 'INCOME',
                  'text-error': transaction.type === 'EXPENSE',
                }"
              >
                {{ formatAmount(transaction) }}
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
                colspan="8"
                class="text-center text-disabled py-8"
              >
                Nenhuma transação encontrada para o período selecionado.
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
    </VCard>

    <AddEditTransactionDialog
      v-model:is-dialog-visible="isAddEditDialogVisible"
      :transaction="selectedTransaction"
      :bank-accounts="bankAccounts"
      :categories="categories"
      :payment-methods="paymentMethods"
      @saved="onTransactionSaved"
    />

    <ConfirmDialog
      v-model:is-dialog-visible="isDeleteDialogVisible"
      :auto-result="false"
      confirmation-question="Tem certeza que deseja excluir esta transação? O saldo da conta envolvida será revertido."
      cancel-title="Ação cancelada"
      cancel-msg="A transação não foi excluída."
      @confirm="onDeleteConfirm"
    />
  </div>
</template>
