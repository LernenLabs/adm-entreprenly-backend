# Entreprenly Platform - Web Services

RESTful API backend for the Entreprenly application, built with **Domain-Driven Design (DDD)** and **CQRS**.

## Tech Stack

- **Language:** Java 26
- **Framework:** Spring Boot 4.0.6
- **Database:** PostgreSQL 15+ (Google Cloud SQL in production)
- **Build:** Maven (via the included Maven Wrapper)
- **Security:** Spring Security with JWT (BCrypt password hashing)
- **Documentation:** SpringDoc OpenAPI (Swagger UI)

## Architecture

Bounded contexts under `src/main/java/online/entreprenly/platform`:

| Context | Responsibility |
|---|---|
| `iam` | Identity & Access Management: users, roles, authentication, JWT |
| `inventory` | Products and lots (unit / weight), stock alerts |
| `sales` | Sales, cash registers |
| `subscription` | Plans, billing, payment confirmation |
| `profile` | User profile, preferences, notification settings |
| `chatbot` | WhatsApp sessions, conversations, chat messages and orders |
| `shared` | Domain bases, persistence, OpenAPI, i18n, error handling |

Each context is layered as `domain` / `application` / `infrastructure` / `interfaces`.

## Getting Started

### Prerequisites

- Java 26 JDK
- PostgreSQL 15+ (a local instance for development)
- Docker (optional, for containerized runs)

### Configuration

Copy `.env.example` to `.env` (or export the variables) and adjust the values.
The development profile defaults to a local PostgreSQL instance. Create the
`daop-entreprenly` database first (PostgreSQL does not auto-create it), e.g.:

```bash
createdb daop-entreprenly
```

### Run

```bash
# Development (default profile, local PostgreSQL)
./mvnw spring-boot:run

# Build a runnable jar
./mvnw clean package

# Run with Docker (prod profile)
docker build -t entreprenly-platform .
docker run --env-file .env -p 8092:8092 entreprenly-platform
```

The API is served under `/api/v1` and Swagger UI is available at
`http://localhost:8092/swagger-ui.html`.

## Deployment

### Option A: Render + Supabase (free tier, no Google Cloud)

1. **Supabase**: create a project, then open *Connect* and copy the **Session pooler**
   details (host `aws-0-<region>.pooler.supabase.com`, port `5432`, user `postgres.<project-ref>`,
   database `postgres`). The tables are created automatically on first start.
2. **Render**: create a *Web Service* from this GitHub repository, runtime **Docker**, and set
   these environment variables:

   | Variable | Value |
   |---|---|
   | `SPRING_PROFILES_ACTIVE` | `cloud` |
   | `DATABASE_HOST` | Supabase pooler host |
   | `DATABASE_PORT` | `5432` |
   | `DATABASE_NAME` | `postgres` |
   | `DATABASE_USER` | `postgres.<project-ref>` |
   | `DATABASE_PASSWORD` | your Supabase database password |
   | `JWT_SECRET` | a long random string |
   | `PUBLIC_API_URL` | the Render URL (shown in Swagger UI) |

3. Open `https://<service>.onrender.com/swagger-ui.html` to verify.

The free Render tier sleeps after ~15 minutes without traffic; the first request afterwards can
take about a minute, so open Swagger UI before a demo.

### Option B: Google Cloud Run + Cloud SQL

The service can be deployed to **Google Cloud Run**. A Cloud Build trigger builds the
container from the `Dockerfile` and rolls out a new revision automatically on every
push to `main` (no manual steps required).

In production the app connects to **Cloud SQL (PostgreSQL)** through the Cloud SQL Java
socket factory, so no public IP or host/port is configured. The Cloud Run service
is set up with:

- Environment variables: `SPRING_PROFILES_ACTIVE=prod`, `CLOUD_SQL_CONNECTION_NAME`
  (`project:region:instance`), `DATABASE_NAME`, `DATABASE_USER`, `DATABASE_PASSWORD`,
  `JWT_SECRET`.
- The Cloud SQL instance attached to the service, and the runtime service account
  granted the `roles/cloudsql.client` role.

## License

See [LICENSE](LICENSE).
