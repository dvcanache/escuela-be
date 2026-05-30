# API Reference — Educational Management Platform

**Base URL:** `http://localhost:8080`
**Auth:** JWT Bearer Token (`Authorization: Bearer <token>`)
**Multi-Tenant:** Header `X-Tenant-ID: <tenantId>`

---

## 1. Authentication

### `POST /api/auth/login`

Authenticates a user and returns a JWT token.

**Request Body:**
```json
{
  "username": "juan.perez",
  "password": "secret123"
}
```

**Response `200 OK`:**
```json
{
  "token": "eyJhbGciOiJIUzI1NiJ9...",
  "type": "Bearer",
  "username": "juan.perez",
  "roles": ["ROLE_PROFESOR"]
}
```

**Errors:**
| Status | Description |
|--------|-------------|
| `401` | Invalid credentials |
| `400` | Validation error (blank fields) |

---

## 2. Test Endpoints (Dev-Only)

All test endpoints are under `/api/test/**` and are **permitAll** (no auth required).

### `GET /api/test/users`

Returns all usernames in the system.

**Response `200 OK`:**
```json
["juan.perez", "maria.garcia", "admin"]
```

### `GET /api/test/security/hash?password=<text>`

Returns the Argon2 hash of a given password (for setup purposes).

**Response `200 OK`:**
```
$argon2id$v=19$m=19456,t=2,p=1$...
```

### `GET /api/test/tenant/current`

Returns the current tenant ID from the `X-Tenant-ID` header (via `TenantContext`).

**Response `200 OK`:**
```json
{
  "tenantId": 1,
  "message": "Tenant Context is working!"
}
```

---

## 3. Security & RBAC Rules

Configured in `SecurityConfig.java` (`src/main/java/.../modules/security/SecurityConfig.java`):

| Path Pattern | Allowed Roles |
|-------------|---------------|
| `/api/auth/**` | **permitAll** (public) |
| `/api/test/**` | **permitAll** (dev only) |
| `/api/admin/**` | `ADMIN`, `PROFESOR` |
| `/api/mobile/**` | `ESTUDIANTE`, `PADRE` |
| any other | **authenticated** (any valid token) |

All endpoints except `/api/auth/**` and `/api/test/**` require:
- `Authorization: Bearer <jwt>` header

JWT configuration (defaults):
- **Algorithm:** HS256
- **Expiration:** 86400000 ms (24h)
- **Configurable via:** `escuela.app.jwtExpirationMs`

---

## 4. Multi-Tenant Architecture

Every request (except test endpoints) should include:

```
X-Tenant-ID: 1
```

Flow:
1. `TenantInterceptor` extracts `X-Tenant-ID` from the request header and stores it in `TenantContext` (ThreadLocal)
2. `TenantFilterAspect` (AOP) enables a Hibernate `@Filter` named `"tenantFilter"` before any `*Repository` method execution, appending `WHERE tenant_id = :tenantId` to all queries

Current entities with tenant isolation:
- `Schedule` (`schedules` table) — already has `@FilterDef` + `@Filter` annotations
- `User` (`users` table) — has `tenant_id` FK but no Hibernate filter annotation yet
- `Tenant` (`tenants` table) — the root entity

---

## 5. Planned Endpoints (Architecture Doc)

From `Informe_Tecnico_Arquitectura_Educativa_v2.txt`, these modules still need implementation:

| Module | Prefix | Planned Endpoints |
|--------|--------|-------------------|
| **Schedule** | `/api/schedules` | CRUD schedules, cancel class, collision validation |
| **Attendance** | `/api/attendance` | Batch attendance recording, status modification |
| **AccessControl** | `/api/access-control` | Turnstile event ingestion (`POST /event`) |
| **Notification** | *(event-driven)* | Webhook receiver, SMTP + WhatsApp adapters |
| **Tenant (Admin)** | `/api/admin/tenants` | Tenant CRUD (branding, domain prefix) |

---

## 6. Error Handling

**`400 Bad Request`** — Validation errors, invalid tenant header
```json
{
  "timestamp": "2026-05-29T12:00:00",
  "status": 400,
  "error": "Bad Request",
  "message": "Invalid Tenant ID format."
}
```

**`401 Unauthorized`** — Missing or invalid JWT
```
WWW-Authenticate: Bearer
```

**`403 Forbidden`** — Valid JWT but insufficient role

---

## 7. Data Model (Entities)

| Entity | Table | PK | Tenant Isolation |
|--------|-------|----|------------------|
| `Tenant` | `tenants` | `id (Long)` | Root |
| `Role` | `roles` | `id (Integer)` | Global (shared) |
| `User` | `users` | `id (Long)` | `tenant_id` FK |
| `Schedule` | `schedules` | `id (Long)` | Hibernate `@Filter` |

Full schema in `docs/schema.sql`.
