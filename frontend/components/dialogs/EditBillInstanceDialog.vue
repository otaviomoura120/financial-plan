<script setup lang="ts">
import { VForm } from 'vuetify/components/VForm'

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

interface Props {
  isDialogVisible: boolean
  bill: BillInstanceResponse | null
  categories: CategoryOption[]
}

interface Emit {
  (e: 'update:isDialogVisible', value: boolean): void
  (e: 'saved', bill: BillInstanceResponse): void
}

const props = defineProps<Props>()
const emit = defineEmits<Emit>()

const { error, setError, clearError } = useApiError()

const formRef = useTemplateRef<InstanceType<typeof VForm>>('formRef')
const name = shallowRef('')
const categoryId = shallowRef<number | null>(null)
const subCategoryId = shallowRef<number | null>(null)
const amount = shallowRef<string>('')
const dueDate = shallowRef<string>('')
const isLoading = shallowRef(false)

function optionLabel<T extends { name: string; active: boolean }>(item: T) {
  return item.active ? item.name : `${item.name} (inativo)`
}

const categoryItems = computed(() => props.categories.map(c => ({ ...c, label: optionLabel(c) })))

const selectedCategory = computed(() =>
  props.categories.find(c => c.id === categoryId.value) ?? null,
)

const subCategoryItems = computed(() =>
  (selectedCategory.value?.subCategories ?? []).map(sc => ({ ...sc, label: optionLabel(sc) })),
)

const nameRules = [(v: string) => !!v || 'Nome é obrigatório']
const amountRules = [(v: string) => (v !== '' && Number(v) > 0) || 'Valor deve ser maior que zero']
const dateRules = [(v: string) => !!v || 'Data é obrigatória']

watch(categoryId, () => {
  if (!subCategoryItems.value.some(sc => sc.id === subCategoryId.value))
    subCategoryId.value = null
})

watch(
  () => props.isDialogVisible,
  visible => {
    if (visible) {
      name.value = props.bill?.name ?? ''
      categoryId.value = props.bill?.categoryId ?? null
      subCategoryId.value = props.bill?.subCategoryId ?? null
      amount.value = props.bill ? String(props.bill.amount) : ''
      dueDate.value = props.bill?.dueDate ?? ''
      clearError()
    }
  },
)

async function onSubmit() {
  const { valid } = await formRef.value!.validate()

  if (!valid || !props.bill)
    return

  isLoading.value = true
  clearError()

  try {
    const saved = await $fetch<BillInstanceResponse>(`/api/bills/instances/${props.bill.id}`, {
      method: 'PUT',
      body: {
        version: props.bill.version,
        name: name.value,
        categoryId: categoryId.value,
        subCategoryId: subCategoryId.value,
        amount: Number(amount.value),
        dueDate: dueDate.value,
      },
    })

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
    :width="$vuetify.display.smAndDown ? 'auto' : 600"
    :model-value="props.isDialogVisible"
    @update:model-value="onClose"
  >
    <DialogCloseBtn @click="onClose" />

    <VCard class="pa-sm-10 pa-4">
      <VCardText>
        <h4 class="text-h4 text-center mb-2">
          Editar Conta
        </h4>
        <p class="text-body-1 text-center mb-6">
          Altera somente esta conta lançada, sem afetar a recorrência.
        </p>

        <ApiErrorAlert
          v-if="error"
          :error="error"
          class="mb-4"
        />

        <VForm ref="formRef">
          <div class="d-flex flex-column gap-4">
            <AppTextField
              v-model="name"
              label="Nome"
              placeholder="Ex: Conta de Luz"
              :rules="nameRules"
            />

            <AppSelect
              v-model="categoryId"
              label="Categoria"
              :items="categoryItems"
              item-title="label"
              item-value="id"
              clearable
            />

            <AppSelect
              v-model="subCategoryId"
              label="Subcategoria"
              :items="subCategoryItems"
              item-title="label"
              item-value="id"
              clearable
              :disabled="!selectedCategory"
            />

            <AppTextField
              v-model="amount"
              type="number"
              step="0.01"
              label="Valor"
              placeholder="0.00"
              :rules="amountRules"
            />

            <AppTextField
              v-model="dueDate"
              type="date"
              label="Data de Vencimento"
              :rules="dateRules"
            />
          </div>

          <div class="d-flex align-center justify-center gap-4 mt-6">
            <VBtn
              :loading="isLoading"
              @click="onSubmit"
            >
              Salvar
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
