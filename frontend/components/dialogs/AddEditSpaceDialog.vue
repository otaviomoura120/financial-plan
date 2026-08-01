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
  visible => {
    if (visible) {
      name.value = props.space?.name ?? ''
      description.value = props.space?.description ?? ''
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
          {{ isEditMode ? 'Editar Espaço' : 'Novo Espaço' }}
        </VCardTitle>
        <VCardSubtitle class="text-wrap">
          {{ isEditMode ? 'Atualize os dados do espaço.' : 'Preencha os dados para criar um novo espaço.' }}
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
              placeholder="Digite o nome do espaço"
              :rules="nameRules"
            />

            <AppTextField
              v-model="description"
              label="Descrição"
              placeholder="Descrição opcional"
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
