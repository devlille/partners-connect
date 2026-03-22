# Data Model: Manage Booth Activities

**Phase**: 1 — Design  
**Date**: 2026-03-21  
**Feature**: `020-manage-booth-activities`

---

## New Entity: BoothActivity

### Domain Model

```kotlin
// partnership/domain/BoothActivity.kt
@Serializable
data class BoothActivity(
    val id: UUID,
    val partnershipId: UUID,
    val title: String,
    val description: String,
    val startTime: LocalDateTime?,
    val endTime: LocalDateTime?,
    val createdAt: LocalDateTime,
)
```

### Database Table: `booth_activities`

```kotlin
// partnership/infrastructure/db/BoothActivitiesTable.kt
object BoothActivitiesTable : UUIDTable("booth_activities") {
    val partnershipId = reference("partnership_id", PartnershipsTable, onDelete = ReferenceOption.CASCADE)
    val title       = varchar("title", 255)
    val description = text("description")
    val startTime   = datetime("start_time").nullable()
    val endTime     = datetime("end_time").nullable()
    val createdAt   = datetime("created_at")
        .clientDefault { Clock.System.now().toLocalDateTime(TimeZone.UTC) }
}
```

| Column | Type | Constraints |
|--------|------|-------------|
| `id` | UUID | PK (inherited from UUIDTable) |
| `partnership_id` | UUID | FK → `partnerships.id` CASCADE DELETE |
| `title` | VARCHAR(255) | NOT NULL |
| `description` | TEXT | NOT NULL |
| `start_time` | DATETIME | NULLABLE |
| `end_time` | DATETIME | NULLABLE |
| `created_at` | DATETIME | NOT NULL, default = now() |

### ORM Entity

```kotlin
// partnership/infrastructure/db/BoothActivityEntity.kt
class BoothActivityEntity(id: EntityID<UUID>) : UUIDEntity(id) {
    companion object : UUIDEntityClass<BoothActivityEntity>(BoothActivitiesTable)

    var partnershipId by BoothActivitiesTable.partnershipId
    var title         by BoothActivitiesTable.title
    var description   by BoothActivitiesTable.description
    var startTime     by BoothActivitiesTable.startTime
    var endTime       by BoothActivitiesTable.endTime
    var createdAt     by BoothActivitiesTable.createdAt
}
```

---

## Relationships

```
PartnershipsTable (existing)
    │
    │  1 : N
    ▼
BoothActivitiesTable  (NEW)
    id, partnershipId, title, description, startTime, endTime, createdAt
```

```
PartnershipOptionsTable (existing) ──FK──► SponsoringOptionsTable (existing)
                                                │
                                        selectableDescriptor == BOOTH
                                                │
                                    → gateway check for activity routes
```

---

## Request / Response Shape

### Create / Update Request

```json
{
  "title": "Live demo: CI/CD at scale",
  "description": "A 30-minute hands-on demo of our CI/CD pipeline.",
  "startTime": "2026-06-14T10:00:00",
  "endTime":   "2026-06-14T10:30:00"
}
```

Fields: `title` (required, non-empty), `description` (required, non-empty), `startTime` (optional ISO datetime), `endTime` (optional ISO datetime).

### Activity Response

```json
{
  "id": "3fa85f64-5717-4562-b3fc-2c963f66afa6",
  "partnershipId": "7c9e6679-7425-40de-944b-e07fc1f90ae7",
  "title": "Live demo: CI/CD at scale",
  "description": "A 30-minute hands-on demo of our CI/CD pipeline.",
  "startTime": "2026-06-14T10:00:00",
  "endTime":   "2026-06-14T10:30:00",
  "createdAt": "2026-03-21T09:00:00"
}
```

### List Response

```json
[
  { /* activity object */ },
  { /* activity object */ }
]
```

Sorted by `startTime ASC NULLS LAST`, then `createdAt ASC`.

---

## Validation Rules

| Rule | Behaviour |
|------|-----------|
| `title` blank/missing | 400 Bad Request (schema validation) |
| `description` blank/missing | 400 Bad Request (schema validation) |
| `startTime` after `endTime` | 400 Bad Request (domain exception) |
| Partnership has no booth option | 403 Forbidden |
| Partnership / activity not found | 404 Not Found |

---

## Booth Eligibility Check

The booth eligibility check is performed at the start of every route handler:

```kotlin
fun hasBoothOption(partnershipId: UUID): Boolean =
    PartnershipOptionsTable
        .innerJoin(SponsoringOptionsTable, { optionId }, { SponsoringOptionsTable.id })
        .selectAll()
        .where { PartnershipOptionsTable.partnershipId eq partnershipId }
        .any { it[SponsoringOptionsTable.selectableDescriptor] == SelectableDescriptor.BOOTH }
```

If `false`, throw `ForbiddenException("Partnership does not have a booth option")` → mapped to 403.

---

## State Transitions

```
[No activities]
    ↓ POST /activities
[Activity created]
    ↓ PUT /activities/{id}
[Activity updated]
    ↓ DELETE /activities/{id}
[Activity deleted]
```

After each successful create/update/delete, `WebhookPartnershipPlugin` automatically fires.

---

## Sorting Query

```kotlin
BoothActivitiesTable
    .selectAll()
    .where { BoothActivitiesTable.partnershipId eq partnershipId }
    .orderBy(
        BoothActivitiesTable.startTime to SortOrder.ASC_NULLS_LAST,
        BoothActivitiesTable.createdAt to SortOrder.ASC,
    )
```
