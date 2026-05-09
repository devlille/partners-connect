---
name: clean-architecture
description: 'Clean architecture package organisation for Kotlin/Ktor modules. Use when creating new domain modules, adding classes to existing modules, or deciding where to place interfaces, models, repository implementations, routes, tables, entities, Koin bindings, mappers, or test files. Covers domain, application, and infrastructure layers.'
---

# Clean Architecture — Package Organisation

## Overview

Every domain module follows a strict 3-layer package structure under `application/src/main/kotlin/fr/devlille/partners/connect/<domain>/`. The layers enforce a dependency rule: **domain ← application ← infrastructure**.

```
<domain>/
├── domain/              # Contracts & models (no implementations)
├── application/         # Repository implementations & mappers
└── infrastructure/
    ├── api/             # HTTP routes & API-level DTOs
    ├── db/              # Exposed tables & entities
    └── bindings/        # Koin DI modules
```

Tests mirror the structure under `application/src/test/kotlin/fr/devlille/partners/connect/<domain>/`.

---

## Layer 1 — `domain/`

**Purpose**: Define the business contract. Contains ONLY interfaces, models, DTOs, and enums. Zero implementations.

### What belongs here

| Kind | Naming | Example |
|------|--------|---------|
| Repository interface | `<Name>Repository.kt` | `CompanyRepository.kt`, `PackRepository.kt` |
| Domain model | `<Name>.kt` | `Company.kt`, `SponsoringPack.kt`, `Partnership.kt` |
| Request/input DTO | `Create<Name>.kt`, `Update<Name>.kt` | `CreateCompany.kt`, `UpdateCompany.kt` |
| Response DTO | `<Name>Response.kt`, `<Name>Detail.kt` | `DetailedPartnershipResponse.kt` |
| Enum | `<Name>Status.kt`, `<Name>Type.kt` | `CompanyStatus.kt`, `DeliveryStatus.kt` |
| Value object | descriptive name | `Address.kt`, `Media.kt`, `Social.kt` |

### Rules

- **All models use `@Serializable`** (kotlinx.serialization).
- **JSON field names use `@SerialName("snake_case")`** when they differ from Kotlin property names.
- **Repository interfaces return domain models**, never entities.
- **No Exposed imports** — the domain layer must not know about the ORM.
- **Sealed hierarchies** for polymorphic types use `@JsonClassDiscriminator("type")` with `@SerialName` on subtypes.

### Example — Repository interface

```kotlin
interface CompanyRepository {
    fun listPaginated(query: String?, status: CompanyStatus?, page: Int, pageSize: Int): PaginatedResponse<Company>
    fun getById(id: UUID): Company
    fun createOrUpdate(input: CreateCompany): UUID
    fun update(id: UUID, input: UpdateCompany): Company
    fun softDelete(id: UUID): UUID
}
```

### Example — Domain model

```kotlin
@Serializable
data class Company(
    val id: String,
    val name: String,
    @SerialName("head_office") val headOffice: Address?,
    val siret: String?,
    val status: CompanyStatus,
    val socials: List<Social>,
)
```

### Example — Enum

```kotlin
@Serializable
enum class CompanyStatus {
    @SerialName("active") ACTIVE,
    @SerialName("inactive") INACTIVE,
}
```

---

## Layer 2 — `application/`

**Purpose**: Implement domain interfaces. Contains Exposed-based repository implementations and entity-to-domain mappers.

### What belongs here

| Kind | Naming | Example |
|------|--------|---------|
| Exposed repository impl | `<Name>RepositoryExposed.kt` | `CompanyRepositoryExposed.kt` |
| External service impl | `<Name>Repository<Provider>.kt` | `CompanyMediaRepositoryGoogleCloud.kt` |
| Mapper extensions | `mappers/<Entity>.ext.kt` | `mappers/CompanyEntity.ext.kt` |

### Rules

- **Repository implementations MUST NOT depend on other repositories** — no constructor injection of sibling repositories.
- **All data access happens via Exposed** within `transaction {}` blocks.
- **Throw domain exceptions** (`NotFoundException`, `ConflictException`) — never return null for missing entities.
- **Mappers are extension functions** on Entity classes, placed in `application/mappers/`.

### Example — Repository implementation

```kotlin
class CompanyRepositoryExposed(
    private val geocode: Geocode,  // External service OK, NOT another repository
) : CompanyRepository {
    override fun getById(id: UUID): Company = transaction {
        val entity = CompanyEntity.findById(id)
            ?: throw NotFoundException("Company $id not found")
        entity.toDomain(entity.socials.map(CompanySocialEntity::toDomain))
    }

    override fun createOrUpdate(input: CreateCompany): UUID = transaction {
        CompanyEntity.new {
            name = input.name
            siteUrl = input.siteUrl
            // ...
        }.id.value
    }
}
```

### Example — Mapper extension

```kotlin
// File: application/mappers/CompanyEntity.ext.kt
internal fun CompanyEntity.toDomain(socials: List<Social>): Company = Company(
    id = id.value.toString(),
    name = name,
    headOffice = if (address != null && city != null) Address(address!!, city!!, zipCode!!, country!!) else null,
    status = status,
    socials = socials,
)
```

---

## Layer 3 — `infrastructure/`

**Purpose**: Glue everything together — HTTP routing, database schema, dependency injection, and external gateways.

### 3a. `infrastructure/api/` — HTTP Routes

| Kind | Naming | Example |
|------|--------|---------|
| Route handler | `<Name>Routes.kt` | `CompanyRoutes.kt`, `SponsoringRoutes.kt` |
| API-level DTO | `<Name>Request.kt` / `<Name>Response.kt` | `AssignOrganiserRequest.kt` |
| Parameter extensions | `StringValues.ext.kt` | `call.parameters.companyUUID` |

#### Route rules

- **Routes inject multiple repositories via Koin** `by inject<T>()`.
- **Routes orchestrate cross-cutting concerns** (notifications, billing) — repositories do NOT.
- **Use `call.receive<T>(schema = "name.schema.json")`** for JSON schema validation.
- **Use `call.parameters.eventSlug`** extensions — no manual null checks.
- **Install `AuthorizedOrganisationPlugin`** for org-scoped routes — no manual permission checks.
- **No try-catch** — throw exceptions and let StatusPages handle HTTP mapping.

#### Example — Route handler

```kotlin
fun Route.sponsoringRoutes() {
    publicPackRoutes()
    orgsPackRoutes()
}

private fun Route.publicPackRoutes() {
    val repository by inject<PackRepository>()
    route("/events/{eventSlug}/sponsoring/packs") {
        get {
            val eventSlug = call.parameters.eventSlug
            val lang = call.request.headers["Accept-Language"]
                ?: throw MissingRequestHeaderException("accept-language")
            call.respond(HttpStatusCode.OK, repository.findPacksByEvent(eventSlug, lang))
        }
    }
}

private fun Route.orgsPackRoutes() {
    val repository by inject<PackRepository>()
    route("/orgs/{orgSlug}/events/{eventSlug}/packs") {
        install(AuthorizedOrganisationPlugin)
        post {
            val eventSlug = call.parameters.eventSlug
            val input = call.receive<CreateSponsoringPack>(schema = "create_sponsoring_pack.schema.json")
            val packId = repository.createPack(eventSlug, input)
            call.respond(HttpStatusCode.Created, mapOf("id" to packId.toString()))
        }
    }
}
```

### 3b. `infrastructure/db/` — Database Schema

| Kind | Naming | Example |
|------|--------|---------|
| Table definition | `<Name>Table.kt` (plural OK) | `CompaniesTable.kt`, `SponsoringPacksTable.kt` |
| Entity class | `<Name>Entity.kt` | `CompanyEntity.kt`, `SponsoringPackEntity.kt` |
| Join table | `<Name>Table.kt` (extends `Table`) | `PackOptionsTable.kt` |

#### Table rules

- **Extend `UUIDTable`** (or `Table` for join tables without their own UUID).
- **Use `datetime()`** for timestamps — NEVER `timestamp()`.
- **Use `enumerationByName<EnumType>()`** for enum columns.
- **Use `clientDefault {}`** for `created_at` with `Clock.System.now().toLocalDateTime(TimeZone.UTC)`.
- **Use `reference()`** for foreign keys.
- **Define indexes** in `init {}` block.

#### Entity rules

- **Extend `UUIDEntity`** with `id: EntityID<UUID>` constructor.
- **Companion object**: `companion object : UUIDEntityClass<EntityName>(TableName)`.
- **Property delegation**: `var field by TableName.column`.
- **Relationships**: `referencedOn` (many-to-one), `referrersOn` (one-to-many), `via` (many-to-many).
- **Static query helpers** as companion functions or extension functions on the entity class.

#### Example — Table

```kotlin
object CompaniesTable : UUIDTable("companies") {
    val name = text("name")
    val status = enumerationByName<CompanyStatus>("status", length = 20)
        .default(CompanyStatus.ACTIVE)
    val createdAt = datetime("created_at").clientDefault {
        Clock.System.now().toLocalDateTime(TimeZone.UTC)
    }

    init {
        index(isUnique = false, status)
    }
}
```

#### Example — Entity

```kotlin
class CompanyEntity(id: EntityID<UUID>) : UUIDEntity(id) {
    companion object : UUIDEntityClass<CompanyEntity>(CompaniesTable) {
        fun listByQueryAndStatus(query: String?, status: CompanyStatus?): SizedIterable<CompanyEntity> {
            // Static query helpers live here
        }
    }

    var name by CompaniesTable.name
    var status by CompaniesTable.status
    val socials by CompanySocialEntity referrersOn CompanySocialsTable.companyId
}
```

### 3c. `infrastructure/bindings/` — Koin DI

| Kind | Naming | Example |
|------|--------|---------|
| Koin module | `<Domain>Module.kt` | `CompanyModule.kt`, `SponsoringModule.kt` |

#### Rules

- **Single file per domain** — one `val <domain>Module = module { ... }`.
- **Bind interface to implementation**: `single<CompanyRepository> { CompanyRepositoryExposed(get()) }`.
- **Use `includes()`** for shared infrastructure modules (storage, maps, etc.).

#### Example

```kotlin
val companyModule = module {
    includes(storageModule, mapsModule)
    single<CompanyRepository> { CompanyRepositoryExposed(get()) }
    single<CompanyJobOfferRepository> { CompanyJobOfferRepositoryExposed() }
    single<CompanyMediaRepository> { CompanyMediaRepositoryGoogleCloud(get()) }
}
```

### 3d. Optional subpackages

Some domains have additional infrastructure subpackages:

| Subpackage | When to use | Example |
|------------|-------------|---------|
| `gateways/` | External API adapters | `QontoBillingGateway.kt`, `SlackNotificationGateway.kt` |
| `providers/` | External service providers | `GoogleProvider.kt`, `QontoProvider.kt` |
| `plugins/` | Ktor plugins | `Security.kt` (in auth module) |

---

## Test Layer

Tests live under `application/src/test/kotlin/fr/devlille/partners/connect/<domain>/`.

```
<domain>/  (test root)
├── factories/                        # Test data factories
│   └── <Entity>.factory.kt
├── infrastructure/api/               # Contract tests (HTTP schema)
│   └── <Feature><Resource>Route<Verb>Test.kt
└── <Feature>RoutesTest.kt            # Integration tests (business logic)
```

### Factories (`factories/`)

| Pattern | Naming | Purpose |
|---------|--------|---------|
| DB entity factory | `insertMocked<Entity>()` | Creates entity in DB |
| Domain object factory | `create<Domain>()` | Creates in-memory domain object |

- **File**: `<Entity>.factory.kt`
- **All parameters MUST have defaults**.
- **Unique fields use UUID**: `name = id.toString()`.
- **NO `transaction {}` inside factories** — caller manages the transaction.

### Contract tests (`infrastructure/api/`)

- **Naming**: `<Feature><Resource>Route<Verb>Test` — e.g., `CompanyListRouteGetTest`.
- **Scope**: HTTP schema validation only (status codes, JSON shape, serialization).

### Integration tests (domain root)

- **Naming**: `<Feature>RoutesTest` (plural) — e.g., `PartnershipSpeakersRoutesTest`.
- **Scope**: End-to-end business logic across multiple endpoints.

---

## Quick Decision Guide

> **"Where do I put this class?"**

| You're creating... | Package | Layer |
|---------------------|---------|-------|
| A repository interface | `domain/` | Domain |
| A data class for API request/response | `domain/` | Domain |
| An enum for business state | `domain/` | Domain |
| A repository implementation (Exposed) | `application/` | Application |
| A mapper from Entity to Domain | `application/mappers/` | Application |
| An HTTP route handler | `infrastructure/api/` | Infrastructure |
| An API-specific DTO (not in domain) | `infrastructure/api/` | Infrastructure |
| A database table definition | `infrastructure/db/` | Infrastructure |
| An ORM entity class | `infrastructure/db/` | Infrastructure |
| A Koin DI module | `infrastructure/bindings/` | Infrastructure |
| An external API adapter | `infrastructure/gateways/` | Infrastructure |
| A test data factory | `factories/` (test) | Test |
| A contract test | `infrastructure/api/` (test) | Test |
| An integration test | domain root (test) | Test |

---

## Module Scaffold

When creating a **new domain module**, create this file structure:

```
<domain>/
├── domain/
│   ├── <Name>.kt                     # Domain model (@Serializable data class)
│   ├── <Name>Repository.kt           # Repository interface
│   └── Create<Name>.kt               # Input DTO (if needed)
├── application/
│   ├── <Name>RepositoryExposed.kt    # Exposed implementation
│   └── mappers/
│       └── <Name>Entity.ext.kt       # Entity → Domain mapper
└── infrastructure/
    ├── api/
    │   └── <Name>Routes.kt           # HTTP route handlers
    ├── db/
    │   ├── <Name>sTable.kt           # Table definition (UUIDTable)
    │   └── <Name>Entity.kt           # Entity class (UUIDEntity)
    └── bindings/
        └── <Name>Module.kt           # Koin DI module
```

Test scaffold:
```
<domain>/ (test)
├── factories/
│   └── <Name>.factory.kt             # insertMocked<Name>()
├── infrastructure/api/
│   ├── <Name>RouteGetTest.kt         # Contract tests per verb
│   └── <Name>RoutePostTest.kt
└── <Name>RoutesTest.kt               # Integration test
```
