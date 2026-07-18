<script setup lang="ts">
interface Props {
  spaceId: number
}

const props = defineProps<Props>()

const emit = defineEmits<{
  'update:total': [total: number]
  'update:period-label': [label: string]
}>()

interface CreditCardResponse {
  id: number
  name: string
  limit: number
  active: boolean
}

interface CreditCardInvoiceResponse {
  creditCardId: number
  referenceMonth: string
  dueDate: string
  totalAmount: number
  paid: boolean
}

interface OpenInvoiceEntry {
  card: CreditCardResponse
  invoice: CreditCardInvoiceResponse | null
}

const { error, setError, clearError } = useApiError()

const showPrevious = ref(false)

const periodOptions = [
  { title: 'Fatura Atual', value: false },
  { title: 'Fatura Anterior', value: true },
]

const creditCards = ref<CreditCardResponse[]>([])
const invoices = ref<CreditCardInvoiceResponse[]>([])
const isLoading = shallowRef(false)

function toLocalDateString(date: Date) {
  const year = date.getFullYear()
  const month = String(date.getMonth() + 1).padStart(2, '0')
  const day = String(date.getDate()).padStart(2, '0')

  return `${year}-${month}-${day}`
}

const activeCards = computed(() => creditCards.value.filter(c => c.active))

const entries = computed<OpenInvoiceEntry[]>(() => {
  const sortedByCard = new Map<number, CreditCardInvoiceResponse[]>()

  for (const invoice of invoices.value) {
    const list = sortedByCard.get(invoice.creditCardId) ?? []

    list.push(invoice)
    sortedByCard.set(invoice.creditCardId, list)
  }

  for (const list of sortedByCard.values())
    list.sort((a, b) => b.dueDate.localeCompare(a.dueDate))

  const index = showPrevious.value ? 1 : 0

  return activeCards.value.map(card => ({
    card,
    invoice: sortedByCard.get(card.id)?.[index] ?? null,
  }))
})

const total = computed(() => entries.value.reduce((sum, e) => sum + (e.invoice?.totalAmount ?? 0), 0))

watch(total, value => emit('update:total', value), { immediate: true })
watch(showPrevious, value => emit('update:period-label', value ? 'Fatura Anterior' : 'Fatura Atual'), { immediate: true })

function usagePercentage(entry: OpenInvoiceEntry) {
  if (entry.card.limit <= 0)
    return 0

  return Math.min(100, ((entry.invoice?.totalAmount ?? 0) / entry.card.limit) * 100)
}

function usageColor(entry: OpenInvoiceEntry) {
  const pct = usagePercentage(entry)

  if (pct > 80)
    return 'error'

  if (pct >= 50)
    return 'warning'

  return 'success'
}

function formatDate(isoDate: string | undefined) {
  if (!isoDate)
    return '—'

  const [year, month, day] = isoDate.split('-')

  return `${day}/${month}/${year}`
}

async function fetchCards() {
  isLoading.value = true
  clearError()

  try {
    const now = new Date()
    const from = toLocalDateString(new Date(now.getFullYear(), now.getMonth() - 1, 1))
    const to = toLocalDateString(new Date(now.getFullYear(), now.getMonth() + 2, 0))

    const [cardsResult, invoicesResult] = await Promise.all([
      $fetch<CreditCardResponse[]>('/api/credit-cards', { query: { spaceId: props.spaceId } }),
      $fetch<CreditCardInvoiceResponse[]>('/api/credit-cards/invoices', {
        query: { spaceId: props.spaceId, from, to },
      }),
    ])

    creditCards.value = cardsResult
    invoices.value = invoicesResult
  }
  catch (e) {
    setError(e)
  }
  finally {
    isLoading.value = false
  }
}

watch(() => props.spaceId, fetchCards, { immediate: true })
</script>

<template>
  <VCard title="Total de Gasto em Cada Cartão">
    <VCardText class="pt-0">
      <VChipGroup
        v-model="showPrevious"
        mandatory
        selected-class="text-primary"
      >
        <VChip
          v-for="option in periodOptions"
          :key="String(option.value)"
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
        v-else-if="activeCards.length === 0"
        class="d-flex flex-column align-center text-center text-disabled py-8"
      >
        <VIcon
          icon="tabler-credit-card"
          size="32"
          class="mb-2"
        />
        <div>Nenhum cartão de crédito ativo cadastrado.</div>
        <VBtn
          class="mt-4"
          variant="tonal"
          size="small"
          to="/credit-cards"
        >
          Cadastrar cartão
        </VBtn>
      </div>

      <VRow v-else>
        <VCol
          v-for="entry in entries"
          :key="entry.card.id"
          cols="12"
          md="6"
        >
          <div class="border rounded pa-4 h-100">
            <div class="mb-2">
              <div class="font-weight-medium">
                {{ entry.card.name }}
              </div>
              <VChip
                size="small"
                variant="tonal"
                class="mt-1"
              >
                Vence {{ formatDate(entry.invoice?.dueDate) }}
              </VChip>
            </div>

            <div class="text-h6 mb-2">
              {{ formatCurrency(entry.invoice?.totalAmount ?? 0) }}
            </div>

            <VProgressLinear
              :model-value="usagePercentage(entry)"
              :color="usageColor(entry)"
              height="8"
              rounded
            />

            <div class="text-caption text-disabled mt-1">
              {{ formatCurrency(entry.invoice?.totalAmount ?? 0) }} de {{ formatCurrency(entry.card.limit) }}
            </div>
          </div>
        </VCol>
      </VRow>
    </VCardText>
  </VCard>
</template>
