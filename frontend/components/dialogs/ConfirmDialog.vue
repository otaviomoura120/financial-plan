<script setup lang="ts">
interface Props {
  confirmationQuestion: string
  isDialogVisible: boolean
  confirmTitle?: string
  confirmMsg?: string
  cancelTitle?: string
  cancelMsg?: string
  autoResult?: boolean
  confirmColor?: string
}

interface Emit {
  (e: 'update:isDialogVisible', value: boolean): void
  (e: 'confirm', value: boolean): void
}

const props = defineProps<Props>()

const emit = defineEmits<Emit>()

const unsubscribed = ref(false)
const cancelled = ref(false)

// A destructive confirmation passes confirmColor="error"; the badge should match it.
const badgeColor = computed(() => props.confirmColor ?? 'warning')

const updateModelValue = (val: boolean) => {
  emit('update:isDialogVisible', val)
}

const onConfirmation = () => {
  emit('confirm', true)
  updateModelValue(false)

  if (props.autoResult !== false) {
    unsubscribed.value = true
  }
}

const onCancel = () => {
  emit('confirm', false)
  emit('update:isDialogVisible', false)

  if (props.autoResult !== false) {
    cancelled.value = true
  }
}
</script>

<template>
  <!-- 👉 Confirm Dialog -->
  <VDialog
    max-width="500"
    :model-value="props.isDialogVisible"
    @update:model-value="updateModelValue"
  >
    <VCard class="text-center px-4 px-sm-8 py-6">
      <VCardText class="pb-4">
        <VAvatar
          :color="badgeColor"
          variant="tonal"
          size="72"
          class="mb-5"
        >
          <VIcon
            icon="tabler-alert-triangle"
            size="36"
          />
        </VAvatar>

        <h6 class="text-h6 font-weight-medium text-wrap">
          {{ props.confirmationQuestion }}
        </h6>
      </VCardText>

      <VCardActions class="pa-0">
        <div class="d-flex flex-column flex-sm-row justify-sm-center gap-3 w-100">
          <VBtn
            variant="elevated"
            :color="props.confirmColor ?? 'primary'"
            :slim="false"
            :block="$vuetify.display.xs"
            @click="onConfirmation"
          >
            Confirmar
          </VBtn>

          <VBtn
            color="secondary"
            variant="tonal"
            :slim="false"
            :block="$vuetify.display.xs"
            @click="onCancel"
          >
            Cancelar
          </VBtn>
        </div>
      </VCardActions>
    </VCard>
  </VDialog>

  <!-- Unsubscribed -->
  <VDialog
    v-model="unsubscribed"
    max-width="500"
  >
    <VCard>
      <VCardText class="text-center px-4 px-sm-8 py-6">
        <VAvatar
          color="success"
          variant="tonal"
          size="72"
          class="mb-5"
        >
          <VIcon
            icon="tabler-check"
            size="36"
          />
        </VAvatar>

        <h5 class="text-h5 mb-2">
          {{ props.confirmTitle }}
        </h5>

        <p class="text-body-2 text-disabled">
          {{ props.confirmMsg }}
        </p>

        <VBtn
          color="success"
          :block="$vuetify.display.xs"
          @click="unsubscribed = false"
        >
          Ok
        </VBtn>
      </VCardText>
    </VCard>
  </VDialog>

  <!-- Cancelled -->
  <VDialog
    v-model="cancelled"
    max-width="500"
  >
    <VCard>
      <VCardText class="text-center px-4 px-sm-8 py-6">
        <VAvatar
          color="error"
          variant="tonal"
          size="72"
          class="mb-5"
        >
          <VIcon
            icon="tabler-x"
            size="36"
          />
        </VAvatar>

        <h5 class="text-h5 mb-2">
          {{ props.cancelTitle }}
        </h5>

        <p class="text-body-2 text-disabled">
          {{ props.cancelMsg }}
        </p>

        <VBtn
          color="secondary"
          variant="tonal"
          :block="$vuetify.display.xs"
          @click="cancelled = false"
        >
          Ok
        </VBtn>
      </VCardText>
    </VCard>
  </VDialog>
</template>
