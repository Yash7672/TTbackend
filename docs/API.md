# FIXORA — API reference

The backend is a Spring Boot REST API. In the Docker stack it is reached **through the
Nginx proxy** on the frontend origin:

| Where | Base URL |
|---|---|
| Through Nginx (what the browser uses) | `http://localhost:3000/api` |
| Directly to the backend container | `http://localhost:8080/api` |

Every response is JSON. Errors use a single shape:

```json
{
  "timestamp": "2026-10-09T07:39:26Z",
  "status": 400,
  "error": "Bad Request",
  "message": "Booking date cannot be in the past",
  "path": "/api/bookings",
  "validationErrors": { "bookingDate": "Booking date cannot be in the past" }
}
```

## Authentication (development only)

There is **no Spring Security, no JWT and no password hashing** in this version. Login exists
only so the role-based flows can be demonstrated.

- `POST /api/auth/login` verifies the email + password pair against the plain-text value stored
  in the database and returns the user plus a message. It does **not** issue a token.
- Protected endpoints read the caller identity from the **`X-User-Id`** request header.
- `@CurrentUserId(adminOnly = true)` endpoints additionally require that user to have the
  `ADMIN` role, otherwise the API returns `403`.

> This is **not a security boundary**. Anyone can send any `X-User-Id`. See README §23.

### Seeded accounts

| Role | Email | Password | Id (fresh seed) |
|---|---|---|---|
| Admin | `admin@fixora.local` | `Admin@123` | 1 |
| Customer | `customer@fixora.local` | `Customer@123` | 2 |
| Provider | `provider@fixora.local` | `Provider@123` | 3 |
| Provider | `provider2@fixora.local` | `Provider@123` | 4 |
| Provider | `provider3@fixora.local` | `Provider@123` | 5 |
| Provider | `provider4@fixora.local` | `Provider@123` | 6 |

Ids are only guaranteed on a **fresh** database. Always read the id from the login response.

## Endpoint summary

### Health

| Method | Path | Notes |
|---|---|---|
| GET | `/api/health` | Liveness + database connectivity. Used by the Docker healthcheck. |

### Auth

| Method | Path | Notes |
|---|---|---|
| POST | `/api/auth/register` | `201`. `ADMIN` can never be self-registered. |
| POST | `/api/auth/login` | Plain-text comparison, no token. |

### AI — works with no API key

All AI endpoints return a `mode` field: `fallback` (deterministic, default), `anthropic`, or
`rules`. With no key configured the deterministic engine answers from real catalogue data.

| Method | Path | Request body |
|---|---|---|
| GET | `/api/ai/status` | — |
| POST | `/api/ai/chat` | `{ message, city? }` |
| POST | `/api/ai/search` | `{ query, city?, maxPrice? }` |
| POST | `/api/ai/recommendations` | `{ query?, city?, maxPrice?, limit? }` |
| POST | `/api/ai/booking-assistance` | `{ message, serviceId?, packageId? }` |
| POST | `/api/ai/generate-service-description` | `{ serviceName, categoryName?, packageNames?, includedWork?, excludedWork?, pricingType? }` |
| POST | `/api/ai/classify-complaint` | `{ subject, description, bookingRef? }` |

### Catalogue (public)

| Method | Path | Notes |
|---|---|---|
| GET | `/api/categories` | Active categories + service counts |
| GET | `/api/categories/{id}` | One category |
| GET | `/api/categories/{id}/services` | `page`, `size` |
| GET | `/api/services` | `categoryId`, `keyword`, `minPrice`, `maxPrice`, `city`, `page`, `size`, `sort` |
| GET | `/api/services/search` | Alias of `/api/services` |
| GET | `/api/services/featured` | Featured services |
| GET | `/api/services/popular?limit=8` | Popular services |
| GET | `/api/services/{id}` | One service |
| GET | `/api/services/{id}/packages` | Priced packages |
| GET | `/api/services/{id}/reviews` | `page`, `size` |
| GET | `/api/services/{id}/rating` | Aggregates recomputed from real reviews |

### Providers (public discovery + own dashboard)

| Method | Path | Auth | Notes |
|---|---|---|---|
| GET | `/api/providers` | — | Approved providers only |
| GET | `/api/providers/search` | — | `city`, `area`, `keyword`, `serviceId`, `page`, `size` |
| GET | `/api/providers/{id}` | — | One provider |
| GET | `/api/providers/{id}/services` | — | Offerings |
| GET | `/api/providers/{id}/availability` | — | Weekly windows |
| GET | `/api/providers/{id}/reviews` | — | `page`, `size` |
| GET | `/api/providers/me` | user | My provider profile |
| POST | `/api/providers` | user | Create profile (`201`) |
| PUT | `/api/providers/me` | user | Update profile |
| GET | `/api/providers/me/services` | user | My offerings |
| POST | `/api/providers/me/services` | user | Add offering (`201`) |
| DELETE | `/api/providers/me/services/{providerServiceId}` | user | Remove offering (`204`) |
| GET | `/api/providers/me/availability` | user | My availability |
| PUT | `/api/providers/me/availability` | user | Replace (JSON array) |
| GET | `/api/providers/me/earnings` | user | Estimate from real bookings |

### Users and addresses

| Method | Path | Auth | Notes |
|---|---|---|---|
| GET | `/api/users/me` | user | Current profile |
| PUT | `/api/users/me` | user | `{ fullName, phone?, city? }` |
| GET | `/api/addresses` | user | My address book |
| POST | `/api/addresses` | user | `201` |
| PUT | `/api/addresses/{id}` | user | Update |
| DELETE | `/api/addresses/{id}` | user | `204` |
| PUT | `/api/addresses/{id}/default` | user | Set default |

### Bookings, reviews, complaints, notifications

| Method | Path | Auth | Notes |
|---|---|---|---|
| POST | `/api/bookings` | user | Price computed server-side; no price field accepted |
| GET | `/api/bookings/me` | user | `status`, `page`, `size` |
| GET | `/api/bookings/{id}` | user | Customer / assigned provider / admin |
| PUT | `/api/bookings/{id}/cancel` | user | Optional `{ note }` |
| PUT | `/api/bookings/{id}/accept` | provider | `PENDING → ACCEPTED` |
| PUT | `/api/bookings/{id}/reject` | provider | `PENDING → REJECTED` |
| PUT | `/api/bookings/{id}/start` | provider | `ACCEPTED → IN_PROGRESS` |
| PUT | `/api/bookings/{id}/complete` | provider | `IN_PROGRESS → COMPLETED` |
| GET | `/api/provider/bookings` | provider | Provider queue |
| POST | `/api/reviews` | user | Only own `COMPLETED` booking; recomputes aggregates |
| POST | `/api/complaints` | user | `201`; category optional (AI may suggest) |
| GET | `/api/complaints/me` | user | My complaints |
| GET | `/api/notifications` | user | In-app only, `page`, `size` |
| GET | `/api/notifications/recent` | user | Recent |
| GET | `/api/notifications/unread-count` | user | `{ unread }` |
| PUT | `/api/notifications/{id}/read` | user | Mark read |

### Admin (`X-User-Id` must be an `ADMIN`, else `403`)

| Method | Path | Notes |
|---|---|---|
| GET | `/api/admin/dashboard` | Live counts |
| GET | `/api/admin/ai/status` | AI provider status |
| GET | `/api/admin/whoami` | Echo of the resolved admin id |
| GET | `/api/admin/users` | `role`, `page`, `size` |
| GET | `/api/admin/providers` | `status`, `page`, `size` |
| PUT | `/api/admin/providers/{id}/status` | Approve / reject / suspend |
| GET | `/api/admin/bookings` | `status`, `page`, `size` |
| PUT | `/api/admin/bookings/{id}/assign?providerId=` | Assign a provider |
| GET | `/api/admin/services` | Includes inactive |
| POST | `/api/admin/categories` | `201` |
| PUT | `/api/admin/categories/{id}` | Update |
| DELETE | `/api/admin/categories/{id}` | `204` |
| POST | `/api/admin/services` | `201` |
| PUT | `/api/admin/services/{id}` | Update |
| DELETE | `/api/admin/services/{id}` | `204` |
| POST | `/api/admin/packages` | `201` |
| PUT | `/api/admin/packages/{id}` | Update |
| DELETE | `/api/admin/packages/{id}` | `204` |
| GET | `/api/admin/complaints` | `status`, `page`, `size` |
| PUT | `/api/admin/complaints/{id}/status` | Final triage decision |
| GET | `/api/admin/reviews` | `page`, `size` |
| PUT | `/api/admin/reviews/{id}/visibility?visible=` | Show / hide |

## Postman collection

Import `postman/Fixora.postman_collection.json`. It covers every endpoint above and:

- stores `baseUrl = http://localhost:3000/api`
- runs API-key auth that sends `X-User-Id` automatically:
  - `{{authUserId}}` (customer) by default,
  - `{{adminUserId}}` for the **Admin** folder,
  - `{{providerUserId}}` for provider-only requests
- captures created ids (`categoryId`, `serviceId`, `packageId`, `addressId`, `bookingId`,
  `providerId`, `complaintId`, `notificationId`) from the responses into collection variables.

Run **Auth → Login (customer)**, **Login (admin)** and **Login (provider)** first so the id
variables are populated from the real seed.

## Quick curl examples

```bash
# health (proves the DB connection too)
curl http://localhost:3000/api/health

# categories
curl http://localhost:3000/api/categories

# search
curl "http://localhost:3000/api/services?keyword=cleaning&maxPrice=1000"

# login, then call a protected endpoint (id comes from the login response)
curl -X POST http://localhost:3000/api/auth/login \
  -H "Content-Type: application/json" \
  -d '{"email":"customer@fixora.local","password":"Customer@123"}'

curl http://localhost:3000/api/bookings/me -H "X-User-Id: 2"

# AI works with no key configured
curl -X POST http://localhost:3000/api/ai/chat \
  -H "Content-Type: application/json" \
  -d '{"message":"my bathroom tap is leaking","city":"Hyderabad"}'
```
