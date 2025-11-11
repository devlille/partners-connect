# Copilot Instructions for partners-connect

## Repository Overview

**partners-connect** is a partner management platform for developer events, enabling companies to register as sponsors and partners, with billing and agreement management. The application consists of:

- **Backend**: Kotlin/Ktor REST API server (Port 8080)
- **Frontend**: Nuxt.js/Vue.js SPA application (Port 3000) 
- **Database**: PostgreSQL with Exposed ORM
- **Integration**: Google Cloud Storage, Slack notifications, BilletWeb for ticketing, Mailjet for mailing, Qonto for billing

**Repository Size**: ~200 files, primarily Kotlin backend (~14 domain modules) and Vue frontend (~15 components/pages)
**Languages**: Kotlin (JVM 21), TypeScript/Vue.js, Docker
**Build Tools**: Gradle 8.13, pnpm/npm/Node.js
**Testing**: 95+ Kotlin tests, Vitest for frontend, oxlint for linting

## Essential Build & Validation Commands

### Server (Kotlin/Ktor) - `/server` directory

**ALWAYS run these commands from the `/server` directory**

**Dependencies & Setup:**
- Java 21 (Amazon Corretto recommended)
- Gradle 8.13+ (wrapper included)
- PostgreSQL for full functionality (H2 in-memory for tests)

**Core Commands:**
```bash
# Build and run all checks (2-3 minutes)
./gradlew check --no-daemon

# Quick build only
./gradlew build --no-daemon

# Run tests only (95+ tests, ~45 seconds)  
./gradlew test --no-daemon

# Code formatting and linting (essential before commit)
./gradlew ktlintCheck --no-daemon
./gradlew detekt --no-daemon

# Fix formatting automatically
./gradlew ktlintFormat --no-daemon

# Run application locally
./gradlew run --no-daemon
```

**Critical Build Notes:**
- **ALWAYS use `--no-daemon`** flag to avoid timeout issues
- Tests use in-memory H2 database - no external dependencies required
- Build artifacts go to `application/build/` directory
- Shadow JAR created at `application/build/libs/*.jar`

### Frontend (Nuxt.js) - `/front` directory

**ALWAYS run these commands from the `/front` directory**

**Dependencies:**
- Node.js 18+
- pnpm (preferred, lockfile: pnpm-lock.yaml) or npm (fallback: package-lock.json)

**Core Commands:**
```bash
# Install dependencies (4+ minutes first time)
pnpm install  # or npm install

# Development server (http://localhost:3000)
pnpm dev  # or npm run dev

# Production build (~2 minutes, font warnings are normal)
pnpm build  # or npm run build

# Preview production build
pnpm preview  # or npm run preview

# Linting (oxlint - very fast)
pnpm lint  # or npm run lint

# Type checking and prepare
pnpm postinstall  # or npm run postinstall

# Generate API client from server OpenAPI spec
pnpm orval  # or npm run orval

# Run tests (Vitest)
pnpm test  # or npm run test
```

**Critical Frontend Notes:**
- Uses **pnpm** as preferred package manager (with npm as fallback)
- Font loading warnings during build are **normal** (external font APIs)
- API client auto-generated from server OpenAPI spec via **Orval** (`pnpm orval`)
- Lint warnings about unused parameters should use `_` prefix
- Frontend runs on port **3000** in dev, backend on port **8080**
- Build outputs to `.output/` directory
- **Do not commit** `.nuxt/`, `.output/`, `node_modules/`

## CI/CD Pipeline Validation

**GitHub Actions** (`.github/workflows/pr.yaml`):
- Triggers on PR changes to `server/**` paths only
- Runs: `./gradlew check` with Java 21 and Gradle 8.13
- Uploads build reports as artifacts
- **Expected runtime**: 3-4 minutes

**To replicate CI locally:**
```bash
cd server
./gradlew check --no-daemon
```

## Architecture & Project Layout

### Server Architecture (`/server`)

**Main Application**: `application/src/main/kotlin/fr/devlille/partners/connect/`
- **App.kt**: Main entry point, configures Ktor server modules
- **Domain Modules** (14 total):
  - `auth/` - Google OAuth authentication  
  - `billing/` - Invoice/quote generation
  - `companies/` - Company management
  - `events/` - Event management
  - `integrations/` - External service integrations
  - `organisations/` - Organisations handling
  - `notifications/` - Slack notifications  
  - `partnership/` - Core partnership workflows
  - `provider/` - Provider management and integrations
  - `sponsoring/` - Sponsorship packages
  - `tickets/` - Ticket generation
  - `users/` - User permissions
  - `webhooks/` - Webhook handling for external services
  - `internal/` - Shared infrastructure

**Key Configuration:**
- `build.gradle.kts` - Dependencies and plugins
- `gradle/libs.versions.toml` - Version catalog
- `docker-compose.yml` - Local development stack
- `Dockerfile` - Production container build

**Database**: 
- Exposed ORM with `*Table` classes in `infrastructure/db/`
- Migrations handled via SchemaUtils in startup
- Test isolation via transaction rollback

### Frontend Architecture (`/front`)

**Structure:**
- `pages/` - Route-based pages (index, hall, settings)
- `components/` - Reusable Vue components (SponsorTile, MainTitle, Footer)
- `layouts/` - Page layouts (default, minimal)
- `public/` - Static assets and custom CSS
- `server/` - Server-side middleware
- `utils/` - API client (auto-generated via Orval)
- `stores/` - Pinia state management

**Key Configuration:**
- `nuxt.config.ts` - Main Nuxt configuration (i18n, modules, runtime config)
- `orval.config.ts` - API client generation from server OpenAPI spec
- `vitest.config.ts` - Vitest testing configuration
- `oxlint.json` - Linting rules
- `tsconfig.json` - TypeScript configuration

### External Dependencies

**Server Runtime Requirements:**
- PostgreSQL database (configurable via env vars)
- Google Cloud Storage (for file uploads)
- Google OAuth (client ID/secret)
- Optional: Slack webhook for notifications
- Optional: Mailjet for mailing
- Optional: Qonto for invoice and quote generation
- Optional: BilletWeb for ticketing management

**Environment Variables** (see `docker-compose.yml`):
- `EXPOSED_DB_*` - Database connection
- `GOOGLE_CLIENT_*` - OAuth credentials  
- `CRYPTO_*` - Encryption keys

### Key Source Files

**Server Entry Points:**
- `App.kt` - Application bootstrap and routing setup
- `*Routes.kt` files - REST API endpoint definitions
- `*Repository*.kt` - Database access layer
- `*Table.kt` - Database schema definitions

**Frontend Entry Points:**
- `app.vue` - Root application component
- `layouts/default.vue` - Main page layout
- `pages/index.vue` - Homepage component

## Validation Checklist

Before submitting changes, **always run**:

1. **Server validation**:
   ```bash
   cd server
   ./gradlew ktlintCheck detekt test build --no-daemon
   ```

2. **Frontend validation**:
   ```bash
   cd front  
   pnpm install  # if package.json changed
   pnpm lint
   pnpm build
   pnpm test  # if tests exist
   ```

3. **Integration test**: Start both services and verify functionality
   ```bash
   # Terminal 1 - Backend
   cd server && ./gradlew run --no-daemon
   
   # Terminal 2 - Frontend  
   cd front && pnpm dev
   ```

## Common Pitfalls & Solutions

**Server Issues:**
- **Timeout errors**: Always use `--no-daemon` flag
- **Test failures**: Check database schema migrations
- **Lint failures**: Run `./gradlew ktlintFormat` to auto-fix

**Frontend Issues:**  
- **Build font warnings**: Normal, ignore external font API failures
- **Type errors**: Run `npm run postinstall` to regenerate types
- **Lint errors**: Use `_` prefix for unused parameters

**Docker Issues:**
- Local development uses `docker-compose.yml` 
- Production uses multi-stage `Dockerfile`
- Database initialization via Exposed schema creation

## File Locations Quick Reference

**Critical Config Files:**
- `server/build.gradle.kts` - Server dependencies 
- `server/gradle/libs.versions.toml` - Version management
- `front/package.json` - Frontend dependencies
- `front/nuxt.config.ts` - Nuxt configuration
- `.github/workflows/pr.yaml` - CI pipeline

**Source Code:**
- Server: `server/application/src/main/kotlin/fr/devlille/partners/connect/`
- Frontend: `front/pages/`, `front/components/`, `front/layouts/`
- Tests: `server/application/src/test/kotlin/`

**Trust these instructions** - they are validated and complete. Only search for additional information if you encounter specific errors not covered here.
