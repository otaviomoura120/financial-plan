<script setup lang="ts">
interface CreditCardTransactionRecurringResponse {
  id: number
  version: number
  creditCardId: number
  userId: number
  categoryId: number | null
  subCategoryId: number | null
  description?: string | null
  defaultAmount: number
  startDate: string
  active: boolean
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

interface Props {
  isDialogVisible: boolean
  creditCardId: number | null
  categories: CategoryOption[]
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

const recurrings = ref<CreditCardTransactionRecurringResponse[]>([])
const isLoading = shallowRef(false)
const isDeleting = shallowRef(false)

const isEditDialogVisible = shallowRef(false)
const isDeleteDialogVisible = shallowRef(false)

const selectedRecurring = shallowRef<CreditCardTransactionRecurringResponse | null>(null)

const categoriesById = computed(() => new Map(props.categories.map(c => [c.id, c])))

const cardRecurrings = computed(() =>
  recurrings.value.filter(r => r.creditCardId === props.creditCardId),
)

watch(
  () => props.isDialogVisible,
  visible => {
    if (visible) {
      clearError()
      fetchRecurrings()
    }
  },
)

async function fetchRecurrings() {
  if (!spaceStore.activeSpace)
    return

  isLoading.value = true
  clearError()

  try {
    recurrings.value = await $fetch<CreditCardTransactionRecurringResponse[]>('/api/credit-card-transactions/recurring', {
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

function openEdit(recurring: CreditCardTransactionRecurringResponse) {
  selectedRecurring.value = recurring
  isEditDialogVisible.value = true
}

function openDelete(recurring: CreditCardTransactionRecurringResponse) {
  selectedRecurring.value = recurring
  isDeleteDialogVisible.value = true
}

function onEdited(saved: CreditCardTransactionRecurringResponse) {
  const idx = recurrings.value.findIndex(r => r.id === saved.id)

  if (idx >= 0)
    recurrings.value[idx] = saved

  showSuccess('Assinatura atualizada com sucesso.')
}

async function onDeleteConfirm(confirmed: boolean) {
  if (!confirmed || !selectedRecurring.value)
    return

  isDeleting.value = true
  clearError()

  try {
    await $fetch(`/api/credit-card-transactions/recurring/${selectedRecurring.value.id}`, { method: 'DELETE' })

    recurrings.value = recurrings.value.filter(r => r.id !== selectedRecurring.value!.id)

    showSuccess('Assinatura excluída com sucesso.')
  }
  catch (e) {
    showError(e)
  }
  finally {
    isDeleting.value = false
    selectedRecurring.value = null
  }
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

function onClose() {
  emit('update:isDialogVisible', false)
}

defineExpose({ fetchRecurrings })
</script>

<template>
  <VDialog
    :width="$vuetify.display.smAndDown ? 'auto' : 900"
    :model-value="props.isDialogVisible"
    scrollable
    @update:model-value="onClose"
  >
    <DialogCloseBtn @click="onClose" />

    <VCard
      class="d-flex flex-column pa-sm-10 pa-4"
      style="block-size: 100%"
    >
      <VCardText
        class="d-flex flex-column flex-grow-1"
        style="overflow: hidden; min-height: 0;"
      >
        <h4 class="text-h4 text-center mb-2">
          Assinaturas Recorrentes
        </h4>
        <p class="text-body-1 text-center mb-6">
          Cobranças que se repetem todo mês neste cartão. Alterações aqui afetam as cobranças pendentes do mês atual em diante; as de meses anteriores ou já pagas não são alteradas.
        </p>

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
          class="flex-grow-1"
          style="overflow: auto; min-height: 0;"
        >
          <VTable
            fixed-header
            class="credit-card-recurrings-table"
          >
            <thead style="white-space: nowrap">
              <tr>
                <th>Descrição</th>
                <th>Categoria</th>
                <th>Valor padrão</th>
                <th>Dia de cobrança</th>
                <th class="text-center">
                  Ações
                </th>
              </tr>
            </thead>
            <tbody>
              <tr
                v-for="recurring in cardRecurrings"
                :key="recurring.id"
              >
                <td>
                  <span class="font-weight-medium">{{ recurring.description || '—' }}</span>
                </td>
                <td class="text-disabled">
                  {{ categoryName(recurring.categoryId) }}
                </td>
                <td>
                  {{ currencyFormatter.format(recurring.defaultAmount) }}
                </td>
                <td class="text-disabled">
                  {{ formatDate(recurring.startDate) }}
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
                    @click="openEdit(recurring)"
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
                    @click="openDelete(recurring)"
                  >
                    <VIcon icon="tabler-trash" />
                    <VTooltip activator="parent">
                      Excluir definitivamente
                    </VTooltip>
                  </VBtn>
                </td>
              </tr>

              <tr v-if="!isLoading && cardRecurrings.length === 0">
                <td
                  colspan="5"
                  class="text-center text-disabled py-8"
                >
                  Nenhuma assinatura recorrente cadastrada.
                </td>
              </tr>
            </tbody>
          </VTable>
        </div>

        <div class="d-flex align-center justify-center mt-6">
          <VBtn
            color="secondary"
            variant="tonal"
            @click="onClose"
          >
            Fechar
          </VBtn>
        </div>
      </VCardText>
    </VCard>

    <EditRecurringCreditCardSubscriptionDialog
      v-model:is-dialog-visible="isEditDialogVisible"
      :recurring="selectedRecurring"
      :categories="categories"
      @saved="onEdited"
    />

    <ConfirmDialog
      v-model:is-dialog-visible="isDeleteDialogVisible"
      :auto-result="false"
      confirm-color="error"
      confirmation-question="Tem certeza que deseja excluir definitivamente esta assinatura? As cobranças do mês atual em diante serão excluídas; as de meses anteriores continuam disponíveis, mas deixam de ficar vinculadas a ela."
      cancel-title="Ação cancelada"
      cancel-msg="A assinatura não foi excluída."
      @confirm="onDeleteConfirm"
    />
  </VDialog>
</template>

<style scoped>
.credit-card-recurrings-table :deep(table) {
  border-collapse: collapse;
}

.credit-card-recurrings-table :deep(.v-table__wrapper) {
  overflow: visible;
}

.credit-card-recurrings-table :deep(thead th) {
  background-color: rgb(var(--v-theme-surface));
}
</style>
