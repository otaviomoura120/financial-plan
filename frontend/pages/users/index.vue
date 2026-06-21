<script setup lang="ts">
definePageMeta({ middleware: 'auth' })

interface SpaceMemberResponse {
  memberId: number
  version: number
  userId: number
  userName: string
  userEmail: string
  roleId: number
  roleName: string
  joinedAt: string
}

interface SpaceInviteResponse {
  inviteId: number
  email: string
  roleId: number
  roleName: string
  status: 'PENDING' | 'ACCEPTED' | 'CANCELLED' | 'DECLINED'
  createdAt: string
  expiresAt: string
}

interface OwnProfileResponse {
  id: number
  name: string
  email: string
  nickname: string | null
  phoneNumber: string | null
  birthdate: string | null
  genre: string | null
  maritalStatus: string | null
}

const spaceStore = useSpaceStore()
const { error, setError, clearError } = useApiError()
const { error: inviteError, setError: setInviteError, clearError: clearInviteError } = useApiError()
const { isVisible: snackbarVisible, message: snackbarMessage, color: snackbarColor, icon: snackbarIcon, showSuccess, showError } = useSnackbar()

const members = ref<SpaceMemberResponse[]>([])
const search = shallowRef('')
const page = shallowRef(1)
const itemsPerPage = shallowRef(10)
const isLoading = shallowRef(false)
const isRemoving = shallowRef(false)

const invites = ref<SpaceInviteResponse[]>([])
const isLoadingInvites = shallowRef(false)
const isCancellingInvite = shallowRef<number | null>(null)

const isAddDialogVisible = shallowRef(false)
const isEditDialogVisible = shallowRef(false)
const isRemoveDialogVisible = shallowRef(false)
const isEditProfileDialogVisible = shallowRef(false)
const selectedMember = shallowRef<SpaceMemberResponse | null>(null)

const filteredMembers = computed(() =>
  members.value.filter(m =>
    m.userName.toLowerCase().includes(search.value.toLowerCase())
    || m.userEmail.toLowerCase().includes(search.value.toLowerCase()),
  ),
)

const paginatedMembers = computed(() => {
  const start = (page.value - 1) * itemsPerPage.value

  return filteredMembers.value.slice(start, start + itemsPerPage.value)
})

const pendingInvites = computed(() => invites.value.filter(i => i.status === 'PENDING'))

watch(
  () => spaceStore.activeSpace,
  async space => {
    if (space) {
      await fetchMembers()
      await fetchInvites()
    }
    else {
      members.value = []
      invites.value = []
    }
  },
  { immediate: true },
)

watch(search, () => {
  page.value = 1
})

async function fetchMembers() {
  if (!spaceStore.activeSpace)
    return

  isLoading.value = true
  clearError()

  try {
    members.value = await $fetch<SpaceMemberResponse[]>(
      `/api/spaces/${spaceStore.activeSpace.id}/members`,
    )
  }
  catch (e) {
    setError(e)
  }
  finally {
    isLoading.value = false
  }
}

function openEdit(member: SpaceMemberResponse) {
  selectedMember.value = member
  isEditDialogVisible.value = true
}

function openRemove(member: SpaceMemberResponse) {
  selectedMember.value = member
  isRemoveDialogVisible.value = true
}

function isCurrentUser(member: SpaceMemberResponse) {
  return spaceStore.dbUser?.id === member.userId
}

async function onRemoveConfirm(confirmed: boolean) {
  if (!confirmed || !selectedMember.value || !spaceStore.activeSpace)
    return

  isRemoving.value = true
  clearError()

  try {
    await $fetch(
      `/api/spaces/${spaceStore.activeSpace.id}/members/${selectedMember.value.userId}`,
      { method: 'DELETE' },
    )
    members.value = members.value.filter(m => m.memberId !== selectedMember.value!.memberId)
    showSuccess('Membro removido com sucesso.')
  }
  catch (e) {
    showError(e)
  }
  finally {
    isRemoving.value = false
    selectedMember.value = null
  }
}

function onMemberSaved(updated: SpaceMemberResponse) {
  const idx = members.value.findIndex(m => m.memberId === updated.memberId)

  if (idx >= 0)
    members.value[idx] = updated

  if (selectedMember.value?.memberId === updated.memberId)
    selectedMember.value = updated
}

function onProfileSaved(profile: OwnProfileResponse) {
  spaceStore.updateDbUser({
    id: profile.id,
    name: profile.name,
    nickname: profile.nickname,
    phoneNumber: profile.phoneNumber,
    birthdate: profile.birthdate,
    genre: profile.genre,
    maritalStatus: profile.maritalStatus,
  })

  const idx = members.value.findIndex(m => m.userId === profile.id)

  if (idx >= 0)
    members.value[idx] = { ...members.value[idx], userName: profile.name }
}

async function fetchInvites() {
  if (!spaceStore.activeSpace)
    return

  isLoadingInvites.value = true
  clearInviteError()

  try {
    invites.value = await $fetch<SpaceInviteResponse[]>(
      `/api/spaces/${spaceStore.activeSpace.id}/invites`,
    )
  }
  catch (e) {
    setInviteError(e)
  }
  finally {
    isLoadingInvites.value = false
  }
}

async function cancelInvite(invite: SpaceInviteResponse) {
  if (!spaceStore.activeSpace)
    return

  isCancellingInvite.value = invite.inviteId
  clearInviteError()

  try {
    await $fetch(
      `/api/spaces/${spaceStore.activeSpace.id}/invites/${invite.inviteId}`,
      { method: 'DELETE' },
    )
    invites.value = invites.value.filter(i => i.inviteId !== invite.inviteId)
  }
  catch (e) {
    setInviteError(e)
  }
  finally {
    isCancellingInvite.value = null
  }
}

async function onInviteSent() {
  await fetchInvites()
}

function formatDate(iso: string) {
  return new Date(iso).toLocaleDateString('pt-BR')
}
</script>

<template>
  <div>
    <VCard>
      <VCardText class="d-flex align-center flex-wrap gap-4">
        <h5 class="text-h5">
          Usuários
        </h5>

        <VSpacer />

        <VTextField
          v-model="search"
          placeholder="Buscar por nome ou e-mail..."
          density="compact"
          prepend-inner-icon="tabler-search"
          class="flex-grow-1"
          style="max-inline-size: 280px"
          hide-details
        />

        <VBtn
          prepend-icon="tabler-user-plus"
          @click="isAddDialogVisible = true"
        >
          Adicionar Usuário
        </VBtn>
      </VCardText>

      <VDivider />

      <ApiErrorAlert
        v-if="error"
        :error="error"
        class="ma-4"
      />

      <VSnackbar
        v-model="snackbarVisible"
        :color="snackbarColor"
        :timeout="3000"
      >
        <div class="d-flex align-center gap-2">
          <VIcon :icon="snackbarIcon" />
          {{ snackbarMessage }}
        </div>
      </VSnackbar>

      <div
        v-if="isLoading"
        class="d-flex justify-center py-10"
      >
        <VProgressCircular indeterminate />
      </div>

      <div v-else style="overflow-x: auto">
        <VTable>
        <thead style="white-space: nowrap">
          <tr>
            <th style="min-width: 200px">Nome</th>
            <th>E-mail</th>
            <th>Role</th>
            <th>Membro desde</th>
            <th class="text-center">
              Ações
            </th>
          </tr>
        </thead>
        <tbody>
          <tr
            v-for="member in paginatedMembers"
            :key="member.memberId"
          >
            <td class="font-weight-medium">
              {{ member.userName }}
            </td>
            <td class="text-disabled">
              {{ member.userEmail }}
            </td>
            <td>
              <VChip
                :color="member.roleName === 'OWNER' ? 'warning' : 'primary'"
                size="small"
                variant="tonal"
              >
                {{ member.roleName }}
              </VChip>
            </td>
            <td>{{ formatDate(member.joinedAt) }}</td>
            <td class="text-center" style="white-space: nowrap">
              <VBtn
                icon
                variant="text"
                size="small"
                color="default"
                :disabled="member.roleName === 'OWNER'"
                @click="openEdit(member)"
              >
                <VIcon icon="tabler-pencil" />
                <VTooltip activator="parent">
                  {{ member.roleName === 'OWNER' ? 'Role do OWNER não pode ser alterada' : 'Alterar role' }}
                </VTooltip>
              </VBtn>

              <VBtn
                icon
                variant="text"
                size="small"
                color="error"
                :disabled="member.roleName === 'OWNER'"
                @click="openRemove(member)"
              >
                <VIcon icon="tabler-user-minus" />
                <VTooltip activator="parent">
                  {{ member.roleName === 'OWNER' ? 'OWNER não pode ser removido' : 'Remover do espaço' }}
                </VTooltip>
              </VBtn>

              <VBtn
                v-if="isCurrentUser(member)"
                icon
                variant="text"
                size="small"
                color="default"
                @click="isEditProfileDialogVisible = true"
              >
                <VIcon icon="tabler-user-edit" />
                <VTooltip activator="parent">
                  Editar meus dados
                </VTooltip>
              </VBtn>
            </td>
          </tr>

          <tr v-if="!isLoading && filteredMembers.length === 0">
            <td
              colspan="5"
              class="text-center text-disabled py-8"
            >
              {{ search ? 'Nenhum usuário encontrado para a busca.' : 'Nenhum membro neste espaço.' }}
            </td>
          </tr>
        </tbody>
        </VTable>
      </div>

      <TablePagination
        v-if="filteredMembers.length > 0"
        :page="page"
        :items-per-page="itemsPerPage"
        :total-items="filteredMembers.length"
        @update:page="page = $event"
      />
    </VCard>

    <VCard class="mt-6">
      <VCardText class="d-flex align-center gap-4">
        <h5 class="text-h5">
          Convites Pendentes
        </h5>

        <VChip
          v-if="pendingInvites.length > 0"
          size="small"
          color="warning"
          variant="tonal"
        >
          {{ pendingInvites.length }}
        </VChip>
      </VCardText>

      <VDivider />

      <ApiErrorAlert
        v-if="inviteError"
        :error="inviteError"
        class="ma-4"
      />

      <div
        v-if="isLoadingInvites"
        class="d-flex justify-center py-10"
      >
        <VProgressCircular indeterminate />
      </div>

      <div v-else style="overflow-x: auto">
        <VTable>
        <thead style="white-space: nowrap">
          <tr>
            <th>E-mail</th>
            <th>Role</th>
            <th>Enviado em</th>
            <th>Expira em</th>
            <th>Status</th>
            <th class="text-center">
              Ações
            </th>
          </tr>
        </thead>
        <tbody>
          <tr
            v-for="invite in pendingInvites"
            :key="invite.inviteId"
          >
            <td class="text-disabled">
              {{ invite.email }}
            </td>
            <td>
              <VChip
                color="primary"
                size="small"
                variant="tonal"
              >
                {{ invite.roleName }}
              </VChip>
            </td>
            <td>{{ formatDate(invite.createdAt) }}</td>
            <td>{{ formatDate(invite.expiresAt) }}</td>
            <td>
              <VChip
                :color="{ PENDING: 'warning', ACCEPTED: 'success', CANCELLED: 'secondary', DECLINED: 'error' }[invite.status]"
                size="small"
                variant="tonal"
              >
                {{ { PENDING: 'Pendente', ACCEPTED: 'Aceito', CANCELLED: 'Cancelado', DECLINED: 'Rejeitado' }[invite.status] }}
              </VChip>
            </td>
            <td class="text-center" style="white-space: nowrap">
              <VBtn
                icon
                variant="text"
                size="small"
                color="error"
                :loading="isCancellingInvite === invite.inviteId"
                @click="cancelInvite(invite)"
              >
                <VIcon icon="tabler-x" />
                <VTooltip activator="parent">
                  Cancelar convite
                </VTooltip>
              </VBtn>
            </td>
          </tr>

          <tr v-if="!isLoadingInvites && pendingInvites.length === 0">
            <td
              colspan="6"
              class="text-center text-disabled py-8"
            >
              Nenhum convite pendente.
            </td>
          </tr>
        </tbody>
        </VTable>
      </div>
    </VCard>

    <AddMemberDialog
      v-model:is-dialog-visible="isAddDialogVisible"
      @invite-sent="onInviteSent"
    />

    <EditMemberRoleDialog
      v-model:is-dialog-visible="isEditDialogVisible"
      :member="selectedMember"
      @saved="onMemberSaved"
    />

    <EditOwnProfileDialog
      v-model:is-dialog-visible="isEditProfileDialogVisible"
      @saved="onProfileSaved"
    />

    <ConfirmDialog
      v-model:is-dialog-visible="isRemoveDialogVisible"
      :auto-result="false"
      confirmation-question="Tem certeza que deseja remover este membro do espaço? Ele perderá o acesso imediatamente."
      cancel-title="Ação cancelada"
      cancel-msg="O membro não foi removido."
      @confirm="onRemoveConfirm"
    />
  </div>
</template>
