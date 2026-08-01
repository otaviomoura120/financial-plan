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
  endDate: string | null
  installments: number | null
  active: boolean
  createdDate: string
}

type RecurrenceEndMode = 'never' | 'onDate' | 'afterInstallments'

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
const endMode = shallowRef<RecurrenceEndMode>('never')
const endDate = shallowRef<string>('')
const installments = shallowRef<number | null>(null)
const isLoading = shallowRef(false)

const isEditMode = computed(() => props.bill !== null)

// The recurrence end only makes sense for a recurring bill: a standalone one is a single occurrence.
const showRecurrenceEnd = computed(() => isEditMode.value || recurring.value)

const endModeItems: { value: RecurrenceEndMode; title: string }[] = [
  { value: 'never', title: 'Nunca' },
  { value: 'onDate', title: 'Em uma data' },
  { value: 'afterInstallments', title: 'Após N parcelas' },
]

function resolveEndMode(bill: BillResponse | null): RecurrenceEndMode {
  if (bill?.installments != null) {
    return 'afterInstallments'
  }

  if (bill?.endDate != null) {
    return 'onDate'
  }

  return 'never'
}

function recurrenceEndPayload() {
  if (!showRecurrenceEnd.value || endMode.value === 'never') {
    return { endDate: null, installments: null }
  }

  if (endMode.value === 'onDate') {
    return { endDate: endDate.value, installments: null }
  }

  return { endDate: null, installments: installments.value }
}

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

const endDateRules = [
  (v: string) => !!v || 'Data de término é obrigatória',
  (v: string) => !v || !startDate.value || v >= startDate.value || 'Término não pode ser antes do vencimento inicial',
]

const installmentsRules = [
  (v: number | null) => (v !== null && v > 0) || 'Informe um número de parcelas maior que zero',
]

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
      defaultAmount.value = props.bill?.defaultAmount ?? null
      startDate.value = props.bill?.startDate ?? toLocalDateString(new Date())
      recurring.value = false
      endMode.value = resolveEndMode(props.bill)
      endDate.value = props.bill?.endDate ?? ''
      installments.value = props.bill?.installments ?? null
      clearError()
    }
  },
)

async function onSubmit() {
  const { valid } = await formRef.value!.validate()

  if (!valid) {
    return
  }

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
          startDate: startDate.value,
          ...recurrenceEndPayload(),
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
          ...recurrenceEndPayload(),
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
          {{ isEditMode ? 'Editar Conta a Pagar' : 'Adicionar Conta a Pagar' }}
        </VCardTitle>
        <VCardSubtitle class="text-wrap">
          {{ isEditMode ? 'Atualize os dados padrão da recorrência.' : 'Preencha os dados para criar uma nova conta a pagar.' }}
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
              v-model="defaultAmount"
              label="Valor padrão"
              placeholder="0,00"
              :rules="amountRules"
            />

            <AppTextField
              v-model="startDate"
              type="date"
              label="Data de Vencimento"
              :hint="isEditMode ? 'Alterar recalcula o vencimento das contas pendentes do mês atual em diante.' : undefined"
              :persistent-hint="isEditMode"
              :rules="dateRules"
            />

            <VCheckbox
              v-if="!isEditMode"
              v-model="recurring"
              label="Conta recorrente (repete todo mês)"
              hide-details
            />

            <template v-if="showRecurrenceEnd">
              <AppSelect
                v-model="endMode"
                label="Término da recorrência"
                :items="endModeItems"
                item-title="title"
                item-value="value"
                hint="Contas dos próximos 12 meses são geradas automaticamente e já aparecem nos relatórios futuros."
                persistent-hint
              />

              <AppTextField
                v-if="endMode === 'onDate'"
                v-model="endDate"
                type="date"
                label="Repetir até"
                :rules="endDateRules"
              />

              <AppTextField
                v-if="endMode === 'afterInstallments'"
                v-model.number="installments"
                type="number"
                min="1"
                label="Número de parcelas"
                placeholder="Ex: 12"
                :rules="installmentsRules"
              />
            </template>
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
            {{ isEditMode ? 'Salvar' : 'Criar' }}
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
