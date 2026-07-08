<script setup lang="ts">
interface Props {
  currentBalance: number
  projectedBalance: number
  pendingCreditCardTotal: number
  pendingBillTotal: number
}

const props = defineProps<Props>()

const currentColor = computed(() => (props.currentBalance >= 0 ? 'primary' : 'error'))
const projectedColor = computed(() => (props.projectedBalance >= 0 ? 'success' : 'error'))
</script>

<template>
  <VCard>
    <VCardText>
      <VRow>
        <VCol
          cols="12"
          sm="6"
          class="d-flex flex-column align-center justify-center text-center"
        >
          <VAvatar
            size="40"
            variant="tonal"
            rounded
            :color="currentColor"
          >
            <VIcon icon="tabler-wallet" />
          </VAvatar>

          <h5 class="text-h5 pt-2 mb-1">
            {{ formatCurrency(props.currentBalance) }}
          </h5>
          <div class="text-body-1">
            Saldo Atual
          </div>
        </VCol>

        <VCol
          cols="12"
          sm="6"
          class="d-flex flex-column align-center justify-center text-center"
        >
          <VAvatar
            size="40"
            variant="tonal"
            rounded
            :color="projectedColor"
          >
            <VIcon icon="tabler-trending-up" />
          </VAvatar>

          <h5 class="text-h5 pt-2 mb-1">
            {{ formatCurrency(props.projectedBalance) }}
          </h5>
          <div class="text-body-1">
            Saldo Previsto
          </div>
        </VCol>
      </VRow>

      <VDivider class="my-4" />

      <div class="d-flex flex-column gap-1 text-body-2 text-disabled">
        <div class="d-flex justify-space-between">
          <span>Saldo Atual</span>
          <span>{{ formatCurrency(props.currentBalance) }}</span>
        </div>
        <div class="d-flex justify-space-between">
          <span>− Faturas de cartão pendentes</span>
          <span>{{ formatCurrency(props.pendingCreditCardTotal) }}</span>
        </div>
        <div class="d-flex justify-space-between">
          <span>− Contas pendentes</span>
          <span>{{ formatCurrency(props.pendingBillTotal) }}</span>
        </div>
        <div class="d-flex justify-space-between text-body-1 font-weight-medium text-high-emphasis">
          <span>= Saldo Previsto</span>
          <span>{{ formatCurrency(props.projectedBalance) }}</span>
        </div>
      </div>
    </VCardText>
  </VCard>
</template>
