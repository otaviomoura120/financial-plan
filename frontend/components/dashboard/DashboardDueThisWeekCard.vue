<script setup lang="ts">
interface Props {
  spaceId: number
}

const props = defineProps<Props>()

const emit = defineEmits<{
  'update:total': [total: number]
  'update:period-label': [label: string]
}>()

type BillInstanceStatus = 'PENDING' | 'PAID'

interface BillInstanceResponse {
  id: number
  name: string
  dueDate: string
  amount: number
  status: BillInstanceStatus
}

interface CreditCardInvoiceResponse {
  creditCardId: number
  creditCardName: string
  referenceMonth: string
  dueDate: string
  totalAmount: number
  paid: boolean
}

interface DueEntry {
  type: 'bill' | 'invoice'
  id: string
  name: string
  dueDate: string
  amount: number
}

const { error, setError, clearError } = useApiError()

const windowDays = ref(7)

const windowOptions = [
  { title: '7 dias', value: 7 },
  { title: '15 dias', value: 15 },
  { title: '30 dias', value: 30 },
]

const bills = ref<BillInstanceResponse[]>([])
const invoices = ref<CreditCardInvoiceResponse[]>([])
const isLoading = shallowRef(false)

function toLocalDateString(date: Date) {
  const year = date.getFullYear()
  const month = String(date.getMonth() + 1).padStart(2, '0')
  const day = String(date.getDate()).padStart(2, '0')

  return `${year}-${month}-${day}`
}

function addDays(date: Date, days: number) {
  const result = new Date(date)

  result.setDate(result.getDate() + days)

  return result
}

const pendingBills = computed(() => bills.value.filter(b => b.status === 'PENDING'))
const unpaidInvoices = computed(() => invoices.value.filter(i => !i.paid))

const entries = computed<DueEntry[]>(() => {
  const billEntries: DueEntry[] = pendingBills.value.map(b => ({
    type: 'bill',
    id: `bill-${b.id}`,
    name: b.name,
    dueDate: b.dueDate,
    amount: b.amount,
  }))

  const invoiceEntries: DueEntry[] = unpaidInvoices.value.map(i => ({
    type: 'invoice',
    id: `invoice-${i.creditCardId}-${i.referenceMonth}`,
    name: `Fatura ${i.creditCardName}`,
    dueDate: i.dueDate,
    amount: i.totalAmount,
  }))

  return [...billEntries, ...invoiceEntries].sort((a, b) => a.dueDate.localeCompare(b.dueDate))
})

const total = computed(() => entries.value.reduce((sum, e) => sum + e.amount, 0))

watch(total, value => emit('update:total', value), { immediate: true })
watch(windowDays, value => emit('update:period-label', `Próximos ${value} dias`), { immediate: true })

function parseIsoDateUtc(isoDate: string) {
  const year = Number(isoDate.slice(0, 4))
  const month = Number(isoDate.slice(5, 7))
  const day = Number(isoDate.slice(8, 10))

  return Date.UTC(year, month - 1, day)
}

function daysUntil(dueDate: string) {
  const todayUtc = parseIsoDateUtc(toLocalDateString(new Date()))
  const dueUtc = parseIsoDateUtc(dueDate)

  return Math.round((dueUtc - todayUtc) / (1000 * 60 * 60 * 24))
}

function urgencyColor(dueDate: string) {
  const days = daysUntil(dueDate)

  if (days <= 0)
    return 'error'

  if (days <= 3)
    return 'warning'

  return 'primary'
}

function urgencyLabel(dueDate: string) {
  const days = daysUntil(dueDate)

  if (days < 0)
    return 'Atrasada'

  if (days === 0)
    return 'Vence hoje'

  return `Vence em ${days} dia${days > 1 ? 's' : ''}`
}

function formatDate(isoDate: string) {
  const [year, month, day] = isoDate.split('-')

  return `${day}/${month}/${year}`
}

async function fetchDueEntries() {
  isLoading.value = true
  clearError()

  try {
    const today = new Date()
    const from = toLocalDateString(today)
    const to = toLocalDateString(addDays(today, windowDays.value - 1))

    const [billsResult, invoicesResult] = await Promise.all([
      $fetch<BillInstanceResponse[]>('/api/bills/instances', {
        query: { spaceId: props.spaceId, from, to },
      }),
      $fetch<CreditCardInvoiceResponse[]>('/api/credit-cards/invoices', {
        query: { spaceId: props.spaceId, from, to },
      }),
    ])

    bills.value = billsResult
    invoices.value = invoicesResult
  }
  catch (e) {
    setError(e)
  }
  finally {
    isLoading.value = false
  }
}

watch([() => props.spaceId, windowDays], fetchDueEntries, { immediate: true })
</script>

<template>
  <VCard title="Contas a Pagar Vencendo">
    <VCardText class="pt-0">
      <VChipGroup
        v-model="windowDays"
        mandatory
        selected-class="text-primary"
      >
        <VChip
          v-for="option in windowOptions"
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
        v-else-if="entries.length === 0"
        class="d-flex flex-column align-center text-center text-disabled py-8"
      >
        <VIcon
          icon="tabler-circle-check"
          size="32"
          class="mb-2"
        />
        <div>Nenhuma conta ou fatura vencendo no período selecionado.</div>
      </div>

      <VTimeline
        v-else
        density="compact"
        align="start"
        truncate-line="both"
        class="pt-2"
      >
        <VTimelineItem
          v-for="entry in entries"
          :key="entry.id"
          :dot-color="urgencyColor(entry.dueDate)"
          size="small"
        >
          <template #icon>
            <VIcon
              :icon="entry.type === 'bill' ? 'tabler-receipt-2' : 'tabler-credit-card'"
              size="14"
            />
          </template>

          <div class="d-flex justify-space-between align-center flex-wrap gap-2">
            <div>
              <div class="font-weight-medium">
                {{ entry.name }}
              </div>
              <div class="d-flex align-center gap-2 mt-1">
                <VChip
                  size="x-small"
                  variant="tonal"
                  :color="urgencyColor(entry.dueDate)"
                >
                  {{ urgencyLabel(entry.dueDate) }}
                </VChip>
                <span class="text-caption text-disabled">{{ formatDate(entry.dueDate) }}</span>
              </div>
            </div>
            <div class="text-body-1 font-weight-medium">
              {{ formatCurrency(entry.amount) }}
            </div>
          </div>
        </VTimelineItem>
      </VTimeline>

      <template v-if="!isLoading && entries.length > 0">
        <VDivider class="my-4" />
        <div class="d-flex justify-space-between text-body-1 font-weight-medium">
          <span>Total</span>
          <span>{{ formatCurrency(total) }}</span>
        </div>
      </template>
    </VCardText>
  </VCard>
</template>
