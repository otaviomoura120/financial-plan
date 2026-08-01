<script setup lang="ts">
import { VForm } from 'vuetify/components/VForm'

interface SubCategoryResponse {
  id: number
  version: number
  categoryId: number
  name: string
  active: boolean
  system: boolean
}

interface CategoryResponse {
  id: number
  version: number
  name: string
  active: boolean
  system: boolean
  subCategories: SubCategoryResponse[]
}

interface Props {
  isDialogVisible: boolean
  category?: CategoryResponse | null
}

interface Emit {
  (e: 'update:isDialogVisible', value: boolean): void
  (e: 'saved', category: CategoryResponse): void
}

const props = withDefaults(defineProps<Props>(), {
  category: null,
})

const emit = defineEmits<Emit>()

const spaceStore = useSpaceStore()
const { error, setError, clearError } = useApiError()

const formRef = useTemplateRef<InstanceType<typeof VForm>>('formRef')
const name = shallowRef('')
const isLoading = shallowRef(false)

const isEditMode = computed(() => props.category !== null)

const nameRules = [(v: string) => !!v || 'Nome é obrigatório']

watch(
  () => props.isDialogVisible,
  visible => {
    if (visible) {
      name.value = props.category?.name ?? ''
      clearError()
    }
  },
)

async function onSubmit() {
  const { valid } = await formRef.value!.validate()

  if (!valid) {
    return
  }

  isLoading.value = true
  clearError()

  try {
    let saved: CategoryResponse

    if (isEditMode.value) {
      saved = await $fetch<CategoryResponse>(`/api/categories/${props.category!.id}`, {
        method: 'PUT',
        body: {
          version: props.category!.version,
          name: name.value,
        },
      })
      saved = { ...saved, subCategories: props.category!.subCategories }
    }
    else {
      saved = await $fetch<CategoryResponse>('/api/categories', {
        method: 'POST',
        body: {
          spaceId: spaceStore.activeSpace!.id,
          name: name.value,
        },
      })
    }

    emit('saved', saved)
    emit('update:isDialogVisible', false)
  }
  catch (e) {
    setError(e)
  }
  finally {
    isLoading.value = false
  }
}

function onClose() {
  emit('update:isDialogVisible', false)
}
</script>

<template>
  <VDialog
    :model-value="props.isDialogVisible"
    :fullscreen="$vuetify.display.smAndDown"
    max-width="600"
    scrollable
    @update:model-value="onClose"
  >
    <DialogCloseBtn
      v-if="!$vuetify.display.smAndDown"
      @click="onClose"
    />

    <VCard class="d-flex flex-column">
      <VCardItem class="px-5 px-sm-8 pt-5 pt-sm-8">
        <VCardTitle class="text-h5 text-wrap">
          {{ isEditMode ? 'Editar Categoria' : 'Adicionar Categoria' }}
        </VCardTitle>
        <VCardSubtitle class="text-wrap">
          {{ isEditMode ? 'Atualize os dados da categoria.' : 'Preencha os dados para criar uma nova categoria.' }}
        </VCardSubtitle>

        <template #append>
          <IconBtn
            v-if="$vuetify.display.smAndDown"
            @click="onClose"
          >
            <VIcon icon="tabler-x" />
          </IconBtn>
        </template>
      </VCardItem>

      <VCardText class="px-5 px-sm-8">
        <ApiErrorAlert
          v-if="error"
          :error="error"
          class="mb-4"
        />

        <VForm ref="formRef">
          <div class="d-flex flex-column gap-4">
            <AppTextField
              v-model="name"
              label="Nome"
              placeholder="Digite o nome da categoria"
              :rules="nameRules"
            />
          </div>
        </VForm>
      </VCardText>

      <VDivider />

      <VCardActions class="px-5 px-sm-8 py-4">
        <div class="d-flex flex-column flex-sm-row justify-sm-end gap-3 w-100">
          <VBtn
            :loading="isLoading"
            color="primary"
            variant="elevated"
            :block="$vuetify.display.xs"
            :slim="false"
            @click="onSubmit"
          >
            {{ isEditMode ? 'Salvar' : 'Criar' }}
          </VBtn>

          <VBtn
            color="secondary"
            variant="tonal"
            :disabled="isLoading"
            :block="$vuetify.display.xs"
            :slim="false"
            @click="onClose"
          >
            Cancelar
          </VBtn>
        </div>
      </VCardActions>
    </VCard>
  </VDialog>
</template>
