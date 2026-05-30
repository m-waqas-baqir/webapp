# Real Investments — property & rental management

A full-stack application for **Real Investments** consultancy: manage **plots**, **rental properties**, **owners**, and **listings**, with JWT authentication and role-based access control.

## Project overview

The system helps operations staff and leadership track inventory: land **plots** (with phase, khayaban, pricing, and status) and **rental properties** (residential/commercial, rent, status). **Owners** link to records where applicable. **Directors** can review aggregated reports; **admins** maintain master data and records; **agents** work day-to-day within permitted scopes.

### Main modules


| Module             | Description                                                                                                                              |
| ------------------ | ---------------------------------------------------------------------------------------------------------------------------------------- |
| **Dashboard**      | Role-aware home: live counts (plots, rentals, owners, users, activity) for staff; agent focus on **My Properties** and scoped inventory. |
| **Plots**          | CRUD for plot inventory; filters by phase, khayaban, status, price/size ranges; Excel import/export; property images.                    |
| **Rentals**        | CRUD for rental inventory; filters by type, status, rent range; property images.                                                         |
| **Owners**         | Owner registry linked to the domain model.                                                                                               |
| **Activity logs**  | Paginated audit trail (create/update/delete) — **DIRECTOR** / **ADMIN**.                                                                 |
| **Reports**        | Director listing–owner report (`GET /api/v1/reports/listings/owners`) — **DIRECTOR** only.                                               |
| **Users**          | User management (create/update, permissions) — **DIRECTOR** / **ADMIN**.                                                                 |
| **My Properties**  | Agent hub linking to scoped plots and rentals.                                                                                           |
| **Training hub**   | `/app/demo` (and `/app/demo/:role`): guided, step-based onboarding (no extra APIs); URL role must match the signed-in user.              |
| **Marketing home** | Public landing with **Leaflet** map (DHA Phase 8 boundary + office marker).                                                              |


---

## Tech stack


| Layer        | Technology                                                                                                 |
| ------------ | ---------------------------------------------------------------------------------------------------------- |
| **Backend**  | Java 17, Spring Boot 3.4, Spring Security (JWT), Spring Data JPA, Spring Cache, springdoc-openapi          |
| **Frontend** | Angular 19, Angular Material, RxJS                                                                         |
| **Database** | Default dev: **H2** (in-memory, PostgreSQL-compatible mode). Production: **PostgreSQL** (driver included). |


---

## Development setup

### Prerequisites

- JDK 17+
- Maven 3.9+
- Node.js 20+ and npm (for Angular)

### Backend

```bash
cd backend
mvn spring-boot:run
```

The API listens on **[http://localhost:8080](http://localhost:8080)**.

### Frontend

```bash
cd frontend
npm install
npm start
```

The app is served at **[http://localhost:4200](http://localhost:4200)** and expects the API at `http://localhost:8080` (see `frontend/src/environments/environment.ts`).

**E2E / alternate ports (optional):**


| Mode            | Angular | API                  | When                                                  |
| --------------- | ------- | -------------------- | ----------------------------------------------------- |
| Default dev     | `:4200` | Spring `:8080`       | `npm start`                                           |
| Mock E2E        | `:4201` | Mock server `:18080` | `ng serve --configuration e2e` + Playwright mock tier |
| Integration E2E | `:4202` | Spring `:8080`       | `ng serve --port 4202` + Playwright integration tier  |


Details, npm scripts, env vars (`E2E_REAL_EMAIL`, `E2E_REAL_PASSWORD`, `E2E_SUITE`, Zod contracts, mock server): see **[frontend/tests/e2e/README.md](frontend/tests/e2e/README.md)**.

### Environment variables (backend)

Configure via environment variables or `application.yml` / profile-specific files. Common overrides:


| Variable / property                                                                 | Purpose                                                            |
| ----------------------------------------------------------------------------------- | ------------------------------------------------------------------ |
| `SPRING_DATASOURCE_URL`, `SPRING_DATASOURCE_USERNAME`, `SPRING_DATASOURCE_PASSWORD` | Database connection (e.g. PostgreSQL in production).               |
| `APP_SECURITY_JWT_SECRET`                                                           | HS256 secret (**minimum 32 characters**).                          |
| `APP_SECURITY_JWT_EXPIRATION_SECONDS`                                               | Access token lifetime.                                             |
| `APP_CORS_ALLOWED_ORIGINS`                                                          | Comma-separated origins for browser clients.                       |
| `APP_STORAGE_ROOT`                                                                  | Directory for uploaded property images (default `./data/uploads`). |
| `APP_SEED_ENABLED`                                                                  | Set `false` to disable demo user seed on startup.                  |


Multipart limits for uploads are set under `spring.servlet.multipart` and `app.storage` in `application.yml`.

### Database setup

- **Local (default):** No extra steps; H2 starts with the application.
- **PostgreSQL:** Point `spring.datasource.`* at your instance and ensure `ddl-auto` / migrations match your deployment policy (`update` is convenient for dev; use Flyway/Liquibase for strict prod).

### Running locally (quick)

1. Start backend: `mvn -f backend spring-boot:run`
2. Start frontend: `cd frontend && npm start`
3. Open [http://localhost:4200](http://localhost:4200) and sign in (see **Test credentials** below if seed is enabled).

---

## Production setup

### Build the Angular app

```bash
cd frontend
npm ci
ng build --configuration=production
```

Static output is under `frontend/dist/<project-name>/` (see `angular.json` for the exact folder). Deploy these files to your CDN or web server (e.g. Nginx `root`).

### Build and run the backend JAR

```bash
cd backend
mvn -Pprod -DskipTests package   # adjust profile if you add one
java -jar target/spring-backend-starter-0.0.1-SNAPSHOT.jar
```

Pass production config via environment variables or `application-prod.yml` on the classpath.

### Nginx (example)

- Serve the Angular `dist` as static files.
- Proxy API requests to Spring Boot, e.g. `location /api/ { proxy_pass http://127.0.0.1:8080; }` and keep auth login at `/auth` if you expose it on the same host.
- Align **CORS** (`app.cors.allowed-origins`) with your public site origin.
- For same-origin production builds, `environment.prod.ts` uses `apiBaseUrl: ''` so the browser calls `/api/...` on the same host; ensure Nginx forwards those to the backend.

### Environment variables in production

Set at least: strong **JWT secret**, real **datasource**, **CORS** origins, **storage path** for uploads (persistent volume), and disable **H2** / dev-only features. Use `APP_SEED_ENABLED=false` unless you intentionally seed demo users.

### Run as a systemd service (optional)

Create a unit file that sets `Environment=` / `EnvironmentFile=` for secrets, `WorkingDirectory=`, and `ExecStart=/usr/bin/java -jar /opt/app/spring-backend-starter.jar`. Use `Restart=on-failure` and log to journald or a log shipper.

---

## API documentation

After starting the backend:

- **Swagger UI:** [http://localhost:8080/swagger-ui.html](http://localhost:8080/swagger-ui.html)  
- **OpenAPI JSON:** [http://localhost:8080/v3/api-docs](http://localhost:8080/v3/api-docs)

Responses follow a standard envelope: `{ "success": boolean, "message": string, "data": ... }` via `ApiResponse`.

Notable endpoints added in the hardened build:

- **Property images:** `/api/v1/property-images` (upload, list by entity, delete, file download)
- **Activity logs (admin):** `GET /api/v1/admin/activity-logs` (paginated; **DIRECTOR** / **ADMIN**)
- **Listing owner report (director):** `GET /api/v1/reports/listings/owners` (**DIRECTOR** only)

---

## Default roles


| Role         | Typical use                                           |
| ------------ | ----------------------------------------------------- |
| **DIRECTOR** | Full oversight; reports; activity logs; broad access. |
| **ADMIN**    | Master data and CRUD; activity logs.                  |
| **AGENT**    | Day-to-day data entry within assigned permissions.    |


Exact route-level rules are defined in services/controllers (JWT + `@PreAuthorize` / role checks where applied).

---

## Test credentials (seed data)

When `app.seed.enabled` is `true` (default), the application creates demo users **only if no users exist yet**:


| Email                 | Password            | Role     |
| --------------------- | ------------------- | -------- |
| `director@seed.local` | `ChangeMeSeed#2026` | DIRECTOR |
| `admin@seed.local`    | `ChangeMeSeed#2026` | ADMIN    |
| `agent@seed.local`    | `ChangeMeSeed#2026` | AGENT    |


**Change these immediately** in any shared or production environment. Disable seed with `app.seed.enabled=false` once you use real accounts.

---

## Architecture (high level)

```
┌──────────────┐     HTTPS / JSON      ┌─────────────────────┐
│   Browser    │ ◄──────────────────► │  Angular SPA        │
│  (Material)  │   JWT Bearer          │  (routes, guards)   │
└──────────────┘                       └──────────┬──────────┘
                                                  │ /api, /auth
                                                  ▼
                                       ┌─────────────────────┐
                                       │  Spring Boot        │
                                       │  Security + JWT     │
                                       │  REST controllers   │
                                       │  Services + Cache   │
                                       │  JPA repositories   │
                                       └──────────┬──────────┘
                                                  │
                    ┌─────────────────────────────┼─────────────────────────────┐
                    ▼                             ▼                             ▼
            ┌──────────────┐              ┌──────────────┐              ┌──────────────┐
            │ PostgreSQL / │              │ File system  │              │ ActivityLog  │
            │ H2           │              │ (images)     │              │ table        │
            └──────────────┘              └──────────────┘              └──────────────┘
```

---

## End-to-end tests (Playwright)

From `frontend/`:

- **Mock tier (CI default):** `npm run test:e2e:mock` — Angular **:4201** + **Zod**-validated mock API (`npx tsx tests/helpers/e2e-mock-api.ts` on **:18080**).
- **Integration tier:** `npm run test:e2e:integration` — Angular **:4202** + real Spring Boot; requires `E2E_REAL_EMAIL` and `E2E_REAL_PASSWORD` (global setup fails fast if missing).

Configs enforce **suite isolation** (mock vs integration must not be mixed). Full layout, helpers, and CI notes: [frontend/tests/e2e/README.md](frontend/tests/e2e/README.md).

---

## Repository layout

```
spring-backend-starter/
├── backend/          # Spring Boot application
├── frontend/         # Angular SPA (workspace, Playwright under frontend/tests/)
└── README.md         # This file
```

---

## License / support

Internal or project-specific; adjust this section for your organization.