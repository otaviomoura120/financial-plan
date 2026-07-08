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

interface Props {
  isDialogVisible: boolean
  bill: BillResponse | null
}

interface Emit {
  (e: 'update:isDialogVisible', value: boolean): void
  (e: 'saved', bill: BillResponse): void
}

const props = defineProps<Props>()
const emit = defineEmits<Emit>()

const { error, setError, clearError } = useApiError()

const formRef = useTemplateRef<InstanceType<typeof VForm>>('formRef')
const recurring = shallowRef(false)
const startDate = shallowRef('')
const isLoading = shallowRef(false)

const dateRules = [(v: string) => !!v || 'Data é obrigatória']

watch(
  () => props.isDialogVisible,
  visible => {
    if (visible) {
      recurring.value = props.bill?.recurring ?? false
      startDate.value = props.bill?.startDate ?? ''
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
    const saved = await $fetch<BillResponse>(`/api/bills/${props.bill.id}/schedule`, {
      method: 'PUT',
      body: {
        version: props.bill.version,
        recurring: recurring.value,
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
    :width="$vuetify.display.smAndDown ? 'auto' : 500"
    :model-value="props.isDialogVisible"
    @update:model-value="onClose"
  >
    <DialogCloseBtn @click="onClose" />

    <VCard class="pa-sm-10 pa-4">
      <VCardText>
        <h4 class="text-h4 text-center mb-2">
          Editar Agenda
        </h4>
        <p class="text-body-1 text-center mb-6">
          {{ props.bill?.name }}
        </p>

        <VAlert
          color="warning"
          variant="tonal"
          class="mb-4"
        >
          Alterar a agenda só afeta instâncias futuras — as já geradas (pendentes ou pagas)
          mantêm o vencimento antigo.
        </VAlert>

        <ApiErrorAlert
          v-if="error"
          :error="error"
          class="mb-4"
        />

        <VForm ref="formRef">
          <div class="d-flex flex-column gap-4">
            <VCheckbox
              v-model="recurring"
              label="Conta recorrente (repete todo mês)"
              hide-details
            />

            <AppTextField
              v-model="startDate"
              type="date"
              label="Data inicial"
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
