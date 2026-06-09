# AGENTS.md

Instrucoes para agentes que trabalham neste frontend.

## Stack do projeto

- Vue 3 com Composition API.
- Vite 7.
- TypeScript com `strict: true`.
- Vuetify 3 como biblioteca principal de UI.
- Pinia para estado compartilhado.
- Vue Router via `unplugin-vue-router`, com rotas baseadas em `src/pages`.
- Layouts via `vite-plugin-vue-meta-layouts`, em `src/layouts`.
- SCSS para estilos globais e variaveis do tema.
- Iconify, ApexCharts, Chart.js, TipTap e VueUse ja fazem parte da stack.

## Comandos principais

Use `pnpm`, pois o repositorio possui `pnpm-lock.yaml`.

- Instalar dependencias: `pnpm install`
- Rodar em desenvolvimento: `pnpm dev`
- Build de producao: `pnpm build`
- Typecheck: `pnpm typecheck`
- Lint com autofix: `pnpm lint`
- Preview do build: `pnpm preview`

Antes de entregar mudancas relevantes, rode pelo menos `pnpm typecheck`. Para alteracoes de UI ou build/configuracao, rode tambem `pnpm build` quando viavel.

## Regras gerais de codigo

- Prefira solucoes pequenas e alinhadas aos padroes existentes do template.
- Nao reescreva ou adicione arquivos de `src/@core` ou `src/@layouts`; elas funcionam como base do tema.
- Use os aliases configurados (`@`, `@core`, `@layouts`, `@images`, `@styles`) em vez de caminhos relativos longos.
- Mantenha TypeScript estrito. Evite `any`; quando inevitavel, documente o motivo no menor escopo possivel.
- Nao edite manualmente arquivos gerados como `auto-imports.d.ts`, `components.d.ts` e `typed-router.d.ts`, salvo quando a tarefa for explicitamente sobre geracao/tipagem.
- Preserve o lockfile existente. Nao gere `package-lock.json` ou `yarn.lock`.

## Vue

- Use `<script setup lang="ts">` por padrao em novos SFCs.
- Ordem dos blocos em `.vue`: `<script>`, depois `<template>`, depois `<style>`.
- Mantenha componentes focados. Extraia componentes menores quando uma tela acumular formulario, lista, filtros, acoes e estado.
- Componentes de pagina em `src/pages` devem orquestrar a tela; mova UI reutilizavel para `src/components` ou para uma pasta de feature.
- Use props para entrada e emits para saida. Use `v-model` apenas quando houver contrato real de edicao bidirecional.
- Derive estado com `computed`; use `watch` para efeitos colaterais, nao para derivacao simples.
- Evite logica pesada no template. Prefira funcoes/computed no `<script setup>`.
- Nao use `v-html` sem sanitizacao e motivo explicito.

## Estado, dados e side effects

- Use Pinia quando o estado atravessar paginas, layouts ou features.
- Para estado local de uma tela, prefira `ref`, `reactive` e `computed` no componente ou em composables.
- Coloque logica reutilizavel ou com side effects em composables `useXxx()` dentro de `src/composables` ou junto da feature.
- Centralize chamadas HTTP nos helpers existentes, especialmente `src/composables/useApi.ts` e `src/utils/api.ts`, quando aplicavel.
- Trate loading, erro e estado vazio nas telas que buscam dados.

## UI e estilos

- Use Vuetify como primeira escolha para componentes de interface.
- Antes de criar CSS customizado, confira se Vuetify, utilitarios existentes ou variaveis SCSS ja resolvem o caso.
- Mantenha compatibilidade com tema claro/escuro quando mexer em cores.
- Prefira icones Iconify/Tabler ja configurados em vez de SVG manual para acoes comuns.
- Evite estilos globais novos. Quando necessario, coloque-os em arquivos de estilo existentes e com escopo claro.
- Garanta responsividade para telas de dashboard/admin, com layouts densos, escaneaveis e previsiveis.

## Rotas e layouts

- Novas paginas devem ficar em `src/pages`, seguindo o roteamento por arquivo.
- Use layouts existentes em `src/layouts` e mantenha a configuracao de layout proxima do padrao ja usado nas paginas.
- Atualize navegacao em `src/navigation/vertical` ou `src/navigation/horizontal` quando uma nova pagina precisar aparecer no menu.
- Nao registre rotas manualmente se o plugin de rotas por arquivo ja cobrir o caso.

## Qualidade e verificacao

- Para mudancas de tipo, composables, stores ou APIs: rode `pnpm typecheck`.
- Para mudancas visuais grandes, rode `pnpm build` e verifique a tela no navegador quando possivel.
- Se `pnpm lint` for usado, lembre que ele aplica `--fix`; revise os arquivos alterados depois.
- Nao esconda falhas de typecheck, lint ou build. Se nao puder rodar um comando, informe o motivo.

## Git e manutencao

- Nao reverta alteracoes de outros sem pedido explicito.
- Mantenha diffs pequenos e relacionados a tarefa.
- Nao misture refatoracoes esteticas com correcao funcional.
- Ao adicionar dependencias, justifique a necessidade e prefira bibliotecas ja presentes na stack.
