<script setup lang="ts">
import { VForm } from 'vuetify/components/VForm'

type TransactionType = 'INCOME' | 'EXPENSE' | 'TRANSFER'

type TransactionSourceType = 'CREDIT_CARD_INVOICE_PAYMENT' | 'BILL_INSTANCE_PAYMENT'

interface TransactionResponse {
  id: number
  version: number
  type: TransactionType
  userId: number
  bankAccountId: number
  destinationBankAccountId: number | null
  categoryId: number | null
  subCategoryId: number | null
  amount: number
  transactionDate: string
  description?: string | null
  createdDate: string
  sourceType: TransactionSourceType | null
  sourceId: number | null
  creditCardInvoiceReferenceMonth: string | null
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
  installmentGroupId: string
  installmentNumber: number
  totalInstallments: number
  anticipated: boolean
  originalReferenceMonth: string | null
  createdDate: string
}

interface PendingCreditCardInvoiceResponse {
  creditCardId: number
  creditCardName: string
  referenceMonth: string
  dueDate: string
  amount: number
}

interface PendingBillInstanceResponse {
  billInstanceId: number
  billRecurringId: number | null
  billName: string
  referenceMonth: string
  dueDate: string
  amount: number
  categoryId: number | null
  subCategoryId: number | null
}

interface ReportResponse {
  transactions: TransactionResponse[]
  totalIncome: number
  totalExpense: number
  balance: number
  currentBalance: number
  pendingCreditCardInvoices: PendingCreditCardInvoiceResponse[]
  pendingCreditCardTotal: number
  pendingBillInstances: PendingBillInstanceResponse[]
  pendingBillTotal: number
  projectedBalance: number
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

const spaceStore = useSpaceStore()
const { error, setError, clearError } = useApiError()

const bankAccounts = ref<BankAccountResponse[]>([])
const categories = ref<CategoryResponse[]>([])

const selectedMonth = shallowRef(currentMonthValue())
const bankAccountId = shallowRef<number | null>(null)
const categoryId = shallowRef<number | null>(null)
const subCategoryId = shallowRef<number | null>(null)
const type = shallowRef<TransactionType | null>(null)

const report = ref<ReportResponse | null>(null)
const isLoading = shallowRef(false)
const isLoadingFilters = shallowRef(false)

const filterFormRef = useTemplateRef<InstanceType<typeof VForm>>('filterFormRef')

const typeItems = [
  { title: 'Todos', value: null },
  { title: 'Receita', value: 'INCOME' },
  { title: 'Despesa', value: 'EXPENSE' },
  { title: 'Transferência', value: 'TRANSFER' },
]

const bankAccountItems = computed(() =>
  bankAccounts.value.map(ba => ({ title: ba.active ? ba.name : `${ba.name} (inativo)`, value: ba.id })),
)

const categoryItems = computed(() =>
  categories.value.map(c => ({ title: c.active ? c.name : `${c.name} (inativo)`, value: c.id })),
)

const selectedCategory = computed(() =>
  categories.value.find(c => c.id === categoryId.value) ?? null,
)

const subCategoryItems = computed(() =>
  (selectedCategory.value?.subCategories ?? []).map(sc => ({ title: sc.active ? sc.name : `${sc.name} (inativo)`, value: sc.id })),
)

const bankAccountsById = computed(() => new Map(bankAccounts.value.map(ba => [ba.id, ba])))
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
  report.value ? [...report.value.transactions].sort((a, b) => b.transactionDate.localeCompare(a.transactionDate)) : [],
)

const expandedIds = ref(new Set<number>())
const invoiceItemsCache = ref(new Map<number, CreditCardTransactionResponse[]>())
const loadingInvoiceItems = ref(new Set<number>())

function isInvoicePaymentRow(transaction: TransactionResponse) {
  return transaction.sourceType === 'CREDIT_CARD_INVOICE_PAYMENT'
    && transaction.sourceId !== null
    && !!transaction.creditCardInvoiceReferenceMonth
}

async function fetchInvoiceItems(transaction: TransactionResponse) {
  if (!spaceStore.activeSpace || !transaction.sourceId || !transaction.creditCardInvoiceReferenceMonth)
    return

  if (invoiceItemsCache.value.has(transaction.id))
    return

  loadingInvoiceItems.value.add(transaction.id)

  try {
    const items = await $fetch<CreditCardTransactionResponse[]>('/api/credit-card-transactions', {
      query: {
        spaceId: spaceStore.activeSpace.id,
        creditCardId: transaction.sourceId,
        referenceMonth: transaction.creditCardInvoiceReferenceMonth,
      },
    })

    invoiceItemsCache.value.set(transaction.id, items)
  }
  finally {
    loadingInvoiceItems.value.delete(transaction.id)
  }
}

function toggleExpand(transaction: TransactionResponse) {
  if (!isInvoicePaymentRow(transaction))
    return

  if (expandedIds.value.has(transaction.id)) {
    expandedIds.value.delete(transaction.id)
  }
  else {
    expandedIds.value.add(transaction.id)
    fetchInvoiceItems(transaction)
  }
}

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

watch(categoryId, () => {
  if (!subCategoryItems.value.some(sc => sc.value === subCategoryId.value))
    subCategoryId.value = null
})

watch(
  () => spaceStore.activeSpace,
  async space => {
    if (space) {
      await fetchFilterData()
      await generateReport()
    }
    else {
      bankAccounts.value = []
      categories.value = []
      report.value = null
    }
  },
  { immediate: true },
)

async function fetchFilterData() {
  if (!spaceStore.activeSpace)
    return

  isLoadingFilters.value = true

  try {
    const spaceId = spaceStore.activeSpace.id

    const [bankAccountsResult, categoriesResult] = await Promise.all([
      $fetch<BankAccountResponse[]>('/api/bank-accounts', { query: { spaceId } }),
      $fetch<CategoryResponse[]>('/api/categories', { query: { spaceId } }),
    ])

    bankAccounts.value = bankAccountsResult
    categories.value = categoriesResult
  }
  finally {
    isLoadingFilters.value = false
  }
}

async function generateReport() {
  if (!spaceStore.activeSpace)
    return

  if (filterFormRef.value) {
    const { valid } = await filterFormRef.value.validate()

    if (!valid)
      return
  }

  isLoading.value = true
  clearError()
  expandedIds.value = new Set()
  invoiceItemsCache.value = new Map()
  loadingInvoiceItems.value = new Set()

  try {
    report.value = await $fetch<ReportResponse>('/api/reports', {
      method: 'POST',
      body: {
        spaceId: spaceStore.activeSpace.id,
        from: selectedMonth.value,
        to: lastDayOfMonthOf(selectedMonth.value),
        bankAccountId: bankAccountId.value,
        categoryId: categoryId.value,
        subCategoryId: subCategoryId.value,
        type: type.value,
      },
    })
  }
  catch (e) {
    setError(e)
  }
  finally {
    isLoading.value = false
  }
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

function formatReferenceMonth(isoDate: string) {
  const [year, month] = isoDate.split('-')

  return `${month}/${year}`
}
</script>

<template>
  <div class="d-flex flex-column gap-6">
    <VCard title="Filtros">
      <VCardText>
        <VForm ref="filterFormRef">
          <VRow>
            <VCol
              cols="12"
              md="6"
            >
              <MonthYearSelect
                v-model="selectedMonth"
                label="Mês"
              />
            </VCol>

            <VCol
              cols="12"
              md="3"
            >
              <AppSelect
                v-model="type"
                label="Tipo"
                :items="typeItems"
              />
            </VCol>

            <VCol
              cols="12"
              md="3"
            >
              <AppSelect
                v-model="bankAccountId"
                label="Conta"
                :items="bankAccountItems"
                clearable
                :loading="isLoadingFilters"
              />
            </VCol>

            <VCol
              cols="12"
              md="3"
            >
              <AppSelect
                v-model="categoryId"
                label="Categoria"
                :items="categoryItems"
                clearable
                :loading="isLoadingFilters"
              />
            </VCol>

            <VCol
              cols="12"
              md="3"
            >
              <AppSelect
                v-model="subCategoryId"
                label="Subcategoria"
                :items="subCategoryItems"
                clearable
                :disabled="!selectedCategory"
              />
            </VCol>
          </VRow>

          <div class="d-flex justify-end mt-2">
            <VBtn
              :loading="isLoading"
              @click="generateReport"
            >
              Gerar Relatório
            </VBtn>
          </div>
        </VForm>
      </VCardText>
    </VCard>

    <ApiErrorAlert
      v-if="error"
      :error="error"
    />

    <template v-if="report">
      <VRow>
        <VCol
          cols="12"
          md="4"
        >
          <CardStatisticsVerticalSimple
            title="Total de Receitas"
            color="success"
            icon="tabler-arrow-up"
            :stats="formatCurrency(report.totalIncome)"
          />
        </VCol>

        <VCol
          cols="12"
          md="4"
        >
          <CardStatisticsVerticalSimple
            title="Total de Despesas"
            color="error"
            icon="tabler-arrow-down"
            :stats="formatCurrency(report.totalExpense)"
          />
        </VCol>

        <VCol
          cols="12"
          md="4"
        >
          <CardStatisticsVerticalSimple
            title="Saldo do Período"
            :color="report.balance >= 0 ? 'primary' : 'error'"
            icon="tabler-scale"
            :stats="formatCurrency(report.balance)"
          />
        </VCol>
      </VRow>

      <VRow>
        <VCol cols="12">
          <ReportBalanceSummaryCard
            :current-balance="report.currentBalance"
            :projected-balance="report.projectedBalance"
            :pending-credit-card-total="report.pendingCreditCardTotal"
            :pending-bill-total="report.pendingBillTotal"
          />
        </VCol>
      </VRow>

      <VRow v-if="report.pendingCreditCardInvoices.length > 0 || report.pendingBillInstances.length > 0">
        <VCol
          v-if="report.pendingCreditCardInvoices.length > 0"
          cols="12"
          md="6"
        >
          <VCard title="Faturas Pendentes">
            <div style="overflow-x: auto">
              <VTable>
                <thead style="white-space: nowrap">
                  <tr>
                    <th>Cartão</th>
                    <th>Mês</th>
                    <th>Vencimento</th>
                    <th class="text-right">
                      Valor
                    </th>
                  </tr>
                </thead>
                <tbody>
                  <tr
                    v-for="invoice in report.pendingCreditCardInvoices"
                    :key="`${invoice.creditCardId}-${invoice.referenceMonth}`"
                  >
                    <td>{{ invoice.creditCardName }}</td>
                    <td class="text-disabled">
                      {{ formatReferenceMonth(invoice.referenceMonth) }}
                    </td>
                    <td class="text-disabled">
                      {{ formatDate(invoice.dueDate) }}
                    </td>
                    <td class="text-right">
                      {{ formatCurrency(invoice.amount) }}
                    </td>
                  </tr>
                </tbody>
              </VTable>
            </div>
            <VCardText class="d-flex justify-space-between text-body-1 font-weight-medium">
              <span>Total</span>
              <span>{{ formatCurrency(report.pendingCreditCardTotal) }}</span>
            </VCardText>
          </VCard>
        </VCol>

        <VCol
          v-if="report.pendingBillInstances.length > 0"
          cols="12"
          md="6"
        >
          <VCard title="Contas Pendentes">
            <div style="overflow-x: auto">
              <VTable>
                <thead style="white-space: nowrap">
                  <tr>
                    <th>Conta</th>
                    <th>Mês</th>
                    <th>Vencimento</th>
                    <th class="text-right">
                      Valor
                    </th>
                  </tr>
                </thead>
                <tbody>
                  <tr
                    v-for="instance in report.pendingBillInstances"
                    :key="instance.billInstanceId"
                  >
                    <td>{{ instance.billName }}</td>
                    <td class="text-disabled">
                      {{ formatReferenceMonth(instance.referenceMonth) }}
                    </td>
                    <td class="text-disabled">
                      {{ formatDate(instance.dueDate) }}
                    </td>
                    <td class="text-right">
                      {{ formatCurrency(instance.amount) }}
                    </td>
                  </tr>
                </tbody>
              </VTable>
            </div>
            <VCardText class="d-flex justify-space-between text-body-1 font-weight-medium">
              <span>Total</span>
              <span>{{ formatCurrency(report.pendingBillTotal) }}</span>
            </VCardText>
          </VCard>
        </VCol>
      </VRow>

      <VRow>
        <VCol cols="12">
          <VCard>
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
                    <th style="width: 40px" />
                    <th>Data</th>
                    <th>Tipo</th>
                    <th>Conta</th>
                    <th style="min-width: 200px">
                      Categoria / Destino
                    </th>
                    <th>Descrição</th>
                    <th
                      class="text-right"
                      style="min-width: 120px"
                    >
                      Valor
                    </th>
                  </tr>
                </thead>
                <tbody>
                  <template
                    v-for="transaction in sortedTransactions"
                    :key="transaction.id"
                  >
                    <tr>
                      <td>
                        <VBtn
                          v-if="isInvoicePaymentRow(transaction)"
                          icon
                          size="x-small"
                          variant="text"
                          @click="toggleExpand(transaction)"
                        >
                          <VIcon :icon="expandedIds.has(transaction.id) ? 'tabler-chevron-down' : 'tabler-chevron-right'" />
                        </VBtn>
                      </td>
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
                    </tr>

                    <tr v-if="expandedIds.has(transaction.id)">
                      <td />
                      <td
                        colspan="6"
                        class="pb-4"
                      >
                        <div
                          v-if="loadingInvoiceItems.has(transaction.id)"
                          class="d-flex justify-center py-4"
                        >
                          <VProgressCircular
                            indeterminate
                            size="24"
                          />
                        </div>
                        <div
                          v-else
                          style="overflow-x: auto"
                        >
                          <VTable density="compact">
                            <thead style="white-space: nowrap">
                              <tr>
                                <th>Data</th>
                                <th style="min-width: 200px">
                                  Categoria
                                </th>
                                <th>Descrição</th>
                                <th>Parcela</th>
                                <th class="text-right">
                                  Valor
                                </th>
                              </tr>
                            </thead>
                            <tbody>
                              <tr
                                v-for="item in invoiceItemsCache.get(transaction.id) ?? []"
                                :key="item.id"
                              >
                                <td>{{ formatDate(item.purchaseDate) }}</td>
                                <td>
                                  {{ categoryName(item.categoryId) }}
                                  <span
                                    v-if="subCategoryName(item.subCategoryId)"
                                    class="text-disabled"
                                  >
                                    / {{ subCategoryName(item.subCategoryId) }}
                                  </span>
                                </td>
                                <td class="text-disabled">
                                  {{ item.description || '—' }}
                                </td>
                                <td>
                                  <VChip
                                    v-if="item.totalInstallments > 1"
                                    size="small"
                                    variant="tonal"
                                    color="info"
                                  >
                                    {{ item.installmentNumber }}/{{ item.totalInstallments }}
                                  </VChip>
                                  <VChip
                                    v-else
                                    size="small"
                                    variant="tonal"
                                  >
                                    À vista
                                  </VChip>
                                </td>
                                <td class="text-right">
                                  {{ formatCurrency(item.amount) }}
                                </td>
                              </tr>

                              <tr v-if="(invoiceItemsCache.get(transaction.id) ?? []).length === 0">
                                <td
                                  colspan="5"
                                  class="text-center text-disabled py-4"
                                >
                                  Nenhum lançamento encontrado nesta fatura.
                                </td>
                              </tr>
                            </tbody>
                          </VTable>
                        </div>
                      </td>
                    </tr>
                  </template>

                  <tr v-if="sortedTransactions.length === 0">
                    <td
                      colspan="7"
                      class="text-center text-disabled py-8"
                    >
                      Nenhuma transação encontrada para o período e filtros selecionados.
                    </td>
                  </tr>
                </tbody>
              </VTable>
            </div>
          </VCard>
        </VCol>
      </VRow>
    </template>
  </div>
</template>
