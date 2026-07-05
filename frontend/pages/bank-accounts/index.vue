<script setup lang="ts">
interface BankAccountResponse {
  id: number
  version: number
  spaceId: number
  name: string
  bankName: string
  balance: number
  active: boolean
  createdDate: string
}

const spaceStore = useSpaceStore()
const { error, setError, clearError } = useApiError()
const { isVisible: snackbarVisible, message: snackbarMessage, color: snackbarColor, icon: snackbarIcon, showSuccess, showError } = useSnackbar()

const currencyFormatter = new Intl.NumberFormat('pt-BR', { style: 'currency', currency: 'BRL' })

const bankAccounts = ref<BankAccountResponse[]>([])
const search = shallowRef('')
const page = shallowRef(1)
const itemsPerPage = shallowRef(10)
const isLoading = shallowRef(false)
const isDeleting = shallowRef(false)
const searchVisible = shallowRef(false)

const isAddEditDialogVisible = shallowRef(false)
const isDeleteDialogVisible = shallowRef(false)

const selectedBankAccount = shallowRef<BankAccountResponse | null>(null)

const filteredBankAccounts = computed(() =>
  bankAccounts.value.filter(ba =>
    ba.name.toLowerCase().includes(search.value.toLowerCase())
    || ba.bankName.toLowerCase().includes(search.value.toLowerCase()),
  ),
)

const paginatedBankAccounts = computed(() => {
  const start = (page.value - 1) * itemsPerPage.value

  return filteredBankAccounts.value.slice(start, start + itemsPerPage.value)
})

watch(
  () => spaceStore.activeSpace,
  async space => {
    if (space)
      await fetchBankAccounts()

    else
      bankAccounts.value = []
  },
  { immediate: true },
)

watch(search, () => {
  page.value = 1
})

async function fetchBankAccounts() {
  if (!spaceStore.activeSpace)
    return

  isLoading.value = true
  clearError()

  try {
    bankAccounts.value = await $fetch<BankAccountResponse[]>('/api/bank-accounts', {
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
  selectedBankAccount.value = null
  isAddEditDialogVisible.value = true
}

function openEdit(bankAccount: BankAccountResponse) {
  selectedBankAccount.value = bankAccount
  isAddEditDialogVisible.value = true
}

function openDelete(bankAccount: BankAccountResponse) {
  selectedBankAccount.value = bankAccount
  isDeleteDialogVisible.value = true
}

async function onDeleteConfirm(confirmed: boolean) {
  if (!confirmed || !selectedBankAccount.value)
    return

  isDeleting.value = true
  clearError()

  try {
    await $fetch(`/api/bank-accounts/${selectedBankAccount.value.id}`, { method: 'DELETE' })

    const idx = bankAccounts.value.findIndex(ba => ba.id === selectedBankAccount.value!.id)

    if (idx >= 0)
      bankAccounts.value[idx] = { ...bankAccounts.value[idx], active: false }

    showSuccess('Conta bancária desativada com sucesso.')
  }
  catch (e) {
    showError(e)
  }
  finally {
    isDeleting.value = false
    selectedBankAccount.value = null
  }
}

function onBankAccountSaved(saved: BankAccountResponse) {
  const idx = bankAccounts.value.findIndex(ba => ba.id === saved.id)

  if (idx >= 0)
    bankAccounts.value[idx] = saved
  else
    bankAccounts.value = [saved, ...bankAccounts.value]

  if (selectedBankAccount.value?.id === saved.id)
    selectedBankAccount.value = saved
}

function toggleSearch() {
  searchVisible.value = !searchVisible.value
  if (!searchVisible.value)
    search.value = ''
}

function formatBalance(balance: number) {
  return currencyFormatter.format(balance)
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
          Contas Bancárias
        </h5>

        <VSpacer />

        <VTextField
          v-model="search"
          placeholder="Buscar por nome ou banco..."
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
          <span class="d-none d-sm-inline">Adicionar Conta Bancária</span>
        </VBtn>

        <VTextField
          v-if="searchVisible"
          v-model="search"
          placeholder="Buscar por nome ou banco..."
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
              <th>Banco</th>
              <th>Saldo</th>
              <th>Status</th>
              <th class="text-center">
                Ações
              </th>
            </tr>
          </thead>
          <tbody>
            <tr
              v-for="bankAccount in paginatedBankAccounts"
              :key="bankAccount.id"
            >
              <td>
                <span class="font-weight-medium">{{ bankAccount.name }}</span>
              </td>
              <td class="text-disabled">
                {{ bankAccount.bankName }}
              </td>
              <td>
                {{ formatBalance(bankAccount.balance) }}
              </td>
              <td>
                <VChip
                  :color="bankAccount.active ? 'success' : 'secondary'"
                  size="small"
                  variant="tonal"
                >
                  {{ bankAccount.active ? 'Ativo' : 'Inativo' }}
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
                  @click="openEdit(bankAccount)"
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
                  :disabled="!bankAccount.active"
                  @click="openDelete(bankAccount)"
                >
                  <VIcon icon="tabler-trash" />
                  <VTooltip activator="parent">
                    {{ bankAccount.active ? 'Excluir' : 'Já inativa' }}
                  </VTooltip>
                </VBtn>
              </td>
            </tr>

            <tr v-if="!isLoading && filteredBankAccounts.length === 0">
              <td
                colspan="5"
                class="text-center text-disabled py-8"
              >
                {{ search ? 'Nenhuma conta bancária encontrada para a busca.' : 'Nenhuma conta bancária cadastrada.' }}
              </td>
            </tr>
          </tbody>
        </VTable>
      </div>

      <TablePagination
        v-if="filteredBankAccounts.length > 0"
        :page="page"
        :items-per-page="itemsPerPage"
        :total-items="filteredBankAccounts.length"
        @update:page="page = $event"
      />
    </VCard>

    <AddEditBankAccountDialog
      v-model:is-dialog-visible="isAddEditDialogVisible"
      :bank-account="selectedBankAccount"
      @saved="onBankAccountSaved"
    />

    <ConfirmDialog
      v-model:is-dialog-visible="isDeleteDialogVisible"
      :auto-result="false"
      confirmation-question="Tem certeza que deseja excluir esta conta bancária?"
      cancel-title="Ação cancelada"
      cancel-msg="A conta bancária não foi excluída."
      @confirm="onDeleteConfirm"
    />
  </div>
</template>
