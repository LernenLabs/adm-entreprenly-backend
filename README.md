# Entreprenly Platform - Web Services

RESTful API backend for the Entreprenly application, built with **Domain-Driven Design (DDD)** and **CQRS**.

## Tech Stack

- **Language:** Java 26
- **Framework:** Spring Boot 4.0.6
- **Database:** PostgreSQL 15+ (Supabase in production)
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

# Run with Docker (see the Deployment section for the profile and variables)
docker build -t entreprenly-platform .
docker run --env-file .env -p 8092:8092 entreprenly-platform
```

The API is served under `/api/v1` and Swagger UI is available at
`http://localhost:8092/swagger-ui.html`.

## Deployment

The backend runs on **Render** (Docker web service) and stores its data in **Supabase**
(managed PostgreSQL).

| | |
|---|---|
| API | https://adm-entreprenly-backend.onrender.com |
| Swagger UI | https://adm-entreprenly-backend.onrender.com/swagger-ui.html |
| Deployed branch | `develop` |
| Spring profile | `cloud` (see `application-cloud.properties`) |

### How a deploy happens

Every push to `develop` runs the **Deploy to Render** GitHub Actions workflow
(`.github/workflows/render-deploy.yml`), which calls the service's Render Deploy Hook. Render then
builds the `Dockerfile` and replaces the running instance. The hook URL is stored in the repository
secret `RENDER_DEPLOY_HOOK`. A deploy can also be started by hand from the Render dashboard
(*Manual Deploy*).

The tables are created automatically on startup (`spring.jpa.hibernate.ddl-auto=update`), so there
are no manual migrations.

### Setting it up from scratch

1. **Supabase**: create a project, then open *Connect* and copy the **Session pooler**
   details (host `aws-0-<region>.pooler.supabase.com`, port `5432`, user `postgres.<project-ref>`,
   database `postgres`). Use the pooler, not the direct connection: Render's free tier only has IPv4.
2. **Render**: create a *Web Service* from this GitHub repository (branch `develop`, runtime
   **Docker**) and set these environment variables:

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

3. **GitHub**: copy the service's *Deploy Hook* URL (Render > Settings) into a repository secret
   named `RENDER_DEPLOY_HOOK` (Settings > Secrets and variables > Actions).
4. Open `/swagger-ui.html` on the service URL to verify.

### Free tier notes

- Render's free instance sleeps after ~15 minutes without traffic; the first request afterwards
  can take about a minute. Open Swagger UI before a demo to wake it up.
- During a deploy the API is unavailable for a few minutes.
- Never commit database passwords, `JWT_SECRET` or the deploy hook URL.

### Alternative: Google Cloud Run + Cloud SQL (not in use)

The `prod` profile connects to **Cloud SQL** through the Cloud SQL Java socket factory and is meant
for Google Cloud Run (`SPRING_PROFILES_ACTIVE=prod`, `CLOUD_SQL_CONNECTION_NAME`, `DATABASE_NAME`,
`DATABASE_USER`, `DATABASE_PASSWORD`, `JWT_SECRET`). It is kept for reference but the project is
currently deployed on Render + Supabase.

## License

See [LICENSE](LICENSE).
