# Categories + SubCategories

Status: **implemented and working** (backend already existed; this doc covers the frontend
piece — Grupo F3 of `backend/IMPLEMENTATION_PLAN.md`). Larger than F1/F2 because subcategories
are a sub-resource managed from a secondary dialog, following the `RolePermissionsDialog.vue`
pattern (dialog opened from a table row) — adapted for full CRUD instead of read/toggle-only.

## Backend

`CategoryController` (`/categories`), already implemented with `@PreAuthorize`. See
`backend/src/main/java/.../infrastructure/controller/CategoryController.java`.

| Method | Path | Body / Query | Response |
|---|---|---|---|
| GET | `/categories` | `spaceId` (query) | `CategoryResponse[]` — **includes nested `subCategories`** |
| POST | `/categories` | `{ spaceId, name }` | `CategoryResponse` (`subCategories: []`) |
| PUT | `/categories/{id}` | `{ version, name }` | `CategoryResponse` (`subCategories: []` — see note below) |
| PATCH | `/categories/{id}/status` | `{ active: boolean }` | `CategoryResponse` |
| DELETE | `/categories/{id}` | — | `204 No Content` |
| POST | `/categories/subcategories` | `{ categoryId, name }` | `SubCategoryResponse` |
| PUT | `/categories/subcategories/{id}` | `{ version, name }` | `SubCategoryResponse` |
| PATCH | `/categories/subcategories/{id}/status` | `{ active: boolean }` | `SubCategoryResponse` |
| DELETE | `/categories/subcategories/{id}` | — | `204 No Content` |

`CategoryResponse`: `{ id, version, name, active, subCategories: SubCategoryResponse[] }`.
`SubCategoryResponse`: `{ id, version, categoryId, name, active }`.

**Important gap to know about:** there is no `GET` endpoint scoped to a single category's
subcategories, and `CreateCategoryService`/`UpdateCategoryService` both always return
`subCategories: []` (they never look them up — only `ListCategoriesService` populates them,
via `SubCategoryRepository.findByCategoryId`). The frontend works around this:
- `AddEditCategoryDialog.vue`, in edit mode, overwrites the backend's empty `subCategories: []`
  with the original category's `subCategories` before emitting `saved`, so the parent table
  doesn't lose the count.
- `ManageSubCategoriesDialog.vue` never re-fetches from the backend — it receives the category's
  current `subCategories` array as a prop (already loaded by the parent page's `GET /categories`)
  and keeps its own local copy in sync as creates/edits/deletes happen, emitting the updated
  array back up via `updated` so the row on the main table stays correct without a full refetch.

`DELETE` is now a **hard delete** at both levels. `DeleteCategoryService` first rejects (`422`)
if the category still has any subcategories (`SubCategoryRepository.existsByCategoryId`), then
if it has any `Transaction`s linked directly (`TransactionRepository.existsByCategoryId`) —
only when both are clear is the row actually removed. `DeleteSubCategoryService` rejects (`422`)
if any `Transaction` references it (`existsBySubCategoryId`). Activating/deactivating is now a
separate, reversible action via `PATCH .../status` at both levels
(`UpdateCategoryStatusService` / `UpdateSubCategoryStatusService`).

## Frontend — file map

| File | Purpose |
|---|---|
| `pages/categories/index.vue` | Main page — table of categories (name, active subcategory count, status, actions) |
| `components/dialogs/AddEditCategoryDialog.vue` | Create/edit dialog for the category itself (`name` only) |
| `components/dialogs/ManageSubCategoriesDialog.vue` | Secondary dialog, opened from a table row — full CRUD of that category's subcategories |
| `components/dialogs/ConfirmDialog.vue` | Reused before deleting a category **and** nested inside `ManageSubCategoriesDialog` before deleting a subcategory |
| `server/api/categories/index.get.ts` | Proxies `GET /categories?spaceId=` |
| `server/api/categories/index.post.ts` | Proxies `POST /categories` |
| `server/api/categories/[id].put.ts` | Proxies `PUT /categories/{id}` |
| `server/api/categories/[id]/status.patch.ts` | Proxies `PATCH /categories/{id}/status` |
| `server/api/categories/[id].delete.ts` | Proxies `DELETE /categories/{id}` — catches backend errors and re-throws via `createError({statusCode, statusMessage, data})` so the real `DomainException` message reaches the client |
| `server/api/categories/subcategories/index.post.ts` | Proxies `POST /categories/subcategories` |
| `server/api/categories/subcategories/[id].put.ts` | Proxies `PUT /categories/subcategories/{id}` |
| `server/api/categories/subcategories/[id]/status.patch.ts` | Proxies `PATCH /categories/subcategories/{id}/status` |
| `server/api/categories/subcategories/[id].delete.ts` | Proxies `DELETE /categories/subcategories/{id}` — same `createError` error-forwarding pattern |

Note the subcategory routes mirror the backend's flat `/categories/subcategories` path (not
nested under a category id in the URL) — `categoryId` travels in the request body instead, same
as the backend controller.

## Page behavior

- `pages/categories/index.vue` fetches on mount and on `spaceStore.activeSpace` change, same as
  the other F1-F2 pages.
- Table shows the **count of active subcategories** (not total), name, status chip, and four
  row actions: Gerenciar subcategorias (opens `ManageSubCategoriesDialog`), Editar,
  Ativar/Inativar, Excluir.
- "Ativar/Inativar" (toggle icon) is always enabled and flips `active` via `PATCH .../status`,
  with its own confirmation dialog; row updates in place.
- "Excluir" is always enabled and performs a real hard delete (`confirm-color="error"`,
  irreversible warning); on success the row is removed from the local array. A `422` (linked
  subcategories or transactions) surfaces the real backend message via the snackbar/alert.

## `ManageSubCategoriesDialog.vue`

- Props: `categoryId`, `categoryName`, `subCategories` (initial array from the row).
- Inline creation: a text field + "Adicionar" button at the top (`Enter` also submits).
- Inline editing: clicking the pencil icon on a row swaps it for a text field with
  confirm/cancel icon buttons (`Enter`/`Esc` also work).
- Each row also has an "Ativar/Inativar" toggle (`PATCH .../status`) and a real "Excluir"
  action (`ConfirmDialog` nested inside this dialog, `confirm-color="error"`) — deleting is a
  hard delete now: on success the row is removed from the local list; if the subcategory has
  transactions linked to it, the backend's `422` message is shown via `ApiErrorAlert`.
- Every create/edit/delete emits `updated` with the full local subcategories array so the
  parent page's row stays in sync without an extra round trip.

## Manual verification

No frontend test suite exists in this project — verified manually: `pnpm dev`, open
`/categories`, create a category, open "Gerenciar subcategorias", create/edit/delete a few
subcategories (confirm the active count on the main table updates after closing the dialog),
toggle a category/subcategory inactive/active, try deleting a category that still has
subcategories (expect the real backend error message), try deleting a category/subcategory used
by a transaction (expect the real backend error message), delete a category/subcategory with no
dependencies (row disappears).
