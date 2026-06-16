<script lang="ts" setup>
import { useConfigStore } from '@core/stores/config'
import { AppContentLayoutNav } from '@layouts/enums'
import { switchToVerticalNavOnLtOverlayNavBreakpoint } from '@layouts/utils'

const DefaultLayoutWithHorizontalNav = defineAsyncComponent(() => import('./components/DefaultLayoutWithHorizontalNav.vue'))
const DefaultLayoutWithVerticalNav = defineAsyncComponent(() => import('./components/DefaultLayoutWithVerticalNav.vue'))

const configStore = useConfigStore()

// ℹ️ This will switch to vertical nav when define breakpoint is reached when in horizontal nav layout
// Remove below composable usage if you are not using horizontal nav layout in your app
switchToVerticalNavOnLtOverlayNavBreakpoint()

const { layoutAttrs, injectSkinClasses } = useSkins()

injectSkinClasses()

const user = useUser()
const spaceStore = useSpaceStore()
const menuStore = useMenuStore()
const inviteStore = useInviteStore()
const { checkAndRedirect, error: onboardingError, clearError: clearOnboardingError } = useOnboarding()

watch(
  user,
  async (u) => {
    if (u) {
      if (!spaceStore.activeSpace)
        await checkAndRedirect()

      await inviteStore.fetchPendingInvites()
    }
  },
  { immediate: true },
)

watch(
  () => spaceStore.activeSpace,
  (space) => {
    if (space)
      menuStore.fetchMenuStructure()
  },
  { immediate: true },
)
</script>

<template>
  <Component
    v-bind="layoutAttrs"
    :is="configStore.appContentLayoutNav === AppContentLayoutNav.Vertical ? DefaultLayoutWithVerticalNav : DefaultLayoutWithHorizontalNav"
  >
    <ApiErrorAlert :error="onboardingError" closable @dismiss="clearOnboardingError" />
    <slot />
  </Component>
</template>

<style lang="scss">
// As we are using `layouts` plugin we need its styles to be imported
@use "@layouts/styles/default-layout";
</style>
