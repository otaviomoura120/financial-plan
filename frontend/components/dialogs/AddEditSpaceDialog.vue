<script setup lang="ts">
import { VForm } from 'vuetify/components/VForm'

interface SpaceResponse {
  id: number
  version: number
  name: string
  description?: string
  createdDate: string
  currentUserRoleName: string | null
}

interface Props {
  isDialogVisible: boolean
  space?: SpaceResponse | null
}

interface Emit {
  (e: 'update:isDialogVisible', value: boolean): void
  (e: 'saved', space: SpaceResponse): void
}

const props = withDefaults(defineProps<Props>(), {
  space: null,
})

const emit = defineEmits<Emit>()

const spaceStore = useSpaceStore()
const { error, setError, clearError } = useApiError()

const formRef = useTemplateRef<InstanceType<typeof VForm>>('formRef')
const name = shallowRef('')
const description = shallowRef('')
const isLoading = shallowRef(false)

const isEditMode = computed(() => props.space !== null)

const nameRules = [(v: string) => !!v || 'Nome é obrigatório']

watch(
  () => props.isDialogVisible,
  (visible) => {
    if (visible) {
      name.value = props.space?.name ?? ''
      description.value = props.space?.description ?? ''
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
    let saved: SpaceResponse

    if (isEditMode.value) {
      saved = await $fetch<SpaceResponse>(`/api/spaces/${props.space!.id}`, {
        method: 'PUT',
        body: { version: props.space!.version, name: name.value },
      })
      saved = { ...saved, description: description.value || undefined, currentUserRoleName: props.space!.currentUserRoleName }
    }
    else {
      saved = await $fetch<SpaceResponse>('/api/spaces', {
        method: 'POST',
        body: {
          name: name.value,
          description: description.value || undefined,
          creatorId: spaceStore.dbUser!.id,
        },
      })
      saved = { ...saved, currentUserRoleName: 'OWNER' }
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
          {{ isEditMode ? 'Editar Espaço' : 'Novo Espaço' }}
        </h4>
        <p class="text-body-1 text-center mb-6">
          {{ isEditMode ? 'Atualize os dados do espaço.' : 'Preencha os dados para criar um novo espaço.' }}
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
              placeholder="Digite o nome do espaço"
              :rules="nameRules"
            />

            <AppTextField
              v-model="description"
              label="Descrição"
              placeholder="Descrição opcional"
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
