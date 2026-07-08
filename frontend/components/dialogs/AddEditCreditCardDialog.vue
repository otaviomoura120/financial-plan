<script setup lang="ts">
import { VForm } from 'vuetify/components/VForm'

interface CreditCardResponse {
  id: number
  version: number
  spaceId: number
  name: string
  limit: number
  closingDay: number
  dueDay: number
  active: boolean
  createdDate: string
}

interface Props {
  isDialogVisible: boolean
  creditCard?: CreditCardResponse | null
}

interface Emit {
  (e: 'update:isDialogVisible', value: boolean): void
  (e: 'saved', creditCard: CreditCardResponse): void
}

const props = withDefaults(defineProps<Props>(), {
  creditCard: null,
})

const emit = defineEmits<Emit>()

const spaceStore = useSpaceStore()
const { error, setError, clearError } = useApiError()

const formRef = useTemplateRef<InstanceType<typeof VForm>>('formRef')
const name = shallowRef('')
const limit = shallowRef<string>('')
const closingDay = shallowRef<string>('')
const dueDay = shallowRef<string>('')
const isLoading = shallowRef(false)

const isEditMode = computed(() => props.creditCard !== null)

const nameRules = [(v: string) => !!v || 'Nome é obrigatório']
const limitRules = [(v: string) => (v !== '' && Number(v) > 0) || 'Limite deve ser maior que zero']

function dayRules(label: string) {
  return [(v: string) => {
    const day = Number(v)

    return (v !== '' && Number.isInteger(day) && day >= 1 && day <= 31) || `${label} deve ser um dia entre 1 e 31`
  }]
}

const closingDayRules = dayRules('Dia de fechamento')
const dueDayRules = dayRules('Dia de vencimento')

watch(
  () => props.isDialogVisible,
  visible => {
    if (visible) {
      name.value = props.creditCard?.name ?? ''
      limit.value = props.creditCard ? String(props.creditCard.limit) : ''
      closingDay.value = props.creditCard ? String(props.creditCard.closingDay) : ''
      dueDay.value = props.creditCard ? String(props.creditCard.dueDay) : ''
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
    let saved: CreditCardResponse

    const commonBody = {
      name: name.value,
      limit: Number(limit.value),
      closingDay: Number(closingDay.value),
      dueDay: Number(dueDay.value),
    }

    if (isEditMode.value) {
      saved = await $fetch<CreditCardResponse>(`/api/credit-cards/${props.creditCard!.id}`, {
        method: 'PUT',
        body: {
          version: props.creditCard!.version,
          ...commonBody,
        },
      })
    }
    else {
      saved = await $fetch<CreditCardResponse>('/api/credit-cards', {
        method: 'POST',
        body: {
          spaceId: spaceStore.activeSpace!.id,
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
    :width="$vuetify.display.smAndDown ? 'auto' : 600"
    :model-value="props.isDialogVisible"
    @update:model-value="onClose"
  >
    <DialogCloseBtn @click="onClose" />

    <VCard class="pa-sm-10 pa-4">
      <VCardText>
        <h4 class="text-h4 text-center mb-2">
          {{ isEditMode ? 'Editar Cartão de Crédito' : 'Adicionar Cartão de Crédito' }}
        </h4>
        <p class="text-body-1 text-center mb-6">
          {{ isEditMode ? 'Atualize os dados do cartão.' : 'Preencha os dados para criar um novo cartão.' }}
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
              placeholder="Ex: Nubank Roxinho"
              :rules="nameRules"
            />

            <AppTextField
              v-model="limit"
              type="number"
              step="0.01"
              label="Limite"
              placeholder="0.00"
              :rules="limitRules"
            />

            <AppTextField
              v-model="closingDay"
              type="number"
              min="1"
              max="31"
              label="Dia de fechamento"
              placeholder="Ex: 20"
              :rules="closingDayRules"
            />

            <AppTextField
              v-model="dueDay"
              type="number"
              min="1"
              max="31"
              label="Dia de vencimento"
              placeholder="Ex: 27"
              :rules="dueDayRules"
            />
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
