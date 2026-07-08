<script setup lang="ts">
type BillInstanceStatus = 'PENDING' | 'PAID'

interface BillInstanceResponse {
  id: number
  version: number
  billId: number
  billName: string
  referenceMonth: string
  dueDate: string
  amount: number
  status: BillInstanceStatus
  paidDate: string | null
  paymentTransactionId: number | null
  bankAccountId: number | null
  createdDate: string
}

interface OptionItem {
  id: number
  name: string
  active: boolean
}

const router = useRouter()
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

function firstDayOfMonth() {
  const now = new Date()

  return toLocalDateString(new Date(now.getFullYear(), now.getMonth(), 1))
}

function lastDayOfMonth() {
  const now = new Date()

  return toLocalDateString(new Date(now.getFullYear(), now.getMonth() + 1, 0))
}

const billInstances = ref<BillInstanceResponse[]>([])
const bankAccounts = ref<OptionItem[]>([])
const categories = ref<OptionItem[]>([])
const paymentMethods = ref<OptionItem[]>([])

const from = shallowRef(firstDayOfMonth())
const to = shallowRef(lastDayOfMonth())

const page = shallowRef(1)
const itemsPerPage = shallowRef(10)
const isLoading = shallowRef(false)
const isUndoing = shallowRef(false)

const editingId = shallowRef<number | null>(null)
const editingAmount = shallowRef('')
const isSavingAmount = shallowRef(false)

const isPayDialogVisible = shallowRef(false)
const isUndoDialogVisible = shallowRef(false)

const selectedInstance = shallowRef<BillInstanceResponse | null>(null)

const sortedInstances = computed(() =>
  [...billInstances.value].sort((a, b) => a.dueDate.localeCompare(b.dueDate)),
)

const paginatedInstances = computed(() => {
  const start = (page.value - 1) * itemsPerPage.value

  return sortedInstances.value.slice(start, start + itemsPerPage.value)
})

watch(
  () => spaceStore.activeSpace,
  async space => {
    if (space) {
      await fetchAll()
    }
    else {
      billInstances.value = []
      bankAccounts.value = []
      categories.value = []
      paymentMethods.value = []
    }
  },
  { immediate: true },
)

async function fetchAll() {
  await Promise.all([fetchDropdownData(), fetchInstances()])
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

async function fetchInstances() {
  if (!spaceStore.activeSpace)
    return

  isLoading.value = true
  clearError()

  try {
    billInstances.value = await $fetch<BillInstanceResponse[]>('/api/bills/instances', {
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

function startEdit(instance: BillInstanceResponse) {
  editingId.value = instance.id
  editingAmount.value = String(instance.amount)
}

function cancelEdit() {
  editingId.value = null
  editingAmount.value = ''
}

async function saveEdit(instance: BillInstanceResponse) {
  const parsed = Number(editingAmount.value)

  if (editingAmount.value === '' || Number.isNaN(parsed) || parsed <= 0)
    return

  isSavingAmount.value = true
  clearError()

  try {
    const updated = await $fetch<BillInstanceResponse>(`/api/bills/instances/${instance.id}/amount`, {
      method: 'PUT',
      body: {
        version: instance.version,
        amount: parsed,
      },
    })

    const idx = billInstances.value.findIndex(bi => bi.id === instance.id)

    if (idx >= 0)
      billInstances.value[idx] = updated

    editingId.value = null
  }
  catch (e) {
    setError(e)
  }
  finally {
    isSavingAmount.value = false
  }
}

function openPay(instance: BillInstanceResponse) {
  selectedInstance.value = instance
  isPayDialogVisible.value = true
}

function openUndo(instance: BillInstanceResponse) {
  selectedInstance.value = instance
  isUndoDialogVisible.value = true
}

function onPaid(paid: BillInstanceResponse) {
  const idx = billInstances.value.findIndex(bi => bi.id === paid.id)

  if (idx >= 0)
    billInstances.value[idx] = paid

  showSuccess('Conta paga com sucesso.')
}

async function onUndoConfirm(confirmed: boolean) {
  if (!confirmed || !selectedInstance.value)
    return

  isUndoing.value = true
  clearError()

  try {
    await $fetch(`/api/bills/instances/${selectedInstance.value.id}/undo-payment`, { method: 'POST' })

    await fetchInstances()

    showSuccess('Pagamento desfeito com sucesso.')
  }
  catch (e) {
    showError(e)
  }
  finally {
    isUndoing.value = false
    selectedInstance.value = null
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
  router.push('/bills')
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
          Contas do Mês
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
          @click="fetchInstances"
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
              <th>Conta</th>
              <th>Mês</th>
              <th>Vencimento</th>
              <th style="min-width: 160px">
                Valor
              </th>
              <th>Status</th>
              <th class="text-center">
                Ações
              </th>
            </tr>
          </thead>
          <tbody>
            <tr
              v-for="instance in paginatedInstances"
              :key="instance.id"
            >
              <td>
                <span class="font-weight-medium">{{ instance.billName }}</span>
              </td>
              <td class="text-disabled">
                {{ formatReferenceMonth(instance.referenceMonth) }}
              </td>
              <td class="text-disabled">
                {{ formatDate(instance.dueDate) }}
              </td>
              <td>
                <div
                  v-if="editingId === instance.id"
                  class="d-flex align-center gap-2"
                >
                  <AppTextField
                    v-model="editingAmount"
                    type="number"
                    step="0.01"
                    density="compact"
                    hide-details
                    style="max-inline-size: 120px"
                    autofocus
                    @keyup.enter="saveEdit(instance)"
                    @keyup.esc="cancelEdit"
                  />
                  <VBtn
                    icon
                    variant="text"
                    size="small"
                    color="success"
                    :loading="isSavingAmount"
                    @click="saveEdit(instance)"
                  >
                    <VIcon icon="tabler-check" />
                  </VBtn>
                  <VBtn
                    icon
                    variant="text"
                    size="small"
                    color="default"
                    :disabled="isSavingAmount"
                    @click="cancelEdit"
                  >
                    <VIcon icon="tabler-x" />
                  </VBtn>
                </div>
                <div
                  v-else
                  class="d-flex align-center gap-2"
                >
                  {{ currencyFormatter.format(instance.amount) }}
                  <VBtn
                    v-if="instance.status === 'PENDING'"
                    icon
                    variant="text"
                    size="x-small"
                    color="default"
                    @click="startEdit(instance)"
                  >
                    <VIcon
                      icon="tabler-pencil"
                      size="16"
                    />
                    <VTooltip activator="parent">
                      Editar valor
                    </VTooltip>
                  </VBtn>
                </div>
              </td>
              <td>
                <VChip
                  :color="instance.status === 'PAID' ? 'success' : 'warning'"
                  size="small"
                  variant="tonal"
                >
                  {{ instance.status === 'PAID' ? 'Paga' : 'Pendente' }}
                </VChip>
              </td>
              <td
                class="text-center"
                style="white-space: nowrap"
              >
                <VBtn
                  v-if="instance.status === 'PENDING'"
                  variant="tonal"
                  size="small"
                  @click="openPay(instance)"
                >
                  Marcar como Paga
                </VBtn>

                <VBtn
                  v-else
                  variant="tonal"
                  color="error"
                  size="small"
                  @click="openUndo(instance)"
                >
                  Desfazer Pagamento
                </VBtn>
              </td>
            </tr>

            <tr v-if="!isLoading && billInstances.length === 0">
              <td
                colspan="6"
                class="text-center text-disabled py-8"
              >
                Nenhuma conta encontrada para o período selecionado.
              </td>
            </tr>
          </tbody>
        </VTable>
      </div>

      <TablePagination
        v-if="billInstances.length > 0"
        :page="page"
        :items-per-page="itemsPerPage"
        :total-items="billInstances.length"
        @update:page="page = $event"
      />
    </VCard>

    <PayBillInstanceDialog
      v-model:is-dialog-visible="isPayDialogVisible"
      :bill-instance="selectedInstance"
      :bank-accounts="bankAccounts"
      :categories="categories"
      :payment-methods="paymentMethods"
      @paid="onPaid"
    />

    <ConfirmDialog
      v-model:is-dialog-visible="isUndoDialogVisible"
      :auto-result="false"
      confirm-color="error"
      :confirmation-question="selectedInstance
        ? `Tem certeza que deseja desfazer o pagamento de '${selectedInstance.billName}'? O saldo da conta utilizada no pagamento será revertido em ${currencyFormatter.format(selectedInstance.amount)}.`
        : ''"
      cancel-title="Ação cancelada"
      cancel-msg="O pagamento não foi desfeito."
      @confirm="onUndoConfirm"
    />
  </div>
</template>
