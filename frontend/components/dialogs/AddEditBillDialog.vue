<script setup lang="ts">
import { VForm } from 'vuetify/components/VForm'

interface BillResponse {
  id: number
  version: number
  spaceId: number
  name: string
  categoryId: number | null
  defaultAmount: number
  startDate: string
  recurring: boolean
  active: boolean
  createdDate: string
}

interface CategoryOption {
  id: number
  name: string
  active: boolean
}

interface Props {
  isDialogVisible: boolean
  bill?: BillResponse | null
  categories: CategoryOption[]
}

interface Emit {
  (e: 'update:isDialogVisible', value: boolean): void
  (e: 'saved', bill: BillResponse): void
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
const defaultAmount = shallowRef<string>('')
const startDate = shallowRef<string>('')
const recurring = shallowRef(false)
const isLoading = shallowRef(false)

const isEditMode = computed(() => props.bill !== null)

function optionLabel(item: CategoryOption) {
  return item.active ? item.name : `${item.name} (inativo)`
}

const categoryItems = computed(() => props.categories.map(c => ({ ...c, label: optionLabel(c) })))

const nameRules = [(v: string) => !!v || 'Nome é obrigatório']
const amountRules = [(v: string) => (v !== '' && Number(v) > 0) || 'Valor deve ser maior que zero']
const dateRules = [(v: string) => !!v || 'Data é obrigatória']

watch(
  () => props.isDialogVisible,
  visible => {
    if (visible) {
      name.value = props.bill?.name ?? ''
      categoryId.value = props.bill?.categoryId ?? null
      defaultAmount.value = props.bill ? String(props.bill.defaultAmount) : ''
      startDate.value = props.bill?.startDate ?? toLocalDateString(new Date())
      recurring.value = props.bill?.recurring ?? false
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
    let saved: BillResponse

    if (isEditMode.value) {
      saved = await $fetch<BillResponse>(`/api/bills/${props.bill!.id}`, {
        method: 'PUT',
        body: {
          version: props.bill!.version,
          name: name.value,
          categoryId: categoryId.value,
          defaultAmount: Number(defaultAmount.value),
        },
      })
    }
    else {
      saved = await $fetch<BillResponse>('/api/bills', {
        method: 'POST',
        body: {
          spaceId: spaceStore.activeSpace!.id,
          name: name.value,
          categoryId: categoryId.value,
          defaultAmount: Number(defaultAmount.value),
          startDate: startDate.value,
          recurring: recurring.value,
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
          {{ isEditMode ? 'Atualize os dados básicos da conta.' : 'Preencha os dados para criar uma nova conta a pagar.' }}
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

            <AppTextField
              v-model="defaultAmount"
              type="number"
              step="0.01"
              label="Valor padrão"
              placeholder="0.00"
              :rules="amountRules"
            />

            <template v-if="!isEditMode">
              <AppTextField
                v-model="startDate"
                type="date"
                label="Data inicial"
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
