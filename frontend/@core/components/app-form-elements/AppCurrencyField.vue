<script lang="ts" setup>
defineOptions({
  name: 'AppCurrencyField',
  inheritAttrs: false,
})

const props = withDefaults(defineProps<Props>(), {
  rules: () => [],
})

const emit = defineEmits<{
  (e: 'update:modelValue', value: number | null): void
}>()

interface Props {
  modelValue: number | null
  rules?: ((v: number | null) => true | string)[]
}

const displayValue = shallowRef('')

function formatCents(cents: number) {
  const intPart = Math.floor(cents / 100)
  const decPart = String(cents % 100).padStart(2, '0')

  return `${intPart.toLocaleString('pt-BR')},${decPart}`
}

watch(
  () => props.modelValue,
  value => {
    displayValue.value = value === null || value === undefined ? '' : formatCents(Math.round(value * 100))
  },
  { immediate: true },
)

const wrappedRules = computed(() => props.rules.map(rule => () => rule(props.modelValue)))

function onInput(raw: string | number) {
  const digitsOnly = String(raw).replace(/\D/g, '')

  if (!digitsOnly) {
    displayValue.value = ''
    emit('update:modelValue', null)

    return
  }

  const cents = Number.parseInt(digitsOnly, 10)

  displayValue.value = formatCents(cents)
  emit('update:modelValue', cents / 100)
}
</script>

<template>
  <AppTextField
    v-bind="$attrs"
    :model-value="displayValue"
    type="text"
    inputmode="decimal"
    :rules="wrappedRules"
    @update:model-value="onInput"
  />
</template>
