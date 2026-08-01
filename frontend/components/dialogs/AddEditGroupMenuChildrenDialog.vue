<script setup lang="ts">
import { VForm } from 'vuetify/components/VForm'

interface GroupMenuChildrenResponse {
  id: number
  version: number
  groupMenuId: number
  name: string
  endpoint: string
  icon: string
  createdAt: string
  updatedAt: string
}

interface Props {
  isDialogVisible: boolean
  groupMenuId: number
  child?: GroupMenuChildrenResponse | null
}

interface Emit {
  (e: 'update:isDialogVisible', value: boolean): void
  (e: 'saved', child: GroupMenuChildrenResponse): void
}

const props = withDefaults(defineProps<Props>(), {
  child: null,
})

const emit = defineEmits<Emit>()

const { error, setError, clearError } = useApiError()

const formRef = useTemplateRef<InstanceType<typeof VForm>>('formRef')
const name = shallowRef('')
const endpoint = shallowRef('')
const icon = shallowRef('')
const isLoading = shallowRef(false)

const isEditMode = computed(() => props.child !== null)

const requiredRule = (label: string) => [(v: string) => !!v || `${label} é obrigatório`]

watch(
  () => props.isDialogVisible,
  visible => {
    if (visible) {
      name.value = props.child?.name ?? ''
      endpoint.value = props.child?.endpoint ?? ''
      icon.value = props.child?.icon ?? ''
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
    let saved: GroupMenuChildrenResponse

    if (isEditMode.value) {
      saved = await $fetch<GroupMenuChildrenResponse>(`/api/group-menus/children/${props.child!.id}`, {
        method: 'PUT',
        body: {
          version: props.child!.version,
          name: name.value,
          endpoint: endpoint.value,
          icon: icon.value,
        },
      })
    }
    else {
      saved = await $fetch<GroupMenuChildrenResponse>(`/api/group-menus/${props.groupMenuId}/children`, {
        method: 'POST',
        body: {
          groupMenuId: props.groupMenuId,
          name: name.value,
          endpoint: endpoint.value,
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
          {{ isEditMode ? 'Editar Item do Menu' : 'Adicionar Item ao Menu' }}
        </VCardTitle>
        <VCardSubtitle class="text-wrap">
          {{ isEditMode ? 'Atualize os dados do item.' : 'Preencha os dados para adicionar um novo item.' }}
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
              placeholder="Ex: Relatório Mensal"
              :rules="requiredRule('Nome')"
            />

            <AppTextField
              v-model="endpoint"
              label="Endpoint"
              placeholder="Ex: /relatorios/mensal"
              :rules="requiredRule('Endpoint')"
            />

            <AppTextField
              v-model="icon"
              label="Ícone"
              placeholder="Ex: tabler-file-report"
              :rules="requiredRule('Ícone')"
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
            {{ isEditMode ? 'Salvar' : 'Adicionar' }}
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
