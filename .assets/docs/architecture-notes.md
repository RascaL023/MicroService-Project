# Backend Architecture Notes

Last updated: 2026-05-24

## Current Direction

This backend is planned as a microservices project. The first rewritten service is `auth-service`, ported from the old Java/Spring backup in `auth-service.bak` into Go using:

- `chi` for HTTP routing
- `pgx` / `pgxpool` for PostgreSQL
- bcrypt for password hashing
- stateful session tokens by default

## Service Ownership

### auth-service

`auth-service` owns authentication and access state.

It should handle:

- Credentials and password hashes
- Login and register flows
- Session creation, validation, and revocation
- Account access status, for example:
  - `ACTIVE`
  - `BANNED`
  - `SUSPENDED`
  - `LOCKED`
- Role and permission grants, unless these are later split into a dedicated authorization service
- Instant ban behavior, including revoking all active sessions for a user

Important decision:

`is_banned` or a richer `account_status` belongs in `auth-service` when its meaning is "this user may not authenticate or continue using an active session." This avoids making login depend on `user-service` availability.

Recommended improvement:

Replace boolean `is_banned` with a status field:

```text
account_status = ACTIVE | BANNED | SUSPENDED | LOCKED
```

### user-service

`user-service` should own user domain/profile data.

It should handle:

- User profile
- Avatar/bio/display metadata
- Favorite products
- User preferences
- Membership/premium user domain data, if membership is treated as product/domain behavior

It should not be required for basic login checks.

## Session And Ban Plan

The desired future runtime store is Redis.

Proposed Redis keys:

```text
session:{session_uuid}       -> user_id, roles, permissions, ttl
user_sessions:{user_id}      -> SET of session_uuid
banned_users                 -> SET of user_id
auth_events                  -> Redis Stream
```

On login:

```text
SET session:{session_uuid} ...
SADD user_sessions:{user_id} {session_uuid}
```

On request authentication:

```text
GET session:{session_uuid}
SISMEMBER banned_users {user_id}
```

On admin ban:

```text
SADD banned_users {user_id}
SMEMBERS user_sessions:{user_id}
DEL session:{uuid1}, session:{uuid2}, ...
DEL user_sessions:{user_id}
XADD auth_events * type user_banned user_id {user_id}
```

## Bearer Header Decision

The project may keep using:

```http
Authorization: Bearer <token>
```

even when the token is a stateful opaque session UUID. `Bearer` is only the HTTP authorization scheme; it does not require JWT.

If Kong/API gateway is used, the token can still be passed as Bearer and introspected against `auth-service` or Redis-backed session validation.

## Current auth-service Notes

Current implementation path:

```text
auth-service/
```

Main files:

```text
auth-service/cmd/server/main.go
auth-service/internal/http/router.go
auth-service/internal/service/auth.go
auth-service/internal/db/db.go
```

Current important endpoints:

```text
POST /api/auths/login
POST /api/auths/register
GET  /api/auths/test
POST /api/users
GET  /api/users
GET  /api/users/{id}
GET  /api/roles
GET  /api/roles/{id}
POST /api/roles
```

Current auth mode:

- Default `AUTH_MODE=stateful`
- Login returns `tokenType: "Session"`
- Protected requests currently expect:

```http
Authorization: Session <accessToken>
```

Future preferred compatibility:

- Accept `Authorization: Bearer <opaque-session-token>` for stateful sessions, to make Kong/API gateway integration simpler.

## How To Continue In A New Chat

When starting a new Codex chat, ask:

```text
Read docs/architecture-notes.md first, then continue from the current auth-service Go rewrite.
```

Useful next tasks:

- Change stateful auth to accept `Bearer <session_uuid>` as an opaque Redis/session token.
- Replace `is_banned` boolean with `account_status`.
- Move session storage from PostgreSQL to Redis.
- Add admin ban endpoint that revokes active sessions.
- Split user domain data into a future `user-service`.
- Decide whether roles/permissions stay in `auth-service` or move to a later authorization service.

<!-- RESUME: codex resume 019e6051-e812-7b02-a968-99d9fe4d88ed -->
codex resume 019e6fff-431b-7091-bbda-0f11dcd8959c
