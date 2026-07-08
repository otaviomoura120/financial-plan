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

interface CreditCardInvoiceResponse {
  creditCardId: number
  creditCardName: string
  referenceMonth: string
  closingDate: string
  dueDate: string
  totalAmount: number
  paid: boolean
  paidDate: string | null
  paidAmount: number | null
  paymentTransactionId: number | null
}

interface OptionItem {
  id: number
  name: string
  active: boolean
}

const route = useRoute()
const router = useRouter()
const spaceStore = useSpaceStore()
const { error, setError, clearError } = useApiError()
const { isVisible: snackbarVisible, message: snackbarMessage, color: snackbarColor, icon: snackbarIcon, showSuccess, showError } = useSnackbar()

const creditCardId = Number(route.params.id)

const currencyFormatter = new Intl.NumberFormat('pt-BR', { style: 'currency', currency: 'BRL' })

function toLocalDateString(date: Date) {
  const year = date.getFullYear()
  const month = String(date.getMonth() + 1).padStart(2, '0')
  const day = String(date.getDate()).padStart(2, '0')

  return `${year}-${month}-${day}`
}

function defaultFrom() {
  const now = new Date()

  return toLocalDateString(new Date(now.getFullYear(), now.getMonth() - 3, 1))
}

function defaultTo() {
  const now = new Date()

  return toLocalDateString(new Date(now.getFullYear(), now.getMonth() + 7, 0))
}

const creditCard = ref<CreditCardResponse | null>(null)
const invoices = ref<CreditCardInvoiceResponse[]>([])
const bankAccounts = ref<OptionItem[]>([])
const categories = ref<OptionItem[]>([])
const paymentMethods = ref<OptionItem[]>([])

const from = shallowRef(defaultFrom())
const to = shallowRef(defaultTo())

const isLoading = shallowRef(false)
const isUndoing = shallowRef(false)

const isPayDialogVisible = shallowRef(false)
const isUndoDialogVisible = shallowRef(false)

const selectedInvoice = shallowRef<CreditCardInvoiceResponse | null>(null)

const sortedInvoices = computed(() =>
  [...invoices.value].sort((a, b) => a.referenceMonth.localeCompare(b.referenceMonth)),
)

watch(
  () => spaceStore.activeSpace,
  async space => {
    if (space) {
      await fetchAll()
    }
    else {
      creditCard.value = null
      invoices.value = []
      bankAccounts.value = []
      categories.value = []
      paymentMethods.value = []
    }
  },
  { immediate: true },
)

async function fetchAll() {
  await Promise.all([fetchCreditCard(), fetchDropdownData(), fetchInvoices()])
}

async function fetchCreditCard() {
  if (!spaceStore.activeSpace)
    return

  const cards = await $fetch<CreditCardResponse[]>('/api/credit-cards', {
    query: { spaceId: spaceStore.activeSpace.id },
  })

  creditCard.value = cards.find(c => c.id === creditCardId) ?? null
}

async function fetchDropdownData() {
  if (!spaceStore.activeSpace)
    return

  const spaceId = spaceStore.activeSpace.id

  const [bankAccountsResult, categoriesResult, paymentMethodsResult] = await Promise.all([
    $fetch<OptionItem[]>('/api/bank-accounts', { query: { spaceId } }),
    $fetch<OptionItem[]>('/api/categories', { query: { spaceId } }),
    $fetch<OptionItem[]>('/api/payment-methods', { query: { spaceId } }),
  ])

  bankAccounts.value = bankAccountsResult
  categories.value = categoriesResult
  paymentMethods.value = paymentMethodsResult
}

async function fetchInvoices() {
  if (!spaceStore.activeSpace)
    return

  isLoading.value = true
  clearError()

  try {
    invoices.value = await $fetch<CreditCardInvoiceResponse[]>('/api/credit-cards/invoices', {
      query: {
        spaceId: spaceStore.activeSpace.id,
        creditCardId,
        from: from.value,
        to: to.value,
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

function openPay(invoice: CreditCardInvoiceResponse) {
  selectedInvoice.value = invoice
  isPayDialogVisible.value = true
}

function openUndo(invoice: CreditCardInvoiceResponse) {
  selectedInvoice.value = invoice
  isUndoDialogVisible.value = true
}

function onPaid() {
  showSuccess('Fatura paga com sucesso.')
  fetchInvoices()
}

async function onUndoConfirm(confirmed: boolean) {
  if (!confirmed || !selectedInvoice.value)
    return

  isUndoing.value = true
  clearError()

  try {
    await $fetch(
      `/api/credit-cards/${creditCardId}/invoices/${selectedInvoice.value.referenceMonth}/undo-payment`,
      { method: 'POST' },
    )

    showSuccess('Pagamento desfeito com sucesso.')
    await fetchInvoices()
  }
  catch (e) {
    showError(e)
  }
  finally {
    isUndoing.value = false
    selectedInvoice.value = null
  }
}

function formatReferenceMonth(isoDate: string) {
  const [year, month] = isoDate.split('-')

  return `${month}/${year}`
}

function formatDate(isoDate: string) {
  const [year, month, day] = isoDate.split('-')

  return `${day}/${month}/${year}`
}

function goBack() {
  router.push('/credit-cards')
}
</script>

<template>
  <div>
    <VCard>
      <VCardText class="d-flex align-center flex-wrap gap-4">
        <VBtn
          icon
          variant="text"
          size="small"
          color="default"
          @click="goBack"
        >
          <VIcon icon="tabler-arrow-left" />
        </VBtn>

        <h5
          class="text-h5 text-truncate"
          style="min-inline-size: 0"
        >
          Fatura {{ creditCard ? `— ${creditCard.name}` : '' }}
        </h5>

        <VSpacer />

        <AppTextField
          v-model="from"
          type="date"
          label="De"
          density="compact"
          hide-details
          style="max-inline-size: 170px"
        />

        <AppTextField
          v-model="to"
          type="date"
          label="Até"
          density="compact"
          hide-details
          style="max-inline-size: 170px"
        />

        <VBtn
          variant="tonal"
          @click="fetchInvoices"
        >
          Filtrar
        </VBtn>
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
              <th>Mês</th>
              <th>Fechamento</th>
              <th>Vencimento</th>
              <th class="text-right">
                Total
              </th>
              <th>Status</th>
              <th class="text-center">
                Ações
              </th>
            </tr>
          </thead>
          <tbody>
            <tr
              v-for="invoice in sortedInvoices"
              :key="invoice.referenceMonth"
            >
              <td class="font-weight-medium">
                {{ formatReferenceMonth(invoice.referenceMonth) }}
              </td>
              <td class="text-disabled">
                {{ formatDate(invoice.closingDate) }}
              </td>
              <td class="text-disabled">
                {{ formatDate(invoice.dueDate) }}
              </td>
              <td class="text-right">
                {{ currencyFormatter.format(invoice.totalAmount) }}
              </td>
              <td>
                <VChip
                  :color="invoice.paid ? 'success' : 'warning'"
                  size="small"
                  variant="tonal"
                >
                  {{ invoice.paid ? 'Paga' : 'Aberta' }}
                </VChip>
              </td>
              <td
                class="text-center"
                style="white-space: nowrap"
              >
                <VBtn
                  v-if="!invoice.paid"
                  variant="tonal"
                  size="small"
                  @click="openPay(invoice)"
                >
                  Pagar Fatura
                </VBtn>

                <VBtn
                  v-else
                  variant="tonal"
                  color="error"
                  size="small"
                  @click="openUndo(invoice)"
                >
                  Desfazer Pagamento
                </VBtn>
              </td>
            </tr>

            <tr v-if="!isLoading && invoices.length === 0">
              <td
                colspan="6"
                class="text-center text-disabled py-8"
              >
                Nenhuma fatura encontrada para o período selecionado.
              </td>
            </tr>
          </tbody>
        </VTable>
      </div>
    </VCard>

    <PayCreditCardInvoiceDialog
      v-model:is-dialog-visible="isPayDialogVisible"
      :credit-card-id="creditCardId"
      :reference-month="selectedInvoice?.referenceMonth ?? null"
      :bank-accounts="bankAccounts"
      :categories="categories"
      :payment-methods="paymentMethods"
      @paid="onPaid"
    />

    <ConfirmDialog
      v-model:is-dialog-visible="isUndoDialogVisible"
      :auto-result="false"
      confirm-color="error"
      :confirmation-question="selectedInvoice
        ? `Tem certeza que deseja desfazer o pagamento desta fatura? O saldo da conta utilizada no pagamento será revertido em ${currencyFormatter.format(selectedInvoice.paidAmount ?? 0)}.`
        : ''"
      cancel-title="Ação cancelada"
      cancel-msg="O pagamento não foi desfeito."
      @confirm="onUndoConfirm"
    />
  </div>
</template>
