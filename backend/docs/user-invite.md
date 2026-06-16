# User & Space Invite System (backend)

Status: **implemented and working**. This doc is the backend-only map of the invite system —
domain model, file locations, validation contracts, and the API surface the frontend consumes.
For the full end-to-end user journey (how the frontend stitches these endpoints together,
notification bell, onboarding integration), see `frontend/docs/user-invite.md`.

## Domain model

A `SpaceInvite` is a standalone entity (not a placeholder `SpaceMember`). Lifecycle:

```
PENDING --accept()--> ACCEPTED
PENDING --cancel()--> CANCELLED
PENDING --decline()--> DECLINED
```

Fields: `id`, `space`, `role` (role to grant on acceptance), `email` (recipient), `token`
(UUID, unique, used internally — no longer exposed in the invite email link), `status`,
`createdAt`, `expiresAt` (createdAt + 7 days).

`SpaceInvite.isExpired()` → `Instant.now().isAfter(expiresAt)`. Expiry is checked at
read-time (no scheduled job flips status to expired — a PENDING invite past `expiresAt` is
just treated as expired wherever it's read: `ListMyInvitesService` filters it out, and
`AcceptInviteService`/`DeclineInviteService` both throw `invite_expired` if attempted).

`email` is used two ways: scoped to a single space (`findBySpaceIdAndEmail`, the duplicate-
invite guard) and unscoped across all spaces (`findByEmailIgnoreCaseAndStatus`, "list my
pending invites" — case-insensitive since the recipient address is set by an admin and may
not match the casing of the eventual `User.email`).

## File map

All paths relative to `backend/src/main/java/com/devhouse/financial_plan/`.

| Layer | File | Purpose |
|---|---|---|
| domain | `domain/SpaceInvite.java` | entity + `cancel()`/`accept()`/`decline()`/`isExpired()`/`validate()` |
| domain | `domain/enums/InviteStatus.java` | `PENDING, ACCEPTED, CANCELLED, DECLINED` |
| domain | `domain/repository/SpaceInviteRepository.java` | interface: `save`, `update`, `delete`, `findByToken`, `findBySpaceId`, `findBySpaceIdAndEmail`, `findByEmailIgnoreCaseAndStatus` |
| infra | `infrastructure/repository/jpa/SpaceInviteEntityJpa.java` | `@Entity @Table(name="space_invites")`, `token` unique, `status` `@Enumerated(STRING)` |
| infra | `infrastructure/repository/jpa/JpaSpaceInviteRepository.java` | Spring Data derived queries, incl. `findByEmailIgnoreCaseAndStatus` |
| infra | `infrastructure/repository/SpaceInviteRepositoryImpl.java` | `@Component @Transactional`, maps JPA ↔ domain |
| infra | `infrastructure/controller/InviteController.java` | `@RequestMapping("/invites")` — all 3 routes authenticated: `GET` (list mine), `POST /{token}/accept`, `POST /{token}/decline` |
| infra | `infrastructure/controller/SpaceController.java` | adds `GET /{id}/invites`, `DELETE /{id}/invites/{inviteId}`, `POST /{id}/invites` (send). **No longer has** a direct-add-member endpoint — `AddSpaceMemberService` was removed; every member addition now goes through an invite. |
| infra | `infrastructure/config/SecurityConfig.java` | all `/invites/**` routes require authentication — the old `GET /invites/*` `permitAll()` rule was removed along with the public single-invite lookup it existed for |
| infra | `infrastructure/config/GlobalHandlerException.java` | `DomainException` → `422` with **plain string body** (not JSON) — see "Error codes" below |
| application | `application/space/InviteSpaceMemberService.java` | creates `SpaceInvite`, blocks duplicate PENDING invite for same email+space, sends HTML email with a generic login link (no token in the URL) |
| application | `application/space/ListSpaceInvitesService.java` | `GET /{id}/invites` backing service (space-scoped, admin-facing) |
| application | `application/space/CancelSpaceInviteService.java` | `DELETE /{id}/invites/{inviteId}` backing service |
| application | `application/invite/ListMyInvitesService.java` | `GET /invites` backing service — all PENDING, non-expired invites for the authenticated user's email, across every space |
| application | `application/invite/AcceptInviteService.java` | **security-critical** — see below |
| application | `application/invite/DeclineInviteService.java` | **security-critical** — see below |
| dto | `application/space/dto/SpaceInviteResponse.java` | `(inviteId, email, roleId, roleName, status, createdAt, expiresAt)` — admin-facing, space-scoped |
| dto | `application/invite/dto/MyInviteResponse.java` | `(token, spaceId, spaceName, roleId, roleName, expiresAt)` — authenticated, always scoped to the requester's own email; intentionally omits `email` and `status` |
| dto | `application/invite/dto/AcceptInviteResponse.java` | `(spaceId, spaceName, roleId, roleName, userId)` |
| config | `resources/application.properties` | `app.frontend-url` (used to build the login link in the invite email), MailTrap SMTP settings |

There is **no public, unauthenticated invite-lookup endpoint anymore**. The old
`GetInviteByTokenService` / `InviteDetailsResponse` / public `GET /invites/{token}` chain was
removed — it existed only to power the old `/join?token=...` frontend page, which no longer
exists. Every invite-related read/write now requires a logged-in user.

### AcceptInviteService — validation order

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
that was sent to a different email address (e.g. forwarded link, guessed token). The error
message shown to the end user must never reveal the invite's real recipient email — only
"this invite was sent to a different email" wording. Since there is no public lookup endpoint
anymore, `MyInviteResponse` is the only place invite details are ever returned over the API,
and it's always scoped to the caller's own email — so this concern is now structurally
harder to violate than before (there's no code path that returns invite details for an email
that isn't the caller's own).

### DeclineInviteService — validation order

Same chain as accept, minus the membership-specific steps (declining doesn't care whether
you're already a member), plus one extra check for the new terminal state:

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
JSON). Full code list:

| Code | Thrown by | Meaning |
|---|---|---|
| `invite_cancelled` | Accept/Decline | invite was cancelled by an admin |
| `invite_already_accepted` | Accept/Decline | invite already used |
| `invite_already_declined` | Decline | invite was already declined by the recipient |
| `invite_expired` | Accept/Decline | past `expiresAt` (7 days) |
| `complete_onboarding` | Accept/Decline | authenticated Auth0 user has no `User` row yet (new user) |
| `invite_email_mismatch` | Accept/Decline | logged-in user's email ≠ invite's email |
| `already_member` | Accept | user is already a member of the target space (accept-only; decline has no equivalent check) |

`ListMyInvitesService` never throws for a missing `User` row — it returns an empty list
instead. This is deliberate: it's polled passively (e.g. by the frontend's notification bell)
and may run before a brand-new user has finished onboarding, so it must degrade gracefully
rather than surfacing an error for a state that isn't actually wrong yet.

## Email sending

`InviteSpaceMemberService.sendInviteEmail()` builds a hardcoded HTML email (no external
template file) via Spring's `JavaMailSender`/`MimeMessageHelper`. The link in the email is
**`{app.frontend-url}/` — the app root, with no token in the URL**. This is intentional: the
recipient is meant to log in (or create an account) first, and only after authenticating does
the app fetch *all* of their pending invites (possibly across multiple spaces) via
`GET /invites`. There is no longer a magic single-invite accept link.

**MailTrap is dev-only.** `application.properties` SMTP config reads
`${MAILTRAP_USERNAME:}` / `${MAILTRAP_PASSWORD:}` — empty by default, meaning email sending
will fail locally unless `.env`/CI secrets set those. No code currently catches a mail-send
failure inside `InviteSpaceMemberService` — verify that path if invites start silently not
arriving.

## Things to know before extending this (backend side)

- **No scheduled cleanup job.** Expired/cancelled/declined invites stay in `space_invites`
  forever. `ListSpaceInvitesService` (admin, space-scoped) returns everything; `ListMyInvitesService`
  (recipient, cross-space) filters to PENDING + non-expired only.
- **Duplicate invite guard** is per `(spaceId, email)` and only blocks while a PENDING invite
  exists — `InviteSpaceMemberService` checks `findBySpaceIdAndEmail` before creating a new one.
  Re-inviting after cancel/accept/decline/expiry is allowed (no PENDING row left to conflict with).
- **Every member addition goes through an invite now** — there is no direct-add endpoint.
  If you're tempted to add one back (e.g. for an "admin force-add" feature), think hard about
  whether that's actually wanted: it was removed specifically so accept/decline is always in
  the recipient's control.
- **OWNER role** is unrelated to invites directly, but if you ever let an invite carry the
  OWNER role, check `UpdateSpaceMemberRoleService`/the frontend's `/users` page — OWNER is
  currently treated as protected/immutable everywhere else in the members UI, so an OWNER
  invite would be an inconsistency worth a deliberate decision, not an oversight.
