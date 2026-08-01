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

// The three kinds are mutually exclusive: an installment plan only makes sense for a purchase, and a
// credit can be neither installed nor recurring. Modelling them as one value makes the rule visible
// in the UI instead of hiding it behind checkboxes that silently reset each other.
type TransactionType = 'purchase' | 'recurring' | 'credit'

const categoryId = shallowRef<number | null>(null)
const subCategoryId = shallowRef<number | null>(null)
const amount = shallowRef<number | null>(null)
const purchaseDate = shallowRef<string>('')
const description = shallowRef('')
const totalInstallments = shallowRef<string>('')
const referenceMonth = shallowRef<string>('')
const transactionType = shallowRef<TransactionType>('purchase')
const isLoading = shallowRef(false)

const isEditMode = computed(() => props.transaction !== null)
const isRecurringSubscription = computed(() => transactionType.value === 'recurring')
const isCredit = computed(() => transactionType.value === 'credit')

const transactionTypeHint = computed(() => {
  if (transactionType.value === 'recurring') {
    return 'Cobrança que se repete todo mês neste cartão.'
  }

  if (transactionType.value === 'credit') {
    return 'Estorno ou pagamento que abate o valor da fatura.'
  }

  return 'Compra à vista ou parcelada.'
})

const purchaseDateLabel = computed(() =>
  isRecurringSubscription.value ? 'Data de início' : 'Data da compra',
)

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
    { value: current, label: `${formatReferenceMonth(current)} (atual)` },
    { value: next, label: `${formatReferenceMonth(next)} (próxima)` },
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
      transactionType.value = 'purchase'
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

watch(transactionType, type => {
  if (type !== 'purchase') {
    totalInstallments.value = ''
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
    :model-value="props.isDialogVisible"
    :fullscreen="$vuetify.display.smAndDown"
    max-width="700"
    scrollable
    @update:model-value="onClose"
  >
    <DialogCloseBtn
      v-if="!$vuetify.display.smAndDown"
      @click="onClose"
    />

    <VCard class="d-flex flex-column">
      <VCardItem class="px-5 px-sm-8 pt-5 pt-sm-8">
        <VCardTitle class="text-h5 text-wrap">
          {{ isEditMode ? 'Editar Lançamento' : 'Adicionar Lançamento' }}
        </VCardTitle>
        <VCardSubtitle class="text-wrap">
          {{ isEditMode ? 'Atualize os dados do lançamento.' : 'Preencha os dados da nova compra no cartão.' }}
        </VCardSubtitle>

        <template #append>
          <IconBtn
            v-if="$vuetify.display.smAndDown"
            @click="onClose"
          >
            <VIcon icon="tabler-x" />
          </IconBtn>
        </template>
      </VCardItem>

      <VCardText class="px-5 px-sm-8">
        <VChip
          v-if="isEditMode && props.transaction?.credit"
          size="small"
          variant="tonal"
          color="success"
          class="mb-6"
        >
          Crédito (abate da fatura)
        </VChip>

        <ApiErrorAlert
          v-if="error"
          :error="error"
          class="mb-6"
        />

        <div
          v-if="!isEditMode"
          class="mb-6"
        >
          <VLabel
            class="mb-1 text-body-2"
            text="Tipo de lançamento"
          />

          <VBtnToggle
            v-model="transactionType"
            mandatory
            divided
            variant="outlined"
            color="primary"
            density="comfortable"
            class="w-100"
          >
            <VBtn
              value="purchase"
              :prepend-icon="$vuetify.display.xs ? undefined : 'tabler-shopping-cart'"
              class="flex-grow-1"
            >
              Compra
            </VBtn>

            <VBtn
              value="recurring"
              :prepend-icon="$vuetify.display.xs ? undefined : 'tabler-repeat'"
              class="flex-grow-1"
            >
              Assinatura
            </VBtn>

            <VBtn
              value="credit"
              :prepend-icon="$vuetify.display.xs ? undefined : 'tabler-arrow-back-up'"
              class="flex-grow-1"
            >
              Crédito
            </VBtn>
          </VBtnToggle>

          <div class="text-body-2 text-disabled mt-2">
            {{ transactionTypeHint }}
          </div>
        </div>

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
                :label="purchaseDateLabel"
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
                hint="Compras no dia do fechamento podem cair na fatura seguinte."
                persistent-hint
              />
            </VCol>

            <VCol
              v-if="!isEditMode && transactionType === 'purchase'"
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
                hint="Em branco ou 1 = à vista."
                persistent-hint
                :rules="installmentsRules"
              />
            </VCol>

            <VCol cols="12">
              <AppTextField
                v-model="description"
                label="Descrição"
                placeholder="Opcional"
              />
            </VCol>
          </VRow>
        </VForm>
      </VCardText>

      <VDivider />

      <VCardActions class="px-5 px-sm-8 py-4">
        <!--
          VCardActions pushes variant: 'text' and slim: true onto nested buttons, which would
          strip the primary fill off the submit button — both are overridden explicitly here.
        -->
        <div class="d-flex flex-column flex-sm-row justify-sm-end gap-3 w-100">
          <VBtn
            color="primary"
            variant="elevated"
            :slim="false"
            :loading="isLoading"
            :block="$vuetify.display.xs"
            @click="onSubmit"
          >
            {{ isEditMode ? 'Salvar' : 'Criar' }}
          </VBtn>

          <VBtn
            color="secondary"
            variant="tonal"
            :slim="false"
            :disabled="isLoading"
            :block="$vuetify.display.xs"
            @click="onClose"
          >
            Cancelar
          </VBtn>
        </div>
      </VCardActions>
    </VCard>
  </VDialog>
</template>
