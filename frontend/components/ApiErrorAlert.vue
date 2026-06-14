<script setup lang="ts">
interface Props {
  error: string | null
  closable?: boolean
}

withDefaults(defineProps<Props>(), { closable: false })
defineEmits<{ dismiss: [] }>()
</script>

<template>
  <VAlert
    v-if="error"
    type="error"
    variant="tonal"
    :closable="closable"
    :text="error"
    class="mb-4"
    @click:close="$emit('dismiss')"
  />
</template>
