# Partners Connect - AI Agent Instructions

## Repository Overview

**partners-connect** is a partner management platform for developer events, enabling companies to register as sponsors and partners, with billing and agreement management.

### Architecture Components

- **Backend**: Kotlin/Ktor REST API server (Port 8080)
- **Frontend**: Nuxt.js/Vue.js SPA application (Port 3000)
- **Database**: PostgreSQL with Exposed ORM
- **Integration**: Google Cloud Storage, Slack notifications, BilletWeb for ticketing, Mailjet for mailing, Qonto for billing

### Repository Statistics

- **Size**: ~200 files
- **Languages**: Kotlin (JVM 21), TypeScript/Vue.js, Docker
- **Build Tools**: Gradle 8.13, pnpm/npm/Node.js
- **Testing**: 95+ Kotlin tests, Vitest for frontend, oxlint for linting
- **Key Technologies**: Orval for API client generation, Koin for DI, Exposed ORM, Pinia for state management

## Quick Start Commands

### Server (Kotlin/Ktor) - `/server` directory

**ALWAYS run these commands from the `/server` directory**

```bash
# Full validation (2-3 minutes)
./gradlew check --no-daemon

# Run tests (95+ tests, ~45 seconds)
./gradlew test --no-daemon

# Code formatting and linting
./gradlew ktlintCheck detekt --no-daemon
./gradlew ktlintFormat --no-daemon

# Run application locally
./gradlew run --no-daemon
```

**Critical**: Always use `--no-daemon` flag to avoid timeout issues.

### Frontend (Nuxt.js) - `/front` directory

**ALWAYS run these commands from the `/front` directory**

```bash
# Install dependencies (4+ minutes first time)
pnpm install  # or npm install

# Development server (http://localhost:3000)
pnpm dev  # or npm run dev

# Production build (~2 minutes, font warnings are normal)
pnpm build  # or npm run build

# Linting
pnpm lint  # or npm run lint

# Generate API client from server OpenAPI spec
pnpm orval  # or npm run orval

# Run tests
pnpm test  # or npm run test
```

**Package Manager**: Uses **pnpm** (preferred) with npm as fallback.

## Technology Stack

### Backend
- Kotlin 1.9.x with JVM 21 (Amazon Corretto)
- Ktor 2.x framework
- Exposed ORM 0.41+
- kotlinx.serialization
- Koin for dependency injection
- PostgreSQL (H2 in-memory for tests)
- Mailjet API v3.1

### Frontend
- Nuxt.js with Vue.js
- TypeScript
- Pinia for state management
- i18n for internationalization
- Orval for API client generation
- Vitest for testing
- oxlint for linting

### Infrastructure
- Docker and Docker Compose
- GitHub Actions for CI/CD
- Google Cloud Platform
- PostgreSQL database

## Project Structure

### Root Level
```
partners-connect/
├── server/           # Kotlin/Ktor backend
├── front/            # Nuxt.js frontend
├── docs/             # General documentation
├── documentation/    # Astro-based documentation site
├── bruno/            # API testing collections
└── .github/          # CI/CD workflows
```

### Server Architecture (`/server`)
```
server/
├── application/src/main/kotlin/fr/devlille/partners/connect/
│   ├── App.kt                    # Main entry point
│   ├── auth/                     # Google OAuth authentication
│   ├── billing/                  # Invoice/quote generation
│   ├── companies/                # Company management
│   ├── events/                   # Event management
│   ├── integrations/             # External service integrations
│   ├── organisations/            # Organisation handling
│   ├── notifications/            # Slack notifications
│   ├── partnership/              # Core partnership workflows
│   ├── provider/                 # Provider management
│   ├── sponsoring/               # Sponsorship packages
│   ├── tickets/                  # Ticket generation
│   ├── users/                    # User permissions
│   ├── webhooks/                 # External webhook handling
│   └── internal/                 # Shared infrastructure
├── build.gradle.kts              # Dependencies and plugins
├── docker-compose.yml            # Local development stack
└── Dockerfile                    # Production container build
```

Each domain module follows Clean Architecture:
- `domain/` - Core business logic and entities
- `application/` - Use cases and application services
- `infrastructure/api/` - REST API routes and DTOs
- `infrastructure/db/` - Database tables and repositories
- `infrastructure/bindings/` - Koin dependency injection modules

### Frontend Architecture (`/front`)
```
front/
├── pages/            # Route-based pages (index, hall, settings)
├── components/       # Reusable Vue components
├── layouts/          # Page layouts (default, minimal)
├── stores/           # Pinia state management
├── utils/            # API client (auto-generated via Orval)
├── public/           # Static assets
├── nuxt.config.ts    # Main Nuxt configuration
├── orval.config.ts   # API client generation config
└── vitest.config.ts  # Testing configuration
```

## Validation Checklist

Before submitting changes, **always run**:

### Server Validation
```bash
cd server
./gradlew ktlintCheck detekt test build --no-daemon
```

### Frontend Validation
```bash
cd front
pnpm install  # if package.json changed
pnpm lint
pnpm build
pnpm test  # if tests exist
```

### Integration Test
Start both services and verify functionality:
```bash
# Terminal 1 - Backend
cd server && ./gradlew run --no-daemon

# Terminal 2 - Frontend
cd front && pnpm dev
```

## CI/CD Pipeline

### GitHub Actions
- **PR validation** (`.github/workflows/pr.yaml`): Triggers on `server/**` changes
- **Server CI** (`.github/workflows/ci-server.yaml`): Builds and pushes container images
- **Frontend CI** (`.github/workflows/ci-front.yaml`): Frontend-specific pipeline
- **Expected runtime**: 3-4 minutes

### Local CI Replication
```bash
cd server
npm install && npm run validate  # OpenAPI validation
./gradlew check --no-daemon      # Full server validation
```

## Common Pitfalls & Solutions

### Server Issues
- **Timeout errors**: Always use `--no-daemon` flag
- **Test failures**: Check database schema migrations
- **Lint failures**: Run `./gradlew ktlintFormat` to auto-fix
- **Build artifacts**: Located in `application/build/` directory

### Frontend Issues
- **Build font warnings**: Normal, ignore external font API failures
- **Type errors**: Run `npm run postinstall` to regenerate types
- **Lint errors**: Use `_` prefix for unused parameters
- **API client sync**: Run `pnpm orval` after backend OpenAPI changes
- **Build outputs**: Located in `.output/` directory

### Docker Issues
- Local development uses `docker-compose.yml`
- Production uses multi-stage `Dockerfile`
- Database initialization via Exposed schema creation

## Environment Configuration

### Server Runtime Requirements
- PostgreSQL database (configurable via env vars)
- Google Cloud Storage (for file uploads)
- Google OAuth (client ID/secret)
- Optional: Slack webhook for notifications
- Optional: Mailjet for mailing
- Optional: Qonto for invoice/quote generation
- Optional: BilletWeb for ticketing management

### Environment Variables
See `server/docker-compose.yml` for complete list:
- `EXPOSED_DB_*` - Database connection
- `GOOGLE_CLIENT_*` - OAuth credentials
- `CRYPTO_*` - Encryption keys

## Key Files Reference

### Critical Configuration Files
- `server/build.gradle.kts` - Server dependencies
- `server/gradle/libs.versions.toml` - Version management
- `front/package.json` - Frontend dependencies
- `front/nuxt.config.ts` - Nuxt configuration
- `.github/workflows/pr.yaml` - CI pipeline

### Source Code Locations
- **Server**: `server/application/src/main/kotlin/fr/devlille/partners/connect/`
- **Frontend**: `front/pages/`, `front/components/`, `front/layouts/`
- **Tests**: `server/application/src/test/kotlin/`

## Quality Gates (NON-NEGOTIABLE)

### Server
- **ktlint + detekt**: MUST pass with zero violations before any commit
- **OpenAPI validation**: Run `npm run validate` for schema compliance
- **Testing**: Contract tests for API schemas + Integration tests for business logic
- **Coverage**: Minimum 80% for new features

### Frontend
- **oxlint**: MUST pass without errors
- **Build**: Production build must complete successfully
- **Type safety**: API client must be in sync with backend (run `pnpm orval`)

## Development Workflow

1. **Start**: Always pull latest changes and verify build passes
2. **Develop**: Make changes in appropriate directory (server/ or front/)
3. **Test**: Run relevant test suite (see commands above)
4. **Lint**: Fix all linting issues before committing
5. **Integration**: Test both services together if making API changes
6. **Commit**: Ensure all quality gates pass

## External Dependencies

- **API Design**: OpenAPI spec at `/swagger` endpoint
- **Type Generation**: Frontend API client auto-generated from server spec
- **Authentication**: Google OAuth with cookie-based sessions
- **File Storage**: Google Cloud Storage
- **Notifications**: Slack webhooks
- **Email**: Mailjet API
- **Billing**: Qonto integration
- **Ticketing**: BilletWeb integration

## Additional Documentation

- **Server-specific**: See `server/AGENTS.md` for detailed server patterns and architecture
- **Testing**: See `server/docs/TESTING.md` for comprehensive testing guide
- **Deployment**: See `docs/DEPLOYMENT.md` for deployment instructions
- **Migrations**: See `docs/MIGRATIONS.md` for database migration strategy

---

**Trust these instructions** - they are validated and complete. Only search for additional information if you encounter specific errors not covered here.
