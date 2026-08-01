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
  competenceMonth: string
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

  if (v === '' || !Number.isInteger(parsed) || parsed < 1) {
    return 'Informe uma quantidade válida de parcelas'
  }

  return parsed <= eligibleInstallments.value.length || `Só há ${eligibleInstallments.value.length} parcela(s) elegível(is) para antecipar`
}]

async function loadGroup() {
  if (!props.installmentGroupId) {
    return
  }

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

  if (!valid || !props.installmentGroupId || !props.targetReferenceMonth) {
    return
  }

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
    :model-value="props.isDialogVisible"
    :fullscreen="$vuetify.display.smAndDown"
    max-width="500"
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
          Antecipar Parcelas
        </VCardTitle>
        <VCardSubtitle class="text-wrap">
          Move as últimas parcelas desta compra para a fatura aberta atual.
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
        </VForm>
      </VCardText>

      <VDivider />

      <VCardActions class="px-5 px-sm-8 py-4">
        <div class="d-flex flex-column flex-sm-row justify-sm-end gap-3 w-100">
          <VBtn
            color="primary"
            variant="elevated"
            :slim="false"
            :loading="isSubmitting"
            :disabled="eligibleInstallments.length === 0"
            :block="$vuetify.display.xs"
            @click="onSubmit"
          >
            Antecipar
          </VBtn>

          <VBtn
            color="secondary"
            variant="tonal"
            :slim="false"
            :disabled="isSubmitting"
            :block="$vuetify.display.xs"
            @click="onClose"
          >
            Cancelar
          </VBtn>
        </div>
      </VCardActions>
    </VCard>
  </VDialog>
</template>
