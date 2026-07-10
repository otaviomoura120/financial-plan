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
  bankAccountId: number | null
  bankAccountName: string | null
}

const spaceStore = useSpaceStore()
const { error, setError, clearError } = useApiError()
const { isVisible: snackbarVisible, message: snackbarMessage, color: snackbarColor, icon: snackbarIcon, showSuccess, showError } = useSnackbar()

const creditCards = ref<CreditCardResponse[]>([])
const search = shallowRef('')
const page = shallowRef(1)
const itemsPerPage = shallowRef(10)
const isLoading = shallowRef(false)
const isDeleting = shallowRef(false)
const searchVisible = shallowRef(false)

const isAddEditDialogVisible = shallowRef(false)
const isDeleteDialogVisible = shallowRef(false)
const isTransactionsDialogVisible = shallowRef(false)
const isInvoicesDialogVisible = shallowRef(false)

const selectedCreditCard = shallowRef<CreditCardResponse | null>(null)

const filteredCreditCards = computed(() =>
  creditCards.value.filter(cc => cc.name.toLowerCase().includes(search.value.toLowerCase())),
)

const paginatedCreditCards = computed(() => {
  const start = (page.value - 1) * itemsPerPage.value

  return filteredCreditCards.value.slice(start, start + itemsPerPage.value)
})

watch(
  () => spaceStore.activeSpace,
  async space => {
    if (space)
      await fetchCreditCards()

    else
      creditCards.value = []
  },
  { immediate: true },
)

watch(search, () => {
  page.value = 1
})

async function fetchCreditCards() {
  if (!spaceStore.activeSpace)
    return

  isLoading.value = true
  clearError()

  try {
    creditCards.value = await $fetch<CreditCardResponse[]>('/api/credit-cards', {
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
  selectedCreditCard.value = null
  isAddEditDialogVisible.value = true
}

function openEdit(creditCard: CreditCardResponse) {
  selectedCreditCard.value = creditCard
  isAddEditDialogVisible.value = true
}

function openDelete(creditCard: CreditCardResponse) {
  selectedCreditCard.value = creditCard
  isDeleteDialogVisible.value = true
}

async function onDeleteConfirm(confirmed: boolean) {
  if (!confirmed || !selectedCreditCard.value)
    return

  isDeleting.value = true
  clearError()

  try {
    await $fetch(`/api/credit-cards/${selectedCreditCard.value.id}`, { method: 'DELETE' })

    creditCards.value = creditCards.value.filter(cc => cc.id !== selectedCreditCard.value!.id)

    showSuccess('Cartão de crédito excluído com sucesso.')
  }
  catch (e) {
    showError(e)
  }
  finally {
    isDeleting.value = false
    selectedCreditCard.value = null
  }
}

function onCreditCardSaved(saved: CreditCardResponse) {
  const idx = creditCards.value.findIndex(cc => cc.id === saved.id)

  if (idx >= 0)
    creditCards.value[idx] = saved
  else
    creditCards.value = [saved, ...creditCards.value]
}

function toggleSearch() {
  searchVisible.value = !searchVisible.value
  if (!searchVisible.value)
    search.value = ''
}

function formatLimit(limit: number) {
  return formatCurrency(limit)
}

function openTransactions(creditCard: CreditCardResponse) {
  selectedCreditCard.value = creditCard
  isTransactionsDialogVisible.value = true
}

function openInvoices(creditCard: CreditCardResponse) {
  selectedCreditCard.value = creditCard
  isInvoicesDialogVisible.value = true
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
          Cartões de Crédito
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
          prepend-icon="tabler-plus"
          @click="openCreate"
        >
          <span class="d-sm-inline">Adicionar</span>
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
              <th>Limite</th>
              <th>Fechamento</th>
              <th>Vencimento</th>
              <th>Conta</th>
              <th>Status</th>
              <th class="text-center">
                Ações
              </th>
            </tr>
          </thead>
          <tbody>
            <tr
              v-for="creditCard in paginatedCreditCards"
              :key="creditCard.id"
            >
              <td>
                <span class="font-weight-medium">{{ creditCard.name }}</span>
              </td>
              <td>
                {{ formatLimit(creditCard.limit) }}
              </td>
              <td class="text-disabled">
                Dia {{ creditCard.closingDay }}
              </td>
              <td class="text-disabled">
                Dia {{ creditCard.dueDay }}
              </td>
              <td class="text-disabled">
                {{ creditCard.bankAccountName ?? '—' }}
              </td>
              <td>
                <VChip
                  :color="creditCard.active ? 'success' : 'secondary'"
                  size="small"
                  variant="tonal"
                >
                  {{ creditCard.active ? 'Ativo' : 'Inativo' }}
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
                  @click="openTransactions(creditCard)"
                >
                  <VIcon icon="tabler-receipt" />
                  <VTooltip activator="parent">
                    Ver Lançamentos
                  </VTooltip>
                </VBtn>

                <VBtn
                  icon
                  variant="text"
                  size="small"
                  color="default"
                  @click="openInvoices(creditCard)"
                >
                  <VIcon icon="tabler-file-invoice" />
                  <VTooltip activator="parent">
                    Ver Fatura
                  </VTooltip>
                </VBtn>

                <VBtn
                  icon
                  variant="text"
                  size="small"
                  color="default"
                  @click="openEdit(creditCard)"
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
                  color="error"
                  @click="openDelete(creditCard)"
                >
                  <VIcon icon="tabler-trash" />
                  <VTooltip activator="parent">
                    Excluir
                  </VTooltip>
                </VBtn>
              </td>
            </tr>

            <tr v-if="!isLoading && filteredCreditCards.length === 0">
              <td
                colspan="7"
                class="text-center text-disabled py-8"
              >
                {{ search ? 'Nenhum cartão encontrado para a busca.' : 'Nenhum cartão de crédito cadastrado.' }}
              </td>
            </tr>
          </tbody>
        </VTable>
      </div>

      <TablePagination
        v-if="filteredCreditCards.length > 0"
        :page="page"
        :items-per-page="itemsPerPage"
        :total-items="filteredCreditCards.length"
        @update:page="page = $event"
      />
    </VCard>

    <AddEditCreditCardDialog
      v-model:is-dialog-visible="isAddEditDialogVisible"
      :credit-card="selectedCreditCard"
      @saved="onCreditCardSaved"
    />

    <ConfirmDialog
      v-model:is-dialog-visible="isDeleteDialogVisible"
      :auto-result="false"
      confirm-color="error"
      confirmation-question="Tem certeza que deseja excluir este cartão de crédito? Esta ação não pode ser desfeita."
      cancel-title="Ação cancelada"
      cancel-msg="O cartão de crédito não foi excluído."
      @confirm="onDeleteConfirm"
    />

    <CreditCardTransactionsDialog
      v-model:is-dialog-visible="isTransactionsDialogVisible"
      :credit-card-id="selectedCreditCard?.id ?? null"
    />

    <CreditCardInvoicesDialog
      v-model:is-dialog-visible="isInvoicesDialogVisible"
      :credit-card-id="selectedCreditCard?.id ?? null"
    />
  </div>
</template>
