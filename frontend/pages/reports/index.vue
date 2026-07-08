<script setup lang="ts">
import { VForm } from 'vuetify/components/VForm'

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

interface PendingCreditCardInvoiceResponse {
  creditCardId: number
  creditCardName: string
  referenceMonth: string
  dueDate: string
  amount: number
}

interface PendingBillInstanceResponse {
  billInstanceId: number
  billId: number
  billName: string
  referenceMonth: string
  dueDate: string
  amount: number
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

interface PaymentMethodResponse {
  id: number
  name: string
  active: boolean
}

interface SpaceMemberResponse {
  memberId: number
  userId: number
  userName: string
}

const spaceStore = useSpaceStore()
const { error, setError, clearError } = useApiError()

const currencyFormatter = new Intl.NumberFormat('pt-BR', { style: 'currency', currency: 'BRL' })

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

const bankAccounts = ref<BankAccountResponse[]>([])
const categories = ref<CategoryResponse[]>([])
const paymentMethods = ref<PaymentMethodResponse[]>([])
const members = ref<SpaceMemberResponse[]>([])

const from = shallowRef(firstDayOfMonth())
const to = shallowRef(lastDayOfMonth())
const userId = shallowRef<number | null>(null)
const bankAccountId = shallowRef<number | null>(null)
const categoryId = shallowRef<number | null>(null)
const subCategoryId = shallowRef<number | null>(null)
const paymentMethodId = shallowRef<number | null>(null)
const type = shallowRef<TransactionType | null>(null)

const report = ref<ReportResponse | null>(null)
const isLoading = shallowRef(false)
const isLoadingFilters = shallowRef(false)

const filterFormRef = useTemplateRef<InstanceType<typeof VForm>>('filterFormRef')

const fromRules = [(v: string) => !!v || 'Data inicial é obrigatória']
const toRules = [(v: string) => !!v || 'Data final é obrigatória']

const typeItems = [
  { title: 'Todos', value: null },
  { title: 'Receita', value: 'INCOME' },
  { title: 'Despesa', value: 'EXPENSE' },
  { title: 'Transferência', value: 'TRANSFER' },
]

const memberItems = computed(() =>
  members.value.map(m => ({ title: m.userName, value: m.userId })),
)

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

const paymentMethodItems = computed(() =>
  paymentMethods.value.map(pm => ({ title: pm.active ? pm.name : `${pm.name} (inativo)`, value: pm.id })),
)

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
  report.value ? [...report.value.transactions].sort((a, b) => b.transactionDate.localeCompare(a.transactionDate)) : [],
)

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
      paymentMethods.value = []
      members.value = []
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

    const [bankAccountsResult, categoriesResult, paymentMethodsResult, membersResult] = await Promise.all([
      $fetch<BankAccountResponse[]>('/api/bank-accounts', { query: { spaceId } }),
      $fetch<CategoryResponse[]>('/api/categories', { query: { spaceId } }),
      $fetch<PaymentMethodResponse[]>('/api/payment-methods', { query: { spaceId } }),
      $fetch<SpaceMemberResponse[]>(`/api/spaces/${spaceId}/members`),
    ])

    bankAccounts.value = bankAccountsResult
    categories.value = categoriesResult
    paymentMethods.value = paymentMethodsResult
    members.value = membersResult
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

  try {
    report.value = await $fetch<ReportResponse>('/api/reports', {
      method: 'POST',
      body: {
        spaceId: spaceStore.activeSpace.id,
        from: from.value,
        to: to.value,
        userId: userId.value,
        bankAccountId: bankAccountId.value,
        categoryId: categoryId.value,
        subCategoryId: subCategoryId.value,
        paymentMethodId: paymentMethodId.value,
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

function paymentMethodName(id: number | null) {
  if (id === null)
    return '—'

  return paymentMethodsById.value.get(id)?.name ?? '—'
}

function formatAmount(transaction: TransactionResponse) {
  const formatted = currencyFormatter.format(transaction.amount)

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
              md="3"
            >
              <AppTextField
                v-model="from"
                type="date"
                label="De"
                :rules="fromRules"
              />
            </VCol>

            <VCol
              cols="12"
              md="3"
            >
              <AppTextField
                v-model="to"
                type="date"
                label="Até"
                :rules="toRules"
              />
            </VCol>

            <VCol
              cols="12"
              md="3"
            >
              <AppSelect
                v-model="userId"
                label="Membro"
                :items="memberItems"
                clearable
                :loading="isLoadingFilters"
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

            <VCol
              cols="12"
              md="3"
            >
              <AppSelect
                v-model="paymentMethodId"
                label="Forma de pagamento"
                :items="paymentMethodItems"
                clearable
                :loading="isLoadingFilters"
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
            :stats="currencyFormatter.format(report.totalIncome)"
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
            :stats="currencyFormatter.format(report.totalExpense)"
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
            :stats="currencyFormatter.format(report.balance)"
          />
        </VCol>
      </VRow>

      <VRow>
        <VCol
          cols="12"
          md="6"
        >
          <CardStatisticsVerticalSimple
            title="Saldo Atual"
            :color="report.currentBalance >= 0 ? 'primary' : 'error'"
            icon="tabler-wallet"
            :stats="currencyFormatter.format(report.currentBalance)"
          />
        </VCol>

        <VCol
          cols="12"
          md="6"
        >
          <CardStatisticsVerticalSimple
            title="Saldo Previsto"
            :color="report.projectedBalance >= 0 ? 'success' : 'error'"
            icon="tabler-trending-up"
            :stats="currencyFormatter.format(report.projectedBalance)"
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
                      {{ currencyFormatter.format(invoice.amount) }}
                    </td>
                  </tr>
                </tbody>
              </VTable>
            </div>
            <VCardText class="d-flex justify-space-between text-body-1 font-weight-medium">
              <span>Total</span>
              <span>{{ currencyFormatter.format(report.pendingCreditCardTotal) }}</span>
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
                      {{ currencyFormatter.format(instance.amount) }}
                    </td>
                  </tr>
                </tbody>
              </VTable>
            </div>
            <VCardText class="d-flex justify-space-between text-body-1 font-weight-medium">
              <span>Total</span>
              <span>{{ currencyFormatter.format(report.pendingBillTotal) }}</span>
            </VCardText>
          </VCard>
        </VCol>
      </VRow>

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
                <th>Data</th>
                <th>Tipo</th>
                <th>Conta</th>
                <th style="min-width: 200px">
                  Categoria / Destino
                </th>
                <th>Forma de Pagamento</th>
                <th>Descrição</th>
                <th class="text-right">
                  Valor
                </th>
              </tr>
            </thead>
            <tbody>
              <tr
                v-for="transaction in sortedTransactions"
                :key="transaction.id"
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
              </tr>

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
    </template>
  </div>
</template>
