<script setup lang="ts">
import { useAuthenticatedUser } from '../composables/useAuthenticatedUser'

const { user, accessToken, isLoading } = useAuthenticatedUser()
</script>

<template>
  <div v-if="!isLoading">
    <h1>Welcome, {{ user?.name }}</h1>
    <p>Access Token: {{ accessToken ? 'Available' : 'Not available' }}</p>
  </div>
  <div v-else>Loading...</div>
</template>