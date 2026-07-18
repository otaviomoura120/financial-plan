<script setup lang="ts">
import type { ApexOptions } from 'apexcharts'
import { useTheme } from 'vuetify'

interface Props {
  spaceId: number
}

const props = defineProps<Props>()

const emit = defineEmits<{
  'update:total': [total: number]
  'update:period-label': [label: string]
}>()

interface CategoryReportSubGroupResponse {
  subCategoryId: number | null
  subCategoryName: string | null
  totalExpense: number
}

interface CategoryReportGroupResponse {
  categoryId: number | null
  categoryName: string | null
  totalExpense: number
  expensePercentage: number
  subGroups: CategoryReportSubGroupResponse[]
}

interface CategoryReportResponse {
  totalIncome: number
  totalExpense: number
  balance: number
  groups: CategoryReportGroupResponse[]
}

interface CategorySlice {
  categoryId: number | null
  name: string
  total: number
  percentage: number
}

type PeriodPreset = 'thisMonth' | 'lastMonth' | 'last30Days' | 'thisYear'

const MAX_CATEGORY_SLICES = 6

// fixed order — index = ranking by totalExpense desc, never cycled
const CATEGORY_COLORS_LIGHT = ['#0E82A8', '#D1662A', '#8C3F79', '#3956A6', '#A98A00', '#5C7A1E']
const CATEGORY_COLORS_DARK = ['#2E9BC7', '#D96B34', '#B565A0', '#7186D6', '#AD861A', '#7FA042']
const OUTROS_COLOR_LIGHT = '#B8B09E'
const OUTROS_COLOR_DARK = '#8A8279'

const { error, setError, clearError } = useApiError()
const vuetifyTheme = useTheme()

const periodPreset = ref<PeriodPreset>('thisMonth')

const periodOptions: { title: string; value: PeriodPreset }[] = [
  { title: 'Este Mês', value: 'thisMonth' },
  { title: 'Mês Passado', value: 'lastMonth' },
  { title: 'Últimos 30 Dias', value: 'last30Days' },
  { title: 'Este Ano', value: 'thisYear' },
]

const periodLabels: Record<PeriodPreset, string> = {
  thisMonth: 'Este Mês',
  lastMonth: 'Mês Passado',
  last30Days: 'Últimos 30 Dias',
  thisYear: 'Este Ano',
}

const categoryReport = ref<CategoryReportResponse | null>(null)
const isLoading = shallowRef(false)
const expandedCategoryId = ref<number | 'none' | null>(null)

function toLocalDateString(date: Date) {
  const year = date.getFullYear()
  const month = String(date.getMonth() + 1).padStart(2, '0')
  const day = String(date.getDate()).padStart(2, '0')

  return `${year}-${month}-${day}`
}

function periodRange(preset: PeriodPreset) {
  const now = new Date()

  if (preset === 'thisMonth') {
    return {
      from: toLocalDateString(new Date(now.getFullYear(), now.getMonth(), 1)),
      to: toLocalDateString(new Date(now.getFullYear(), now.getMonth() + 1, 0)),
    }
  }

  if (preset === 'lastMonth') {
    return {
      from: toLocalDateString(new Date(now.getFullYear(), now.getMonth() - 1, 1)),
      to: toLocalDateString(new Date(now.getFullYear(), now.getMonth(), 0)),
    }
  }

  if (preset === 'last30Days') {
    const from = new Date(now)

    from.setDate(from.getDate() - 29)

    return { from: toLocalDateString(from), to: toLocalDateString(now) }
  }

  return {
    from: toLocalDateString(new Date(now.getFullYear(), 0, 1)),
    to: toLocalDateString(new Date(now.getFullYear(), 11, 31)),
  }
}

const totalExpense = computed(() => categoryReport.value?.totalExpense ?? 0)

const expenseGroups = computed(() =>
  (categoryReport.value?.groups ?? [])
    .filter(g => g.totalExpense > 0)
    .sort((a, b) => b.totalExpense - a.totalExpense),
)

const topGroups = computed(() => expenseGroups.value.slice(0, MAX_CATEGORY_SLICES))

const categorySlices = computed<CategorySlice[]>(() => {
  const top: CategorySlice[] = topGroups.value.map(g => ({
    categoryId: g.categoryId,
    name: g.categoryName ?? 'Sem categoria',
    total: g.totalExpense,
    percentage: g.expensePercentage,
  }))

  const rest = expenseGroups.value.slice(MAX_CATEGORY_SLICES)

  if (rest.length > 0) {
    top.push({
      categoryId: null,
      name: 'Outros',
      total: rest.reduce((sum, g) => sum + g.totalExpense, 0),
      percentage: rest.reduce((sum, g) => sum + g.expensePercentage, 0),
    })
  }

  return top
})

function isOutrosSlice(slice: CategorySlice, index: number) {
  return slice.categoryId === null && index === categorySlices.value.length - 1 && expenseGroups.value.length > MAX_CATEGORY_SLICES
}

const chartColors = computed(() => {
  const isDark = vuetifyTheme.current.value.dark
  const colors = isDark ? CATEGORY_COLORS_DARK : CATEGORY_COLORS_LIGHT
  const outrosColor = isDark ? OUTROS_COLOR_DARK : OUTROS_COLOR_LIGHT

  return categorySlices.value.map((slice, i) =>
    isOutrosSlice(slice, i) ? outrosColor : colors[i % colors.length],
  )
})

const chartOptions = computed<ApexOptions>(() => ({
  chart: { type: 'donut' },
  labels: categorySlices.value.map(s => s.name),
  colors: chartColors.value,
  legend: {
    position: 'bottom',
    labels: { colors: vuetifyTheme.current.value.colors['on-surface'] },
  },
  dataLabels: { enabled: false },
  tooltip: {
    theme: vuetifyTheme.current.value.dark ? 'dark' : 'light',
    y: { formatter: (val: number) => formatCurrency(val) },
  },
  stroke: {
    colors: [vuetifyTheme.current.value.colors.surface],
    width: 2,
  },
  plotOptions: {
    pie: {
      donut: {
        labels: {
          show: true,
          total: {
            show: true,
            label: 'Total',
            formatter: () => formatCurrency(totalExpense.value),
          },
        },
      },
    },
  },
  responsive: [{ breakpoint: 600, options: { legend: { position: 'bottom' } } }],
}))

watch(totalExpense, value => emit('update:total', value), { immediate: true })
watch(periodPreset, value => emit('update:period-label', periodLabels[value]), { immediate: true })

async function fetchCategoryReport() {
  isLoading.value = true
  clearError()
  expandedCategoryId.value = null

  try {
    const { from, to } = periodRange(periodPreset.value)

    categoryReport.value = await $fetch<CategoryReportResponse>('/api/reports/by-category', {
      method: 'POST',
      body: { spaceId: props.spaceId, from, to, type: 'EXPENSE' },
    })
  }
  catch (e) {
    setError(e)
  }
  finally {
    isLoading.value = false
  }
}

watch([() => props.spaceId, periodPreset], fetchCategoryReport, { immediate: true })

function toggleExpand(categoryId: number | null) {
  const key = categoryId ?? 'none'

  expandedCategoryId.value = expandedCategoryId.value === key ? null : key
}

function subGroupsFor(categoryId: number | null) {
  return topGroups.value.find(g => g.categoryId === categoryId)?.subGroups ?? []
}
</script>

<template>
  <VCard title="Gastos por Categoria e Subcategoria">
    <VCardText class="pt-0">
      <VChipGroup
        v-model="periodPreset"
        mandatory
        selected-class="text-primary"
      >
        <VChip
          v-for="option in periodOptions"
          :key="option.value"
          :value="option.value"
          size="small"
          variant="tonal"
          filter
        >
          {{ option.title }}
        </VChip>
      </VChipGroup>
    </VCardText>

    <VCardText>
      <ApiErrorAlert :error="error" />

      <div
        v-if="isLoading"
        class="d-flex justify-center py-8"
      >
        <VProgressCircular indeterminate />
      </div>

      <div
        v-else-if="totalExpense === 0"
        class="d-flex flex-column align-center text-center text-disabled py-8"
      >
        <VIcon
          icon="tabler-chart-donut"
          size="32"
          class="mb-2"
        />
        <div>Nenhuma despesa registrada no período selecionado.</div>
      </div>

      <template v-else>
        <VueApexCharts
          type="donut"
          height="300"
          :series="categorySlices.map(s => s.total)"
          :options="chartOptions"
        />

        <VDivider class="my-4" />

        <div class="d-flex flex-column gap-1">
          <div
            v-for="(slice, index) in categorySlices"
            :key="slice.categoryId ?? 'outros'"
          >
            <div
              class="d-flex align-center gap-2 py-1"
              :style="!isOutrosSlice(slice, index) ? 'cursor: pointer' : undefined"
              @click="!isOutrosSlice(slice, index) && toggleExpand(slice.categoryId)"
            >
              <VIcon
                icon="tabler-circle-filled"
                size="10"
                :color="chartColors[index]"
              />
              <span class="flex-grow-1 text-body-2">{{ slice.name }}</span>
              <span class="text-caption text-disabled">{{ slice.percentage.toFixed(1) }}%</span>
              <span class="text-body-2 font-weight-medium">{{ formatCurrency(slice.total) }}</span>
              <VBtn
                v-if="!isOutrosSlice(slice, index)"
                icon
                size="x-small"
                variant="text"
              >
                <VIcon :icon="expandedCategoryId === (slice.categoryId ?? 'none') ? 'tabler-chevron-down' : 'tabler-chevron-right'" />
              </VBtn>
            </div>

            <div
              v-if="expandedCategoryId === (slice.categoryId ?? 'none')"
              class="ps-6 d-flex flex-column gap-1 pb-2"
            >
              <div
                v-for="subGroup in subGroupsFor(slice.categoryId)"
                :key="subGroup.subCategoryId ?? 'none'"
                class="d-flex align-center gap-2 text-body-2 text-disabled"
              >
                <span class="flex-grow-1">{{ subGroup.subCategoryName ?? 'Sem subcategoria' }}</span>
                <span>{{ formatCurrency(subGroup.totalExpense) }}</span>
              </div>
            </div>
          </div>
        </div>
      </template>

      <div class="d-flex justify-end mt-4">
        <VBtn
          variant="tonal"
          color="primary"
          append-icon="tabler-arrow-right"
          to="/reports/by-category"
        >
          Ver relatório completo
        </VBtn>
      </div>
    </VCardText>
  </VCard>
</template>
