<script setup lang="ts">
definePageMeta({ middleware: 'auth' })

const user = useUser()
</script>

<template>
  <div>
    <VCard
      class="mb-6"
      title="Kick start your project 🚀"
    >
      <VCardText>All the best for your new project.</VCardText>
      <VCardText v-if="user">
        Signed in as <strong>{{ user.name || user.email }}</strong>
      </VCardText>
    </VCard>

    <VCard title="Auth0 Authentication 🔒">
      <VCardText>Your app is secured with Auth0 server-side session authentication.</VCardText>
      <VCardText>
        <VBtn
          variant="tonal"
          color="error"
          href="/auth/logout"
          prepend-icon="tabler-logout"
        >
          Sign out
        </VBtn>
      </VCardText>
    </VCard>
  </div>
</template>
