<script setup lang="ts">
import { VForm } from 'vuetify/components/VForm'

interface CreditCardInvoicePaymentResponse {
  id: number
  creditCardId: number
  referenceMonth: string
  dueDate: string
  paidAmount: number
  paidDate: string
  paymentTransactionId: number
  bankAccountId: number
}

interface OptionItem {
  id: number
  name: string
  active: boolean
}

interface Props {
  isDialogVisible: boolean
  creditCardId: number | null
  referenceMonth: string | null
  bankAccounts: OptionItem[]
}

interface Emit {
  (e: 'update:isDialogVisible', value: boolean): void
  (e: 'paid', payment: CreditCardInvoicePaymentResponse): void
}

const props = defineProps<Props>()
const emit = defineEmits<Emit>()

const { error, setError, clearError } = useApiError()

function toLocalDateString(date: Date) {
  const year = date.getFullYear()
  const month = String(date.getMonth() + 1).padStart(2, '0')
  const day = String(date.getDate()).padStart(2, '0')

  return `${year}-${month}-${day}`
}

const formRef = useTemplateRef<InstanceType<typeof VForm>>('formRef')

const bankAccountId = shallowRef<number | null>(null)
const paidDate = shallowRef<string>('')
const isLoading = shallowRef(false)

function optionLabel(item: OptionItem) {
  return item.active ? item.name : `${item.name} (inativo)`
}

const bankAccountItems = computed(() => props.bankAccounts.map(ba => ({ ...ba, label: optionLabel(ba) })))

const bankAccountRules = [(v: number | null) => v !== null || 'Conta é obrigatória']
const dateRules = [(v: string) => !!v || 'Data é obrigatória']

watch(
  () => props.isDialogVisible,
  visible => {
    if (visible) {
      bankAccountId.value = null
      paidDate.value = toLocalDateString(new Date())
      clearError()
    }
  },
)

async function onSubmit() {
  const { valid } = await formRef.value!.validate()

  if (!valid || props.creditCardId === null || props.referenceMonth === null) {
    return
  }

  isLoading.value = true
  clearError()

  try {
    const paid = await $fetch<CreditCardInvoicePaymentResponse>(
      `/api/credit-cards/${props.creditCardId}/invoices/${props.referenceMonth}/pay`,
      {
        method: 'POST',
        body: {
          bankAccountId: bankAccountId.value,
          paidDate: paidDate.value,
        },
      },
    )

    emit('paid', paid)
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

    <VCard class="pa-4 pa-sm-8">
      <VCardText class="pa-0">
        <h5 class="text-h5 mb-1">
          Pagar Fatura
        </h5>
        <p class="text-body-2 text-disabled mb-6">
          Informe os dados do pagamento. A categoria é preenchida automaticamente pelo sistema.
        </p>

        <ApiErrorAlert
          v-if="error"
          :error="error"
          class="mb-4"
        />

        <VForm ref="formRef">
          <div class="d-flex flex-column gap-4">
            <AppSelect
              v-model="bankAccountId"
              label="Conta bancária"
              :items="bankAccountItems"
              item-title="label"
              item-value="id"
              :rules="bankAccountRules"
            />

            <AppTextField
              v-model="paidDate"
              type="date"
              label="Data do pagamento"
              :rules="dateRules"
            />
          </div>

          <div class="d-flex flex-column flex-sm-row justify-sm-end gap-3 mt-6">
            <VBtn
              :loading="isLoading"
              :block="$vuetify.display.xs"
              @click="onSubmit"
            >
              Pagar
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
