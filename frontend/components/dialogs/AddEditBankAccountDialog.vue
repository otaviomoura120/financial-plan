<script setup lang="ts">
import { VForm } from 'vuetify/components/VForm'

interface BankAccountResponse {
  id: number
  version: number
  spaceId: number
  name: string
  bankName: string
  balance: number
  active: boolean
  createdDate: string
}

interface Props {
  isDialogVisible: boolean
  bankAccount?: BankAccountResponse | null
}

interface Emit {
  (e: 'update:isDialogVisible', value: boolean): void
  (e: 'saved', bankAccount: BankAccountResponse): void
}

const props = withDefaults(defineProps<Props>(), {
  bankAccount: null,
})

const emit = defineEmits<Emit>()

const spaceStore = useSpaceStore()
const { error, setError, clearError } = useApiError()

const formRef = useTemplateRef<InstanceType<typeof VForm>>('formRef')
const name = shallowRef('')
const bankName = shallowRef('')
const initialBalance = shallowRef<string>('0')
const isLoading = shallowRef(false)

const isEditMode = computed(() => props.bankAccount !== null)

const nameRules = [(v: string) => !!v || 'Nome é obrigatório']
const bankNameRules = [(v: string) => !!v || 'Banco é obrigatório']
const balanceRules = [(v: string) => (v !== '' && !Number.isNaN(Number(v))) || 'Saldo inicial deve ser um número válido']

watch(
  () => props.isDialogVisible,
  visible => {
    if (visible) {
      name.value = props.bankAccount?.name ?? ''
      bankName.value = props.bankAccount?.bankName ?? ''
      initialBalance.value = '0'
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
    let saved: BankAccountResponse

    if (isEditMode.value) {
      saved = await $fetch<BankAccountResponse>(`/api/bank-accounts/${props.bankAccount!.id}`, {
        method: 'PUT',
        body: {
          version: props.bankAccount!.version,
          name: name.value,
          bankName: bankName.value,
        },
      })
    }
    else {
      saved = await $fetch<BankAccountResponse>('/api/bank-accounts', {
        method: 'POST',
        body: {
          spaceId: spaceStore.activeSpace!.id,
          name: name.value,
          bankName: bankName.value,
          initialBalance: Number(initialBalance.value),
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
          {{ isEditMode ? 'Editar Conta Bancária' : 'Adicionar Conta Bancária' }}
        </h4>
        <p class="text-body-1 text-center mb-6">
          {{ isEditMode ? 'Atualize os dados da conta bancária.' : 'Preencha os dados para criar uma nova conta bancária.' }}
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
              placeholder="Ex: Conta Corrente"
              :rules="nameRules"
            />

            <AppTextField
              v-model="bankName"
              label="Banco"
              placeholder="Ex: Nubank"
              :rules="bankNameRules"
            />

            <AppTextField
              v-if="!isEditMode"
              v-model="initialBalance"
              type="number"
              step="0.01"
              label="Saldo inicial"
              placeholder="0.00"
              :rules="balanceRules"
            />

            <AppTextField
              v-else
              :model-value="bankAccount?.balance"
              label="Saldo"
              readonly
              disabled
              hint="O saldo só muda a partir de transações."
              persistent-hint
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
