<script setup lang="ts">
import { VForm } from 'vuetify/components/VForm'

interface CreditCardTransactionResponse {
  id: number
  version: number
  creditCardId: number
  userId: number
  categoryId: number | null
  subCategoryId: number | null
  amount: number
  credit: boolean
  purchaseDate: string
  description?: string | null
  referenceMonth: string
  competenceMonth: string
  installmentGroupId: string
  installmentNumber: number
  totalInstallments: number
  anticipated: boolean
  originalReferenceMonth: string | null
  createdDate: string
  totalAmount: number
}

interface SubCategoryOption {
  id: number
  categoryId: number
  name: string
  active: boolean
  system: boolean
}

interface CategoryOption {
  id: number
  name: string
  active: boolean
  system: boolean
  subCategories: SubCategoryOption[]
}

interface Props {
  isDialogVisible: boolean
  creditCardId: number | null
  closingDay: number | null
  transaction?: CreditCardTransactionResponse | null
  categories: CategoryOption[]
}

interface Emit {
  (e: 'update:isDialogVisible', value: boolean): void
  (e: 'saved', transaction: CreditCardTransactionResponse): void
  (e: 'recurringSaved'): void
}

const props = withDefaults(defineProps<Props>(), {
  transaction: null,
})

const emit = defineEmits<Emit>()

const spaceStore = useSpaceStore()
const { error, setError, clearError } = useApiError()

function toLocalDateString(date: Date) {
  const year = date.getFullYear()
  const month = String(date.getMonth() + 1).padStart(2, '0')
  const day = String(date.getDate()).padStart(2, '0')

  return `${year}-${month}-${day}`
}

function parseLocalDateString(value: string) {
  const parts = value.split('-')

  return { year: Number(parts[0]), month: Number(parts[1]), day: Number(parts[2]) }
}

// Mirrors CreditCardInvoiceCycle.resolveReferenceMonth on the backend: a purchase made on or after
// the closing day already belongs to the next invoice.
function defaultReferenceMonth(purchase: string, closingDay: number) {
  const { year, month, day } = parseLocalDateString(purchase)
  const daysInMonth = new Date(year, month, 0).getDate()
  const clampedClosingDay = Math.min(closingDay, daysInMonth)

  const referenceDate = day < clampedClosingDay
    ? new Date(year, month - 1, 1)
    : new Date(year, month, 1)

  return toLocalDateString(referenceDate)
}

function nextReferenceMonth(referenceMonthValue: string) {
  const { year, month } = parseLocalDateString(referenceMonthValue)

  return toLocalDateString(new Date(year, month, 1))
}

function formatReferenceMonth(referenceMonthValue: string) {
  const { year, month } = parseLocalDateString(referenceMonthValue)
  const label = new Date(year, month - 1, 1).toLocaleDateString('pt-BR', { month: 'long', year: 'numeric' })

  return label.charAt(0).toUpperCase() + label.slice(1)
}

const formRef = useTemplateRef<InstanceType<typeof VForm>>('formRef')

const categoryId = shallowRef<number | null>(null)
const subCategoryId = shallowRef<number | null>(null)
const amount = shallowRef<number | null>(null)
const purchaseDate = shallowRef<string>('')
const description = shallowRef('')
const totalInstallments = shallowRef<string>('')
const referenceMonth = shallowRef<string>('')
const isRecurringSubscription = shallowRef(false)
const isCredit = shallowRef(false)
const isLoading = shallowRef(false)

const isEditMode = computed(() => props.transaction !== null)

// An installment or an anticipated row already carries a reference month that this dialog must not
// recompute — the anticipation flow owns those. Its current value is still sent back untouched.
const canChooseReferenceMonth = computed(() => {
  if (isRecurringSubscription.value || props.closingDay === null) {
    return false
  }

  if (!isEditMode.value) {
    return true
  }

  return props.transaction!.totalInstallments <= 1 && !props.transaction!.anticipated
})

const referenceMonthItems = computed(() => {
  if (props.closingDay === null || purchaseDate.value === '') {
    return []
  }

  const current = defaultReferenceMonth(purchaseDate.value, props.closingDay)
  const next = nextReferenceMonth(current)

  return [
    { value: current, label: `${formatReferenceMonth(current)} — fatura atual` },
    { value: next, label: `${formatReferenceMonth(next)} — próxima fatura` },
  ]
})

function optionLabel<T extends { name: string; active: boolean }>(item: T) {
  return item.active ? item.name : `${item.name} (inativo)`
}

const categoryItems = computed(() =>
  props.categories.filter(c => !c.system).map(c => ({ ...c, label: optionLabel(c) })),
)

const selectedCategory = computed(() =>
  props.categories.find(c => c.id === categoryId.value) ?? null,
)

const subCategoryItems = computed(() =>
  (selectedCategory.value?.subCategories ?? []).map(sc => ({ ...sc, label: optionLabel(sc) })),
)

const categoryRules = [(v: number | null) => v !== null || 'Categoria é obrigatória']
const amountRules = [(v: number | null) => (v !== null && v > 0) || 'Valor deve ser maior que zero']
const dateRules = [(v: string) => !!v || 'Data é obrigatória']

const installmentsRules = [(v: string) => {
  if (v === '') {
    return true
  }

  const parsed = Number(v)

  return (Number.isInteger(parsed) && parsed >= 1 && parsed <= 60) || 'Parcelas deve ser um número entre 1 e 60'
}]

watch(categoryId, () => {
  if (!subCategoryItems.value.some(sc => sc.id === subCategoryId.value)) {
    subCategoryId.value = null
  }
})

watch(
  () => props.isDialogVisible,
  visible => {
    if (visible) {
      const t = props.transaction

      categoryId.value = t?.categoryId ?? null
      subCategoryId.value = t?.subCategoryId ?? null
      amount.value = t?.amount ?? null
      purchaseDate.value = t?.purchaseDate ?? toLocalDateString(new Date())
      description.value = t?.description ?? ''
      totalInstallments.value = ''
      referenceMonth.value = t?.referenceMonth
        ?? (props.closingDay !== null ? defaultReferenceMonth(purchaseDate.value, props.closingDay) : '')
      isRecurringSubscription.value = false
      isCredit.value = false
      clearError()
    }
  },
)

// Keep an explicit choice as long as it still matches the new purchase date's cycle; otherwise fall
// back to the invoice the backend would pick on its own.
watch(purchaseDate, () => {
  if (!canChooseReferenceMonth.value) {
    return
  }

  const items = referenceMonthItems.value

  if (!items.some(item => item.value === referenceMonth.value)) {
    referenceMonth.value = items[0]?.value ?? ''
  }
})

watch(isRecurringSubscription, recurring => {
  if (recurring) {
    totalInstallments.value = ''
    isCredit.value = false
  }
})

watch(isCredit, credit => {
  if (credit) {
    totalInstallments.value = ''
    isRecurringSubscription.value = false
  }
})

async function onSubmit() {
  const { valid } = await formRef.value!.validate()

  if (!valid || (!isEditMode.value && props.creditCardId === null)) {
    return
  }

  isLoading.value = true
  clearError()

  try {
    if (!isEditMode.value && isRecurringSubscription.value) {
      await $fetch('/api/credit-card-transactions/recurring', {
        method: 'POST',
        body: {
          creditCardId: props.creditCardId,
          userId: spaceStore.dbUser!.id,
          categoryId: categoryId.value,
          subCategoryId: subCategoryId.value,
          description: description.value || undefined,
          defaultAmount: amount.value,
          startDate: purchaseDate.value,
        },
      })

      emit('recurringSaved')
      emit('update:isDialogVisible', false)

      return
    }

    let saved: CreditCardTransactionResponse

    if (isEditMode.value) {
      saved = await $fetch<CreditCardTransactionResponse>(`/api/credit-card-transactions/${props.transaction!.id}`, {
        method: 'PUT',
        body: {
          version: props.transaction!.version,
          categoryId: categoryId.value,
          subCategoryId: subCategoryId.value,
          amount: amount.value,
          purchaseDate: purchaseDate.value,
          description: description.value || undefined,
          referenceMonth: referenceMonth.value || undefined,
        },
      })
    }
    else {
      saved = await $fetch<CreditCardTransactionResponse>('/api/credit-card-transactions', {
        method: 'POST',
        body: {
          creditCardId: props.creditCardId,
          userId: spaceStore.dbUser!.id,
          categoryId: categoryId.value,
          subCategoryId: subCategoryId.value,
          amount: amount.value,
          credit: isCredit.value,
          purchaseDate: purchaseDate.value,
          description: description.value || undefined,
          totalInstallments: totalInstallments.value !== '' ? Number(totalInstallments.value) : undefined,
          referenceMonth: referenceMonth.value || undefined,
        },
      })
    }

    emit('saved', saved)
    emit('update:isDialogVisible', false)
  }
  catch (e) {
    setError(e)
  }
  finally {
    isLoading.value = false
  }
}

function onClose() {
  emit('update:isDialogVisible', false)
}
</script>

<template>
  <VDialog
    :width="$vuetify.display.smAndDown ? 'auto' : 700"
    :model-value="props.isDialogVisible"
    @update:model-value="onClose"
  >
    <DialogCloseBtn @click="onClose" />

    <VCard class="pa-sm-10 pa-4">
      <VCardText>
        <h4 class="text-h4 text-center mb-2">
          {{ isEditMode ? 'Editar Lançamento' : 'Adicionar Lançamento' }}
        </h4>
        <p class="text-body-1 text-center mb-2">
          {{ isEditMode ? 'Atualize os dados do lançamento.' : 'Preencha os dados da nova compra no cartão.' }}
        </p>

        <div
          v-if="isEditMode && props.transaction?.credit"
          class="d-flex justify-center mb-6"
        >
          <VChip
            size="small"
            variant="tonal"
            color="success"
          >
            Crédito (abate da fatura)
          </VChip>
        </div>
        <div
          v-else
          class="mb-6"
        />

        <ApiErrorAlert
          v-if="error"
          :error="error"
          class="mb-4"
        />

        <VForm ref="formRef">
          <VRow>
            <VCol
              cols="12"
              md="6"
            >
              <AppSelect
                v-model="categoryId"
                label="Categoria"
                :items="categoryItems"
                item-title="label"
                item-value="id"
                :rules="categoryRules"
              />
            </VCol>

            <VCol
              cols="12"
              md="6"
            >
              <AppSelect
                v-model="subCategoryId"
                label="Subcategoria"
                :items="subCategoryItems"
                item-title="label"
                item-value="id"
                clearable
                :disabled="!selectedCategory"
              />
            </VCol>

            <VCol
              cols="12"
              md="6"
            >
              <AppCurrencyField
                v-model="amount"
                label="Valor"
                placeholder="0,00"
                :rules="amountRules"
              />
            </VCol>

            <VCol
              cols="12"
              md="6"
            >
              <AppTextField
                v-model="purchaseDate"
                type="date"
                label="Data da compra"
                :rules="dateRules"
              />
            </VCol>

            <VCol
              v-if="canChooseReferenceMonth"
              cols="12"
              md="6"
            >
              <AppSelect
                v-model="referenceMonth"
                label="Fatura"
                :items="referenceMonthItems"
                item-title="label"
                item-value="value"
                hint="No dia do fechamento o banco pode jogar a compra para a fatura seguinte"
                persistent-hint
              />
            </VCol>

            <VCol
              cols="12"
              :md="isEditMode && !canChooseReferenceMonth ? 12 : 6"
            >
              <AppTextField
                v-model="description"
                label="Descrição"
                placeholder="Opcional"
              />
            </VCol>

            <VCol
              v-if="!isEditMode && !isRecurringSubscription && !isCredit"
              cols="12"
              md="6"
            >
              <AppTextField
                v-model="totalInstallments"
                type="number"
                min="1"
                max="60"
                label="Parcelas"
                placeholder="Ex: 6"
                hint="Deixe em branco ou 1 para compra à vista"
                persistent-hint
                :rules="installmentsRules"
              />
            </VCol>

            <VCol
              v-if="!isEditMode && !isCredit"
              cols="12"
              :md="isRecurringSubscription ? 12 : 6"
              class="d-flex align-center"
            >
              <VCheckbox
                v-model="isRecurringSubscription"
                label="Assinatura recorrente (cobra todo mês)"
                hide-details
              />
            </VCol>

            <VCol
              v-if="!isEditMode && !isRecurringSubscription"
              cols="12"
              :md="isCredit ? 12 : 6"
              class="d-flex align-center"
            >
              <VCheckbox
                v-model="isCredit"
                label="Lançar como crédito (abate da fatura)"
                hide-details
              />
            </VCol>
          </VRow>

          <div class="d-flex align-center justify-center gap-4 mt-6">
            <VBtn
              :loading="isLoading"
              @click="onSubmit"
            >
              {{ isEditMode ? 'Salvar' : 'Criar' }}
            </VBtn>

            <VBtn
              color="secondary"
              variant="tonal"
              :disabled="isLoading"
              @click="onClose"
            >
              Cancelar
            </VBtn>
          </div>
        </VForm>
      </VCardText>
    </VCard>
  </VDialog>
</template>
