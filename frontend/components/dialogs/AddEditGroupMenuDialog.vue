<script setup lang="ts">
import { VForm } from 'vuetify/components/VForm'

interface GroupMenuResponse {
  id: number
  version: number
  name: string
  icon: string
  createdAt: string
  updatedAt: string
}

interface Props {
  isDialogVisible: boolean
  groupMenu?: GroupMenuResponse | null
}

interface Emit {
  (e: 'update:isDialogVisible', value: boolean): void
  (e: 'saved', groupMenu: GroupMenuResponse): void
}

const props = withDefaults(defineProps<Props>(), {
  groupMenu: null,
})

const emit = defineEmits<Emit>()

const { error, setError, clearError } = useApiError()

const formRef = useTemplateRef<InstanceType<typeof VForm>>('formRef')
const name = shallowRef('')
const icon = shallowRef('')
const isLoading = shallowRef(false)

const isEditMode = computed(() => props.groupMenu !== null)

const requiredRule = (label: string) => [(v: string) => !!v || `${label} é obrigatório`]

watch(
  () => props.isDialogVisible,
  visible => {
    if (visible) {
      name.value = props.groupMenu?.name ?? ''
      icon.value = props.groupMenu?.icon ?? ''
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
    let saved: GroupMenuResponse

    if (isEditMode.value) {
      saved = await $fetch<GroupMenuResponse>(`/api/group-menus/${props.groupMenu!.id}`, {
        method: 'PUT',
        body: {
          version: props.groupMenu!.version,
          name: name.value,
          icon: icon.value,
        },
      })
    }
    else {
      saved = await $fetch<GroupMenuResponse>('/api/group-menus', {
        method: 'POST',
        body: {
          name: name.value,
          icon: icon.value,
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
          {{ isEditMode ? 'Editar Group Menu' : 'Adicionar Group Menu' }}
        </h4>
        <p class="text-body-1 text-center mb-6">
          {{ isEditMode ? 'Atualize os dados do group menu.' : 'Preencha os dados para criar um novo group menu.' }}
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
              placeholder="Ex: Financeiro"
              :rules="requiredRule('Nome')"
            />

            <AppTextField
              v-model="icon"
              label="Ícone"
              placeholder="Ex: tabler-chart-bar"
              :rules="requiredRule('Ícone')"
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
