<script setup lang="ts">
interface Props {
  isDialogVisible: boolean
}

type DeleteInstallmentResult = 'single' | 'future' | null

interface Emit {
  (e: 'update:isDialogVisible', value: boolean): void
  (e: 'confirm', value: DeleteInstallmentResult): void
}

const props = defineProps<Props>()
const emit = defineEmits<Emit>()

function updateModelValue(val: boolean) {
  emit('update:isDialogVisible', val)
}

function choose(result: DeleteInstallmentResult) {
  emit('confirm', result)
  updateModelValue(false)
}
</script>

<template>
  <VDialog
    max-width="500"
    :model-value="props.isDialogVisible"
    @update:model-value="updateModelValue"
  >
    <VCard class="text-center px-10 py-6">
      <VCardText>
        <VBtn
          icon
          variant="outlined"
          color="warning"
          class="my-4"
          style=" block-size: 88px;inline-size: 88px; pointer-events: none;"
        >
          <span class="text-5xl">!</span>
        </VBtn>

        <h6 class="text-lg font-weight-medium">
          Esta parcela faz parte de uma compra parcelada. O que deseja excluir?
        </h6>
      </VCardText>

      <VCardText class="d-flex flex-column align-center gap-2">
        <VBtn
          variant="elevated"
          color="error"
          block
          @click="choose('single')"
        >
          Excluir somente esta parcela
        </VBtn>

        <VBtn
          variant="elevated"
          color="error"
          block
          @click="choose('future')"
        >
          Excluir esta e as parcelas futuras
        </VBtn>

        <VBtn
          color="secondary"
          variant="tonal"
          block
          @click="choose(null)"
        >
          Cancelar
        </VBtn>
      </VCardText>
    </VCard>
  </VDialog>
</template>
