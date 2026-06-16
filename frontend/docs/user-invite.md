# User & Space Invite System

Status: **implemented and working** (backend + frontend). This doc is a map for whoever
(human or agent) needs to extend or debug it next — it favors "where is the code and how
does it flow" over prose explanation.

Related, already-built feature: `/users` page (space member management — list, change role,
remove). The invite system below is the **only** way to add anyone to a space, whether they
already have an account or not — there is no direct-add path.

See also `backend/docs/user-invite.md` for the backend-only view of this same system (domain
model, validation orders, error code contract). This doc covers the full user journey,
stitching backend endpoints together with the frontend pages/components that drive them.

## Domain model

A `SpaceInvite` is a standalone entity (not a placeholder `SpaceMember`). Lifecycle:

```
PENDING --accept()--> ACCEPTED
PENDING --cancel()--> CANCELLED
PENDING --decline()--> DECLINED
```

Fields: `id`, `space`, `role` (role to grant on acceptance), `email` (recipient), `token`
(UUID, unique — used internally for accept/decline calls, **never exposed in the invite
email link**), `status`, `createdAt`, `expiresAt` (createdAt + 7 days).

`SpaceInvite.isExpired()` → `Instant.now().isAfter(expiresAt)`. Expiry is checked at
read-time (no scheduled job flips status to expired — a PENDING invite past `expiresAt` is
just treated as expired wherever it's read).

`email` is the lookup key both ways: scoped to one space (duplicate-invite guard) and
**unscoped across every space the user's email has been invited to** (the "list my pending
invites" use case that powers the notification bell and the `/invites` page).

## Backend — file map

All paths relative to `backend/src/main/java/com/devhouse/financial_plan/`.

| Layer | File | Purpose |
|---|---|---|
| domain | `domain/SpaceInvite.java` | entity + `cancel()`/`accept()`/`decline()`/`isExpired()`/`validate()` |
| domain | `domain/enums/InviteStatus.java` | `PENDING, ACCEPTED, CANCELLED, DECLINED` |
| domain | `domain/repository/SpaceInviteRepository.java` | interface: `save`, `update`, `delete`, `findByToken`, `findBySpaceId`, `findBySpaceIdAndEmail`, `findByEmailIgnoreCaseAndStatus` |
| infra | `infrastructure/repository/jpa/SpaceInviteEntityJpa.java` | `@Entity @Table(name="space_invites")`, `token` unique, `status` `@Enumerated(STRING)` |
| infra | `infrastructure/repository/jpa/JpaSpaceInviteRepository.java` | Spring Data derived queries |
| infra | `infrastructure/repository/SpaceInviteRepositoryImpl.java` | `@Component @Transactional`, maps JPA ↔ domain |
| infra | `infrastructure/controller/InviteController.java` | `@RequestMapping("/invites")`, all authenticated — `GET` (list mine), `POST /{token}/accept`, `POST /{token}/decline` |
| infra | `infrastructure/controller/SpaceController.java` | adds `GET /{id}/invites`, `DELETE /{id}/invites/{inviteId}`, `POST /{id}/invites` (send). No direct-add-member endpoint exists anymore. |
| infra | `infrastructure/config/SecurityConfig.java` | every `/invites/**` route requires authentication (no public lookup rule) |
| infra | `infrastructure/config/GlobalHandlerException.java` | `DomainException` → `422` with **plain string body** (not JSON) — see "Error codes" below |
| application | `application/space/InviteSpaceMemberService.java` | creates `SpaceInvite`, blocks duplicate PENDING invite for same email+space, sends HTML email with a generic login link (no token) |
| application | `application/space/ListSpaceInvitesService.java` | `GET /{id}/invites` backing service (admin, space-scoped) |
| application | `application/space/CancelSpaceInviteService.java` | `DELETE /{id}/invites/{inviteId}` backing service |
| application | `application/invite/ListMyInvitesService.java` | `GET /invites` backing service — PENDING, non-expired invites for the caller's email, across all spaces |
| application | `application/invite/AcceptInviteService.java` | **security-critical** — see below |
| application | `application/invite/DeclineInviteService.java` | **security-critical** — see below |
| dto | `application/space/dto/SpaceInviteResponse.java` | `(inviteId, email, roleId, roleName, status, createdAt, expiresAt)` — admin-facing |
| dto | `application/invite/dto/MyInviteResponse.java` | `(token, spaceId, spaceName, roleId, roleName, expiresAt)` — always scoped to the requester's own email; intentionally omits `email`/`status` |
| dto | `application/invite/dto/AcceptInviteResponse.java` | `(spaceId, spaceName, roleId, roleName, userId)` |
| config | `resources/application.properties` | `app.frontend-url` (used to build the login link in the invite email), MailTrap SMTP settings |

### AcceptInviteService — validation order (important if you touch this)

```
1. invite exists                         → DomainException("Invite not found")
2. invite.status == CANCELLED            → "invite_cancelled"
3. invite.status == ACCEPTED             → "invite_already_accepted"
4. invite.isExpired()                    → "invite_expired"
5. user not found by auth0Sub            → "complete_onboarding"
6. user.email != invite.email (case-insensitive) → "invite_email_mismatch"
7. user already a SpaceMember of invite.space → "already_member"
→ otherwise: create SpaceMember, invite.accept(), persist
```

**Step 6 is the anti-hijacking check**: it stops a logged-in user from accepting an invite
that was sent to a different email address. The error message shown to the end user must
never reveal the invite's real recipient email. Since there's no public, unauthenticated
invite-lookup endpoint anymore, `MyInviteResponse` (always scoped to the caller's own email)
is the only place invite details are ever returned — this threat model is structurally
harder to violate than it was with the old public lookup.

### DeclineInviteService — validation order

Same chain, minus the membership steps, plus a check for the new terminal state:

```
1. invite exists                         → DomainException("Invite not found")
2. invite.status == CANCELLED            → "invite_cancelled"
3. invite.status == ACCEPTED             → "invite_already_accepted"
4. invite.status == DECLINED             → "invite_already_declined"
5. invite.isExpired()                    → "invite_expired"
6. user not found by auth0Sub            → "complete_onboarding"
7. user.email != invite.email (case-insensitive) → "invite_email_mismatch"
→ otherwise: invite.decline(), persist
```

### Error codes (string contract between backend and frontend)

`GlobalHandlerException` turns any `DomainException` into `HTTP 422` with the exception
message as a **raw plain-text body** (`ResponseEntity.body(exception.getMessage())`, not
JSON). This matters for the frontend — see "Nitro proxy hardening" below. Full code list:

| Code | Thrown by | Meaning |
|---|---|---|
| `invite_cancelled` | Accept/Decline | invite was cancelled by an admin |
| `invite_already_accepted` | Accept/Decline | invite already used |
| `invite_already_declined` | Decline | invite was already declined by the recipient |
| `invite_expired` | Accept/Decline | past `expiresAt` (7 days) |
| `complete_onboarding` | Accept/Decline | authenticated Auth0 user has no `User` row yet (new user) |
| `invite_email_mismatch` | Accept/Decline | logged-in user's email ≠ invite's email |
| `already_member` | Accept | user is already a member of the target space |

## Frontend — file map

All paths relative to `frontend/`.

| File | Role |
|---|---|
| `server/api/spaces/[id]/invites/index.get.ts` | proxy `GET /spaces/{id}/invites` (auth) |
| `server/api/spaces/[id]/invites/index.post.ts` | proxy `POST /spaces/{id}/invites` (auth) — send invite |
| `server/api/spaces/[id]/invites/[inviteId].delete.ts` | proxy `DELETE /spaces/{id}/invites/{inviteId}` (auth) |
| `server/api/invites/index.get.ts` | proxy `GET /invites` (auth) — list invites for the logged-in user, across all spaces |
| `server/api/invites/[token]/accept.post.ts` | proxy `POST /invites/{token}/accept` (auth) |
| `server/api/invites/[token]/decline.post.ts` | proxy `POST /invites/{token}/decline` (auth) |
| `stores/invites.ts` | Pinia store (`useInviteStore`) — `pendingInvites`, `fetchPendingInvites()`, `removeInvite()`; shared by the notification bell and `/invites` |
| `layouts/default.vue` | the onboarding-check `watch(user, ...)` also calls `inviteStore.fetchPendingInvites()` here, for every authenticated session |
| `layouts/components/NavBarNotifications.vue` | notification bell — maps `useInviteStore().pendingInvites` to the bell's `Notification[]`, navigates to `/invites` on click |
| `layouts/components/DefaultLayoutWithVerticalNav.vue` / `DefaultLayoutWithHorizontalNav.vue` | both render `<NavBarNotifications />` in the navbar slot, next to `<UserProfile />` |
| `@layouts/types.ts` | `Notification.id` is `number \| string` (invite tokens are UUID strings) |
| `@core/components/Notifications.vue` | template-framework bell dropdown; emits `view-all` on its footer button (wired by `NavBarNotifications.vue`) |
| `components/dialogs/AddMemberDialog.vue` | 3-step dialog (search email → found → not-found); **both** "found" and "not-found" branches now send an invite — there is no immediate-add path |
| `pages/users/index.vue` | members table + "Convites Pendentes" `VCard` (list + cancel, admin-facing, space-scoped) |
| `pages/invites/index.vue` | **"Meus Convites"** — authenticated page listing every PENDING invite for the logged-in user's email, with Accept/Decline per row |
| `pages/onboarding/profile.vue` | after creating the user profile, checks `GET /api/invites`; redirects to `/invites` if non-empty, else `/onboarding/space` as before |

### Nitro proxy hardening (read this before touching error handling)

`accept.post.ts` and `decline.post.ts` wrap the backend `$fetch` call in `try/catch` and
rethrow via `createError({ statusCode, statusMessage, data: fetchError.data })`.

This is **not** boilerplate — it's required because:
- The backend returns the error code as a **plain string body**, not `{ message: "..." }`.
- If the ofetch error from the backend call is left to propagate unhandled out of the Nitro
  handler, Nitro's default error serialization does not reliably forward that raw string to
  the browser client.
- `pages/invites/index.vue` branches on the *exact* code (`invite_already_declined`,
  `invite_email_mismatch`, etc.), so losing the string breaks the accept/decline UX silently
  (falls through to a generic error).

If you add new invite-related proxy routes that the frontend needs to branch on by code, copy
this same try/catch + `createError({ data })` pattern. Routes that only ever display a
generic error (e.g. `server/api/invites/index.get.ts`, the "list mine" GET) don't need it —
a failure there is just "couldn't load the list," no code-branching required.

`pages/invites/index.vue` has its own `extractErrorCode()` helper (separate from
`useApiError()`) specifically to read the raw code out of `error.data` (string or `{message}`
shapes both handled) for branching logic — `useApiError()`'s fallback chain doesn't read a
raw-string `data`, only `data?.message`.

## Request flows

### A. Invite anyone — existing user or not, always via invite

`AddMemberDialog` → search by email → found or not-found → **both** branches converge on
`POST /api/spaces/{id}/invites` (body `{email, roleId}`) → backend creates `SpaceInvite`
(PENDING, token, expiresAt = now+7d) and sends an HTML email with a **generic login link**
(`{app.frontend-url}/`, no token) → dialog emits `inviteSent` → `pages/users/index.vue`
refetches the invites list.

### B. Recipient receives the email and logs in (new or existing account)

Email link → app root `/` → `server/middleware/auth.server.ts` redirects to `/auth/login` if
there's no session → Auth0 round-trip → back on `/` (or `/onboarding/profile` if brand new,
see flow D) → `layouts/default.vue`'s `watch(user, ...)` fires `inviteStore.fetchPendingInvites()`
→ the notification bell shows an unread badge if any PENDING invites exist for that email.

### C. Recipient views and responds to their invites

Click the bell (or its "View All Notifications" footer) → `navigateTo('/invites')` →
`GET /api/invites` lists every PENDING, non-expired invite for the logged-in user's email,
across all spaces → each row has Accept/Decline.

- **Accept** → `POST /api/invites/{token}/accept` → backend creates the `SpaceMember`, marks
  the invite `ACCEPTED` → frontend removes the row, sets `activeSpace` from the response if
  none was set yet (first-space case).
- **Decline** → `POST /api/invites/{token}/decline` → backend marks the invite `DECLINED`
  (no membership side effects) → frontend removes the row.

### D. Recipient is a brand-new user (no `User` row yet)

Same as flow B, but after the Auth0 round-trip the user lands on `/onboarding/profile` →
fills the profile form → `pages/onboarding/profile.vue` POSTs `/api/users`, then calls
`GET /api/invites`: if any pending invites exist, `navigateTo('/invites')`; otherwise
`navigateTo('/onboarding/space')` as before (no invite-specific token in the query string
anymore — the redirect decision is based purely on whether the user's email has any PENDING
invites at the time of profile creation).

### E. Cancel a pending invite (admin side, unchanged)

`pages/users/index.vue` → "Convites Pendentes" table → cancel button → `DELETE
/api/spaces/{id}/invites/{inviteId}` → row removed from local state on success.

## Things to know before extending this

- **No scheduled cleanup job.** Expired/cancelled/declined invites stay in `space_invites`
  forever. `ListSpaceInvitesService` (admin) returns everything for the space;
  `pages/users/index.vue` only *displays* the ones with `status === 'PENDING'` (client-side
  filter) — other statuses are fetched but hidden, not deleted. `ListMyInvitesService`
  (recipient-facing) filters to PENDING + non-expired server-side.
- **Duplicate invite guard** is per `(spaceId, email)` and only blocks while a PENDING invite
  exists — `InviteSpaceMemberService` checks `findBySpaceIdAndEmail` before creating a new
  one. Re-inviting after cancel/accept/decline/expiry is allowed.
- **There is no direct-add-member path anymore.** Every space membership is created by
  `AcceptInviteService`, never by a controller endpoint that bypasses the invite. If you're
  ever tempted to add a "force add" admin shortcut back, that's a deliberate product decision
  to make, not a gap to silently patch.
- **The invite email never reveals identity info beyond what's needed**, and now reveals
  even less than before: the link carries no token at all, just a generic login URL. The
  only place invite details are ever returned over the API is `MyInviteResponse`, and it's
  always scoped to the caller's own email via `auth0Sub` — there is no public,
  unauthenticated lookup endpoint to misuse.
- **OWNER role** is unrelated to invites directly, but if you ever let an invite carry the
  OWNER role, check `UpdateSpaceMemberRoleService`/`pages/users/index.vue` — OWNER is currently
  treated as protected/immutable everywhere else in the members UI, so an OWNER invite would be
  an inconsistency worth a deliberate decision, not an oversight.
- **MailTrap is dev-only.** `application.properties` SMTP config reads
  `${MAILTRAP_USERNAME:}` / `${MAILTRAP_PASSWORD:}` — empty by default, meaning email sending
  will fail locally unless `.env`/CI secrets set those. No code currently catches a mail-send
  failure inside `InviteSpaceMemberService` — verify that path if invites start silently not
  arriving.
- **The notification bell is a lightweight indicator, not a second action surface.** It only
  shows pending invites and routes to `/invites` — it does not let you accept/decline inline,
  and it has no "mark as read" concept distinct from "acted upon." If you want richer
  notifications later (other event types beyond invites), `@layouts/types.ts`'s `Notification`
  type and `useInviteStore` would both need to generalize beyond invite-only data.

## Manual test checklist

1. Invite a non-existing email via `AddMemberDialog` → appears in "Convites Pendentes" as
   PENDING.
2. Invite an *existing* user's email via `AddMemberDialog` → confirm it also creates a
   PENDING `SpaceInvite` instead of adding them to the space immediately (member list does
   not grow; "Convites Pendentes" does).
3. Check MailTrap inbox for both → link is `{frontend-url}/` with **no token** in the URL;
   copy says to log in to see the invite, not to click-to-accept.
4. Log in as the invited (existing) user → bell shows an unread badge without visiting any
   specific page → click through → `/invites` lists it with correct space/role/expiry.
5. Accept one → `SpaceMember` row created (check via `/users` member list), invite disappears
   from `/invites` and the bell, `activeSpace` set if it was the user's first space.
6. Decline another → disappears from `/invites`/bell; confirm status is now `DECLINED`.
7. Brand-new Auth0 user with a pending invite waiting → after `/onboarding/profile` submit,
   redirected to `/invites` (not `/onboarding/space`) → accept/decline works the same.
8. Brand-new Auth0 user with **no** pending invites → normal `/onboarding/space` flow
   unaffected.
9. Accept/decline a token belonging to a different email than the logged-in user → confirm
   `invite_email_mismatch`, real recipient email never revealed.
10. Decline an already-declined invite (e.g. two browser tabs) → confirm
    `invite_already_declined` surfaces cleanly, not a generic error.
11. Cancel a pending invite in `/users` → confirm it disappears from the admin list.
12. Manually backdate `expires_at` in `space_invites` (or wait 7 days) → confirm the invite no
    longer appears in `GET /api/invites`, and a direct accept/decline attempt on its token
    still returns `invite_expired`.
