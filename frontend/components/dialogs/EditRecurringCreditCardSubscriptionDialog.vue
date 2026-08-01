<script setup lang="ts">
import { VForm } from 'vuetify/components/VForm'

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
  recurring: CreditCardTransactionRecurringResponse | null
  categories: CategoryOption[]
}

interface Emit {
  (e: 'update:isDialogVisible', value: boolean): void
  (e: 'saved', recurring: CreditCardTransactionRecurringResponse): void
}

const props = defineProps<Props>()
const emit = defineEmits<Emit>()

const { error, setError, clearError } = useApiError()

const formRef = useTemplateRef<InstanceType<typeof VForm>>('formRef')

const categoryId = shallowRef<number | null>(null)
const subCategoryId = shallowRef<number | null>(null)
const defaultAmount = shallowRef<number | null>(null)
const description = shallowRef('')
const startDate = shallowRef('')
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

const categoryRules = [(v: number | null) => v !== null || 'Categoria é obrigatória']
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
      categoryId.value = props.recurring?.categoryId ?? null
      subCategoryId.value = props.recurring?.subCategoryId ?? null
      defaultAmount.value = props.recurring?.defaultAmount ?? null
      description.value = props.recurring?.description ?? ''
      startDate.value = props.recurring?.startDate ?? ''
      clearError()
    }
  },
)

async function onSubmit() {
  const { valid } = await formRef.value!.validate()

  if (!valid || !props.recurring) {
    return
  }

  isLoading.value = true
  clearError()

  try {
    const saved = await $fetch<CreditCardTransactionRecurringResponse>(`/api/credit-card-transactions/recurring/${props.recurring.id}`, {
      method: 'PUT',
      body: {
        version: props.recurring.version,
        categoryId: categoryId.value,
        subCategoryId: subCategoryId.value,
        defaultAmount: defaultAmount.value,
        description: description.value || undefined,
        startDate: startDate.value,
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

    <VCard class="pa-4 pa-sm-8">
      <VCardText class="pa-0">
        <div class="d-flex align-start gap-2 mb-1">
          <h5 class="text-h5">
            Editar Assinatura
          </h5>

          <VSpacer />

          <IconBtn
            v-if="$vuetify.display.smAndDown"
            @click="onClose"
          >
            <VIcon icon="tabler-x" />
          </IconBtn>
        </div>

        <p class="text-body-2 text-disabled mb-6">
          Afeta as cobranças pendentes do mês atual em diante; as de meses anteriores ou já pagas não são alteradas.
        </p>

        <ApiErrorAlert
          v-if="error"
          :error="error"
          class="mb-4"
        />

        <VForm ref="formRef">
          <div class="d-flex flex-column gap-4">
            <AppTextField
              v-model="description"
              label="Descrição"
              placeholder="Ex: Netflix"
            />

            <AppSelect
              v-model="categoryId"
              label="Categoria"
              :items="categoryItems"
              item-title="label"
              item-value="id"
              :rules="categoryRules"
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
              v-model="defaultAmount"
              label="Valor padrão"
              placeholder="0,00"
              :rules="amountRules"
            />

            <AppTextField
              v-model="startDate"
              type="date"
              label="Dia de cobrança"
              hint="Recalcula a data das cobranças pendentes."
              persistent-hint
              :rules="dateRules"
            />
          </div>

          <div class="d-flex flex-column flex-sm-row justify-sm-end gap-3 mt-6">
            <VBtn
              :loading="isLoading"
              :block="$vuetify.display.xs"
              @click="onSubmit"
            >
              Salvar
            </VBtn>

            <VBtn
              color="secondary"
              variant="tonal"
              :disabled="isLoading"
              :block="$vuetify.display.xs"
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
