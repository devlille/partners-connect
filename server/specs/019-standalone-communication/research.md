# Research: Schedule Standalone Communication

**Branch**: `019-standalone-communication` | **Date**: 2026-03-16

All NEEDS CLARIFICATION items from Technical Context resolved below.

---

## Decision 1: Existing Infrastructure

**Question**: Does a communication entity already exist or must one be created from scratch?

**Decision**: Create a new `communication_plans` table with an optional `partnershipId` FK.

**Rationale**: The current implementation stores communication scheduling data directly on `PartnershipsTable` (`communicationPublicationDate`, `communicationSupportUrl`). This makes standalone (non-partnership) communications impossible. A dedicated table cleanly separates the concern, supports both linked and standalone entries through a nullable FK, and can be extended with a future nullable `integrationId` FK without structural changes.

**Alternatives considered**:
- Extend `PartnershipsTable` with a "standalone" sentinel — Rejected: semantically wrong; a row in `PartnershipsTable` must represent a partnership.
- Reuse a generic `events` sub-resource table — Rejected: unnecessary abstraction; the planning concept is specific to communication scheduling.

**Evidence**: `PartnershipsTable.kt` (lines 30–31) — `communicationPublicationDate` and `communicationSupportUrl` are nullable columns on `PartnershipsTable`. `PartnershipCommunicationRepositoryExposed.listCommunicationPlan()` reads directly from these columns.

---

## Decision 2: Migration Pattern

**Question**: How are database migrations done in this codebase?

**Decision**: Two migration objects registered in `MigrationRegistry.allMigrations` in order:
1. `CreateCommunicationPlansTableMigration` — DDL: creates the new table via `SchemaUtils.create(CommunicationPlansTable)`.
2. `MigratePartnershipCommunicationsMigration` — DML: reads all `PartnershipEntity` rows where `communicationPublicationDate != null`, inserts a `CommunicationPlanEntity` row per result using the company name as title. Wrapped in a `transaction {}`.

**Rationale**: The `Migration` interface exposes `id: String` (datestamp slug) and `up()`. `AddPartnershipCommunicationFieldsMigration` uses `SchemaUtils.createMissingTablesAndColumns()`. For a new table + data migration we use `SchemaUtils.create()` then a separate DML migration to maintain atomicity and auditability.

**Migration IDs**:
- `"20260316_create_communication_plans_table"`
- `"20260316_migrate_partnership_communications"`

**Alternatives considered**:
- Single migration combining DDL + DML — Rejected: if DML fails, DDL cannot be easily undone; separation is safer and matches existing pattern.

---

## Decision 3: Data Model for `CommunicationPlansTable`

**Question**: What columns does the new table need?

**Decision**: 

```
communication_plans (UUIDTable)
  id                  UUID PK
  event_id            UUID FK → events (NOT NULL)
  partnership_id      UUID FK → partnerships NULLABLE (ReferenceOption.SET_NULL on delete)
  title               VARCHAR(255) NOT NULL
  scheduled_date      DATETIME NULLABLE
  description         TEXT NULLABLE
  support_url         TEXT NULLABLE
  created_at          DATETIME (clientDefault: Clock.System.now())
  updated_at          DATETIME (clientDefault: Clock.System.now(), updated on each write)
```

**Rationale**:
- `event_id` is NOT NULL — every entry belongs to an event.
- `partnership_id` is NULLABLE — null means standalone, non-null means linked to a partnership.
- `ReferenceOption.SET_NULL` on partnership delete — if a partnership is deleted, the communication entry becomes standalone rather than being cascade-deleted (preserves planning history).
- `scheduled_date` NULLABLE — allows entries to exist without a date (consistent with current `unplanned` group).
- No `audience` field — confirmed out of scope (audience determined at send time).
- No `integration_id` column now — schema is extensible (nullable FK can be added via `ALTER TABLE` migration later without touching existing columns).

**Alternatives considered**:
- `ReferenceOption.CASCADE` on partnership delete — Rejected: deletes planning history silently.
- `ReferenceOption.NO_ACTION` — Rejected: would block partnership deletion if communication entries exist.

---

## Decision 4: GET /communication Response Compatibility

**Question**: Must the existing `GET /communication` response shape change?

**Decision**: The response shape of `CommunicationPlan` (`done`, `planned`, `unplanned` lists of `CommunicationItem`) is preserved. `CommunicationItem` gains one new optional field: `standalone: Boolean` (true when `partnershipId` is null). For migrated/linked entries `companyName` is populated from the partnership's company; for standalone entries `companyName` is null/absent.

**Updated `CommunicationItem`**:
```kotlin
@Serializable
data class CommunicationItem(
    val id: String,                         // NEW — communication_plans.id
    @SerialName("partnership_id")
    val partnershipId: String? = null,      // null for standalone
    @SerialName("company_name")
    val companyName: String? = null,        // null for standalone
    val title: String,                      // NEW — communication_plans.title
    @SerialName("publication_date")
    val publicationDate: LocalDateTime? = null,
    @SerialName("support_url")
    val supportUrl: String? = null,
    val standalone: Boolean,               // true when partnershipId == null
)
```

**Rationale**: Existing clients already handle nullable `publicationDate` and `supportUrl`. Adding `id`, `title`, and `standalone` with backward-compatible nullability/defaults ensures no breaking change. `companyName` is made nullable since standalone entries have no company.

**Alternatives considered**:
- Remove `companyName` entirely — Rejected: existing clients depend on it for partnership-linked entries.
- Keep `companyName` non-null (use title for standalone) — Rejected: misleading; standalone entries have a title, not a company name.

---

## Decision 5: Updating Existing PUT Routes

**Question**: How should `PUT .../communication/publication` and `PUT .../communication/support` write to the new table?

**Decision**: Both routes look up the `CommunicationPlanEntity` by `partnershipId` (via `CommunicationPlansTable.partnershipId eq partnershipId`). If a row exists, it is updated. If not (first time setting a date/support on this partnership), a new row is **created** with `title = company.name` and `partnershipId` set.

**Rationale**: This upsert behaviour preserves the existing API contract (no new required fields from callers) while migrating writes transparently to the new table. After the data migration all existing rows will already exist, so in practice it will always be an update — but defensive creation is correct for safety.

**Alternatives considered**:
- Require callers to create entries first via the new `POST` endpoint — Rejected: breaking change to existing integration clients.
- Keep `PartnershipsTable` as the write target and sync to new table in background — Rejected: dual-write complexity with no benefit.

---

## Decision 6: New CRUD Endpoint Request/Response Shape

**Question**: What request and response bodies do the new endpoints use?

**POST `/communication-plan`** — create standalone entry:
```json
Request:
{
  "title": "Welcome sponsors email",
  "scheduled_date": "2026-06-15T09:00:00",  // optional
  "description": "General welcome message to all sponsors",  // optional
  "support_url": "https://..."  // optional
}

Response 201:
{
  "id": "uuid",
  "title": "Welcome sponsors email",
  "scheduled_date": "2026-06-15T09:00:00",
  "description": "...",
  "support_url": "...",
  "standalone": true,
  "partnership_id": null,
  "company_name": null,
  "created_at": "2026-03-16T10:00:00"
}
```

**PUT `/communication-plan/{id}`** — update any entry (standalone or partnership-linked):
```json
Request: same shape as POST (title required, others optional)
Response 200: same shape as POST response
```

**DELETE `/communication-plan/{id}`** — delete any entry:
```
Response 204 No Content
```

---

## Decision 7: Authorization

**Question**: How is authorization enforced on the new endpoints?

**Decision**: `install(AuthorizedOrganisationPlugin)` on the `/orgs/{orgSlug}/events/{eventSlug}/communication-plan` route block. Same pattern as all other organiser-scoped routes. Additionally, the repository verifies the entry belongs to the correct event (by joining through `CommunicationPlansTable.eventId eq event.id`) to prevent cross-event manipulation.

---

## Decision 8: Test Factory

**Question**: What factory function is needed?

**Decision**: `insertMockedCommunicationPlan(...)` in `partnership/factories/CommunicationPlan.factory.kt`:

```kotlin
fun insertMockedCommunicationPlan(
    id: UUID = UUID.randomUUID(),
    eventId: UUID,
    partnershipId: UUID? = null,
    title: String = id.toString(),
    scheduledDate: LocalDateTime? = null,
    description: String? = null,
    supportUrl: String? = null,
): CommunicationPlanEntity = CommunicationPlanEntity.new(id) { ... }
```

**Rationale**: Follows existing factory conventions exactly (`insertMocked<Entity>`, UUID-based title default, no transaction management, all parameters have defaults except required FKs).

---

## Resolved Unknowns Summary

| Unknown | Status | Resolution |
|---------|--------|------------|
| Existing communication entity? | Resolved | New `communication_plans` table |
| Migration pattern | Resolved | Two `Migration` objects in `MigrationRegistry`; date-slug IDs |
| Table schema | Resolved | See Decision 3 |
| GET /communication compatibility | Resolved | Additive fields; `companyName` made nullable |
| Existing PUT routes behaviour | Resolved | Upsert by `partnershipId` |
| New endpoint shape | Resolved | See Decision 6 |
| Authorization | Resolved | `AuthorizedOrganisationPlugin` + event-scoped query guard |
| Test factory | Resolved | `insertMockedCommunicationPlan` |
