# Server - AI Agent Instructions

## Overview

This document provides server-specific instructions for the partners-connect Kotlin/Ktor backend application. For general project instructions, see the root `AGENTS.md` file.

## Technology Stack

- **Language**: Kotlin 1.9.x with JVM 21 (Amazon Corretto)
- **Framework**: Ktor 2.x
- **ORM**: Exposed 0.41+
- **Serialization**: kotlinx.serialization
- **Dependency Injection**: Koin
- **Database**: PostgreSQL (H2 in-memory for tests)
- **External APIs**: Mailjet v3.1, Qonto, BilletWeb, Slack

## Essential Build Commands

**CRITICAL**: All commands must be run from the `/server` directory with the `--no-daemon` flag.

```bash
# Full validation (2-3 minutes)
./gradlew check --no-daemon

# Quick build only
./gradlew build --no-daemon

# Run tests (95+ tests, ~45 seconds)
./gradlew test --no-daemon

# Code formatting and linting (essential before commit)
./gradlew ktlintCheck --no-daemon
./gradlew detekt --no-daemon

# Fix formatting automatically
./gradlew ktlintFormat --no-daemon

# Run application locally (Port 8080)
./gradlew run --no-daemon
```

## Domain Architecture

The application follows **Clean Architecture** with 14 domain modules:

### Core Domains
- **`auth/`** - Google OAuth authentication
- **`billing/`** - Invoice and quote generation (Qonto integration)
- **`companies/`** - Company management and profiles
- **`events/`** - Event management and configuration
- **`organisations/`** - Organisation handling and permissions
- **`partnership/`** - Core partnership workflows and approvals
- **`sponsoring/`** - Sponsorship packages and options
- **`users/`** - User permissions and roles

### Supporting Domains
- **`integrations/`** - External service integrations
- **`notifications/`** - Slack notifications
- **`provider/`** - Provider management and integrations
- **`tickets/`** - Ticket generation (BilletWeb integration)
- **`webhooks/`** - External webhook handling
- **`internal/`** - Shared infrastructure and utilities

### Module Structure

Each domain follows this structure:
```
<domain>/
├── domain/                    # Core business logic
├── application/              # Use cases and services
├── infrastructure/
│   ├── api/                  # REST API routes and DTOs
│   │   └── *Routes.kt       # Route definitions
│   ├── db/                   # Database layer
│   │   ├── *Table.kt        # Table schemas
│   │   ├── *Entity.kt       # Entity mappings
│   │   └── *Repository*.kt  # Repository implementations
│   └── bindings/            # Koin DI modules
└── factories/                # Test data factories (in test/)
```

## Testing Architecture

> **CRITICAL**: See [TESTING.md](docs/TESTING.md) for comprehensive testing guide.

### Test Types

1. **Contract Tests (Unit Tests)**
   - **Location**: `<feature>.infrastructure.api` package
   - **Naming**: `<Feature><EndpointResource>Route<Verb>Test`
   - **Purpose**: Validate HTTP contract (request/response schemas)
   - **Example**: `PartnershipRegisterRoutePostTest`

2. **Integration Tests**
   - **Location**: `<feature>` package (root of domain)
   - **Naming**: `<Feature>(<EndpointResource>)RoutesTest` (plural)
   - **Purpose**: Validate end-to-end business logic
   - **Example**: `PartnershipSpeakersRoutesTest`

### Shared Database Pattern (MANDATORY)

All tests use a shared in-memory H2 database:

```kotlin
import fr.devlille.partners.connect.internal.moduleSharedDb

@Test
fun `test name`() = testApplication {
    val userId = UUID.randomUUID()
    val orgId = UUID.randomUUID()
    val eventId = UUID.randomUUID()
    
    application {
        moduleSharedDb(userId = userId)
        transaction {
            // Initialize all test data in a SINGLE transaction
            insertMockedOrganisationEntity(orgId)
            insertMockedFutureEvent(eventId, orgId = orgId)
            insertMockedCompany(companyId)
        }
    }
    
    // Execute test assertions
}
```

### Factory Functions (CRITICAL RULES)

- **Naming**: `insertMocked<Entity>()` for database entities, `create<Domain>()` for domain objects
- **File naming**: `<Name>.factory.kt` (e.g., `Company.factory.kt`)
- **Location**: `<feature>/factories/` package
- **All parameters MUST have defaults**
- **Unique fields MUST use UUID-based defaults**: `name = id.toString()`
- **NO transaction management in factories**
- **Single responsibility**: Each factory does one specific thing

Example factory:
```kotlin
fun insertMockedCompany(
    id: UUID = UUID.randomUUID(),
    name: String = id.toString(), // Unique default using UUID
    address: String = "123 Mock St",
    city: String = "Mock City",
    status: CompanyStatus = CompanyStatus.ACTIVE,
): CompanyEntity = CompanyEntity.new(id) {
    this.name = name
    this.address = address
    this.city = city
    this.status = status
}
```

### Schema Files (Required for all endpoints)

- **Location**: `application/src/main/resources/schemas/{name}.schema.json`
- **Format**: OpenAPI 3.1.0 compatible (use union types, NOT `nullable: true`)
- **Usage**: Reference in `openapi.yaml` components, then use in route operations
- **Validation**: Use `call.receive<T>(schema)` pattern in routes

## Database Patterns (CRITICAL)

### Table Definitions

**MUST** extend `UUIDTable`:
```kotlin
object CompaniesTable : UUIDTable() {
    val name = varchar("name", 255)
    val createdAt = datetime("created_at")  // NEVER use timestamp()
        .clientDefault { Clock.System.now().toLocalDateTime(TimeZone.UTC) }
    val status = enumerationByName<CompanyStatus>("status", 50)
}
```

**Critical Rules**:
- Always extend `UUIDTable`
- Use `datetime()` for timestamps (NEVER `timestamp()`)
- Use `enumerationByName<EnumType>()` for enums

### Entity Definitions

**MUST** extend `UUIDEntity`:
```kotlin
class CompanyEntity(id: EntityID<UUID>) : UUIDEntity(id) {
    companion object : UUIDEntityClass<CompanyEntity>(CompaniesTable)
    
    var name by CompaniesTable.name
    var createdAt by CompaniesTable.createdAt
    var status by CompaniesTable.status
}
```

**Critical Rules**:
- Extend `UUIDEntity`
- Companion object pattern: `UUIDEntityClass<EntityType>(TableName)`
- Property delegation: `var field by TableName.field`

## API Implementation Standards (CRITICAL)

### Route Implementation

```kotlin
route("/orgs/{orgSlug}/events/{eventSlug}/partnerships") {
    // Authorization handled by plugin (NO manual checks)
    install(AuthorizedOrganisationPlugin)
    
    post("/{id}/approve") {
        // Parameter extraction (NO manual null checks)
        val id = call.parameters.id
        
        // Business logic
        val partnership = partnershipRepository.approve(id)
        val event = eventRepository.findById(partnership.eventId)
        
        // Notifications (in route layer, NOT repository)
        val variables = NotificationVariables.PartnershipApproved(...)
        notificationRepository.sendMessage(event.orgSlug, variables)
        
        // Response
        call.respond(HttpStatusCode.OK, partnership)
    }
}
```

### Critical Patterns

1. **JSON Schema Validation**: Use `call.receive<T>(schema)` pattern
2. **Authorization**: Use `AuthorizedOrganisationPlugin` (NO manual permission checks)
3. **Parameter Extraction**: Use `call.parameters.eventSlug` extensions (NO manual null checks)
4. **Exception Handling**: Throw domain exceptions, let StatusPages handle HTTP mapping (NO try-catch in routes)
5. **Notifications**: Send in route layer, NEVER in repositories

### Repository Architecture (NON-NEGOTIABLE)

**Repository implementations MUST NOT depend on other repositories**

```kotlin
// ❌ BAD: Repository depends on another repository
class PartnershipRepository(
    private val notificationRepository: NotificationRepository  // WRONG!
) {
    fun approve(id: UUID) {
        // ...
        notificationRepository.sendMessage()  // WRONG! Notifications in routes only
    }
}

// ✅ GOOD: Repository only handles data, route orchestrates
class PartnershipRepository {
    fun approve(id: UUID): Partnership {
        // Only data operations
        return partnership
    }
}

// Route orchestrates cross-cutting concerns
post("/{id}/approve") {
    val partnership = partnershipRepository.approve(id)
    val event = eventRepository.findById(partnership.eventId)
    notificationRepository.sendMessage(event.orgSlug, variables)
    call.respond(HttpStatusCode.OK, partnership)
}
```

**Pattern**: Routes inject multiple repositories, fetch data separately, then orchestrate notifications.

## Error Handling

### Domain Exceptions

Define domain-specific exceptions:
```kotlin
class UnauthorizedException(message: String) : Exception(message)
class ConflictException(message: String) : Exception(message)
class NotFoundException(message: String) : Exception(message)
```

### StatusPages Configuration

Exceptions are automatically mapped to HTTP status codes in `App.kt`:
```kotlin
install(StatusPages) {
    exception<UnauthorizedException> { call, cause ->
        call.respond(HttpStatusCode.Unauthorized, cause.message ?: "Unauthorized")
    }
    exception<ConflictException> { call, cause ->
        call.respond(HttpStatusCode.Conflict, cause.message ?: "Conflict")
    }
}
```

**NO try-catch blocks in routes** - throw exceptions and let StatusPages handle them.

## Key Configuration Files

### Build Configuration
- **`build.gradle.kts`** - Dependencies, plugins, Shadow JAR configuration
- **`gradle/libs.versions.toml`** - Version catalog (centralized version management)
- **`gradle.properties`** - Gradle settings

### Application Configuration
- **`App.kt`** - Main entry point, Ktor module configuration
- **`docker-compose.yml`** - Local development stack (PostgreSQL, etc.)
- **`Dockerfile`** - Production container build (multi-stage)
- **`application/src/main/resources/openapi.yaml`** - OpenAPI specification

### Development Files
- **`package.json`** - NPM scripts for OpenAPI validation
- **`.editorconfig`** - Code style configuration
- **`detekt.yml`** - Static analysis rules

## OpenAPI Specification

### Validation

```bash
cd server
npm install
npm run validate  # Validates OpenAPI schema
```

### Key Files
- **`application/src/main/resources/openapi.yaml`** - Main OpenAPI spec
- **`application/src/main/resources/schemas/*.schema.json`** - JSON schemas for validation

### Usage in Routes

```kotlin
import io.ktor.server.request.receive

val schema = Schema.from(this::class.java.getResource("/schemas/partnership.schema.json"))

post("/partnerships") {
    val request = call.receive<PartnershipRequest>(schema)  // Auto-validates
    // ...
}
```

## Quality Gates (NON-NEGOTIABLE)

Before any commit, **MUST** pass:

1. **ktlint**: Zero formatting violations
   ```bash
   ./gradlew ktlintCheck --no-daemon
   ./gradlew ktlintFormat --no-daemon  # Auto-fix
   ```

2. **detekt**: Zero static analysis violations
   ```bash
   ./gradlew detekt --no-daemon
   ```

3. **Tests**: All tests passing, minimum 80% coverage
   ```bash
   ./gradlew test --no-daemon
   ```

4. **OpenAPI Validation**: Schema must be valid
   ```bash
   npm run validate
   ```

5. **Build**: Full build must succeed
   ```bash
   ./gradlew build --no-daemon
   ```

## CI/CD Integration

### GitHub Actions

**PR Validation** (`.github/workflows/pr.yaml`):
- Triggers on `server/**` changes only
- Runs: Java 21 + Gradle 8.13
- Executes: `./gradlew check` (includes all quality gates)
- Expected runtime: 3-4 minutes

**To replicate CI locally**:
```bash
cd server
npm install && npm run validate
./gradlew check --no-daemon
```

### Container Build

**Development**:
```bash
docker-compose up  # Starts PostgreSQL + app
```

**Production**:
```bash
docker build -t partners-connect-server .
```

Build artifacts: `application/build/libs/*.jar`

## Common Issues & Solutions

### Timeout Errors
**Symptom**: Gradle daemon timeout
**Solution**: Always use `--no-daemon` flag

### Database Schema Issues
**Symptom**: Test failures related to schema
**Solution**: Check `MigrationRegistry` and table definitions

### Lint Failures
**Symptom**: ktlint or detekt errors
**Solution**: Run `./gradlew ktlintFormat --no-daemon` to auto-fix

### Test Data Conflicts
**Symptom**: Constraint violations in tests
**Solution**: Use UUID-based defaults in factories: `name = id.toString()`

### Transaction Issues
**Symptom**: "Transaction already active" error
**Solution**: Remove transaction management from factory functions

## Environment Variables

See `docker-compose.yml` for complete list:

### Database
- `EXPOSED_DB_HOST` - Database host
- `EXPOSED_DB_PORT` - Database port
- `EXPOSED_DB_NAME` - Database name
- `EXPOSED_DB_USER` - Database username
- `EXPOSED_DB_PASSWORD` - Database password

### Authentication
- `GOOGLE_CLIENT_ID` - Google OAuth client ID
- `GOOGLE_CLIENT_SECRET` - Google OAuth client secret

### Security
- `CRYPTO_SECRET_KEY` - Encryption key for secrets
- `CRYPTO_ENCRYPTION_KEY` - Secondary encryption key

### External Services
- `SLACK_WEBHOOK_URL` - Slack webhook for notifications
- `MAILJET_API_KEY` - Mailjet API key
- `MAILJET_SECRET_KEY` - Mailjet secret key
- `QONTO_SECRET_KEY` - Qonto API key
- `BILLETWEB_API_KEY` - BilletWeb API key

## Development Patterns

### Authorization Pattern
```kotlin
route("/orgs/{orgSlug}/events/{eventSlug}/resource") {
    install(AuthorizedOrganisationPlugin)  // Handles all permission checking
    post { /* No manual auth needed */ }
}
```

### Repository Pattern
```kotlin
class CompanyRepository {
    fun findById(id: UUID): CompanyEntity? {
        return CompanyEntity.findById(id)
    }
    
    fun create(company: Company): CompanyEntity {
        return CompanyEntity.new(UUID.randomUUID()) {
            name = company.name
            address = company.address
        }
    }
}
```

### Route Pattern
```kotlin
post("/partnerships") {
    val id = call.parameters.id  // Extension handles null check
    val partnership = partnershipRepository.approve(id)
    call.respond(HttpStatusCode.OK, partnership)
}
```

## Key Source Files

### Entry Points
- **`App.kt`** - Application bootstrap and routing setup
- **`*Routes.kt`** files - REST API endpoint definitions

### Data Layer
- **`*Table.kt`** - Database schema definitions (Exposed DSL)
- **`*Entity.kt`** - Entity mappings (ORM)
- **`*Repository*.kt`** - Database access layer

### Business Logic
- **`domain/models/*.kt`** - Domain entities
- **`domain/exceptions/*.kt`** - Domain exceptions
- **`application/*.kt`** - Use cases and services

## Migration Notes

The codebase uses Exposed ORM with automatic schema creation via `MigrationRegistry`. For migration strategy details, see `docs/MIGRATIONS.md`.

## Additional Resources

- **Testing Guide**: See `docs/TESTING.md` for comprehensive testing documentation
- **Deployment Guide**: See `docs/DEPLOYMENT.md` for production deployment
- **General Instructions**: See root `AGENTS.md` for project-wide guidance
- **OpenAPI Spec**: Available at `http://localhost:8080/swagger` when running locally

---

**Trust these instructions** - they are validated, complete, and reflect the current codebase architecture. Only search for additional information if you encounter specific errors not covered here.
