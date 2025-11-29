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
**Key Integrations**: Orval for API client generation, Koin for DI, Exposed ORM, Pinia for state management

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

**Server Quality Gates (NON-NEGOTIABLE):**
- **ALWAYS use `--no-daemon`** flag to avoid timeout issues
- **ktlint + detekt**: MUST pass with zero violations before any commit
- **OpenAPI validation**: Run `npm run validate` for schema compliance  
- **Testing**: Contract tests for API schemas + Integration tests for business logic

## CI/CD Pipeline Validation

**GitHub Actions**:
- **PR validation** (`.github/workflows/pr.yaml`): Triggers on `server/**` changes only
- **Server CI** (`.github/workflows/ci-server.yaml`): Builds and pushes container images
- **Frontend CI** (`.github/workflows/ci-front.yaml`): Frontend-specific pipeline
- Runs: `./gradlew check` with Java 21 and Gradle 8.13, includes OpenAPI validation
- **Expected runtime**: 3-4 minutes

**To replicate CI locally:**
```bash
cd server
npm install && npm run validate  # OpenAPI validation
./gradlew check --no-daemon      # Full server validation
```

## Testing Strategy (CRITICAL)

**Contract Tests** (API Schema Validation):
- **MUST be written BEFORE implementation** (TDD approach)
- Focus on request/response schema validation ONLY, not business logic
- Use `call.receive<T>(schema)` pattern with JSON schemas in `/schemas/` directory
- Use mock factory functions for entities: `insertMockedCompany()`, `insertMockedEvent()`, `insertMockedPartnership()`
- Create new factories for missing entities following existing naming conventions

**Integration Tests** (Business Logic):
- HTTP route testing with H2 in-memory database (NOT repository tests)
- End-to-end validation including serialization, validation, error handling
- Cross-domain operations, notifications, complex workflows
- **Minimum 80% coverage** for new features

**Schema Files** (Required for all new endpoints):
- Create in `server/application/src/main/resources/schemas/{name}.schema.json`
- OpenAPI 3.1.0 compatible (use union types, not `nullable: true`)
- Reference in `openapi.yaml` components, then use in route operations

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
- **API client sync**: Run `pnpm orval` after backend OpenAPI changes

**Docker Issues:**
- Local development uses `docker-compose.yml` 
- Production uses multi-stage `Dockerfile`
- Database initialization via Exposed schema creation

## Project-Specific Patterns

**Server Domain Architecture:**
Each domain module follows Clean Architecture structure:
- `domain/` - Core business logic and entities
- `application/` - Use cases and application services  
- `infrastructure/api/` - REST API routes and DTOs
- `infrastructure/db/` - Database tables and repositories
- `infrastructure/bindings/` - Koin dependency injection modules

**Key Architectural Decisions:**
- **Modular design**: 15 domain modules with clear boundaries (`auth/`, `billing/`, `companies/`, etc.)
- **Dependency Injection**: Koin-based DI configured in `App.kt` 
- **Database**: Exposed DSL with automatic migrations via `MigrationRegistry`
- **API-First**: OpenAPI spec at `/swagger` drives frontend client generation
- **Authentication**: Google OAuth with cookie-based sessions (`UserSession`)

**Frontend Type Safety:**
- **API client auto-generation**: Backend OpenAPI spec → Orval → `utils/api.ts`
- **Custom Axios instance**: `custom-instance.ts` handles auth tokens and error interceptors
- **Runtime config**: Environment-specific API URLs via `nuxt.config.ts.runtimeConfig`

**Error Handling Patterns:**
- Backend: Custom exceptions (`UnauthorizedException`, `ConflictException`) mapped to HTTP status codes
- Frontend: Axios interceptors handle 401 redirects and token refresh
- Testing: H2 in-memory DB with transaction rollback for isolation

**Repository Architecture (NON-NEGOTIABLE):**
- **Repository implementations MUST NOT depend on other repositories**
- **Notification sending happens in route layer, NEVER in repositories**
- **Repositories return data; routes orchestrate cross-cutting concerns**
- Pattern: Routes inject multiple repositories, fetch data separately, then orchestrate notifications

**Database Patterns (CRITICAL):**
- **Tables**: MUST extend `UUIDTable`, use `datetime()` (NEVER `timestamp()`), use `enumerationByName<EnumType>()`
- **Entities**: MUST extend `UUIDEntity`, companion object pattern, property delegation via `by TableName.columnName`
- **Dual structure**: Table objects define schema, Entity classes provide ORM mapping

**API Implementation Standards (CRITICAL):**
- **JSON Schema Validation**: Use `call.receive<T>(schema)` pattern for automatic validation
- **Authorization**: Use `AuthorizedOrganisationPlugin` for org-protected routes (NO manual permission checks)
- **Parameter Extraction**: Use `call.parameters.eventSlug` extensions (NO manual null checks)
- **Exception Handling**: Throw domain exceptions, let StatusPages handle HTTP mapping (NO try-catch in routes)

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

## Development Patterns Reference

**Route Implementation Example** (Partnership approval):
```kotlin
post("/{id}/approve") {
    val id = call.parameters.id
    val partnership = partnershipRepository.approve(id)
    val event = eventRepository.findById(partnership.eventId)
    val variables = NotificationVariables.PartnershipApproved(...)
    notificationRepository.sendMessage(event.orgSlug, variables)
    call.respond(HttpStatusCode.OK, partnership)
}
```

**Database Entity Pattern**:
```kotlin
// Table definition
object CompaniesTable : UUIDTable() {
    val name = varchar("name", 255)
    val createdAt = datetime("created_at").clientDefault { Clock.System.now().toLocalDateTime(TimeZone.UTC) }
}

// Entity class  
class CompanyEntity(id: EntityID<UUID>) : UUIDEntity(id) {
    companion object : UUIDEntityClass<CompanyEntity>(CompaniesTable)
    var name by CompaniesTable.name
    var createdAt by CompaniesTable.createdAt
}
```

**Authorization Pattern**:
```kotlin
route("/orgs/{orgSlug}/events/{eventSlug}/resource") {
    install(AuthorizedOrganisationPlugin)  // Handles all permission checking
    post { /* No manual auth needed */ }
}
```

**Trust these instructions** - they are validated and complete. Only search for additional information if you encounter specific errors not covered here.

## Active Technologies
- Kotlin (JVM 21) with Ktor framework + Ktor 2.x, Exposed ORM, Koin (DI), kotlinx.serialization (012-sync-pack-options)
- PostgreSQL database with Exposed ORM, H2 in-memory for tests (012-sync-pack-options)
- Kotlin with JVM 21 (Amazon Corretto) + Ktor 2.x, Exposed ORM, Koin (DI), kotlinx.serialization (013-partnership-option-display)
- PostgreSQL with Exposed ORM (H2 in-memory for tests) (013-partnership-option-display)
- Kotlin 1.9.x with JVM 21 (Amazon Corretto) + Ktor 2.x, Exposed ORM 0.41+, kotlinx.serialization, Koin (DI) (013-update-partnership)
- PostgreSQL database (H2 in-memory for tests) (013-update-partnership)

## Recent Changes
- 012-sync-pack-options: Added Kotlin (JVM 21) with Ktor framework + Ktor 2.x, Exposed ORM, Koin (DI), kotlinx.serialization
