<!--<script setup lang="ts">-->
<!--import { useAuth0 } from '@auth0/auth0-vue'-->

<!--const {-->
<!--  isLoading,-->
<!--  isAuthenticated,-->
<!--  error,-->
<!--  loginWithRedirect,-->
<!--  logout: auth0Logout,-->
<!--  user-->
<!--} = useAuth0()-->

<!--const signup = () =>-->
<!--    loginWithRedirect({ authorizationParams: { screen_hint: 'signup' } })-->

<!--const login = () => loginWithRedirect()-->

<!--const logout = () =>-->
<!--    auth0Logout({ logoutParams: { returnTo: window.location.origin } })-->
<!--</script>-->

<!--<template>-->
<!--  <div v-if="isLoading">Loading...</div>-->

<!--  <div v-else-if="isAuthenticated && user">-->
<!--    <p>Logged in as {{ user.email }}</p>-->

<!--    <h1>User Profile</h1>-->

<!--    <pre>{{ JSON.stringify(user, null, 2) }}</pre>-->

<!--    <button @click="logout">Logout</button>-->
<!--  </div>-->

<!--  <div v-else>-->
<!--    <p v-if="error">Error: {{ error.message }}</p>-->

<!--    <button @click="signup">Signup</button>-->

<!--    <button @click="login">Login</button>-->
<!--  </div>-->
<!--</template>-->

<!--&lt;!&ndash;<template>&ndash;&gt;-->
<!--&lt;!&ndash;  <div>&ndash;&gt;-->
<!--&lt;!&ndash;    <VCard&ndash;&gt;-->
<!--&lt;!&ndash;      class="mb-6"&ndash;&gt;-->
<!--&lt;!&ndash;      title="Kick start your project 🚀"&ndash;&gt;-->
<!--&lt;!&ndash;    >&ndash;&gt;-->
<!--&lt;!&ndash;      <VCardText>All the best for your new project.</VCardText>&ndash;&gt;-->
<!--&lt;!&ndash;      <VCardText>&ndash;&gt;-->
<!--&lt;!&ndash;        Please make sure to read our <a&ndash;&gt;-->
<!--&lt;!&ndash;          href="https://demos.pixinvent.com/vuexy-vuejs-admin-template/documentation/"&ndash;&gt;-->
<!--&lt;!&ndash;          target="_blank"&ndash;&gt;-->
<!--&lt;!&ndash;          rel="noopener noreferrer"&ndash;&gt;-->
<!--&lt;!&ndash;          class="text-decoration-none"&ndash;&gt;-->
<!--&lt;!&ndash;        >&ndash;&gt;-->
<!--&lt;!&ndash;          Template Documentation&ndash;&gt;-->
<!--&lt;!&ndash;        </a> to understand where to go from here and how to use our template.&ndash;&gt;-->
<!--&lt;!&ndash;      </VCardText>&ndash;&gt;-->
<!--&lt;!&ndash;    </VCard>&ndash;&gt;-->

<!--&lt;!&ndash;    <VCard title="Want to integrate JWT? 🔒">&ndash;&gt;-->
<!--&lt;!&ndash;      <VCardText>We carefully crafted JWT flow so you can implement JWT with ease and with minimum efforts.</VCardText>&ndash;&gt;-->
<!--&lt;!&ndash;      <VCardText>Please read our  JWT Documentation to get more out of JWT authentication.</VCardText>&ndash;&gt;-->
<!--&lt;!&ndash;    </VCard>&ndash;&gt;-->
<!--&lt;!&ndash;  </div>&ndash;&gt;-->
<!--&lt;!&ndash;</template>&ndash;&gt;-->
