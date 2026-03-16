# Data Model: Schedule Standalone Communication

**Branch**: `019-standalone-communication` | **Date**: 2026-03-16

---

## Overview

This feature introduces a dedicated `communication_plans` table as the single source of truth for all communication planning entries across an event. It replaces the two ad-hoc columns (`communicationPublicationDate`, `communicationSupportUrl`) previously stored directly on `PartnershipsTable`.

---

## Database Tables

### CommunicationPlansTable

```kotlin
object CommunicationPlansTable : UUIDTable("communication_plans") {
    val eventId = reference("event_id", EventsTable)
    val partnershipId = reference(
        "partnership_id",
        PartnershipsTable,
        onDelete = ReferenceOption.SET_NULL,
    ).nullable()
    val title = varchar("title", 255)
    val scheduledDate = datetime("scheduled_date").nullable()
    val description = text("description").nullable()
    val supportUrl = text("support_url").nullable()
    val createdAt = datetime("created_at")
        .clientDefault { Clock.System.now().toLocalDateTime(TimeZone.UTC) }
    val updatedAt = datetime("updated_at")
        .clientDefault { Clock.System.now().toLocalDateTime(TimeZone.UTC) }

    init {
        // Index for efficient per-event planning view queries (SC-002)
        index(false, eventId)
    }
}
```

**Design notes**:
- Extends `UUIDTable` per codebase standards.
- `eventId` is NOT NULL — every entry is scoped to one event.
- `partnershipId` is NULLABLE with `ReferenceOption.SET_NULL` — when a partnership is deleted the communication entry survives as standalone rather than being cascade-deleted.
- `scheduledDate` NULLABLE — supports the `unplanned` group in the planning view (though POST/PUT require it for standalone entries; existing migrated rows may be null if the original partnership row was inserted before the communication date was set, which cannot happen in practice since migration only reads rows where `communicationPublicationDate IS NOT NULL`).
- `updatedAt` must be explicitly updated on every PUT operation (no DB-level trigger; application sets it).
- Schema is intentionally free of an `integrationId` column — it will be added by a future migration as a nullable FK without touching existing columns.

---

### CommunicationPlanEntity

```kotlin
class CommunicationPlanEntity(id: EntityID<UUID>) : UUIDEntity(id) {
    companion object : UUIDEntityClass<CommunicationPlanEntity>(CommunicationPlansTable)

    var event by EventEntity referencedOn CommunicationPlansTable.eventId
    var partnership by PartnershipEntity optionalReferencedOn CommunicationPlansTable.partnershipId
    var title by CommunicationPlansTable.title
    var scheduledDate by CommunicationPlansTable.scheduledDate
    var description by CommunicationPlansTable.description
    var supportUrl by CommunicationPlansTable.supportUrl
    var createdAt by CommunicationPlansTable.createdAt
    var updatedAt by CommunicationPlansTable.updatedAt
}
```

---

## Domain Model

### CommunicationPlanEntry (new)

```kotlin
@Serializable
data class CommunicationPlanEntry(
    val id: String,
    @SerialName("event_id")
    val eventId: String,
    @SerialName("partnership_id")
    val partnershipId: String? = null,
    @SerialName("company_name")
    val companyName: String? = null,
    val title: String,
    @SerialName("scheduled_date")
    val scheduledDate: LocalDateTime? = null,
    val description: String? = null,
    @SerialName("support_url")
    val supportUrl: String? = null,
    val standalone: Boolean,
    @SerialName("created_at")
    val createdAt: LocalDateTime,
)
```

### Updated CommunicationItem (in GET /communication response)

`CommunicationItem` in `CommunicationPlan.kt` gains `id`, `title`, `standalone` fields and `companyName` becomes nullable:

```kotlin
@Serializable
data class CommunicationItem(
    val id: String,                               // NEW
    @SerialName("partnership_id")
    val partnershipId: String? = null,
    @SerialName("company_name")
    val companyName: String? = null,              // was non-null
    val title: String,                            // NEW
    @SerialName("publication_date")
    val publicationDate: LocalDateTime? = null,
    @SerialName("support_url")
    val supportUrl: String? = null,
    val standalone: Boolean,                      // NEW
)
```

---

### CommunicationPlanRepository (new interface)

```kotlin
interface CommunicationPlanRepository {
    fun create(
        eventSlug: String,
        title: String,
        scheduledDate: LocalDateTime,
        description: String?,
        supportUrl: String?,
    ): CommunicationPlanEntry

    fun findById(eventSlug: String, id: UUID): CommunicationPlanEntry

    fun update(
        eventSlug: String,
        id: UUID,
        title: String,
        scheduledDate: LocalDateTime?,  // nullable: null clears date → moves entry to unplanned
        description: String?,
        supportUrl: String?,
    ): CommunicationPlanEntry

    fun delete(eventSlug: String, id: UUID)

    /**
     * Upsert a communication plan entry for a partnership.
     * Called by the existing PUT .../communication/publication and PUT .../communication/support routes.
     * If an entry already exists for this partnership, it is updated; otherwise a new entry is created
     * with title = company name.
     */
    fun upsertForPartnership(
        eventSlug: String,
        partnershipId: UUID,
        scheduledDate: LocalDateTime?,
        supportUrl: String?,
    ): CommunicationPlanEntry
}
```

**Notes**:
- `eventSlug` scoping in every method prevents cross-event operations.
- `findById` / `update` / `delete` throw `NotFoundException` if entry not found **or** entry belongs to a different event.
- `create` takes a non-nullable `scheduledDate` (required for new standalone entries per FR-008).
- `update` accepts a nullable `scheduledDate`; null clears the date and moves the entry to `unplanned`.
- `upsertForPartnership` is used only by the existing partnership PUT routes to keep them backwards-compatible.
- No dependency on other repositories (constitution requirement).

---

## Migrations

### Migration 1 — DDL

```kotlin
object CreateCommunicationPlansTableMigration : Migration {
    override val id = "20260316_create_communication_plans_table"
    override val description = "Create communication_plans table"

    override fun up() {
        SchemaUtils.create(CommunicationPlansTable)
    }

    override fun down() {
        throw UnsupportedOperationException(
            "Rollback not supported — dropping communication_plans would cause data loss",
        )
    }
}
```

### Migration 2 — DML (data migration)

```kotlin
object MigratePartnershipCommunicationsMigration : Migration {
    override val id = "20260316_migrate_partnership_communications"
    override val description = "Migrate communication dates/urls from partnerships to communication_plans"

    override fun up() {
        transaction {
            PartnershipEntity
                .find { PartnershipsTable.communicationPublicationDate.isNotNull() }
                .forEach { partnership ->
                    CommunicationPlanEntity.new {
                        event = partnership.event
                        this.partnership = partnership
                        title = partnership.company.name
                        scheduledDate = partnership.communicationPublicationDate
                        supportUrl = partnership.communicationSupportUrl
                    }
                }
        }
    }

    override fun down() {
        throw UnsupportedOperationException(
            "Rollback not supported — would require re-populating PartnershipsTable columns",
        )
    }
}
```

### MigrationRegistry update

Add both migrations **after** `AddPartnershipPriceOverridesMigration` (currently last in list):

```kotlin
AddPartnershipPriceOverridesMigration,
CreateCommunicationPlansTableMigration,
MigratePartnershipCommunicationsMigration,
```

---

## Entity → Domain Mapping

```kotlin
fun CommunicationPlanEntity.toDomain(): CommunicationPlanEntry = CommunicationPlanEntry(
    id = id.value.toString(),
    eventId = event.id.value.toString(),
    partnershipId = partnership?.id?.value?.toString(),
    companyName = partnership?.company?.name,
    title = title,
    scheduledDate = scheduledDate,
    description = description,
    supportUrl = supportUrl,
    standalone = partnership == null,
    createdAt = createdAt,
)

fun CommunicationPlanEntity.toCommunicationItem(): CommunicationItem = CommunicationItem(
    id = id.value.toString(),
    partnershipId = partnership?.id?.value?.toString(),
    companyName = partnership?.company?.name,
    title = title,
    publicationDate = scheduledDate,
    supportUrl = supportUrl,
    standalone = partnership == null,
)
```

---

## Test Factory

**File**: `application/src/test/kotlin/fr/devlille/partners/connect/partnership/factories/CommunicationPlan.factory.kt`

```kotlin
@Suppress("LongParameterList")
fun insertMockedCommunicationPlan(
    id: UUID = UUID.randomUUID(),
    eventId: UUID,
    partnershipId: UUID? = null,
    title: String = id.toString(),
    scheduledDate: LocalDateTime? = null,
    description: String? = null,
    supportUrl: String? = null,
): CommunicationPlanEntity = CommunicationPlanEntity.new(id) {
    this.event = EventEntity[eventId]
    this.partnership = partnershipId?.let { PartnershipEntity[it] }
    this.title = title
    this.scheduledDate = scheduledDate
    this.description = description
    this.supportUrl = supportUrl
}
```

**Rules**: No transaction management; all parameters have defaults except `eventId`; UUID-based unique default for `title`; follows `insertMocked<Entity>` naming convention.

---

## Validation Rules

### CommunicationPlanEntry
- `title`: required, non-blank, max 255 characters (validated by JSON schema).
- `scheduled_date`: optional ISO 8601 local datetime. Past dates accepted.
- `description`: optional, no length limit (TEXT column).
- `support_url`: optional, no format validation (stored as-is).

---

## Impact on Existing Code

| File | Change |
|------|--------|
| `CommunicationPlan.kt` | `CommunicationItem.companyName` made nullable; add `id`, `title`, `standalone` fields |
| `PartnershipCommunicationRepositoryExposed.listCommunicationPlan()` | Re-source from `CommunicationPlanEntity` instead of `PartnershipEntity` |
| `PartnershipCommunicationRepositoryExposed.updateCommunicationPublicationDate()` | Upsert into `CommunicationPlansTable` by `partnershipId` |
| `PartnershipCommunicationRepositoryExposed.updateCommunicationSupportUrl()` | Upsert into `CommunicationPlansTable` by `partnershipId` |
| `PartnershipModule.kt` | Add `single<CommunicationPlanRepository> { CommunicationPlanRepositoryExposed() }` |
| `PartnershipRoutes.kt` | Add `orgsEventCommunicationPlanRoutes()` call |
| `MigrationRegistry.kt` | Append two new migrations |
| `EventCommunicationPlanRouteGetTest.kt` | Update test data setup to use `insertMockedCommunicationPlan` factory |
