<script setup lang="ts">
definePageMeta({ middleware: 'auth' })

const spaceStore = useSpaceStore()

const totalBalance = shallowRef(0)

const dueTotal = shallowRef(0)
const duePeriodLabel = shallowRef('')

const expenseTotal = shallowRef(0)
const expensePeriodLabel = shallowRef('')

const cardsTotal = shallowRef(0)
const cardsPeriodLabel = shallowRef('')
</script>

<template>
  <div
    v-if="!spaceStore.activeSpace"
    class="d-flex justify-center py-10"
  >
    <VProgressCircular indeterminate />
  </div>

  <div
    v-else
    class="d-flex flex-column gap-6"
  >
    <VRow>
      <VCol
        cols="12"
        sm="6"
        md="3"
      >
        <CardStatisticsVerticalSimple
          title="Saldo Total"
          :color="totalBalance >= 0 ? 'primary' : 'error'"
          icon="tabler-wallet"
          :stats="formatCurrency(totalBalance)"
        />
      </VCol>

      <VCol
        cols="12"
        sm="6"
        md="3"
      >
        <CardStatisticsVerticalSimple
          title="A Pagar"
          color="warning"
          icon="tabler-calendar-due"
          :stats="formatCurrency(dueTotal)"
        />
        <div class="text-caption text-disabled text-center mt-1">
          {{ duePeriodLabel }}
        </div>
      </VCol>

      <VCol
        cols="12"
        sm="6"
        md="3"
      >
        <CardStatisticsVerticalSimple
          title="Gasto"
          color="error"
          icon="tabler-arrow-down"
          :stats="formatCurrency(expenseTotal)"
        />
        <div class="text-caption text-disabled text-center mt-1">
          {{ expensePeriodLabel }}
        </div>
      </VCol>

      <VCol
        cols="12"
        sm="6"
        md="3"
      >
        <CardStatisticsVerticalSimple
          title="Faturas"
          color="secondary"
          icon="tabler-credit-card"
          :stats="formatCurrency(cardsTotal)"
        />
        <div class="text-caption text-disabled text-center mt-1">
          {{ cardsPeriodLabel }}
        </div>
      </VCol>
    </VRow>

    <VRow>
      <VCol cols="12">
        <DashboardDueThisWeekCard
          :space-id="spaceStore.activeSpace.id"
          @update:total="dueTotal = $event"
          @update:period-label="duePeriodLabel = $event"
        />
      </VCol>
    </VRow>

    <VRow>
      <VCol cols="12">
        <DashboardAccountBalancesCard
          :space-id="spaceStore.activeSpace.id"
          @update:total="totalBalance = $event"
        />
      </VCol>
    </VRow>

    <VRow>
      <VCol
        cols="12"
        md="7"
      >
        <DashboardCategorySpendingCard
          :space-id="spaceStore.activeSpace.id"
          @update:total="expenseTotal = $event"
          @update:period-label="expensePeriodLabel = $event"
        />
      </VCol>

      <VCol
        cols="12"
        md="5"
      >
        <DashboardCreditCardSpendingCard
          :space-id="spaceStore.activeSpace.id"
          @update:total="cardsTotal = $event"
          @update:period-label="cardsPeriodLabel = $event"
        />
      </VCol>
    </VRow>
  </div>
</template>
