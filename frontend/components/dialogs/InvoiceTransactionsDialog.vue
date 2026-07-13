<script setup lang="ts">
interface CreditCardTransactionResponse {
  id: number
  version: number
  creditCardId: number
  userId: number
  categoryId: number | null
  subCategoryId: number | null
  amount: number
  purchaseDate: string
  description?: string | null
  referenceMonth: string
  installmentGroupId: string
  installmentNumber: number
  totalInstallments: number
  anticipated: boolean
  originalReferenceMonth: string | null
  createdDate: string
}

interface SubCategoryResponse {
  id: number
  categoryId: number
  name: string
  active: boolean
}

interface CategoryResponse {
  id: number
  name: string
  active: boolean
  subCategories: SubCategoryResponse[]
}

interface Props {
  isDialogVisible: boolean
  creditCardId: number | null
  referenceMonth: string | null
  categories: CategoryResponse[]
}

interface Emit {
  (e: 'update:isDialogVisible', value: boolean): void
}

const props = defineProps<Props>()
const emit = defineEmits<Emit>()

const spaceStore = useSpaceStore()
const { error, setError, clearError } = useApiError()

const currencyFormatter = new Intl.NumberFormat('pt-BR', { style: 'currency', currency: 'BRL' })

const items = ref<CreditCardTransactionResponse[]>([])
const isLoading = shallowRef(false)

const categoriesById = computed(() => new Map(props.categories.map(c => [c.id, c])))

const subCategoriesById = computed(() => {
  const map = new Map<number, SubCategoryResponse>()

  for (const category of props.categories) {
    for (const subCategory of category.subCategories)
      map.set(subCategory.id, subCategory)
  }

  return map
})

const sortedItems = computed(() =>
  [...items.value].sort((a, b) => a.purchaseDate.localeCompare(b.purchaseDate)),
)

watch(
  () => props.isDialogVisible,
  visible => {
    if (visible && props.creditCardId && props.referenceMonth) {
      fetchItems()
    }
    else {
      items.value = []
      clearError()
    }
  },
)

async function fetchItems() {
  if (!spaceStore.activeSpace || !props.creditCardId || !props.referenceMonth)
    return

  isLoading.value = true
  clearError()

  try {
    items.value = await $fetch<CreditCardTransactionResponse[]>('/api/credit-card-transactions', {
      query: {
        spaceId: spaceStore.activeSpace.id,
        creditCardId: props.creditCardId,
        referenceMonth: props.referenceMonth,
      },
    })
  }
  catch (e) {
    setError(e)
  }
  finally {
    isLoading.value = false
  }
}

function categoryName(id: number | null) {
  if (id === null)
    return '—'

  return categoriesById.value.get(id)?.name ?? '—'
}

function subCategoryName(id: number | null) {
  if (id === null)
    return null

  return subCategoriesById.value.get(id)?.name ?? null
}

function formatDate(isoDate: string) {
  const [year, month, day] = isoDate.split('-')

  return `${day}/${month}/${year}`
}

function formatReferenceMonth(isoDate: string) {
  const [year, month] = isoDate.split('-')

  return `${month}/${year}`
}

function onClose() {
  emit('update:isDialogVisible', false)
}
</script>

<template>
  <VDialog
    :width="$vuetify.display.smAndDown ? 'auto' : 800"
    :model-value="props.isDialogVisible"
    scrollable
    @update:model-value="onClose"
  >
    <DialogCloseBtn @click="onClose" />

    <VCard
      class="d-flex flex-column"
      style="block-size: 100%"
    >
      <VCardText>
        <h4 class="text-h4 text-center mb-2">
          Itens da Fatura {{ referenceMonth ? `— ${formatReferenceMonth(referenceMonth)}` : '' }}
        </h4>
        <p class="text-body-1 text-center mb-0">
          Lançamentos que compõem esta fatura, ordenados por data.
        </p>
      </VCardText>

      <VDivider />

      <VCardText
        class="flex-grow-1"
        style="overflow-y: auto"
      >
        <ApiErrorAlert
          v-if="error"
          :error="error"
          class="mb-4"
        />

        <div
          v-if="isLoading"
          class="d-flex justify-center py-10"
        >
          <VProgressCircular indeterminate />
        </div>

        <div
          v-else
          style="overflow-x: auto"
        >
          <VTable>
            <thead style="white-space: nowrap">
              <tr>
                <th>Data da Compra</th>
                <th style="min-width: 200px">
                  Categoria
                </th>
                <th>Descrição</th>
                <th>Parcela</th>
                <th class="text-right">
                  Valor
                </th>
              </tr>
            </thead>
            <tbody>
              <tr
                v-for="item in sortedItems"
                :key="item.id"
              >
                <td>{{ formatDate(item.purchaseDate) }}</td>
                <td>
                  {{ categoryName(item.categoryId) }}
                  <span
                    v-if="subCategoryName(item.subCategoryId)"
                    class="text-disabled"
                  >
                    / {{ subCategoryName(item.subCategoryId) }}
                  </span>
                </td>
                <td class="text-disabled">
                  {{ item.description || '—' }}
                </td>
                <td>
                  <VChip
                    v-if="item.totalInstallments > 1"
                    size="small"
                    variant="tonal"
                    color="info"
                  >
                    {{ item.installmentNumber }}/{{ item.totalInstallments }}
                  </VChip>
                  <VChip
                    v-else
                    size="small"
                    variant="tonal"
                  >
                    À vista
                  </VChip>
                  <VChip
                    v-if="item.anticipated"
                    size="small"
                    variant="tonal"
                    color="warning"
                    class="ms-1"
                  >
                    Antecipada
                  </VChip>
                </td>
                <td class="text-right">
                  {{ currencyFormatter.format(item.amount) }}
                </td>
              </tr>

              <tr v-if="!isLoading && items.length === 0">
                <td
                  colspan="5"
                  class="text-center text-disabled py-8"
                >
                  Nenhum lançamento encontrado nesta fatura.
                </td>
              </tr>
            </tbody>
          </VTable>
        </div>
      </VCardText>

      <VDivider />

      <VCardText class="d-flex justify-center">
        <VBtn
          color="secondary"
          variant="tonal"
          @click="onClose"
        >
          Fechar
        </VBtn>
      </VCardText>
    </VCard>
  </VDialog>
</template>
