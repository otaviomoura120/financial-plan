<script setup lang="ts">
import { VForm } from 'vuetify/components/VForm'

interface RoleResponse {
  id: number
  version: number
  spaceId: number
  name: string
  description?: string
  createdAt: string
  updatedAt: string
}

interface Props {
  isDialogVisible: boolean
  role?: RoleResponse | null
}

interface Emit {
  (e: 'update:isDialogVisible', value: boolean): void
  (e: 'saved', role: RoleResponse): void
}

const props = withDefaults(defineProps<Props>(), {
  role: null,
})

const emit = defineEmits<Emit>()

const spaceStore = useSpaceStore()
const { error, setError, clearError } = useApiError()

const formRef = useTemplateRef<InstanceType<typeof VForm>>('formRef')
const name = shallowRef('')
const description = shallowRef('')
const isLoading = shallowRef(false)

const isEditMode = computed(() => props.role !== null)

const nameRules = [(v: string) => !!v || 'Nome é obrigatório']

watch(
  () => props.isDialogVisible,
  visible => {
    if (visible) {
      name.value = props.role?.name ?? ''
      description.value = props.role?.description ?? ''
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
    let saved: RoleResponse

    if (isEditMode.value) {
      saved = await $fetch<RoleResponse>(`/api/roles/${props.role!.id}`, {
        method: 'PUT',
        body: {
          version: props.role!.version,
          name: name.value,
          description: description.value || undefined,
        },
      })
    }
    else {
      saved = await $fetch<RoleResponse>('/api/roles', {
        method: 'POST',
        body: {
          spaceId: spaceStore.activeSpace!.id,
          name: name.value,
          description: description.value || undefined,
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
          {{ isEditMode ? 'Editar Role' : 'Adicionar Role' }}
        </h4>
        <p class="text-body-1 text-center mb-6">
          {{ isEditMode ? 'Atualize os dados da role.' : 'Preencha os dados para criar uma nova role.' }}
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
              placeholder="Digite o nome da role"
              :rules="nameRules"
              :disabled="isEditMode && props.role?.name === 'OWNER'"
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
