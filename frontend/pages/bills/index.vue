<script setup lang="ts">
interface BillResponse {
  id: number
  version: number
  spaceId: number
  name: string
  categoryId: number | null
  defaultAmount: number
  startDate: string
  recurring: boolean
  active: boolean
  createdDate: string
}

interface CategoryResponse {
  id: number
  name: string
  active: boolean
}

const router = useRouter()
const spaceStore = useSpaceStore()
const { error, setError, clearError } = useApiError()
const { isVisible: snackbarVisible, message: snackbarMessage, color: snackbarColor, icon: snackbarIcon, showSuccess, showError } = useSnackbar()

const currencyFormatter = new Intl.NumberFormat('pt-BR', { style: 'currency', currency: 'BRL' })

const bills = ref<BillResponse[]>([])
const categories = ref<CategoryResponse[]>([])
const search = shallowRef('')
const page = shallowRef(1)
const itemsPerPage = shallowRef(10)
const isLoading = shallowRef(false)
const isDeleting = shallowRef(false)
const searchVisible = shallowRef(false)

const isAddEditDialogVisible = shallowRef(false)
const isScheduleDialogVisible = shallowRef(false)
const isDeleteDialogVisible = shallowRef(false)

const selectedBill = shallowRef<BillResponse | null>(null)

const categoriesById = computed(() => new Map(categories.value.map(c => [c.id, c])))

const filteredBills = computed(() =>
  bills.value.filter(b => b.name.toLowerCase().includes(search.value.toLowerCase())),
)

const paginatedBills = computed(() => {
  const start = (page.value - 1) * itemsPerPage.value

  return filteredBills.value.slice(start, start + itemsPerPage.value)
})

watch(
  () => spaceStore.activeSpace,
  async space => {
    if (space) {
      await fetchAll()
    }
    else {
      bills.value = []
      categories.value = []
    }
  },
  { immediate: true },
)

watch(search, () => {
  page.value = 1
})

async function fetchAll() {
  await Promise.all([fetchBills(), fetchCategories()])
}

async function fetchCategories() {
  if (!spaceStore.activeSpace)
    return

  categories.value = await $fetch<CategoryResponse[]>('/api/categories', {
    query: { spaceId: spaceStore.activeSpace.id },
  })
}

async function fetchBills() {
  if (!spaceStore.activeSpace)
    return

  isLoading.value = true
  clearError()

  try {
    bills.value = await $fetch<BillResponse[]>('/api/bills', {
      query: { spaceId: spaceStore.activeSpace.id },
    })
  }
  catch (e) {
    setError(e)
  }
  finally {
    isLoading.value = false
  }
}

function openCreate() {
  selectedBill.value = null
  isAddEditDialogVisible.value = true
}

function openEdit(bill: BillResponse) {
  selectedBill.value = bill
  isAddEditDialogVisible.value = true
}

function openSchedule(bill: BillResponse) {
  selectedBill.value = bill
  isScheduleDialogVisible.value = true
}

function openDelete(bill: BillResponse) {
  selectedBill.value = bill
  isDeleteDialogVisible.value = true
}

async function onDeleteConfirm(confirmed: boolean) {
  if (!confirmed || !selectedBill.value)
    return

  isDeleting.value = true
  clearError()

  try {
    await $fetch(`/api/bills/${selectedBill.value.id}`, { method: 'DELETE' })

    bills.value = bills.value.filter(b => b.id !== selectedBill.value!.id)

    showSuccess('Conta a pagar excluída com sucesso.')
  }
  catch (e) {
    showError(e)
  }
  finally {
    isDeleting.value = false
    selectedBill.value = null
  }
}

function onBillSaved(saved: BillResponse) {
  const idx = bills.value.findIndex(b => b.id === saved.id)

  if (idx >= 0)
    bills.value[idx] = saved
  else
    bills.value = [saved, ...bills.value]
}

function toggleSearch() {
  searchVisible.value = !searchVisible.value
  if (!searchVisible.value)
    search.value = ''
}

function categoryName(id: number | null) {
  if (id === null)
    return '—'

  return categoriesById.value.get(id)?.name ?? '—'
}

function formatDate(isoDate: string) {
  const [year, month, day] = isoDate.split('-')

  return `${day}/${month}/${year}`
}

function goToInstances() {
  router.push('/bills/instances')
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

        <VTextField
          v-model="search"
          placeholder="Buscar por nome..."
          density="compact"
          prepend-inner-icon="tabler-search"
          class="d-none d-md-block flex-grow-1"
          style="max-inline-size: 280px"
          hide-details
        />

        <VBtn
          class="d-md-none"
          icon
          variant="text"
          size="small"
          color="default"
          @click="toggleSearch"
        >
          <VIcon :icon="searchVisible ? 'tabler-x' : 'tabler-search'" />
        </VBtn>

        <VBtn
          variant="tonal"
          prepend-icon="tabler-calendar-due"
          @click="goToInstances"
        >
          <span class="d-none d-sm-inline">Contas do Mês</span>
        </VBtn>

        <VBtn
          prepend-icon="tabler-plus"
          @click="openCreate"
        >
          <span class="d-none d-sm-inline">Adicionar Conta</span>
        </VBtn>

        <VTextField
          v-if="searchVisible"
          v-model="search"
          placeholder="Buscar por nome..."
          density="compact"
          prepend-inner-icon="tabler-search"
          class="d-md-none w-100"
          hide-details
          autofocus
        />
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
              <th>Nome</th>
              <th>Categoria</th>
              <th>Valor padrão</th>
              <th>Recorrente</th>
              <th>Início</th>
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
              <td>
                {{ currencyFormatter.format(bill.defaultAmount) }}
              </td>
              <td>
                <VChip
                  :color="bill.recurring ? 'info' : 'secondary'"
                  size="small"
                  variant="tonal"
                >
                  {{ bill.recurring ? 'Sim' : 'Não' }}
                </VChip>
              </td>
              <td class="text-disabled">
                {{ formatDate(bill.startDate) }}
              </td>
              <td>
                <VChip
                  :color="bill.active ? 'success' : 'secondary'"
                  size="small"
                  variant="tonal"
                >
                  {{ bill.active ? 'Ativa' : 'Inativa' }}
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
                  @click="openEdit(bill)"
                >
                  <VIcon icon="tabler-pencil" />
                  <VTooltip activator="parent">
                    Editar
                  </VTooltip>
                </VBtn>

                <VBtn
                  icon
                  variant="text"
                  size="small"
                  color="default"
                  @click="openSchedule(bill)"
                >
                  <VIcon icon="tabler-calendar-cog" />
                  <VTooltip activator="parent">
                    Editar Agenda
                  </VTooltip>
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
              </td>
            </tr>

            <tr v-if="!isLoading && filteredBills.length === 0">
              <td
                colspan="7"
                class="text-center text-disabled py-8"
              >
                {{ search ? 'Nenhuma conta encontrada para a busca.' : 'Nenhuma conta a pagar cadastrada.' }}
              </td>
            </tr>
          </tbody>
        </VTable>
      </div>

      <TablePagination
        v-if="filteredBills.length > 0"
        :page="page"
        :items-per-page="itemsPerPage"
        :total-items="filteredBills.length"
        @update:page="page = $event"
      />
    </VCard>

    <AddEditBillDialog
      v-model:is-dialog-visible="isAddEditDialogVisible"
      :bill="selectedBill"
      :categories="categories"
      @saved="onBillSaved"
    />

    <UpdateBillScheduleDialog
      v-model:is-dialog-visible="isScheduleDialogVisible"
      :bill="selectedBill"
      @saved="onBillSaved"
    />

    <ConfirmDialog
      v-model:is-dialog-visible="isDeleteDialogVisible"
      :auto-result="false"
      confirm-color="error"
      confirmation-question="Tem certeza que deseja excluir esta conta a pagar? Esta ação não pode ser desfeita."
      cancel-title="Ação cancelada"
      cancel-msg="A conta a pagar não foi excluída."
      @confirm="onDeleteConfirm"
    />
  </div>
</template>
