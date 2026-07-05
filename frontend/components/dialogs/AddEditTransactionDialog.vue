<script setup lang="ts">
import { VForm } from 'vuetify/components/VForm'

type TransactionType = 'INCOME' | 'EXPENSE' | 'TRANSFER'

interface TransactionResponse {
  id: number
  version: number
  type: TransactionType
  userId: number
  bankAccountId: number
  destinationBankAccountId: number | null
  categoryId: number | null
  subCategoryId: number | null
  paymentMethodId: number | null
  amount: number
  transactionDate: string
  description?: string | null
  createdDate: string
}

interface BankAccountOption {
  id: number
  name: string
  active: boolean
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

interface PaymentMethodOption {
  id: number
  name: string
  active: boolean
}

interface Props {
  isDialogVisible: boolean
  transaction?: TransactionResponse | null
  bankAccounts: BankAccountOption[]
  categories: CategoryOption[]
  paymentMethods: PaymentMethodOption[]
}

interface Emit {
  (e: 'update:isDialogVisible', value: boolean): void
  (e: 'saved', transaction: TransactionResponse): void
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

const formRef = useTemplateRef<InstanceType<typeof VForm>>('formRef')

const type = shallowRef<TransactionType>('EXPENSE')
const bankAccountId = shallowRef<number | null>(null)
const destinationBankAccountId = shallowRef<number | null>(null)
const categoryId = shallowRef<number | null>(null)
const subCategoryId = shallowRef<number | null>(null)
const paymentMethodId = shallowRef<number | null>(null)
const amount = shallowRef<string>('')
const transactionDate = shallowRef<string>('')
const description = shallowRef('')
const isLoading = shallowRef(false)

const isEditMode = computed(() => props.transaction !== null)
const isTransfer = computed(() => type.value === 'TRANSFER')

const typeItems = [
  { title: 'Receita', value: 'INCOME' },
  { title: 'Despesa', value: 'EXPENSE' },
  { title: 'Transferência', value: 'TRANSFER' },
]

function optionLabel<T extends { name: string; active: boolean }>(item: T) {
  return item.active ? item.name : `${item.name} (inativo)`
}

const bankAccountItems = computed(() =>
  props.bankAccounts.map(ba => ({ ...ba, label: optionLabel(ba) })),
)

const destinationBankAccountItems = computed(() =>
  bankAccountItems.value.filter(ba => ba.id !== bankAccountId.value),
)

const categoryItems = computed(() =>
  props.categories.map(c => ({ ...c, label: optionLabel(c) })),
)

const selectedCategory = computed(() =>
  props.categories.find(c => c.id === categoryId.value) ?? null,
)

const subCategoryItems = computed(() =>
  (selectedCategory.value?.subCategories ?? []).map(sc => ({ ...sc, label: optionLabel(sc) })),
)

const paymentMethodItems = computed(() =>
  props.paymentMethods.map(pm => ({ ...pm, label: optionLabel(pm) })),
)

const typeRules = [(v: string) => !!v || 'Tipo é obrigatório']
const bankAccountRules = [(v: number | null) => v !== null || 'Conta de origem é obrigatória']

const destinationRules = [
  (v: number | null) => !isTransfer.value || v !== null || 'Conta de destino é obrigatória',
  (v: number | null) => !isTransfer.value || v !== bankAccountId.value || 'Conta de destino deve ser diferente da origem',
]

const categoryRules = [(v: number | null) => isTransfer.value || v !== null || 'Categoria é obrigatória']
const paymentMethodRules = [(v: number | null) => isTransfer.value || v !== null || 'Forma de pagamento é obrigatória']
const amountRules = [(v: string) => (v !== '' && Number(v) > 0) || 'Valor deve ser maior que zero']
const dateRules = [(v: string) => !!v || 'Data é obrigatória']

watch(type, () => {
  if (isTransfer.value) {
    categoryId.value = null
    subCategoryId.value = null
    paymentMethodId.value = null
  }
  else {
    destinationBankAccountId.value = null
  }
})

watch(categoryId, () => {
  if (!subCategoryItems.value.some(sc => sc.id === subCategoryId.value))
    subCategoryId.value = null
})

watch(
  () => props.isDialogVisible,
  visible => {
    if (visible) {
      const t = props.transaction

      type.value = t?.type ?? 'EXPENSE'
      bankAccountId.value = t?.bankAccountId ?? null
      destinationBankAccountId.value = t?.destinationBankAccountId ?? null
      categoryId.value = t?.categoryId ?? null
      subCategoryId.value = t?.subCategoryId ?? null
      paymentMethodId.value = t?.paymentMethodId ?? null
      amount.value = t ? String(t.amount) : ''
      transactionDate.value = t?.transactionDate ?? toLocalDateString(new Date())
      description.value = t?.description ?? ''
      clearError()
    }
  },
)

async function onSubmit() {
  const { valid } = await formRef.value!.validate()

  if (!valid)
    return

  isLoading.value = true
  clearError()

  try {
    let saved: TransactionResponse

    const commonBody = {
      type: type.value,
      bankAccountId: bankAccountId.value,
      destinationBankAccountId: isTransfer.value ? destinationBankAccountId.value : null,
      categoryId: isTransfer.value ? null : categoryId.value,
      subCategoryId: isTransfer.value ? null : subCategoryId.value,
      paymentMethodId: isTransfer.value ? null : paymentMethodId.value,
      amount: Number(amount.value),
      transactionDate: transactionDate.value,
      description: description.value || undefined,
    }

    if (isEditMode.value) {
      saved = await $fetch<TransactionResponse>(`/api/transactions/${props.transaction!.id}`, {
        method: 'PUT',
        body: {
          version: props.transaction!.version,
          ...commonBody,
        },
      })
    }
    else {
      saved = await $fetch<TransactionResponse>('/api/transactions', {
        method: 'POST',
        body: {
          userId: spaceStore.dbUser!.id,
          ...commonBody,
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
          {{ isEditMode ? 'Editar Transação' : 'Adicionar Transação' }}
        </h4>
        <p class="text-body-1 text-center mb-6">
          {{ isEditMode ? 'Atualize os dados da transação.' : 'Preencha os dados para criar uma nova transação.' }}
        </p>

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
                v-model="type"
                label="Tipo"
                :items="typeItems"
                :rules="typeRules"
              />
            </VCol>

            <VCol
              cols="12"
              md="6"
            >
              <AppTextField
                v-model="amount"
                type="number"
                step="0.01"
                label="Valor"
                placeholder="0.00"
                :rules="amountRules"
              />
            </VCol>

            <VCol
              cols="12"
              :md="isTransfer ? 6 : 12"
            >
              <AppSelect
                v-model="bankAccountId"
                :label="isTransfer ? 'Conta de origem' : 'Conta'"
                :items="bankAccountItems"
                item-title="label"
                item-value="id"
                :rules="bankAccountRules"
              />
            </VCol>

            <VCol
              v-if="isTransfer"
              cols="12"
              md="6"
            >
              <AppSelect
                v-model="destinationBankAccountId"
                label="Conta de destino"
                :items="destinationBankAccountItems"
                item-title="label"
                item-value="id"
                :rules="destinationRules"
              />
            </VCol>

            <template v-else>
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

              <VCol cols="12">
                <AppSelect
                  v-model="paymentMethodId"
                  label="Forma de pagamento"
                  :items="paymentMethodItems"
                  item-title="label"
                  item-value="id"
                  :rules="paymentMethodRules"
                />
              </VCol>
            </template>

            <VCol
              cols="12"
              md="6"
            >
              <AppTextField
                v-model="transactionDate"
                type="date"
                label="Data"
                :rules="dateRules"
              />
            </VCol>

            <VCol
              cols="12"
              md="6"
            >
              <AppTextField
                v-model="description"
                label="Descrição"
                placeholder="Opcional"
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
