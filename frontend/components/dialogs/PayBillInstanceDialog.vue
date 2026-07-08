<script setup lang="ts">
import { VForm } from 'vuetify/components/VForm'

type BillInstanceStatus = 'PENDING' | 'PAID'

interface BillInstanceResponse {
  id: number
  version: number
  billRecurringId: number | null
  name: string
  categoryId: number | null
  subCategoryId: number | null
  referenceMonth: string
  dueDate: string
  amount: number
  status: BillInstanceStatus
  paidDate: string | null
  paymentTransactionId: number | null
  bankAccountId: number | null
  createdDate: string
}

interface OptionItem {
  id: number
  name: string
  active: boolean
}

interface Props {
  isDialogVisible: boolean
  billInstance: BillInstanceResponse | null
  bankAccounts: OptionItem[]
  paymentMethods: OptionItem[]
}

interface Emit {
  (e: 'update:isDialogVisible', value: boolean): void
  (e: 'paid', billInstance: BillInstanceResponse): void
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
const paymentMethodId = shallowRef<number | null>(null)
const paidDate = shallowRef<string>('')
const isLoading = shallowRef(false)

function optionLabel(item: OptionItem) {
  return item.active ? item.name : `${item.name} (inativo)`
}

const bankAccountItems = computed(() => props.bankAccounts.map(ba => ({ ...ba, label: optionLabel(ba) })))
const paymentMethodItems = computed(() => props.paymentMethods.map(pm => ({ ...pm, label: optionLabel(pm) })))

const bankAccountRules = [(v: number | null) => v !== null || 'Conta é obrigatória']
const paymentMethodRules = [(v: number | null) => v !== null || 'Forma de pagamento é obrigatória']
const dateRules = [(v: string) => !!v || 'Data é obrigatória']

watch(
  () => props.isDialogVisible,
  visible => {
    if (visible) {
      bankAccountId.value = null
      paymentMethodId.value = null
      paidDate.value = toLocalDateString(new Date())
      clearError()
    }
  },
)

async function onSubmit() {
  const { valid } = await formRef.value!.validate()

  if (!valid || !props.billInstance)
    return

  isLoading.value = true
  clearError()

  try {
    const paid = await $fetch<BillInstanceResponse>(`/api/bills/instances/${props.billInstance.id}/pay`, {
      method: 'POST',
      body: {
        bankAccountId: bankAccountId.value,
        paymentMethodId: paymentMethodId.value,
        paidDate: paidDate.value,
      },
    })

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

    <VCard class="pa-sm-10 pa-4">
      <VCardText>
        <h4 class="text-h4 text-center mb-2">
          Marcar como Paga
        </h4>
        <p class="text-body-1 text-center mb-6">
          {{ props.billInstance?.name }}
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

            <AppSelect
              v-model="paymentMethodId"
              label="Forma de pagamento"
              :items="paymentMethodItems"
              item-title="label"
              item-value="id"
              :rules="paymentMethodRules"
            />

            <AppTextField
              v-model="paidDate"
              type="date"
              label="Data do pagamento"
              :rules="dateRules"
            />
          </div>

          <div class="d-flex align-center justify-center gap-4 mt-6">
            <VBtn
              :loading="isLoading"
              @click="onSubmit"
            >
              Pagar
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
