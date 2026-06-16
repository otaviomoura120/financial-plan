<script lang="ts" setup>
import type { Notification } from '@layouts/types'

const inviteStore = useInviteStore()

const notifications = computed<Notification[]>(() =>
  (Array.isArray(inviteStore.pendingInvites) ? inviteStore.pendingInvites : []).map(invite => ({
    id: invite.token,
    icon: 'tabler-mail',
    title: 'Novo convite recebido',
    subtitle: `Convite para o espaço "${invite.spaceName}" (${invite.roleName})`,
    time: '',
    isSeen: false,
  })),
)

function goToInvites() {
  navigateTo('/invites')
}
</script>

<template>
  <Notifications
    :notifications="notifications"
    @click:notification="goToInvites"
    @view-all="goToInvites"
  />
</template>
