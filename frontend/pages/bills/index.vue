<script setup lang="ts">
type BillInstanceStatus = 'PENDING' | 'PAID'

interface BillInstanceResponse {
  id: number
  version: number
  billRecurringId: number | null
  name: string
  categoryId: number | null
  subCategoryId: number | null
  referenceMonth: string
  dueDate: string
  amount: number
  status: BillInstanceStatus
  paidDate: string | null
  paymentTransactionId: number | null
  bankAccountId: number | null
  createdDate: string
}

interface SubCategoryOption {
  id: number
  categoryId: number
  name: string
  active: boolean
}

interface CategoryOption {
  id: number
  name: string
  active: boolean
  subCategories: SubCategoryOption[]
}

interface OptionItem {
  id: number
  name: string
  active: boolean
}

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

const bills = ref<BillInstanceResponse[]>([])
const bankAccounts = ref<OptionItem[]>([])
const categories = ref<CategoryOption[]>([])
const paymentMethods = ref<OptionItem[]>([])

const from = shallowRef(firstDayOfMonth())
const to = shallowRef(lastDayOfMonth())

const page = shallowRef(1)
const itemsPerPage = shallowRef(10)
const isLoading = shallowRef(false)
const isDeleting = shallowRef(false)
const isUndoing = shallowRef(false)

const isAddDialogVisible = shallowRef(false)
const isEditDialogVisible = shallowRef(false)
const isRecurrenceDialogVisible = shallowRef(false)
const isPayDialogVisible = shallowRef(false)
const isUndoDialogVisible = shallowRef(false)
const isDeleteDialogVisible = shallowRef(false)

const selectedBill = shallowRef<BillInstanceResponse | null>(null)

const categoriesById = computed(() => new Map(categories.value.map(c => [c.id, c])))

const subCategoriesById = computed(() => {
  const map = new Map<number, SubCategoryOption>()

  for (const category of categories.value) {
    for (const subCategory of category.subCategories)
      map.set(subCategory.id, subCategory)
  }

  return map
})

const sortedBills = computed(() =>
  [...bills.value].sort((a, b) => a.dueDate.localeCompare(b.dueDate)),
)

const paginatedBills = computed(() => {
  const start = (page.value - 1) * itemsPerPage.value

  return sortedBills.value.slice(start, start + itemsPerPage.value)
})

watch(
  () => spaceStore.activeSpace,
  async space => {
    if (space) {
      await fetchAll()
    }
    else {
      bills.value = []
      bankAccounts.value = []
      categories.value = []
      paymentMethods.value = []
    }
  },
  { immediate: true },
)

async function fetchAll() {
  await Promise.all([fetchDropdownData(), fetchBills()])
}

async function fetchDropdownData() {
  if (!spaceStore.activeSpace)
    return

  const spaceId = spaceStore.activeSpace.id

  const [bankAccountsResult, categoriesResult, paymentMethodsResult] = await Promise.all([
    $fetch<OptionItem[]>('/api/bank-accounts', { query: { spaceId } }),
    $fetch<CategoryOption[]>('/api/categories', { query: { spaceId } }),
    $fetch<OptionItem[]>('/api/payment-methods', { query: { spaceId } }),
  ])

  bankAccounts.value = bankAccountsResult
  categories.value = categoriesResult
  paymentMethods.value = paymentMethodsResult
}

async function fetchBills() {
  if (!spaceStore.activeSpace)
    return

  isLoading.value = true
  clearError()

  try {
    bills.value = await $fetch<BillInstanceResponse[]>('/api/bills/instances', {
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

function openAdd() {
  isAddDialogVisible.value = true
}

function openEdit(bill: BillInstanceResponse) {
  selectedBill.value = bill
  isEditDialogVisible.value = true
}

function openRecurrenceSettings() {
  isRecurrenceDialogVisible.value = true
}

function openPay(bill: BillInstanceResponse) {
  selectedBill.value = bill
  isPayDialogVisible.value = true
}

function openUndo(bill: BillInstanceResponse) {
  selectedBill.value = bill
  isUndoDialogVisible.value = true
}

function openDelete(bill: BillInstanceResponse) {
  selectedBill.value = bill
  isDeleteDialogVisible.value = true
}

function onAdded() {
  fetchBills()
  showSuccess('Conta cadastrada com sucesso.')
}

function onEdited(updated: BillInstanceResponse) {
  const idx = bills.value.findIndex(b => b.id === updated.id)

  if (idx >= 0)
    bills.value[idx] = updated

  showSuccess('Conta atualizada com sucesso.')
}

function onPaid(paid: BillInstanceResponse) {
  const idx = bills.value.findIndex(b => b.id === paid.id)

  if (idx >= 0)
    bills.value[idx] = paid

  showSuccess('Conta paga com sucesso.')
}

async function onUndoConfirm(confirmed: boolean) {
  if (!confirmed || !selectedBill.value)
    return

  isUndoing.value = true
  clearError()

  try {
    await $fetch(`/api/bills/instances/${selectedBill.value.id}/undo-payment`, { method: 'POST' })

    await fetchBills()

    showSuccess('Pagamento desfeito com sucesso.')
  }
  catch (e) {
    showError(e)
  }
  finally {
    isUndoing.value = false
    selectedBill.value = null
  }
}

async function onDeleteConfirm(confirmed: boolean) {
  if (!confirmed || !selectedBill.value)
    return

  isDeleting.value = true
  clearError()

  try {
    await $fetch(`/api/bills/instances/${selectedBill.value.id}`, { method: 'DELETE' })

    bills.value = bills.value.filter(b => b.id !== selectedBill.value!.id)

    showSuccess('Conta excluída com sucesso.')
  }
  catch (e) {
    showError(e)
  }
  finally {
    isDeleting.value = false
    selectedBill.value = null
  }
}

function categoryName(id: number | null) {
  if (id === null)
    return '—'

  return categoriesById.value.get(id)?.name ?? '—'
}

function subCategoryName(id: number | null) {
  if (id === null)
    return '—'

  return subCategoriesById.value.get(id)?.name ?? '—'
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
          Contas a Pagar
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
          @click="fetchBills"
        >
          Filtrar
        </VBtn>

        <VBtn
          variant="tonal"
          prepend-icon="tabler-calendar-cog"
          @click="openRecurrenceSettings"
        >
          <span class="d-none d-sm-inline">Configurações de Recorrência</span>
        </VBtn>

        <VBtn
          prepend-icon="tabler-plus"
          @click="openAdd"
        >
          <span class="d-none d-sm-inline">Adicionar Conta</span>
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
              <th>Categoria</th>
              <th>Subcategoria</th>
              <th>Vencimento</th>
              <th>Valor</th>
              <th>Status</th>
              <th class="text-center">
                Ações
              </th>
            </tr>
          </thead>
          <tbody>
            <tr
              v-for="bill in paginatedBills"
              :key="bill.id"
            >
              <td>
                <span class="font-weight-medium">{{ bill.name }}</span>
              </td>
              <td class="text-disabled">
                {{ categoryName(bill.categoryId) }}
              </td>
              <td class="text-disabled">
                {{ subCategoryName(bill.subCategoryId) }}
              </td>
              <td class="text-disabled">
                {{ formatDate(bill.dueDate) }}
              </td>
              <td>
                {{ currencyFormatter.format(bill.amount) }}
              </td>
              <td>
                <VChip
                  :color="bill.status === 'PAID' ? 'success' : 'warning'"
                  size="small"
                  variant="tonal"
                >
                  {{ bill.status === 'PAID' ? 'Paga' : 'Pendente' }}
                </VChip>
              </td>
              <td
                class="text-center"
                style="white-space: nowrap"
              >
                <template v-if="bill.status === 'PENDING'">
                  <VBtn
                    icon
                    variant="text"
                    size="small"
                    color="default"
                    @click="openEdit(bill)"
                  >
                    <VIcon icon="tabler-pencil" />
                    <VTooltip activator="parent">
                      Editar
                    </VTooltip>
                  </VBtn>

                  <VBtn
                    variant="tonal"
                    size="small"
                    class="mx-1"
                    @click="openPay(bill)"
                  >
                    Marcar como Paga
                  </VBtn>

                  <VBtn
                    icon
                    variant="text"
                    size="small"
                    color="error"
                    @click="openDelete(bill)"
                  >
                    <VIcon icon="tabler-trash" />
                    <VTooltip activator="parent">
                      Excluir
                    </VTooltip>
                  </VBtn>
                </template>

                <VBtn
                  v-else
                  variant="tonal"
                  color="error"
                  size="small"
                  @click="openUndo(bill)"
                >
                  Desfazer Pagamento
                </VBtn>
              </td>
            </tr>

            <tr v-if="!isLoading && bills.length === 0">
              <td
                colspan="7"
                class="text-center text-disabled py-8"
              >
                Nenhuma conta encontrada para o período selecionado.
              </td>
            </tr>
          </tbody>
        </VTable>
      </div>

      <TablePagination
        v-if="bills.length > 0"
        :page="page"
        :items-per-page="itemsPerPage"
        :total-items="bills.length"
        @update:page="page = $event"
      />
    </VCard>

    <AddEditBillDialog
      v-model:is-dialog-visible="isAddDialogVisible"
      :categories="categories"
      @saved="onAdded"
    />

    <EditBillInstanceDialog
      v-model:is-dialog-visible="isEditDialogVisible"
      :bill="selectedBill"
      :categories="categories"
      @saved="onEdited"
    />

    <RecurrenceSettingsDialog
      v-model:is-dialog-visible="isRecurrenceDialogVisible"
      :categories="categories"
    />

    <PayBillInstanceDialog
      v-model:is-dialog-visible="isPayDialogVisible"
      :bill-instance="selectedBill"
      :bank-accounts="bankAccounts"
      :payment-methods="paymentMethods"
      @paid="onPaid"
    />

    <ConfirmDialog
      v-model:is-dialog-visible="isUndoDialogVisible"
      :auto-result="false"
      confirm-color="error"
      :confirmation-question="selectedBill
        ? `Tem certeza que deseja desfazer o pagamento de '${selectedBill.name}'? O saldo da conta utilizada no pagamento será revertido em ${currencyFormatter.format(selectedBill.amount)}.`
        : ''"
      cancel-title="Ação cancelada"
      cancel-msg="O pagamento não foi desfeito."
      @confirm="onUndoConfirm"
    />

    <ConfirmDialog
      v-model:is-dialog-visible="isDeleteDialogVisible"
      :auto-result="false"
      confirm-color="error"
      confirmation-question="Tem certeza que deseja excluir esta conta? Esta ação não pode ser desfeita."
      cancel-title="Ação cancelada"
      cancel-msg="A conta não foi excluída."
      @confirm="onDeleteConfirm"
    />
  </div>
</template>
