<script lang="ts" setup>
const { injectSkinClasses } = useSkins()

injectSkinClasses()

const isFallbackStateActive = ref(false)
const refLoadingIndicator = ref<any>(null)

watch([isFallbackStateActive, refLoadingIndicator], () => {
  if (isFallbackStateActive.value && refLoadingIndicator.value)
    refLoadingIndicator.value.fallbackHandle()

  if (!isFallbackStateActive.value && refLoadingIndicator.value)
    refLoadingIndicator.value.resolveHandle()
}, { immediate: true })
</script>

<template>
  <AppLoadingIndicator ref="refLoadingIndicator" />

  <VApp class="onboarding-layout">
    <VMain>
      <VContainer
        class="d-flex align-center justify-center"
        style="min-block-size: 100dvh;"
      >
        <RouterView #="{ Component }">
          <Suspense
            :timeout="0"
            @fallback="isFallbackStateActive = true"
            @resolve="isFallbackStateActive = false"
          >
            <Component :is="Component" />
          </Suspense>
        </RouterView>
      </VContainer>
    </VMain>
  </VApp>
</template>
