<script setup lang="ts">
import logoHorizontal from '@images/logo-horizontal.svg?raw'
import logoHorizontalMono from '@images/logo-horizontal-mono.svg?raw'
import sealIcon from '@images/logo.svg?raw'

definePageMeta({
  layout: 'blank',
  middleware: 'guest',
  public: true,
})

const highlights = [
  {
    icon: 'tabler-wallet',
    color: 'primary',
    title: 'Visão clara das suas contas',
    description: 'Acompanhe saldos, contas bancárias e cartões em um só lugar, sem planilhas.',
  },
  {
    icon: 'tabler-users',
    color: 'secondary',
    title: 'Planejamento em família',
    description: 'Convide quem divide as contas com você e organize o orçamento junto, no mesmo Espaço.',
  },
  {
    icon: 'tabler-target-arrow',
    color: 'warning',
    title: 'Metas e faturas sob controle',
    description: 'Saiba o que vence essa semana, quanto já foi gasto e quanto falta para suas metas.',
  },
]

const currentYear = new Date().getFullYear()
</script>

<template>
  <div class="landing-page">
    <header class="landing-header d-flex align-center px-4 px-md-12 py-4">
      <NuxtLink
        to="/"
        class="landing-logo-link d-flex align-center"
      >
        <span
          class="landing-header-logo"
          v-html="logoHorizontal"
        />
      </NuxtLink>

      <VSpacer />

      <VBtn
        to="/dashboard"
        color="primary"
        variant="flat"
      >
        Acessar Sistema
      </VBtn>
    </header>

    <section class="landing-hero px-4 px-md-12">
      <div
        class="landing-hero-seal"
        v-html="sealIcon"
      />

      <div class="landing-hero-content">
        <h1 class="landing-hero-title">
          Cuide bem do que foi <span class="text-primary">confiado a você</span>
        </h1>
        <p class="landing-hero-subtitle">
          O Prover organiza a vida financeira da sua família: contas, cartões, faturas e metas, tudo em um só
          lugar, com clareza e cuidado.
        </p>
        <VBtn
          to="/dashboard"
          color="primary"
          size="x-large"
          variant="flat"
          append-icon="tabler-arrow-right"
        >
          Acessar Sistema
        </VBtn>
      </div>
    </section>

    <section class="landing-highlights px-4 px-md-12">
      <VRow>
        <VCol
          v-for="item in highlights"
          :key="item.title"
          cols="12"
          md="4"
        >
          <VCard
            flat
            border
            class="landing-highlight-card pa-6"
          >
            <VAvatar
              :color="item.color"
              variant="tonal"
              size="48"
              class="mb-4"
            >
              <VIcon
                :icon="item.icon"
                size="26"
              />
            </VAvatar>

            <h3 class="text-h6 mb-2">
              {{ item.title }}
            </h3>
            <p class="text-body-2 text-medium-emphasis mb-0">
              {{ item.description }}
            </p>
          </VCard>
        </VCol>
      </VRow>
    </section>

    <footer class="landing-footer px-4 px-md-12 py-8">
      <span
        class="landing-footer-logo"
        v-html="logoHorizontalMono"
      />
      <p class="text-caption mb-0 mt-4">
        © {{ currentYear }} Prover. Todos os direitos reservados.
      </p>
    </footer>
  </div>
</template>

<style lang="scss" scoped>
.landing-page {
  display: flex;
  flex-direction: column;
  min-block-size: 100vh;
  background: rgb(var(--v-theme-background));
  color: rgb(var(--v-theme-on-background));
}

.landing-header {
  border-block-end: 1px solid rgba(var(--v-theme-on-background), 0.08);
}

.landing-logo-link {
  text-decoration: none;
}

.landing-header-logo :deep(svg) {
  block-size: 36px;
  inline-size: auto;
  display: block;
}

.landing-hero {
  position: relative;
  flex: 1;
  display: flex;
  align-items: center;
  min-block-size: 60vh;
  overflow: hidden;
  padding-block: 4rem;
}

.landing-hero-content {
  position: relative;
  z-index: 1;
  max-inline-size: 640px;
}

.landing-hero-title {
  font-family: "Cormorant Garamond", serif;
  font-weight: 600;
  font-size: clamp(2.5rem, 5vw, 4rem);
  line-height: 1.1;
  margin-block-end: 1.5rem;
}

.landing-hero-subtitle {
  font-size: 1.125rem;
  line-height: 1.6;
  opacity: 0.75;
  margin-block-end: 2rem;
  max-inline-size: 32rem;
}

.landing-hero-seal {
  position: absolute;
  inset-block-start: 50%;
  inset-inline-end: -5%;
  transform: translateY(-50%);
  inline-size: min(45vw, 520px);
  opacity: 0.08;
  pointer-events: none;
}

.landing-hero-seal :deep(svg) {
  inline-size: 100%;
  block-size: auto;
}

.landing-highlights {
  padding-block: 1rem 4rem;
}

.landing-highlight-card {
  block-size: 100%;
  background: rgb(var(--v-theme-surface));
}

.landing-footer {
  margin-block-start: auto;
  background: #1F3D2F;
  color: #FFFDF8;
}

.landing-footer-logo :deep(svg) {
  block-size: 32px;
  inline-size: auto;
  display: block;
}

@media (max-width: 960px) {
  .landing-hero {
    min-block-size: auto;
    padding-block: 3rem;
  }

  .landing-hero-seal {
    display: none;
  }
}
</style>
