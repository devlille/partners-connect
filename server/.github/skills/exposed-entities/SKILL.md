---
name: exposed-entities
description: 'Exposed ORM tables, entities, companion object queries, and DSL patterns. Use when creating or modifying database tables, entities, writing queries, defining relationships, pagination, or implementing repository data access. Covers UUIDTable, UUIDEntity, companion query helpers, DSL operators, and transaction management.'
---

# Exposed Tables, Entities & DSL — Usage Guide

## Overview

All database access uses JetBrains Exposed ORM with the DAO (Entity) API. Tables live in `infrastructure/db/`, repository implementations in `application/`. Entity companion objects are the **primary location for reusable query helpers** — they keep queries close to the entity and make them easy to reuse across packages.

---

## 1 — Table Definitions

Tables define the database schema. They live in `<domain>/infrastructure/db/<Name>Table.kt`.

### 1.1 Standard UUIDTable

Most tables extend `UUIDTable`, which provides an auto-generated UUID primary key.

```kotlin
object CompaniesTable : UUIDTable("companies") {
    val name = text("name")
    val siteUrl = text("site_url").nullable()
    val country = varchar("country", 2).nullable()
    val status = enumerationByName<CompanyStatus>("status", length = 20)
        .default(defaultValue = CompanyStatus.ACTIVE)
    val createdAt = datetime("created_at").clientDefault {
        Clock.System.now().toLocalDateTime(TimeZone.UTC)
    }

    init {
        index(isUnique = false, status)
    }
}
```

### 1.2 Column Types

| Exposed function | Kotlin type | When to use |
|---|---|---|
| `text("col")` | `String` | Large/unbounded text |
| `varchar("col", length)` | `String` | Bounded strings |
| `integer("col")` | `Int` | Integer values |
| `bool("col")` | `Boolean` | Boolean flags |
| `datetime("col")` | `LocalDateTime` | Timestamps (**NEVER use `timestamp()`**) |
| `uuid("col")` | `UUID` | Non-PK UUID columns |
| `enumerationByName<E>("col", length)` | `E` | Enum stored as string (**preferred**) |
| `enumeration<E>("col")` | `E` | Enum stored as ordinal (legacy only) |
| `reference("col", OtherTable)` | `EntityID<UUID>` | Required foreign key |

### 1.3 Column Modifiers

| Modifier | Purpose | Example |
|---|---|---|
| `.nullable()` | Optional column | `text("bio").nullable()` |
| `.default(value)` | Static default | `.default(CompanyStatus.ACTIVE)` |
| `.clientDefault { }` | Computed default | `.clientDefault { Clock.System.now().toLocalDateTime(TimeZone.UTC) }` |
| `.uniqueIndex()` | Inline unique index | `varchar("slug", 255).uniqueIndex()` |

### 1.4 Index Definitions

Indexes are declared in an `init {}` block.

```kotlin
init {
    // Non-unique single column
    index(isUnique = false, companyId)

    // Composite index
    index(false, companyId, createdAt)

    // Unique composite index
    uniqueIndex(eventId, providerId)

    // Alternative unique syntax
    index(true, optionId, value)
}
```

### 1.5 Foreign Keys

```kotlin
object PartnershipsTable : UUIDTable("partnerships") {
    val eventId = reference("event_id", EventsTable)                          // required FK
    val companyId = reference("company_id", CompaniesTable)                   // required FK
    val selectedPackId = reference("selected_pack_id", SponsoringPacksTable).nullable()  // optional FK
}
```

### 1.6 Join Tables (Many-to-Many)

Join tables extend `Table` (no UUID PK) and declare a composite `primaryKey`.

```kotlin
object PackOptionsTable : Table("pack_options") {
    val pack = reference("pack_id", SponsoringPacksTable)
    val option = reference("option_id", SponsoringOptionsTable)
    val required = bool("required").default(false)
    override val primaryKey = PrimaryKey(pack, option)
}
```

### 1.7 Custom ID Tables (Non-UUID)

When the primary key is not a UUID:

```kotlin
object PartnershipTicketsTable : IdTable<String>("partnership_tickets") {
    override val id: Column<EntityID<String>> = varchar("ticket_id", length = 50).entityId()
    val partnershipId = reference("partnership_id", PartnershipsTable)
}
```

### Rules

- **ALWAYS** extend `UUIDTable` unless you have a join table (use `Table`) or a custom PK (use `IdTable<T>`).
- **ALWAYS** use `datetime()` for timestamps — **NEVER** `timestamp()`.
- **ALWAYS** use `enumerationByName<E>()` for enum columns (stores the name, not ordinal).
- **ALWAYS** use `clientDefault {}` with `Clock.System.now().toLocalDateTime(TimeZone.UTC)` for `created_at`.
- **ALWAYS** pass the SQL table name to the constructor: `UUIDTable("table_name")`.
- **ALWAYS** define indexes in `init {}` block.

---

## 2 — Entity Definitions

Entities map table rows to Kotlin objects. They live in `<domain>/infrastructure/db/<Name>Entity.kt`.

### 2.1 Standard UUIDEntity

```kotlin
class CompanyEntity(id: EntityID<UUID>) : UUIDEntity(id) {
    companion object : UUIDEntityClass<CompanyEntity>(CompaniesTable)

    var name by CompaniesTable.name
    var siteUrl by CompaniesTable.siteUrl
    var status by CompaniesTable.status
    var createdAt by CompaniesTable.createdAt
}
```

### 2.2 Custom ID Entity

```kotlin
class PartnershipTicketEntity(id: EntityID<String>) : Entity<String>(id) {
    companion object : EntityClass<String, PartnershipTicketEntity>(PartnershipTicketsTable)

    var orderId by PartnershipTicketsTable.orderId
    var firstname by PartnershipTicketsTable.firstname
}
```

### 2.3 Relationship Properties

| Pattern | Cardinality | Syntax |
|---|---|---|
| `referencedOn` | Many-to-one (required FK) | `var event by EventEntity referencedOn PartnershipsTable.eventId` |
| `optionalReferencedOn` | Many-to-one (nullable FK) | `var organiser by UserEntity optionalReferencedOn PartnershipsTable.organiserId` |
| `referrersOn` | One-to-many (reverse) | `val socials by CompanySocialEntity referrersOn CompanySocialsTable.companyId` |
| `via` | Many-to-many (join table) | `val options by SponsoringOptionEntity via PackOptionsTable` |

```kotlin
class PartnershipEntity(id: EntityID<UUID>) : UUIDEntity(id) {
    companion object : UUIDEntityClass<PartnershipEntity>(PartnershipsTable)

    // Required foreign keys
    var event by EventEntity referencedOn PartnershipsTable.eventId
    var company by CompanyEntity referencedOn PartnershipsTable.companyId

    // Optional foreign keys
    var selectedPack by SponsoringPackEntity optionalReferencedOn PartnershipsTable.selectedPackId
    var organiser by UserEntity optionalReferencedOn PartnershipsTable.organiserId

    // Reverse relationships
    val emails by PartnershipEmailEntity referrersOn PartnershipEmailsTable.partnershipId

    // Many-to-many
    val options by SponsoringOptionEntity via PackOptionsTable

    // Scalar columns
    var phone by PartnershipsTable.phone
    var language by PartnershipsTable.language
}
```

### Rules

- **ALWAYS** extend `UUIDEntity` with `id: EntityID<UUID>` constructor parameter.
- **ALWAYS** declare `companion object : UUIDEntityClass<EntityName>(TableName)`.
- Use `var field by TableName.column` for scalar property delegation.
- Use `referencedOn` for **required** FK, `optionalReferencedOn` for **nullable** FK.
- Use `referrersOn` for one-to-many back-references (`val`, not `var` — read-only).
- Use `via` for many-to-many through a join table (`val`, not `var` — read-only).

---

## 3 — Companion Object Query Helpers (CRITICAL)

The entity **companion object** is the preferred location for reusable query methods. This keeps queries close to the entity and makes them easily accessible across packages via `EntityClass.methodName()`.

### 3.1 Extension Functions on UUIDEntityClass (Preferred Pattern)

Define queries as **extension functions** on the companion type. This keeps the companion object clean and allows queries to be organised in separate files if needed.

```kotlin
// Simple lookup
fun UUIDEntityClass<EventEntity>.findBySlug(slug: String): EventEntity? =
    find { EventsTable.slug eq slug }.singleOrNull()

// List by foreign key
fun UUIDEntityClass<SponsoringPackEntity>.listPacksByEvent(eventId: UUID): List<SponsoringPackEntity> =
    find { SponsoringPacksTable.eventId eq eventId }.toList()

// Single with error throwing
fun UUIDEntityClass<SponsoringPackEntity>.singlePackById(eventId: UUID, packId: UUID): SponsoringPackEntity =
    find { (SponsoringPacksTable.id eq packId) and (SponsoringPacksTable.eventId eq eventId) }
        .singleOrNull()
        ?: throw NotFoundException("Pack not found")
```

### 3.2 Companion Methods (For Complex Queries)

For queries with many parameters or complex logic, define them directly in the companion object.

```kotlin
class PartnershipEntity(id: EntityID<UUID>) : UUIDEntity(id) {
    companion object : UUIDEntityClass<PartnershipEntity>(PartnershipsTable) {

        fun filters(
            eventId: UUID,
            packId: UUID?,
            validated: Boolean?,
            organiserUserId: UUID?,
            declined: Boolean = false,
        ): SizedIterable<PartnershipEntity> {
            var op = PartnershipsTable.eventId eq eventId
            if (packId != null) {
                op = op and (PartnershipsTable.selectedPackId eq packId)
            }
            if (validated != null) {
                op = if (validated) {
                    op and (PartnershipsTable.validatedAt.isNotNull())
                } else {
                    op and (PartnershipsTable.validatedAt.isNull())
                }
            }
            if (organiserUserId != null) {
                op = op and (PartnershipsTable.organiserId eq organiserUserId)
            }
            op = if (declined) {
                op and (PartnershipsTable.declinedAt.isNotNull())
            } else {
                op and (PartnershipsTable.declinedAt.isNull())
            }
            return find { op }
        }

        fun findAgreementReady(eventId: UUID): List<PartnershipEntity> =
            find {
                (PartnershipsTable.eventId eq eventId) and
                    PartnershipsTable.declinedAt.isNull() and
                    PartnershipsTable.validatedAt.isNotNull() and
                    PartnershipsTable.agreementUrl.isNull() and
                    (PartnershipsTable.contactName neq "")
            }.toList()
    }
    // ... properties ...
}
```

### 3.3 Boolean Check Helpers

```kotlin
fun UUIDEntityClass<OrganisationPermissionEntity>.hasPermission(
    organisationId: UUID,
    userId: UUID,
): Boolean = find {
    (OrganisationPermissionsTable.organisationId eq organisationId) and
        (OrganisationPermissionsTable.canEdit eq true) and
        (OrganisationPermissionsTable.userId eq userId)
}.empty().not()
```

### 3.4 Companion.emails / Companion List Helpers

```kotlin
fun PartnershipEmailEntity.Companion.emails(partnershipId: UUID): List<String> =
    find { PartnershipEmailsTable.partnershipId eq partnershipId }.map { it.email }

fun PartnershipOptionEntity.Companion.listByPartnershipAndPack(
    partnershipId: UUID,
    packId: UUID,
): SizedIterable<PartnershipOptionEntity> =
    find {
        (PartnershipOptionsTable.partnershipId eq partnershipId) and
            (PartnershipOptionsTable.packId eq packId)
    }

fun PartnershipOptionEntity.Companion.hasBoothOption(partnershipId: UUID): Boolean =
    find { PartnershipOptionsTable.partnershipId eq partnershipId }
        .any { it.option.selectableDescriptor == SelectableDescriptor.BOOTH }
```

### 3.5 Bulk Deletion Helpers

```kotlin
fun CompanySocialEntity.Companion.deleteAllByCompanyId(companyId: UUID): Unit =
    find { CompanySocialsTable.companyId eq companyId }.forEach { it.delete() }
```

### 3.6 Create Helpers

```kotlin
fun SelectableValueEntity.Companion.createForOption(
    optionId: UUID,
    value: String,
    price: Int,
): SelectableValueEntity = new {
    this.option = SponsoringOptionEntity[optionId]
    this.value = value
    this.price = price
}
```

### Rules

- **PREFER** extension functions on `UUIDEntityClass<E>` for simple queries (findByX, listByX).
- **USE** companion methods for complex multi-parameter filtering.
- **RETURN** `SizedIterable<E>` when the caller may need to chain `.count()`, `.paginated()`, or further filtering.
- **RETURN** `List<E>` when the result set is always fully consumed.
- **THROW** `NotFoundException` in "single-or-fail" methods — do not return null.
- **NEVER** put transaction management inside companion helpers — the caller manages transactions.

---

## 4 — Entity Extension Functions

Use extension functions on entities for computed properties and domain logic that depends on entity data.

### 4.1 Computed Properties

```kotlin
fun CompanyEntity.hasCompleteAddress(): Boolean =
    !siret.isNullOrBlank() &&
        !address.isNullOrBlank() &&
        !zipCode.isNullOrBlank() &&
        !city.isNullOrBlank() &&
        !country.isNullOrBlank()

fun SponsoringPackEntity.hasBoothFromOptions(): Boolean =
    options.any { option ->
        option.optionType == OptionType.TYPED_SELECTABLE &&
            option.selectableDescriptor == SelectableDescriptor.BOOTH
    }

fun SponsoringPackEntity.getTotalTicketsFromOptions(): Int =
    options.filter { option ->
        option.optionType == OptionType.TYPED_NUMBER &&
            option.numberDescriptor == NumberDescriptor.NB_TICKET
    }.sumOf { option -> option.fixedQuantity ?: 0 }
```

### Rules

- Place computed property extensions alongside the entity file or in `application/mappers/`.
- Keep extension functions pure — no side effects, no transaction management.

---

## 5 — DSL Query Operators

### 5.1 Comparison Operators

```kotlin
// Equality
CompaniesTable.status eq CompanyStatus.ACTIVE

// Inequality
PartnershipsTable.contactName neq ""

// Greater/Less
PartnershipsTable.communicationPublicationDate greaterEq startOfDay
PartnershipsTable.communicationPublicationDate less endOfDay
```

### 5.2 Logical Operators

```kotlin
// AND
(PartnershipsTable.eventId eq eventId) and (PartnershipsTable.selectedPackId eq packId)

// Building conditions incrementally
var op = PartnershipsTable.eventId eq eventId
if (packId != null) {
    op = op and (PartnershipsTable.selectedPackId eq packId)
}
return find { op }
```

### 5.3 Null Checks

```kotlin
PartnershipsTable.validatedAt.isNotNull()
PartnershipsTable.declinedAt.isNull()
```

### 5.4 Collection Operators

```kotlin
// IN list
SponsoringOptionsTable.id inList options.required.map(UUID::fromString)
```

### 5.5 String Operators

```kotlin
// Case-insensitive LIKE search
CompaniesTable.name.lowerCase() like "%${query.lowercase()}%"
```

### 5.6 Ordering

```kotlin
companies.orderBy(CompaniesTable.name to SortOrder.ASC)

CompanyJobOfferEntity.find {
    CompanyJobOfferTable.companyId eq companyId
}.orderBy(CompanyJobOfferTable.createdAt to SortOrder.DESC)
```

---

## 6 — CRUD Operations

### 6.1 Create

```kotlin
val company = CompanyEntity.new {
    name = input.name
    siteUrl = input.siteUrl
    status = CompanyStatus.ACTIVE
}
// company.id.value gives the UUID

// With entity reference
val partnership = PartnershipEntity.new {
    this.event = event         // Assign entity directly for FK
    this.company = company     // Assign entity directly for FK
    this.language = register.language
}
```

### 6.2 Read

```kotlin
// By ID (returns nullable)
val company = CompanyEntity.findById(companyId)
    ?: throw NotFoundException("Company with id $companyId not found")

// By ID (throws if missing)
val company = CompanyEntity[companyId]

// By condition
EventEntity.find { EventsTable.slug eq slug }.singleOrNull()
```

### 6.3 Update

```kotlin
val company = CompanyEntity.findById(id)
    ?: throw NotFoundException("Company not found")

// Direct assignment
company.name = input.name
company.status = CompanyStatus.INACTIVE

// Conditional update with let
input.name?.let { company.name = it }
input.siteUrl?.let { company.siteUrl = it }
```

### 6.4 Delete

```kotlin
// Single entity
entity.delete()

// Bulk delete via entity iteration
CompanySocialEntity.deleteAllByCompanyId(companyId)

// Delete via DSL (bypasses entity lifecycle)
OptionTranslationsTable.deleteWhere { OptionTranslationsTable.option eq optionId }
```

---

## 7 — Raw DSL (Table-level) Queries

Use raw DSL when you need joins, aggregates, or queries not easily expressed through entities.

### 7.1 selectAll with where

```kotlin
PackOptionsTable.selectAll()
    .where { PackOptionsTable.pack eq packId }
    .toList()
```

### 7.2 Joins

```kotlin
PackOptionsTable.innerJoin(SponsoringPacksTable)
    .selectAll()
    .where {
        (SponsoringPacksTable.eventId eq eventId) and
            (PackOptionsTable.option eq optionId)
    }
```

### 7.3 Count and Empty

```kotlin
val total = companies.count()

val exists = !PackOptionsTable.selectAll()
    .where { PackOptionsTable.pack eq packId }
    .empty()
```

---

## 8 — Pagination

Use the project-specific pagination utilities on `SizedIterable`.

```kotlin
override fun listPaginated(
    query: String?,
    status: CompanyStatus?,
    page: Int,
    pageSize: Int,
): PaginatedResponse<Company> = transaction {
    val companies = CompanyEntity.listByQueryAndStatus(query, status)
    val total = companies.count()
    companies
        .paginated(page, pageSize)                          // Extension: limit + offset
        .map { it.toDomain(it.socials.map(CompanySocialEntity::toDomain)) }
        .toPaginatedResponse(total, page, pageSize)         // Wraps in PaginatedResponse
}
```

**Key**: Call `.count()` **before** `.paginated()` to get the unfiltered total.

---

## 9 — Transaction Management

### 9.1 Repository Methods Wrap in transaction {}

```kotlin
override fun getById(id: UUID): Company = transaction {
    val company = CompanyEntity.findById(id)
        ?: throw NotFoundException("Company not found")
    company.toDomain()
}

override suspend fun create(companyId: UUID, input: CreateJobOffer): UUID = transaction {
    val entity = CompanyJobOfferEntity.new { /* ... */ }
    entity.id.value
}
```

### 9.2 Multi-Step Transactions

Multiple operations in one transaction ensure atomicity.

```kotlin
override fun register(eventSlug: String, register: RegisterPartnership): UUID = transaction {
    val event = EventEntity.findBySlug(eventSlug)
        ?: throw NotFoundException("Event not found")
    val company = CompanyEntity.findById(register.companyId.toUUID())
        ?: throw NotFoundException("Company not found")

    val partnership = PartnershipEntity.new {
        this.event = event
        this.company = company
    }

    register.emails.forEach {
        PartnershipEmailEntity.new {
            this.partnership = partnership
            this.email = it
        }
    }

    partnership.id.value
}
```

### Rules

- **EVERY** repository method wraps its body in `transaction { }`.
- **NEVER** nest transactions — one `transaction` per repository method.
- **NEVER** put `transaction` inside entity companion helpers or factory functions.
- Exceptions thrown inside `transaction {}` automatically roll back.

---

## 10 — Quick Decision Guide

| You need to... | Pattern |
|---|---|
| Find by unique field | `Entity.find { Table.field eq value }.singleOrNull()` |
| Find by ID or throw | `Entity.findById(id) ?: throw NotFoundException(...)` |
| Find by ID (throws directly) | `Entity[id]` |
| List by FK | `Entity.find { Table.fk eq id }.toList()` |
| List with filters | Companion method building `Op<Boolean>` incrementally |
| Create new row | `Entity.new { this.field = value }` |
| Update existing row | Find then assign: `entity.name = newValue` |
| Partial update | `input.name?.let { entity.name = it }` |
| Delete single | `entity.delete()` |
| Delete by condition (DSL) | `Table.deleteWhere { condition }` |
| Delete by condition (Entity) | `Entity.find { ... }.forEach { it.delete() }` |
| Paginate | `.count()` then `.paginated(page, size).map { }.toPaginatedResponse(total, page, size)` |
| Check existence | `Entity.find { ... }.empty().not()` |
| Case-insensitive search | `Table.col.lowerCase() like "%${q.lowercase()}%"` |
| Reusable query (simple) | Extension on `UUIDEntityClass<E>` |
| Reusable query (complex) | Companion method on Entity |
| Computed property | Extension function on Entity |

---

## Anti-Patterns (NEVER DO)

| Anti-pattern | Correct approach |
|---|---|
| `timestamp()` column type | Use `datetime()` |
| Transaction in companion query helper | Caller manages transaction |
| Transaction in factory function | Caller manages transaction |
| Repository depending on another repository | Route handler orchestrates multiple repositories |
| Notifications sent from repository | Send from route handler |
| `Entity.findById(id)!!` | Use `?: throw NotFoundException(...)` |
| Manual null checks on route parameters | Use `call.parameters.eventSlug` extensions |
