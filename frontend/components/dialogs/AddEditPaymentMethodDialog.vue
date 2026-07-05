<script setup lang="ts">
import { VForm } from 'vuetify/components/VForm'

interface PaymentMethodResponse {
  id: number
  version: number
  name: string
  active: boolean
}

interface Props {
  isDialogVisible: boolean
  paymentMethod?: PaymentMethodResponse | null
}

interface Emit {
  (e: 'update:isDialogVisible', value: boolean): void
  (e: 'saved', paymentMethod: PaymentMethodResponse): void
}

const props = withDefaults(defineProps<Props>(), {
  paymentMethod: null,
})

const emit = defineEmits<Emit>()

const spaceStore = useSpaceStore()
const { error, setError, clearError } = useApiError()

const formRef = useTemplateRef<InstanceType<typeof VForm>>('formRef')
const name = shallowRef('')
const isLoading = shallowRef(false)

const isEditMode = computed(() => props.paymentMethod !== null)

const nameRules = [(v: string) => !!v || 'Nome é obrigatório']

watch(
  () => props.isDialogVisible,
  visible => {
    if (visible) {
      name.value = props.paymentMethod?.name ?? ''
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
    let saved: PaymentMethodResponse

    if (isEditMode.value) {
      saved = await $fetch<PaymentMethodResponse>(`/api/payment-methods/${props.paymentMethod!.id}`, {
        method: 'PUT',
        body: {
          version: props.paymentMethod!.version,
          name: name.value,
        },
      })
    }
    else {
      saved = await $fetch<PaymentMethodResponse>('/api/payment-methods', {
        method: 'POST',
        body: {
          spaceId: spaceStore.activeSpace!.id,
          name: name.value,
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
          {{ isEditMode ? 'Editar Meio de Pagamento' : 'Adicionar Meio de Pagamento' }}
        </h4>
        <p class="text-body-1 text-center mb-6">
          {{ isEditMode ? 'Atualize os dados do meio de pagamento.' : 'Preencha os dados para criar um novo meio de pagamento.' }}
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
              placeholder="Digite o nome do meio de pagamento"
              :rules="nameRules"
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
