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

  if (!valid || !props.billInstance) {
    return
  }

  isLoading.value = true
  clearError()

  try {
    const paid = await $fetch<BillInstanceResponse>(`/api/bills/instances/${props.billInstance.id}/pay`, {
      method: 'POST',
      body: {
        bankAccountId: bankAccountId.value,
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
          Marcar como Paga
        </VCardTitle>
        <VCardSubtitle class="text-wrap">
          {{ props.billInstance?.name }}
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
            Pagar
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
