<script setup lang="ts">
interface Props {
  spaceId: number
}

const props = defineProps<Props>()

const emit = defineEmits<{
  'update:total': [total: number]
}>()

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

const { error, setError, clearError } = useApiError()

const accounts = ref<BankAccountResponse[]>([])
const isLoading = shallowRef(false)

const activeAccounts = computed(() => accounts.value.filter(a => a.active))
const totalBalance = computed(() => activeAccounts.value.reduce((sum, a) => sum + a.balance, 0))

watch(totalBalance, value => emit('update:total', value), { immediate: true })

async function fetchAccounts() {
  isLoading.value = true
  clearError()

  try {
    accounts.value = await $fetch<BankAccountResponse[]>('/api/bank-accounts', {
      query: { spaceId: props.spaceId },
    })
  }
  catch (e) {
    setError(e)
  }
  finally {
    isLoading.value = false
  }
}

watch(() => props.spaceId, fetchAccounts, { immediate: true })
</script>

<template>
  <VCard title="Saldo Atual das Contas">
    <VCardText>
      <ApiErrorAlert :error="error" />

      <div
        v-if="isLoading"
        class="d-flex justify-center py-8"
      >
        <VProgressCircular indeterminate />
      </div>

      <div
        v-else-if="activeAccounts.length === 0"
        class="d-flex flex-column align-center text-center text-disabled py-8"
      >
        <VIcon
          icon="tabler-building-bank"
          size="32"
          class="mb-2"
        />
        <div>Nenhuma conta bancária ativa cadastrada.</div>
        <VBtn
          class="mt-4"
          variant="tonal"
          size="small"
          to="/bank-accounts"
        >
          Cadastrar conta
        </VBtn>
      </div>

      <VRow v-else>
        <VCol
          v-for="account in activeAccounts"
          :key="account.id"
          cols="6"
          sm="4"
          md="3"
        >
          <div class="d-flex flex-column align-center text-center pa-2">
            <VAvatar
              size="40"
              variant="tonal"
              rounded
              :color="account.balance < 0 ? 'error' : 'primary'"
            >
              <VIcon icon="tabler-building-bank" />
            </VAvatar>

            <div class="text-caption text-disabled mt-2">
              {{ account.bankName }}
            </div>
            <div class="text-body-2 font-weight-medium">
              {{ account.name }}
            </div>
            <div
              class="text-body-1 font-weight-medium"
              :class="account.balance < 0 ? 'text-error' : 'text-high-emphasis'"
            >
              {{ formatCurrency(account.balance) }}
            </div>
          </div>
        </VCol>
      </VRow>
    </VCardText>
  </VCard>
</template>
