# CLAUDE.md

This file provides guidance to Claude Code (claude.ai/code) when working with code in this repository.

## Commands

```bash
pnpm install       # install deps (also runs nuxt prepare + build:icons)
pnpm dev           # dev server
pnpm build         # production build (Node heap limited to 8 GB)
pnpm generate      # static generation
pnpm lint          # ESLint with auto-fix (.ts, .js, .vue, .tsx, .jsx)
pnpm build:icons   # rebuild the bundled Iconify icon set (run after adding icons)
```

Copy `.env.example` to `.env` and fill in Auth0 credentials before running locally. Auth0 must be configured as a **Regular Web Application** with callback URL `http://localhost:3000/auth/callback`.

## Stack

- **Nuxt 4** + **Vue 3** (Composition API / `<script setup>`)
- **Vuetify 3** (Vuexy admin template)
- **Pinia** (`@pinia/nuxt`) for state management
- **Auth0** via `@auth0/auth0-nuxt` (server-side session + `useUser()` / `useAuth0()` composables)
- **TypeScript** throughout; `pnpm build` runs `vue-tsc` type checking
- **pnpm** as package manager

## Architecture

### API client

All backend calls go through Nuxt server routes (Nitro proxy) — never call the backend directly from the client:

- **Client → Nitro**: use `$fetch('/api/<resource>', { method, body })` for imperative calls (event handlers); use `useFetch('/api/<resource>')` for reactive data fetching in components.
- **Nitro → backend**: each server route in `server/api/` calls `useAuth0(event).getAccessToken()` to get the token server-side and forwards the request to the real API with `Authorization: Bearer`.

`utils/api.ts` and `composables/useApi.ts` are legacy helpers that injected the token client-side via cookie. They are no longer the primary pattern.

### Auth flow

Auth0 session is managed **server-side** by Nitro:

1. `server/middleware/auth.server.ts` — redirects all non-public paths to `/auth/login` if no Auth0 session exists.
2. `server/api/_auth/token.get.ts` — returns the current access token via `useAuth0(event).getAccessToken()`.
3. Client-side: `middleware/auth.ts` guards routes using `useUser()` from `@auth0/auth0-nuxt`.

### Onboarding flow

On first login (`/onboarding/profile` → `/onboarding/space` → `/`):

- `GET /users/me` (404 → profile form; 200 → fetch spaces)
- `GET /spaces/user/:userId` (0 → space form; 1 → set active; 2+ → select-space page)

The check runs inside the **authenticated layout** (not a router middleware) via a `watch` on `isLoading`/`isAuthenticated` to avoid `useAuth0()` being called outside setup context. Onboarding pages use a separate `onboarding` layout (no sidebar/watch).

The `useSpaceStore` Pinia store holds `dbUser`, `activeSpace`, and `availableSpaces` in memory — it re-hydrates on refresh via the layout watch. See `onboarding-front.md` at the repo root for full implementation reference including all contract shapes.

### Layout system

`@layouts/` and `@core/` are internal template framework modules with their own stores, SCSS, components, and composables. Important constraints:
- Do not import `@core` from within `@layouts/` (ESLint enforces this).
- Do not use `useLayouts()` composable outside `@layouts/` or `@core/` — use `useThemeConfig` instead.

`layouts/default.vue` dynamically renders `DefaultLayoutWithVerticalNav` or `DefaultLayoutWithHorizontalNav` based on `useConfigStore().appContentLayoutNav`. Global layout/theme settings are configured in `themeConfig.ts`.

Navigation items for the sidebar/topbar are defined in `navigation/vertical/index.ts` and `navigation/horizontal/index.ts`.

### Component & import conventions

- **Components**: `@core/components` and `~/components` are both auto-imported without path prefix. `~/components/global` are registered globally. Use **PascalCase** in templates (enforced by ESLint).
- **Composables**: `@core/composable/`, `composables/`, and `plugins/*/composables/` are all auto-imported.
- **Utils**: `@core/utils/` and `utils/` are auto-imported — `$api` is available globally without import.
- **Icons**: use Iconify with the `tabler-`, `mdi-`, or `fa-` prefix in Vuetify's `icon` prop. Do not import `vuetify/components` or `vue3-apexcharts` directly — they are auto-imported.
- **Path aliases**: use `@images/` instead of `@/assets/images/` and `@styles/` instead of `@/assets/styles/` (ESLint enforces this).

## Language conventions

- **Code, variable names, comments, and documentation**: always in English.
- **User-facing labels, placeholders, validation messages, and UI text**: always in pt-BR (Brazilian Portuguese).

### ESLint style rules

2-space indent, no semicolons, trailing commas, camelCase naming. Key non-obvious rules:
- `@typescript-eslint/consistent-type-imports` is enforced — use `import type { Foo }` for type-only imports.
- Blank lines required between `const` and `expression` statements (`padding-line-between-statements`).
- New line required before `return` statements (`newline-before-return`).
