<script setup lang="ts">
interface PaymentMethodResponse {
  id: number
  version: number
  name: string
  active: boolean
}

const spaceStore = useSpaceStore()
const { error, setError, clearError } = useApiError()
const { isVisible: snackbarVisible, message: snackbarMessage, color: snackbarColor, icon: snackbarIcon, showSuccess, showError } = useSnackbar()

const paymentMethods = ref<PaymentMethodResponse[]>([])
const search = shallowRef('')
const page = shallowRef(1)
const itemsPerPage = shallowRef(10)
const isLoading = shallowRef(false)
const isDeleting = shallowRef(false)
const searchVisible = shallowRef(false)

const isAddEditDialogVisible = shallowRef(false)
const isDeleteDialogVisible = shallowRef(false)
const isStatusDialogVisible = shallowRef(false)
const isTogglingStatus = shallowRef(false)

const selectedPaymentMethod = shallowRef<PaymentMethodResponse | null>(null)

const filteredPaymentMethods = computed(() =>
  paymentMethods.value.filter(pm =>
    pm.name.toLowerCase().includes(search.value.toLowerCase()),
  ),
)

const paginatedPaymentMethods = computed(() => {
  const start = (page.value - 1) * itemsPerPage.value

  return filteredPaymentMethods.value.slice(start, start + itemsPerPage.value)
})

watch(
  () => spaceStore.activeSpace,
  async space => {
    if (space)
      await fetchPaymentMethods()

    else
      paymentMethods.value = []
  },
  { immediate: true },
)

watch(search, () => {
  page.value = 1
})

async function fetchPaymentMethods() {
  if (!spaceStore.activeSpace)
    return

  isLoading.value = true
  clearError()

  try {
    paymentMethods.value = await $fetch<PaymentMethodResponse[]>('/api/payment-methods', {
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
  selectedPaymentMethod.value = null
  isAddEditDialogVisible.value = true
}

function openEdit(paymentMethod: PaymentMethodResponse) {
  selectedPaymentMethod.value = paymentMethod
  isAddEditDialogVisible.value = true
}

function openDelete(paymentMethod: PaymentMethodResponse) {
  selectedPaymentMethod.value = paymentMethod
  isDeleteDialogVisible.value = true
}

async function onDeleteConfirm(confirmed: boolean) {
  if (!confirmed || !selectedPaymentMethod.value)
    return

  isDeleting.value = true
  clearError()

  try {
    await $fetch(`/api/payment-methods/${selectedPaymentMethod.value.id}`, { method: 'DELETE' })

    paymentMethods.value = paymentMethods.value.filter(pm => pm.id !== selectedPaymentMethod.value!.id)

    showSuccess('Meio de pagamento excluído com sucesso.')
  }
  catch (e) {
    showError(e)
  }
  finally {
    isDeleting.value = false
    selectedPaymentMethod.value = null
  }
}

function openToggleStatus(paymentMethod: PaymentMethodResponse) {
  selectedPaymentMethod.value = paymentMethod
  isStatusDialogVisible.value = true
}

async function onToggleStatusConfirm(confirmed: boolean) {
  if (!confirmed || !selectedPaymentMethod.value)
    return

  const target = selectedPaymentMethod.value
  const nextActive = !target.active

  isTogglingStatus.value = true
  clearError()

  try {
    const updated = await $fetch<PaymentMethodResponse>(`/api/payment-methods/${target.id}/status`, {
      method: 'PATCH',
      body: { active: nextActive },
    })

    const idx = paymentMethods.value.findIndex(pm => pm.id === target.id)

    if (idx >= 0)
      paymentMethods.value[idx] = updated

    showSuccess(nextActive ? 'Meio de pagamento ativado com sucesso.' : 'Meio de pagamento inativado com sucesso.')
  }
  catch (e) {
    showError(e)
  }
  finally {
    isTogglingStatus.value = false
    selectedPaymentMethod.value = null
  }
}

function onPaymentMethodSaved(saved: PaymentMethodResponse) {
  const idx = paymentMethods.value.findIndex(pm => pm.id === saved.id)

  if (idx >= 0)
    paymentMethods.value[idx] = saved
  else
    paymentMethods.value = [saved, ...paymentMethods.value]

  if (selectedPaymentMethod.value?.id === saved.id)
    selectedPaymentMethod.value = saved
}

function toggleSearch() {
  searchVisible.value = !searchVisible.value
  if (!searchVisible.value)
    search.value = ''
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
          Meios de Pagamento
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
          <span class="d-none d-sm-inline">Adicionar Meio de Pagamento</span>
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
              <th>Status</th>
              <th class="text-center">
                Ações
              </th>
            </tr>
          </thead>
          <tbody>
            <tr
              v-for="paymentMethod in paginatedPaymentMethods"
              :key="paymentMethod.id"
            >
              <td>
                <span class="font-weight-medium">{{ paymentMethod.name }}</span>
              </td>
              <td>
                <VChip
                  :color="paymentMethod.active ? 'success' : 'secondary'"
                  size="small"
                  variant="tonal"
                >
                  {{ paymentMethod.active ? 'Ativo' : 'Inativo' }}
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
                  @click="openEdit(paymentMethod)"
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
                  :color="paymentMethod.active ? 'secondary' : 'success'"
                  @click="openToggleStatus(paymentMethod)"
                >
                  <VIcon :icon="paymentMethod.active ? 'tabler-toggle-right' : 'tabler-toggle-left'" />
                  <VTooltip activator="parent">
                    {{ paymentMethod.active ? 'Inativar' : 'Ativar' }}
                  </VTooltip>
                </VBtn>

                <VBtn
                  icon
                  variant="text"
                  size="small"
                  color="error"
                  @click="openDelete(paymentMethod)"
                >
                  <VIcon icon="tabler-trash" />
                  <VTooltip activator="parent">
                    Excluir definitivamente
                  </VTooltip>
                </VBtn>
              </td>
            </tr>

            <tr v-if="!isLoading && filteredPaymentMethods.length === 0">
              <td
                colspan="3"
                class="text-center text-disabled py-8"
              >
                {{ search ? 'Nenhum meio de pagamento encontrado para a busca.' : 'Nenhum meio de pagamento cadastrado.' }}
              </td>
            </tr>
          </tbody>
        </VTable>
      </div>

      <TablePagination
        v-if="filteredPaymentMethods.length > 0"
        :page="page"
        :items-per-page="itemsPerPage"
        :total-items="filteredPaymentMethods.length"
        @update:page="page = $event"
      />
    </VCard>

    <AddEditPaymentMethodDialog
      v-model:is-dialog-visible="isAddEditDialogVisible"
      :payment-method="selectedPaymentMethod"
      @saved="onPaymentMethodSaved"
    />

    <ConfirmDialog
      v-model:is-dialog-visible="isDeleteDialogVisible"
      :auto-result="false"
      confirm-color="error"
      confirmation-question="Tem certeza que deseja excluir definitivamente este meio de pagamento? Esta ação não pode ser desfeita."
      cancel-title="Ação cancelada"
      cancel-msg="O meio de pagamento não foi excluído."
      @confirm="onDeleteConfirm"
    />

    <ConfirmDialog
      v-model:is-dialog-visible="isStatusDialogVisible"
      :auto-result="false"
      :confirmation-question="selectedPaymentMethod?.active
        ? 'Tem certeza que deseja inativar este meio de pagamento?'
        : 'Tem certeza que deseja ativar este meio de pagamento?'"
      cancel-title="Ação cancelada"
      cancel-msg="O status do meio de pagamento não foi alterado."
      @confirm="onToggleStatusConfirm"
    />
  </div>
</template>
