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

const allowedWidgets = ref<Set<string>>(new Set())
const isLoadingWidgetPermissions = shallowRef(true)

function canShow(widgetKey: string) {
  return allowedWidgets.value.has(widgetKey)
}

watch(
  () => spaceStore.activeSpace,
  async space => {
    if (!space) {
      allowedWidgets.value = new Set()

      return
    }

    isLoadingWidgetPermissions.value = true

    try {
      const keys = await $fetch<string[]>('/api/dashboard-widgets', { query: { spaceId: space.id } })

      allowedWidgets.value = new Set(keys)
    }
    finally {
      isLoadingWidgetPermissions.value = false
    }
  },
  { immediate: true },
)
</script>

<template>
  <div
    v-if="!spaceStore.activeSpace || isLoadingWidgetPermissions"
    class="d-flex justify-center py-10"
  >
    <VProgressCircular indeterminate />
  </div>

  <div
    v-else
    class="d-flex flex-column gap-6"
  >
    <VRow v-if="canShow(DASHBOARD_WIDGET_SUMMARY_TILES)">
      <VCol
        v-if="canShow(DASHBOARD_WIDGET_ACCOUNT_BALANCES)"
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
        v-if="canShow(DASHBOARD_WIDGET_DUE_THIS_WEEK)"
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
        v-if="canShow(DASHBOARD_WIDGET_CATEGORY_SPENDING)"
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
        v-if="canShow(DASHBOARD_WIDGET_CREDIT_CARD_SPENDING)"
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

    <VRow v-if="canShow(DASHBOARD_WIDGET_DUE_THIS_WEEK)">
      <VCol cols="12">
        <DashboardDueThisWeekCard
          :space-id="spaceStore.activeSpace.id"
          @update:total="dueTotal = $event"
          @update:period-label="duePeriodLabel = $event"
        />
      </VCol>
    </VRow>

    <VRow v-if="canShow(DASHBOARD_WIDGET_ACCOUNT_BALANCES)">
      <VCol cols="12">
        <DashboardAccountBalancesCard
          :space-id="spaceStore.activeSpace.id"
          @update:total="totalBalance = $event"
        />
      </VCol>
    </VRow>

    <VRow v-if="canShow(DASHBOARD_WIDGET_CATEGORY_SPENDING) || canShow(DASHBOARD_WIDGET_CREDIT_CARD_SPENDING)">
      <VCol
        v-if="canShow(DASHBOARD_WIDGET_CATEGORY_SPENDING)"
        cols="12"
        :md="canShow(DASHBOARD_WIDGET_CREDIT_CARD_SPENDING) ? 7 : 12"
      >
        <DashboardCategorySpendingCard
          :space-id="spaceStore.activeSpace.id"
          @update:total="expenseTotal = $event"
          @update:period-label="expensePeriodLabel = $event"
        />
      </VCol>

      <VCol
        v-if="canShow(DASHBOARD_WIDGET_CREDIT_CARD_SPENDING)"
        cols="12"
        :md="canShow(DASHBOARD_WIDGET_CATEGORY_SPENDING) ? 5 : 12"
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
