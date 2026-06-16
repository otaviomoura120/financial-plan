<script setup lang="ts">
definePageMeta({ middleware: 'auth' })

const spaceStore = useSpaceStore()
const inviteStore = useInviteStore()

const isLoading = shallowRef(true)
const actioningToken = shallowRef<string | null>(null)
const errorMessage = shallowRef<string | null>(null)

function extractErrorCode(e: unknown): string {
  if (typeof e === 'string')
    return e

  const err = e as { data?: unknown; statusMessage?: string; message?: string }

  if (typeof err?.data === 'string')
    return err.data

  if (err?.data && typeof err.data === 'object' && 'message' in (err.data as object))
    return String((err.data as { message?: string }).message)

  return err?.statusMessage ?? err?.message ?? 'unknown'
}

const errorMessageMap: Record<string, string> = {
  invite_cancelled: 'Este convite foi cancelado.',
  invite_already_accepted: 'Este convite já foi aceito anteriormente.',
  invite_already_declined: 'Este convite já foi recusado anteriormente.',
  invite_expired: 'Este convite expirou. Solicite um novo convite ao administrador do espaço.',
  invite_email_mismatch: 'Este convite foi enviado para outro endereço de e-mail.',
  complete_onboarding: 'Finalize seu cadastro antes de responder a este convite.',
}

function formatDate(iso: string) {
  return new Date(iso).toLocaleDateString('pt-BR', { day: '2-digit', month: 'long', year: 'numeric' })
}

onMounted(async () => {
  await inviteStore.fetchPendingInvites()
  isLoading.value = false
})

async function acceptInvite(token: string) {
  actioningToken.value = token
  errorMessage.value = null

  try {
    const response = await $fetch<{ spaceId: number; spaceName: string }>(
      `/api/invites/${token}/accept`,
      { method: 'POST' },
    )

    inviteStore.removeInvite(token)

    if (!spaceStore.activeSpace)
      spaceStore.setActiveSpace({ id: response.spaceId, name: response.spaceName })
  }
  catch (e) {
    errorMessage.value = errorMessageMap[extractErrorCode(e)] ?? 'Não foi possível aceitar o convite. Tente novamente mais tarde.'
  }
  finally {
    actioningToken.value = null
  }
}

async function declineInvite(token: string) {
  actioningToken.value = token
  errorMessage.value = null

  try {
    await $fetch(`/api/invites/${token}/decline`, { method: 'POST' })
    inviteStore.removeInvite(token)
  }
  catch (e) {
    errorMessage.value = errorMessageMap[extractErrorCode(e)] ?? 'Não foi possível recusar o convite. Tente novamente mais tarde.'
  }
  finally {
    actioningToken.value = null
  }
}
</script>

<template>
  <VCard>
    <VCardText>
      <h5 class="text-h5">
        Meus Convites
      </h5>
    </VCardText>

    <VDivider />

    <ApiErrorAlert
      v-if="errorMessage"
      :error="errorMessage"
      class="ma-4"
    />

    <div
      v-if="isLoading"
      class="d-flex justify-center py-10"
    >
      <VProgressCircular indeterminate />
    </div>

    <VList v-else-if="inviteStore.pendingInvites.length > 0">
      <template
        v-for="(invite, index) in inviteStore.pendingInvites"
        :key="invite.token"
      >
        <VDivider v-if="index > 0" />

        <VListItem>
          <VListItemTitle>
            {{ invite.spaceName }} — {{ invite.roleName }}
          </VListItemTitle>
          <VListItemSubtitle>
            Válido até {{ formatDate(invite.expiresAt) }}
          </VListItemSubtitle>

          <template #append>
            <VBtn
              color="primary"
              size="small"
              class="me-2"
              :loading="actioningToken === invite.token"
              :disabled="actioningToken !== null && actioningToken !== invite.token"
              @click="acceptInvite(invite.token)"
            >
              Aceitar
            </VBtn>
            <VBtn
              color="secondary"
              variant="tonal"
              size="small"
              :disabled="actioningToken !== null"
              @click="declineInvite(invite.token)"
            >
              Recusar
            </VBtn>
          </template>
        </VListItem>
      </template>
    </VList>

    <VCardText
      v-else
      class="text-center text-disabled py-8"
    >
      Você não tem convites pendentes.
    </VCardText>
  </VCard>
</template>
