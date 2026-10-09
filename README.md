# FIXORA — Backend

The backend half of **Fixora**, a Smart Local Services Marketplace. This repository
contains the Spring Boot REST API and the MySQL setup. The React frontend lives in the
companion repository ([TTfrontend](https://github.com/Yash7672/TTfrontend)).

```text
controller → service (+ service.ai) → repository → entity → MySQL
```

The booking workflow enforces its rules on the server: prices are calculated from the
package in the database, provider approval and availability are verified, and overlapping
bookings are rejected. Every API response is a DTO — JPA entities are never serialised.

> **Honest scope note:** this version deliberately has **no Spring Security, no JWT and no
> password hashing**. Protected endpoints read the `X-User-Id` header (development only).
> See [Security limitations](#security-limitations) before deploying anywhere.

---

## Tech stack

| Layer | Technology |
|---|---|
| Language | Java 17 |
| Framework | Spring Boot 3.5.16 (Web, Data JPA, Validation, Hibernate) |
| Database | MySQL 8.0 (H2 in-memory for tests only) |
| Build | Maven 3.9 |
| Other | Lombok, Jakarta Validation, `BigDecimal` money |
| Tests | JUnit 5, Mockito, Spring Boot Test, MockMvc, H2 |
| Docs | [`docs/API.md`](docs/API.md) + [`postman/Fixora.postman_collection.json`](postman/Fixora.postman_collection.json) |

---

## Quick start (Docker — recommended)

Requires Docker Desktop (or Docker Engine + Compose v2). No local Java, Maven or MySQL needed.

```powershell
# first time only
Copy-Item .env.example .env

# build and start MySQL + the backend
docker compose up --build
```

- API base: **http://localhost:8080/api**
- Health (proves the DB connection): **http://localhost:8080/api/health**

Run in the background with `docker compose up --build -d`, follow logs with
`docker compose logs -f backend`.

The backend waits for the MySQL **healthcheck** before starting, creates the schema
(`ddl-auto=update`) and seeds the demo catalogue once.

---

## Seeded development accounts

Created on first start (only when the database has no categories yet):

| Role | Email | Password |
|---|---|---|
| Administrator | `admin@fixora.local` | `Admin@123` |
| Provider (approved) | `provider@fixora.local` | `Provider@123` |
| Provider (approved) | `provider2@fixora.local` | `Provider@123` |
| Provider (approved) | `provider3@fixora.local` | `Provider@123` |
| Provider (approved) | `provider4@fixora.local` | `Provider@123` |
| Customer | `customer@fixora.local` | `Customer@123` |

Seed contents: 14 categories, 96 services, ~260 packages, 4 approved providers with
offerings and availability, 2 addresses, 4 bookings in different states, 1 complaint and
2 notifications. Demo city: **Hyderabad**. All prices are demo data.

---

## Environment configuration

Copy `.env.example` to `.env` and adjust as needed. Every variable has a working default.
`.env` is git-ignored and must never be committed.

| Variable | Default | Meaning |
|---|---|---|
| `MYSQL_DATABASE` | `fixora_db` | Database created in the container |
| `MYSQL_USER` | `yashwanth` | Application database user |
| `MYSQL_PASSWORD` | `root` | Password for that user |
| `MYSQL_ROOT_PASSWORD` | `root` | MySQL root password |
| `MYSQL_PORT` | `3307` | Host port for MySQL (container port is always 3306) |
| `BACKEND_PORT` | `8080` | Host port for the backend |
| `SEED_ENABLED` | `true` | Load the demo catalogue and demo accounts on first start |
| `AI_PROVIDER` | `fallback` | `fallback` (deterministic) or `anthropic` |
| `ANTHROPIC_API_KEY` | *(empty)* | Backend-only key. Empty means fallback mode |
| `AI_MODEL` | *(empty)* | Model id; defaults to a current documented Claude model |
| `AI_TIMEOUT_SECONDS` | `20` | Timeout before falling back to the deterministic engine |

Database credentials are read from `SPRING_DATASOURCE_USERNAME` /
`SPRING_DATASOURCE_PASSWORD` (or the `application.properties` defaults), so no real
secret is hardcoded in the source.

### AI (optional)

Fixora talks to AI **only from the backend**; the browser never sees a key. With
`AI_PROVIDER=fallback` (or `anthropic` without a key, or on any API failure/timeout) a
deterministic, catalogue-driven engine answers. Responses carry a `mode` field
(`fallback`, `anthropic` or `rules`) so the client can state which path produced an answer.
**Fallback mode is not an LLM and never claims to be.**

To use Anthropic:

```dotenv
AI_PROVIDER=anthropic
ANTHROPIC_API_KEY=sk-ant-...      # your own key
AI_MODEL=claude-sonnet-4-5        # optional
```

then `docker compose up -d --build backend` and check `GET /api/ai/status`.

---

## Connect to a managed MySQL (Aiven)

The datasource is fully environment-driven and nothing is hardcoded. Point the backend
at Aiven with three variables (see `.env.example`):

| Variable | Example value |
|---|---|
| `SPRING_DATASOURCE_URL` | `jdbc:mysql://<host>:<port>/<database>?sslMode=VERIFY_CA&serverTimezone=UTC&trustCertificateKeyStoreUrl=file:/path/aiven-truststore.p12&trustCertificateKeyStoreType=PKCS12&trustCertificateKeyStorePassword=<store-pass>` |
| `SPRING_DATASOURCE_USERNAME` | `avnadmin` |
| `SPRING_DATASOURCE_PASSWORD` | *(your Aiven password)* |

### 1. Build a Java truststore from the Aiven CA (recommended)

Aiven requires TLS. Save the project CA from the Aiven console as `certs/aiven-ca.pem`,
then build a PKCS12 truststore that the JDBC driver can use:

```bash
keytool -importcert -alias aiven-mysql-ca \
  -file certs/aiven-ca.pem \
  -keystore certs/aiven-truststore.p12 \
  -storetype PKCS12 -storepass changeit -noprompt
```

`certs/` and every `*.pem` / `*.p12` / `*.jks` file are git-ignored — the CA and
truststore must never be committed. If you would rather not verify the CA, use
`sslMode=REQUIRED` in the URL and drop the `trustCertificateKeyStore*` parameters; TLS is
still enforced either way.

### 2. Run against Aiven

Put the three variables in `backend/.env` (git-ignored) and start the app — Spring reads
that file automatically for plain host runs:

```bash
mvn spring-boot:run
# or, after `mvn -DskipTests package`:
java -jar target/fixora-backend.jar
```

For Docker, inject the variables and mount the truststore:

```bash
docker build -t fixora-backend .
docker run --rm -p 8080:8080 \
  -e SPRING_DATASOURCE_URL="jdbc:mysql://<host>:<port>/<database>?sslMode=VERIFY_CA&serverTimezone=UTC&trustCertificateKeyStoreUrl=file:/certs/aiven-truststore.p12&trustCertificateKeyStoreType=PKCS12&trustCertificateKeyStorePassword=changeit" \
  -e SPRING_DATASOURCE_USERNAME=avnadmin \
  -e SPRING_DATASOURCE_PASSWORD='<your-password>' \
  -v "$PWD/certs:/certs:ro" \
  fixora-backend
```

Confirm it with `GET http://localhost:8080/api/health` — `"database": "UP"` proves the
connection.

The first run creates the Fixora tables (`ddl-auto=update`, additive only — it never drops
or recreates the database) and, unless `FIXORA_SEED_ENABLED=false`, loads the demo
catalogue. Existing tables and rows are preserved.

---

## Run locally without Docker

Requires JDK 17, Maven 3.9 and a MySQL 8 instance matching `application.properties`
(or set `SPRING_DATASOURCE_URL`, `SPRING_DATASOURCE_USERNAME`,
`SPRING_DATASOURCE_PASSWORD`).

```bash
mvn spring-boot:run
```

## Run the automated tests

44 tests (services, booking rules, reviews, complaints, AI fallback, context). They run
against an H2 in-memory database, so MySQL does not need to be running.

```bash
mvn test
```

---

## Project structure

```text
backend/
├── Dockerfile                # multi-stage: Maven build → JRE run
├── pom.xml
├── docker-compose.yml        # backend + MySQL (standalone)
├── .env.example
├── database/
│   ├── init/01-charset.sql   # mounted into the MySQL container
│   └── README.md
├── docs/API.md               # endpoint reference + auth + curl examples
├── postman/                  # importable Postman collection
└── src/
    ├── main/java/com/fixora/
    │   ├── FixoraApplication.java
    │   ├── config/           # web config, AI properties, seed data, current-user resolver
    │   ├── controller/       # 13 REST controllers
    │   ├── dto/request/      # 21 request DTOs (records + Jakarta Validation)
    │   ├── dto/response/     # 23 response DTOs (records)
    │   ├── entity/           # 14 JPA entities
    │   ├── enums/            # 10 enums
    │   ├── exception/        # 8 exceptions + @RestControllerAdvice
    │   ├── mapper/           # 9 entity → DTO mappers (batch aware)
    │   ├── repository/       # 14 Spring Data repositories
    │   ├── service/          # 10 service interfaces + NotificationService
    │   ├── service/impl/     # 9 service implementations
    │   ├── service/ai/       # AI provider abstraction + deterministic engine
    │   ├── util/             # slug + paging helpers
    │   └── validation/       # @CurrentUserId annotation
    ├── main/resources/application.properties
    └── test/                 # 44 automated tests (JUnit 5 + Mockito + H2)
```

---

## API overview

All endpoints are under `/api`. Full reference: [`docs/API.md`](docs/API.md).

| Area | Highlights |
|---|---|
| Health | `GET /api/health` — status + `select 1` against MySQL |
| Auth / users | `POST /api/auth/register`, `POST /api/auth/login`, `GET/PUT /api/users/me` |
| Catalogue | `/api/categories`, `/api/services` (keyword, price, city, sort, paging), `/api/services/search` |
| Providers | `/api/providers`, `/api/providers/me*`, availability, earnings |
| Addresses | `/api/addresses` CRUD + `PUT /api/addresses/{id}/default` |
| Bookings | `POST /api/bookings` (no price accepted), accept/reject/start/complete/cancel |
| Reviews | `POST /api/reviews` — completed booking only, one per booking |
| Complaints | `POST /api/complaints` (advisory AI classification), `GET /api/complaints/me` |
| Notifications | list, recent, unread count, mark read |
| AI | chat, search, recommendations, booking assistance, description drafts, complaint triage, status |
| Admin | dashboard, users, provider verification, catalogue CRUD, oversight, triage, moderation |

### How identity works (development only)

Protected endpoints read the `X-User-Id` request header. Admin endpoints additionally
require the user to have the `ADMIN` role. This is **not** secure authentication.

---

## Security limitations

1. **No real authentication** — no Spring Security, JWT or session; the API trusts `X-User-Id`.
2. **No password hashing** — passwords are stored as plain text. Never reuse a real password.
3. **Frontend role checks are not a boundary** — the backend still re-checks roles and
   ownership, but not against a forged identity header.
4. **Development credentials** — remove the seeded accounts before a real deployment.
5. **No payment integration** — bookings record a `paymentStatus` only.
6. **Notifications are in-app only** — no email/SMS/push provider.
7. **`ddl-auto=update`** — development convenience; production should use Flyway/Liquibase.

The code is structured so real authentication can be added later without reshaping the
domains: replace the `@CurrentUserId` resolver with JWT/session auth and add BCrypt.

---

## Stopping and data

```bash
docker compose stop            # stop containers, keep everything
docker compose down            # remove containers, KEEP the fixora_mysql_data volume
docker compose down -v         # ⚠️ DESTRUCTIVE: also deletes all data
```

Back up the database:

```bash
docker compose exec mysql mysqldump -u root -proot fixora_db > fixora-backup.sql
```

---

## Frontend

The React app is in [TTfrontend](https://github.com/Yash7672/TTfrontend). In production
Nginx serves it and proxies `/api/` to this backend on the shared `fixora-net` Docker
network, so the browser only ever uses one origin.

Built as a learning project. Prices are demo data and no payment is processed.
