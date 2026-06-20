<script setup lang="ts">
import avatar1 from '@images/avatars/generic-avatar-1.png'

interface SpaceMemberResponse {
  memberId: number
  userId: number
  roleName: string
}

const spaceStore = useSpaceStore()

const isProfileDialogVisible = ref(false)
const currentUserRole = shallowRef<string | null>(null)

watch(
  () => spaceStore.activeSpace,
  async space => {
    if (!space || !spaceStore.dbUser?.id) {
      currentUserRole.value = null

      return
    }
    try {
      const members = await $fetch<SpaceMemberResponse[]>(`/api/spaces/${space.id}/members`)
      const me = members.find(m => m.userId === spaceStore.dbUser!.id)

      currentUserRole.value = me?.roleName ?? null
    }
    catch {
      currentUserRole.value = null
    }
  },
  { immediate: true },
)

function onProfileSaved(profile: { name: string }) {
  spaceStore.updateDbUser({ name: profile.name })
}
</script>

<template>
  <VBadge
    dot
    location="bottom right"
    offset-x="3"
    offset-y="3"
    bordered
    color="success"
  >
    <VAvatar
      class="cursor-pointer"
      color="primary"
      variant="tonal"
    >
      <VImg :src="avatar1" />

      <!-- SECTION Menu -->
      <VMenu
        activator="parent"
        width="230"
        location="bottom end"
        offset="14px"
      >
        <VList>
          <!-- 👉 User Avatar & Name -->
          <VListItem>
            <template #prepend>
              <VListItemAction start>
                <VBadge
                  dot
                  location="bottom right"
                  offset-x="3"
                  offset-y="3"
                  color="success"
                >
                  <VAvatar
                    color="primary"
                    variant="tonal"
                  >
                    <VImg :src="avatar1" />
                  </VAvatar>
                </VBadge>
              </VListItemAction>
            </template>

            <VListItemTitle class="font-weight-semibold">
              {{ spaceStore.dbUser?.name ?? 'Usuário' }}
            </VListItemTitle>
            <VListItemSubtitle>{{ currentUserRole ?? '' }}</VListItemSubtitle>
          </VListItem>

          <VDivider class="my-2" />

          <!-- 👉 Profile -->
          <VListItem @click="isProfileDialogVisible = true">
            <template #prepend>
              <VIcon
                class="me-2"
                icon="tabler-user"
                size="22"
              />
            </template>

            <VListItemTitle>Perfil</VListItemTitle>
          </VListItem>

          <!-- Divider -->
          <VDivider class="my-2" />

          <!-- 👉 Logout -->
          <VListItem href="/auth/logout">
            <template #prepend>
              <VIcon
                class="me-2"
                icon="tabler-logout"
                size="22"
              />
            </template>

            <VListItemTitle>Sair</VListItemTitle>
          </VListItem>
        </VList>
      </VMenu>
      <!-- !SECTION -->
    </VAvatar>
  </VBadge>

  <EditOwnProfileDialog
    v-model:is-dialog-visible="isProfileDialogVisible"
    @saved="onProfileSaved"
  />
</template>
