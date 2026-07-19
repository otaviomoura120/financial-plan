<script setup lang="ts">
import { VForm } from 'vuetify/components/VForm'

type TransactionType = 'INCOME' | 'EXPENSE' | 'TRANSFER'

type CategoryReportItemSource = 'TRANSACTION' | 'CREDIT_CARD'

interface CategoryReportItemResponse {
  id: number
  source: CategoryReportItemSource
  type: TransactionType
  date: string
  description?: string | null
  amount: number
  userId: number | null
  bankAccountId: number | null
  creditCardId: number | null
  creditCardName: string | null
  installmentNumber: number | null
  totalInstallments: number | null
  totalAmount: number | null
  referenceMonth: string | null
  dueDate: string | null
}

interface CategoryReportSubGroupResponse {
  subCategoryId: number | null
  subCategoryName: string | null
  totalIncome: number
  totalExpense: number
  total: number
  items: CategoryReportItemResponse[]
}

interface CategoryReportGroupResponse {
  categoryId: number | null
  categoryName: string | null
  totalIncome: number
  totalExpense: number
  total: number
  incomePercentage: number
  expensePercentage: number
  subGroups: CategoryReportSubGroupResponse[]
}

interface CategoryReportResponse {
  totalIncome: number
  totalExpense: number
  balance: number
  groups: CategoryReportGroupResponse[]
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

interface CreditCardResponse {
  id: number
  name: string
  active: boolean
  bankAccountId: number | null
  bankAccountName: string | null
}

interface FlatReportItem extends CategoryReportItemResponse {
  categoryName: string | null
  subCategoryName: string | null
}

const spaceStore = useSpaceStore()
const { error, setError, clearError } = useApiError()

const bankAccounts = ref<BankAccountResponse[]>([])
const categories = ref<CategoryResponse[]>([])
const creditCards = ref<CreditCardResponse[]>([])

const selectedMonth = shallowRef(currentMonthValue())
const bankAccountId = shallowRef<number | null>(null)
const categoryId = shallowRef<number | null>(null)
const subCategoryId = shallowRef<number | null>(null)
const creditCardId = shallowRef<number | null>(null)
const type = shallowRef<TransactionType | null>(null)
const groupByCategory = shallowRef(true)

const report = ref<CategoryReportResponse | null>(null)
const isLoading = shallowRef(false)
const isLoadingFilters = shallowRef(false)

const filterFormRef = useTemplateRef<InstanceType<typeof VForm>>('filterFormRef')

const typeItems = [
  { title: 'Todos', value: null },
  { title: 'Receita', value: 'INCOME' },
  { title: 'Despesa', value: 'EXPENSE' },
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

const creditCardItems = computed(() =>
  creditCards.value.map(cc => ({ title: cc.active ? cc.name : `${cc.name} (inativo)`, value: cc.id })),
)

const bankAccountsById = computed(() => new Map(bankAccounts.value.map(ba => [ba.id, ba])))

const expandedCategories = ref(new Set<string>())
const expandedSubGroups = ref(new Set<string>())

const flatItems = computed<FlatReportItem[]>(() => {
  if (!report.value)
    return []

  const items: FlatReportItem[] = []

  for (const group of report.value.groups) {
    for (const subGroup of group.subGroups) {
      for (const item of subGroup.items)
        items.push({ ...item, categoryName: group.categoryName, subCategoryName: subGroup.subCategoryName })
    }
  }

  return items.sort((a, b) => b.date.localeCompare(a.date))
})

function categoryKey(group: CategoryReportGroupResponse) {
  return String(group.categoryId ?? 'none')
}

function subGroupKey(group: CategoryReportGroupResponse, subGroup: CategoryReportSubGroupResponse) {
  return `${categoryKey(group)}:${subGroup.subCategoryId ?? 'none'}`
}

function toggleCategory(group: CategoryReportGroupResponse) {
  const key = categoryKey(group)

  if (expandedCategories.value.has(key))
    expandedCategories.value.delete(key)
  else
    expandedCategories.value.add(key)
}

function toggleSubGroup(group: CategoryReportGroupResponse, subGroup: CategoryReportSubGroupResponse) {
  const key = subGroupKey(group, subGroup)

  if (expandedSubGroups.value.has(key))
    expandedSubGroups.value.delete(key)
  else
    expandedSubGroups.value.add(key)
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
      creditCards.value = []
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

    const [bankAccountsResult, categoriesResult, creditCardsResult] = await Promise.all([
      $fetch<BankAccountResponse[]>('/api/bank-accounts', { query: { spaceId } }),
      $fetch<CategoryResponse[]>('/api/categories', { query: { spaceId } }),
      $fetch<CreditCardResponse[]>('/api/credit-cards', { query: { spaceId } }),
    ])

    bankAccounts.value = bankAccountsResult
    categories.value = categoriesResult
    creditCards.value = creditCardsResult
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
  expandedCategories.value = new Set()
  expandedSubGroups.value = new Set()

  try {
    report.value = await $fetch<CategoryReportResponse>('/api/reports/by-category', {
      method: 'POST',
      body: {
        spaceId: spaceStore.activeSpace.id,
        from: selectedMonth.value,
        to: lastDayOfMonthOf(selectedMonth.value),
        bankAccountId: bankAccountId.value,
        categoryId: categoryId.value,
        subCategoryId: subCategoryId.value,
        type: type.value,
        creditCardId: creditCardId.value,
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

function formatSignedAmount(itemType: TransactionType, amount: number) {
  const formatted = formatCurrency(amount)

  if (itemType === 'INCOME')
    return `+ ${formatted}`

  return `- ${formatted}`
}

function formatPercentage(value: number) {
  return `${value.toFixed(2).replace('.', ',')}%`
}

function groupPercentage(group: CategoryReportGroupResponse) {
  if (group.totalExpense > 0)
    return { label: `${formatPercentage(group.expensePercentage)} das despesas`, color: 'error' }

  if (group.totalIncome > 0)
    return { label: `${formatPercentage(group.incomePercentage)} das receitas`, color: 'success' }

  return null
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

            <VCol
              cols="12"
              md="3"
            >
              <AppSelect
                v-model="creditCardId"
                label="Cartão de Crédito"
                :items="creditCardItems"
                clearable
                :loading="isLoadingFilters"
                persistent-hint
                hint="Ao filtrar, mostra apenas as compras do cartão"
              />
            </VCol>

            <VCol
              cols="12"
              md="3"
              class="d-flex align-center"
            >
              <VCheckbox
                v-model="groupByCategory"
                label="Agrupar por categoria"
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
          <VCard :title="groupByCategory ? 'Gastos por Categoria' : 'Lançamentos do Período'">
            <div
              v-if="isLoading"
              class="d-flex justify-center py-10"
            >
              <VProgressCircular indeterminate />
            </div>

            <div
              v-else-if="groupByCategory"
              style="overflow-x: auto"
            >
              <VTable>
                <thead style="white-space: nowrap">
                  <tr>
                    <th style="width: 40px" />
                    <th style="min-width: 220px">
                      Categoria
                    </th>
                    <th>% do Período</th>
                    <th class="text-right">
                      Receitas
                    </th>
                    <th class="text-right">
                      Despesas
                    </th>
                    <th
                      class="text-right"
                      style="min-width: 120px"
                    >
                      Total
                    </th>
                  </tr>
                </thead>
                <tbody>
                  <template
                    v-for="group in report.groups"
                    :key="categoryKey(group)"
                  >
                    <tr>
                      <td>
                        <VBtn
                          icon
                          size="x-small"
                          variant="text"
                          @click="toggleCategory(group)"
                        >
                          <VIcon :icon="expandedCategories.has(categoryKey(group)) ? 'tabler-chevron-down' : 'tabler-chevron-right'" />
                        </VBtn>
                      </td>
                      <td class="font-weight-medium">
                        {{ group.categoryName ?? 'Sem categoria' }}
                      </td>
                      <td>
                        <VChip
                          v-if="groupPercentage(group)"
                          size="small"
                          variant="tonal"
                          :color="groupPercentage(group)!.color"
                        >
                          {{ groupPercentage(group)!.label }}
                        </VChip>
                      </td>
                      <td class="text-right text-success">
                        {{ formatCurrency(group.totalIncome) }}
                      </td>
                      <td class="text-right text-error">
                        {{ formatCurrency(group.totalExpense) }}
                      </td>
                      <td
                        class="text-right font-weight-medium"
                        :class="group.total >= 0 ? 'text-success' : 'text-error'"
                      >
                        {{ formatCurrency(group.total) }}
                      </td>
                    </tr>

                    <template v-if="expandedCategories.has(categoryKey(group))">
                      <template
                        v-for="subGroup in group.subGroups"
                        :key="subGroupKey(group, subGroup)"
                      >
                        <tr>
                          <td />
                          <td class="ps-8">
                            <VBtn
                              icon
                              size="x-small"
                              variant="text"
                              @click="toggleSubGroup(group, subGroup)"
                            >
                              <VIcon :icon="expandedSubGroups.has(subGroupKey(group, subGroup)) ? 'tabler-chevron-down' : 'tabler-chevron-right'" />
                            </VBtn>
                            {{ subGroup.subCategoryName ?? 'Sem subcategoria' }}
                          </td>
                          <td />
                          <td class="text-right text-success">
                            {{ formatCurrency(subGroup.totalIncome) }}
                          </td>
                          <td class="text-right text-error">
                            {{ formatCurrency(subGroup.totalExpense) }}
                          </td>
                          <td
                            class="text-right"
                            :class="subGroup.total >= 0 ? 'text-success' : 'text-error'"
                          >
                            {{ formatCurrency(subGroup.total) }}
                          </td>
                        </tr>

                        <tr v-if="expandedSubGroups.has(subGroupKey(group, subGroup))">
                          <td />
                          <td
                            colspan="5"
                            class="pb-4"
                          >
                            <div style="overflow-x: auto">
                              <VTable density="compact">
                                <thead style="white-space: nowrap">
                                  <tr>
                                    <th>Data</th>
                                    <th>Origem</th>
                                    <th>Descrição</th>
                                    <th class="text-right">
                                      Valor
                                    </th>
                                  </tr>
                                </thead>
                                <tbody>
                                  <tr
                                    v-for="item in subGroup.items"
                                    :key="`${item.source}-${item.id}`"
                                  >
                                    <td>
                                      {{ formatDate(item.date) }}
                                      <div
                                        v-if="item.source === 'CREDIT_CARD' && item.dueDate"
                                        class="text-caption text-disabled"
                                      >
                                        Fatura: {{ formatReferenceMonth(item.dueDate) }}
                                      </div>
                                    </td>
                                    <td>
                                      <template v-if="item.source === 'CREDIT_CARD'">
                                        <VChip
                                          size="small"
                                          variant="tonal"
                                          color="secondary"
                                          prepend-icon="tabler-credit-card"
                                        >
                                          {{ item.creditCardName ?? 'Cartão' }}
                                        </VChip>
                                        <VChip
                                          v-if="(item.totalInstallments ?? 0) > 1"
                                          size="small"
                                          variant="tonal"
                                          color="info"
                                          class="ms-1"
                                        >
                                          {{ item.installmentNumber }}/{{ item.totalInstallments }}
                                        </VChip>
                                      </template>
                                      <template v-else>
                                        {{ bankAccountName(item.bankAccountId) }}
                                      </template>
                                    </td>
                                    <td class="text-disabled">
                                      {{ item.description || '—' }}
                                    </td>
                                    <td
                                      class="text-right"
                                      :class="item.type === 'INCOME' ? 'text-success' : 'text-error'"
                                    >
                                      {{ formatSignedAmount(item.type, item.amount) }}
                                      <div
                                        v-if="item.source === 'CREDIT_CARD' && (item.totalInstallments ?? 0) > 1"
                                        class="text-caption text-disabled"
                                      >
                                        Total: {{ formatCurrency(item.totalAmount ?? 0) }}
                                      </div>
                                    </td>
                                  </tr>
                                </tbody>
                              </VTable>
                            </div>
                          </td>
                        </tr>
                      </template>
                    </template>
                  </template>

                  <tr v-if="report.groups.length === 0">
                    <td
                      colspan="6"
                      class="text-center text-disabled py-8"
                    >
                      Nenhum lançamento encontrado para o período e filtros selecionados.
                    </td>
                  </tr>
                </tbody>
              </VTable>
            </div>

            <div
              v-else
              style="overflow-x: auto"
            >
              <VTable>
                <thead style="white-space: nowrap">
                  <tr>
                    <th>Data</th>
                    <th style="min-width: 200px">
                      Categoria
                    </th>
                    <th>Origem</th>
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
                  <tr
                    v-for="item in flatItems"
                    :key="`${item.source}-${item.id}`"
                  >
                    <td>
                      {{ formatDate(item.date) }}
                      <div
                        v-if="item.source === 'CREDIT_CARD' && item.dueDate"
                        class="text-caption text-disabled"
                      >
                        Fatura: {{ formatReferenceMonth(item.dueDate) }}
                      </div>
                    </td>
                    <td>
                      {{ item.categoryName ?? 'Sem categoria' }}
                      <span
                        v-if="item.subCategoryName"
                        class="text-disabled"
                      >
                        / {{ item.subCategoryName }}
                      </span>
                    </td>
                    <td>
                      <template v-if="item.source === 'CREDIT_CARD'">
                        <VChip
                          size="small"
                          variant="tonal"
                          color="secondary"
                          prepend-icon="tabler-credit-card"
                        >
                          {{ item.creditCardName ?? 'Cartão' }}
                        </VChip>
                        <VChip
                          v-if="(item.totalInstallments ?? 0) > 1"
                          size="small"
                          variant="tonal"
                          color="info"
                          class="ms-1"
                        >
                          {{ item.installmentNumber }}/{{ item.totalInstallments }}
                        </VChip>
                      </template>
                      <template v-else>
                        {{ bankAccountName(item.bankAccountId) }}
                      </template>
                    </td>
                    <td class="text-disabled">
                      {{ item.description || '—' }}
                    </td>
                    <td
                      class="text-right"
                      :class="item.type === 'INCOME' ? 'text-success' : 'text-error'"
                    >
                      {{ formatSignedAmount(item.type, item.amount) }}
                      <div
                        v-if="item.source === 'CREDIT_CARD' && (item.totalInstallments ?? 0) > 1"
                        class="text-caption text-disabled"
                      >
                        Total: {{ formatCurrency(item.totalAmount ?? 0) }}
                      </div>
                    </td>
                  </tr>

                  <tr v-if="flatItems.length === 0">
                    <td
                      colspan="5"
                      class="text-center text-disabled py-8"
                    >
                      Nenhum lançamento encontrado para o período e filtros selecionados.
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
