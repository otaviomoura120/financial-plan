<script setup lang="ts">
interface Props {
  modelValue: string
  label?: string
}

interface Emit {
  (e: 'update:modelValue', value: string): void
}

const props = defineProps<Props>()
const emit = defineEmits<Emit>()

const monthNames = [
  'Janeiro',
  'Fevereiro',
  'Março',
  'Abril',
  'Maio',
  'Junho',
  'Julho',
  'Agosto',
  'Setembro',
  'Outubro',
  'Novembro',
  'Dezembro',
]

const monthOptions = monthNames.map((title, index) => ({ title, value: index + 1 }))

const selectedYear = computed(() => Number(props.modelValue.slice(0, 4)))
const selectedMonth = computed(() => Number(props.modelValue.slice(5, 7)))

const yearOptions = computed(() => {
  const currentYear = new Date().getFullYear()
  const years = []

  for (let year = currentYear - 5; year <= currentYear + 10; year++)
    years.push(year)

  if (!years.includes(selectedYear.value))
    years.unshift(selectedYear.value)

  return years
})

function update(year: number, month: number) {
  emit('update:modelValue', `${year}-${String(month).padStart(2, '0')}-01`)
}
</script>

<template>
  <div class="d-flex gap-2">
    <AppSelect
      :model-value="selectedMonth"
      :items="monthOptions"
      item-title="title"
      item-value="value"
      :label="props.label ?? 'Mês'"
      hide-details
      style="min-inline-size: 160px"
      @update:model-value="(value: number) => update(selectedYear, value)"
    />
    <AppSelect
      :model-value="selectedYear"
      :items="yearOptions"
      label="Ano"
      hide-details
      style="min-inline-size: 110px"
      @update:model-value="(value: number) => update(value, selectedMonth)"
    />
  </div>
</template>
