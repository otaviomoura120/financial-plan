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

interface OptionItem {
  id: number
  name: string
  active: boolean
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
const categories = ref<CategoryResponse[]>([])
const paymentMethods = ref<OptionItem[]>([])

const from = shallowRef(defaultFrom())
const to = shallowRef(defaultTo())

const isLoading = shallowRef(false)
const isUndoing = shallowRef(false)

const isPayDialogVisible = shallowRef(false)
const isUndoDialogVisible = shallowRef(false)
const isInvoiceTransactionsDialogVisible = shallowRef(false)

const selectedInvoice = shallowRef<CreditCardInvoiceResponse | null>(null)
const selectedInvoiceForItems = shallowRef<CreditCardInvoiceResponse | null>(null)

const sortedInvoices = computed(() =>
  [...invoices.value].sort((a, b) => a.referenceMonth.localeCompare(b.referenceMonth)),
)

watch(
  () => props.isDialogVisible,
  visible => {
    if (visible && props.creditCardId) {
      fetchAll()
    }
    else {
      creditCard.value = null
      invoices.value = []
      bankAccounts.value = []
      categories.value = []
      paymentMethods.value = []
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

async function fetchAll() {
  await Promise.all([fetchCreditCard(), fetchDropdownData(), fetchInvoices()])
}

async function fetchCreditCard() {
  if (!spaceStore.activeSpace || !props.creditCardId)
    return

  const cards = await $fetch<CreditCardResponse[]>('/api/credit-cards', {
    query: { spaceId: spaceStore.activeSpace.id },
  })

  creditCard.value = cards.find(c => c.id === props.creditCardId) ?? null
}

async function fetchDropdownData() {
  if (!spaceStore.activeSpace)
    return

  const spaceId = spaceStore.activeSpace.id

  const [bankAccountsResult, categoriesResult, paymentMethodsResult] = await Promise.all([
    $fetch<OptionItem[]>('/api/bank-accounts', { query: { spaceId } }),
    $fetch<CategoryResponse[]>('/api/categories', { query: { spaceId } }),
    $fetch<OptionItem[]>('/api/payment-methods', { query: { spaceId } }),
  ])

  bankAccounts.value = bankAccountsResult
  categories.value = categoriesResult
  paymentMethods.value = paymentMethodsResult
}

async function fetchInvoices() {
  if (!spaceStore.activeSpace || !props.creditCardId)
    return

  isLoading.value = true
  clearError()

  try {
    invoices.value = await $fetch<CreditCardInvoiceResponse[]>('/api/credit-cards/invoices', {
      query: {
        spaceId: spaceStore.activeSpace.id,
        creditCardId: props.creditCardId,
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

function openInvoiceItems(invoice: CreditCardInvoiceResponse) {
  selectedInvoiceForItems.value = invoice
  isInvoiceTransactionsDialogVisible.value = true
}

function onPaid() {
  showSuccess('Fatura paga com sucesso.')
  fetchInvoices()
}

async function onUndoConfirm(confirmed: boolean) {
  if (!confirmed || !selectedInvoice.value || !props.creditCardId)
    return

  isUndoing.value = true
  clearError()

  try {
    await $fetch(
      `/api/credit-cards/${props.creditCardId}/invoices/${selectedInvoice.value.referenceMonth}/undo-payment`,
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
                  Fatura {{ creditCard ? `— ${creditCard.name}` : '' }}
                </h5>

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
                    @click="fetchInvoices"
                  >
                    Filtrar
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
                            icon
                            variant="text"
                            size="small"
                            color="default"
                            @click="openInvoiceItems(invoice)"
                          >
                            <VIcon icon="tabler-list-details" />
                            <VTooltip activator="parent">
                              Ver Itens da Fatura
                            </VTooltip>
                          </VBtn>

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
              </VCardText>
            </VCard>
          </div>
        </VCardText>
      </VCard>
    </div>

    <PayCreditCardInvoiceDialog
      v-model:is-dialog-visible="isPayDialogVisible"
      :credit-card-id="props.creditCardId"
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

    <InvoiceTransactionsDialog
      v-model:is-dialog-visible="isInvoiceTransactionsDialogVisible"
      :credit-card-id="props.creditCardId"
      :reference-month="selectedInvoiceForItems?.referenceMonth ?? null"
      :categories="categories"
    />
  </VDialog>
</template>
