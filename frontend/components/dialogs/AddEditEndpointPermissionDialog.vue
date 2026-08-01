<script setup lang="ts">
import { VForm } from 'vuetify/components/VForm'

interface EndpointPermissionResponse {
  id: number
  version: number
  endpoint: string
  name: string
  icon?: string
  sequence?: number
  type: 'API' | 'FRONT_PAGE' | 'WIDGET'
  permittedMethods?: string
  group: string
  createdAt: string
  updatedAt: string
}

interface Props {
  isDialogVisible: boolean
  item?: EndpointPermissionResponse | null
}

interface Emit {
  (e: 'update:isDialogVisible', value: boolean): void
  (e: 'saved', item: EndpointPermissionResponse): void
}

const props = withDefaults(defineProps<Props>(), {
  item: null,
})

const emit = defineEmits<Emit>()

const { error, setError, clearError } = useApiError()

const formRef = useTemplateRef<InstanceType<typeof VForm>>('formRef')
const name = shallowRef('')
const endpoint = shallowRef('')
const group = shallowRef('')
const type = shallowRef<'API' | 'FRONT_PAGE' | 'WIDGET'>('API')
const icon = shallowRef('')
const sequence = shallowRef<number | null>(null)
const permittedMethods = shallowRef('')
const isLoading = shallowRef(false)

const isEditMode = computed(() => props.item !== null)

const typeOptions = [
  { title: 'API', value: 'API' },
  { title: 'Front Page', value: 'FRONT_PAGE' },
  { title: 'Widget', value: 'WIDGET' },
]

const requiredRule = (label: string) => (v: string) => !!v || `${label} é obrigatório`

watch(
  () => props.isDialogVisible,
  visible => {
    if (visible) {
      name.value = props.item?.name ?? ''
      endpoint.value = props.item?.endpoint ?? ''
      group.value = props.item?.group ?? ''
      type.value = props.item?.type ?? 'API'
      icon.value = props.item?.icon ?? ''
      sequence.value = props.item?.sequence ?? null
      permittedMethods.value = props.item?.permittedMethods ?? ''
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
    let saved: EndpointPermissionResponse

    const body = {
      name: name.value,
      endpoint: endpoint.value,
      group: group.value,
      type: type.value,
      icon: icon.value || undefined,
      sequence: sequence.value ?? undefined,
      permittedMethods: permittedMethods.value || undefined,
    }

    if (isEditMode.value) {
      saved = await $fetch<EndpointPermissionResponse>(`/api/endpoint-permissions/${props.item!.id}`, {
        method: 'PUT',
        body: { version: props.item!.version, ...body },
      })
    }
    else {
      saved = await $fetch<EndpointPermissionResponse>('/api/endpoint-permissions', {
        method: 'POST',
        body,
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
    max-width="640"
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
          {{ isEditMode ? 'Editar Permissão' : 'Adicionar Permissão' }}
        </VCardTitle>
        <VCardSubtitle class="text-wrap">
          {{ isEditMode ? 'Atualize os dados da permissão de endpoint.' : 'Preencha os dados para criar uma nova permissão.' }}
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
              placeholder="Ex: Listar Funções"
              :rules="[requiredRule('Nome')]"
            />

            <AppTextField
              v-model="endpoint"
              label="Endpoint"
              placeholder="Ex: /roles ou /roles/[0-9]+"
              :rules="[requiredRule('Endpoint')]"
            />

            <AppTextField
              v-model="group"
              label="Grupo"
              placeholder="Ex: Role, Financeiro, internal_management"
              :rules="[requiredRule('Grupo')]"
            />

            <AppSelect
              v-model="type"
              label="Tipo"
              :items="typeOptions"
              :rules="[requiredRule('Tipo')]"
            />

            <AppTextField
              v-model="permittedMethods"
              label="Métodos Permitidos"
              placeholder="Ex: GET ou GET,POST,PUT"
            />

            <AppTextField
              v-model="icon"
              label="Ícone"
              placeholder="Ex: tabler-home"
            />

            <AppTextField
              v-model.number="sequence"
              label="Sequência"
              placeholder="Ex: 10"
              type="number"
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
