<script setup lang="ts">
import { VForm } from 'vuetify/components/VForm'

interface CreditCardTransactionResponse {
  id: number
  version: number
  creditCardId: number
  userId: number
  categoryId: number | null
  subCategoryId: number | null
  amount: number
  purchaseDate: string
  description?: string | null
  referenceMonth: string
  installmentGroupId: string
  installmentNumber: number
  totalInstallments: number
  anticipated: boolean
  originalReferenceMonth: string | null
  createdDate: string
  totalAmount: number
}

interface Props {
  isDialogVisible: boolean
  installmentGroupId: string | null
  targetReferenceMonth: string | null
}

interface Emit {
  (e: 'update:isDialogVisible', value: boolean): void
  (e: 'anticipated', transactions: CreditCardTransactionResponse[]): void
}

const props = defineProps<Props>()
const emit = defineEmits<Emit>()

const { error, setError, clearError } = useApiError()

const formRef = useTemplateRef<InstanceType<typeof VForm>>('formRef')

const installments = ref<CreditCardTransactionResponse[]>([])
const installmentsToAnticipate = shallowRef<string>('')
const isLoadingGroup = shallowRef(false)
const isSubmitting = shallowRef(false)

const eligibleInstallments = computed(() =>
  installments.value.filter(t => props.targetReferenceMonth !== null && t.referenceMonth > props.targetReferenceMonth),
)

const installmentsRules = [(v: string) => {
  const parsed = Number(v)

  if (v === '' || !Number.isInteger(parsed) || parsed < 1)
    return 'Informe uma quantidade válida de parcelas'

  return parsed <= eligibleInstallments.value.length || `Só há ${eligibleInstallments.value.length} parcela(s) elegível(is) para antecipar`
}]

async function loadGroup() {
  if (!props.installmentGroupId)
    return

  isLoadingGroup.value = true
  clearError()

  try {
    installments.value = await $fetch<CreditCardTransactionResponse[]>(
      `/api/credit-card-transactions/installment-groups/${props.installmentGroupId}`,
    )
  }
  catch (e) {
    setError(e)
  }
  finally {
    isLoadingGroup.value = false
  }
}

watch(
  () => props.isDialogVisible,
  visible => {
    if (visible) {
      installmentsToAnticipate.value = ''
      clearError()
      loadGroup()
    }
  },
)

async function onSubmit() {
  const { valid } = await formRef.value!.validate()

  if (!valid || !props.installmentGroupId || !props.targetReferenceMonth)
    return

  isSubmitting.value = true
  clearError()

  try {
    const updated = await $fetch<CreditCardTransactionResponse[]>(
      `/api/credit-card-transactions/installment-groups/${props.installmentGroupId}/anticipate`,
      {
        method: 'POST',
        body: {
          targetReferenceMonth: props.targetReferenceMonth,
          installmentsToAnticipate: Number(installmentsToAnticipate.value),
        },
      },
    )

    emit('anticipated', updated)
    emit('update:isDialogVisible', false)
  }
  catch (e) {
    setError(e)
  }
  finally {
    isSubmitting.value = false
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
          Antecipar Parcelas
        </h4>
        <p class="text-body-1 text-center mb-6">
          Move as últimas parcelas desta compra para a fatura aberta atual.
        </p>

        <ApiErrorAlert
          v-if="error"
          :error="error"
          class="mb-4"
        />

        <div
          v-if="isLoadingGroup"
          class="d-flex justify-center py-6"
        >
          <VProgressCircular indeterminate />
        </div>

        <VForm
          v-else
          ref="formRef"
        >
          <p class="text-body-2 mb-4">
            Parcelas elegíveis para antecipação: <strong>{{ eligibleInstallments.length }}</strong>
          </p>

          <AppTextField
            v-model="installmentsToAnticipate"
            type="number"
            min="1"
            :max="eligibleInstallments.length"
            label="Quantidade de parcelas a antecipar"
            placeholder="Ex: 2"
            :rules="installmentsRules"
          />

          <div class="d-flex align-center justify-center gap-4 mt-6">
            <VBtn
              :loading="isSubmitting"
              :disabled="eligibleInstallments.length === 0"
              @click="onSubmit"
            >
              Antecipar
            </VBtn>

            <VBtn
              color="secondary"
              variant="tonal"
              :disabled="isSubmitting"
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
