<script setup lang="ts">
import { VForm } from 'vuetify/components/VForm'

interface SubCategoryResponse {
  id: number
  version: number
  categoryId: number
  name: string
  active: boolean
}

interface CategoryResponse {
  id: number
  version: number
  name: string
  active: boolean
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

  if (!valid)
    return

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
    :width="$vuetify.display.smAndDown ? 'auto' : 600"
    :model-value="props.isDialogVisible"
    @update:model-value="onClose"
  >
    <DialogCloseBtn @click="onClose" />

    <VCard class="pa-sm-10 pa-4">
      <VCardText>
        <h4 class="text-h4 text-center mb-2">
          {{ isEditMode ? 'Editar Categoria' : 'Adicionar Categoria' }}
        </h4>
        <p class="text-body-1 text-center mb-6">
          {{ isEditMode ? 'Atualize os dados da categoria.' : 'Preencha os dados para criar uma nova categoria.' }}
        </p>

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

          <div class="d-flex align-center justify-center gap-4 mt-6">
            <VBtn
              :loading="isLoading"
              @click="onSubmit"
            >
              {{ isEditMode ? 'Salvar' : 'Criar' }}
            </VBtn>

            <VBtn
              color="secondary"
              variant="tonal"
              :disabled="isLoading"
              @click="onClose"
            >
              Cancelar
            </VBtn>
          </div>
        </VForm>
      </VCardText>
    </VCard>
  </VDialog>
</template>
