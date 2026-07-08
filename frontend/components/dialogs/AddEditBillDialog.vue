<script setup lang="ts">
import { VForm } from 'vuetify/components/VForm'

interface BillResponse {
  id: number
  version: number
  spaceId: number
  name: string
  categoryId: number | null
  subCategoryId: number | null
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
  bill?: BillResponse | null
  categories: CategoryOption[]
}

interface Emit {
  (e: 'update:isDialogVisible', value: boolean): void
  (e: 'saved'): void
}

const props = withDefaults(defineProps<Props>(), {
  bill: null,
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
const name = shallowRef('')
const categoryId = shallowRef<number | null>(null)
const subCategoryId = shallowRef<number | null>(null)
const defaultAmount = shallowRef<number | null>(null)
const startDate = shallowRef<string>('')
const recurring = shallowRef(false)
const isLoading = shallowRef(false)

const isEditMode = computed(() => props.bill !== null)

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
const amountRules = [(v: number | null) => (v !== null && v > 0) || 'Valor deve ser maior que zero']
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
      defaultAmount.value = props.bill?.defaultAmount ?? null
      startDate.value = props.bill?.startDate ?? toLocalDateString(new Date())
      recurring.value = false
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
    if (isEditMode.value) {
      await $fetch<BillResponse>(`/api/bills/${props.bill!.id}`, {
        method: 'PUT',
        body: {
          version: props.bill!.version,
          name: name.value,
          categoryId: categoryId.value,
          subCategoryId: subCategoryId.value,
          defaultAmount: defaultAmount.value,
        },
      })
    }
    else if (recurring.value) {
      await $fetch<BillResponse>('/api/bills', {
        method: 'POST',
        body: {
          spaceId: spaceStore.activeSpace!.id,
          name: name.value,
          categoryId: categoryId.value,
          subCategoryId: subCategoryId.value,
          defaultAmount: defaultAmount.value,
          startDate: startDate.value,
        },
      })
    }
    else {
      await $fetch('/api/bills/instances', {
        method: 'POST',
        body: {
          spaceId: spaceStore.activeSpace!.id,
          name: name.value,
          categoryId: categoryId.value,
          subCategoryId: subCategoryId.value,
          amount: defaultAmount.value,
          dueDate: startDate.value,
        },
      })
    }

    emit('saved')
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
          {{ isEditMode ? 'Editar Conta a Pagar' : 'Adicionar Conta a Pagar' }}
        </h4>
        <p class="text-body-1 text-center mb-6">
          {{ isEditMode ? 'Atualize os dados padrão da recorrência.' : 'Preencha os dados para criar uma nova conta a pagar.' }}
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

            <AppCurrencyField
              v-model="defaultAmount"
              label="Valor padrão"
              placeholder="0,00"
              :rules="amountRules"
            />

            <template v-if="!isEditMode">
              <AppTextField
                v-model="startDate"
                type="date"
                label="Data de Vencimento"
                :rules="dateRules"
              />

              <VCheckbox
                v-model="recurring"
                label="Conta recorrente (repete todo mês)"
                hide-details
              />
            </template>
          </div>

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
