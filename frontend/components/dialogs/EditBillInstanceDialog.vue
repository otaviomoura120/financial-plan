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
const amount = shallowRef<number | null>(null)
const dueDate = shallowRef<string>('')
const isLoading = shallowRef(false)

function optionLabel<T extends { name: string; active: boolean }>(item: T) {
  return item.active ? item.name : `${item.name} (inativo)`
}

const categoryItems = computed(() => props.categories.filter(c => !c.system).map(c => ({ ...c, label: optionLabel(c) })))

const selectedCategory = computed(() =>
  props.categories.find(c => c.id === categoryId.value) ?? null,
)

const subCategoryItems = computed(() =>
  (selectedCategory.value?.subCategories ?? []).map(sc => ({ ...sc, label: optionLabel(sc) })),
)

const nameRules = [(v: string) => !!v || 'Nome é obrigatório']
const amountRules = [(v: number | null) => (v !== null && v > 0) || 'Valor deve ser maior que zero']
const dateRules = [(v: string) => !!v || 'Data é obrigatória']

watch(categoryId, () => {
  if (!subCategoryItems.value.some(sc => sc.id === subCategoryId.value)) {
    subCategoryId.value = null
  }
})

watch(
  () => props.isDialogVisible,
  visible => {
    if (visible) {
      name.value = props.bill?.name ?? ''
      categoryId.value = props.bill?.categoryId ?? null
      subCategoryId.value = props.bill?.subCategoryId ?? null
      amount.value = props.bill?.amount ?? null
      dueDate.value = props.bill?.dueDate ?? ''
      clearError()
    }
  },
)

async function onSubmit() {
  const { valid } = await formRef.value!.validate()

  if (!valid || !props.bill) {
    return
  }

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
        amount: amount.value,
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
    :model-value="props.isDialogVisible"
    :fullscreen="$vuetify.display.smAndDown"
    max-width="600"
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
          Editar Conta
        </VCardTitle>
        <VCardSubtitle class="text-wrap">
          Altera somente esta conta lançada, sem afetar a recorrência.
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

            <AppCurrencyField
              v-model="amount"
              label="Valor"
              placeholder="0,00"
              :rules="amountRules"
            />

            <AppTextField
              v-model="dueDate"
              type="date"
              label="Data de Vencimento"
              :rules="dateRules"
            />
          </div>
        </VForm>
      </VCardText>

      <VDivider />

      <VCardActions class="px-5 px-sm-8 py-4">
        <div class="d-flex flex-column flex-sm-row justify-sm-end gap-3 w-100">
          <VBtn
            :loading="isLoading"
            color="primary"
            variant="elevated"
            :block="$vuetify.display.xs"
            :slim="false"
            @click="onSubmit"
          >
            Salvar
          </VBtn>

          <VBtn
            color="secondary"
            variant="tonal"
            :disabled="isLoading"
            :block="$vuetify.display.xs"
            :slim="false"
            @click="onClose"
          >
            Cancelar
          </VBtn>
        </div>
      </VCardActions>
    </VCard>
  </VDialog>
</template>
