# Data Model: Provider Management Enhancement

## Enhanced Provider Entity

### ProvidersTable (Modified)
```kotlin
object ProvidersTable : UUIDTable("providers") {
    val name = varchar("name", 255)
    val type = varchar("type", 100)
    val website = text("website").nullable()
    val phone = varchar("phone", 30).nullable()
    val email = varchar("email", 255).nullable()
    val organisationId = reference("organisation_id", OrganisationsTable)  // NEW: Non-nullable foreign key
    val createdAt = datetime("created_at").clientDefault {
        Clock.System.now().toLocalDateTime(TimeZone.UTC)
    }
}
```

### ProviderEntity (Enhanced)
```kotlin
class ProviderEntity(id: EntityID<UUID>) : UUIDEntity(id) {
    companion object : UUIDEntityClass<ProviderEntity>(ProvidersTable)
    
    var name by ProvidersTable.name
    var type by ProvidersTable.type
    var website by ProvidersTable.website
    var phone by ProvidersTable.phone
    var email by ProvidersTable.email
    var organisation by OrganisationEntity referencedOn ProvidersTable.organisationId  // NEW
    var createdAt by ProvidersTable.createdAt
    
    // NEW: Many-to-many relationship with events via existing EventProvidersTable
    val events by EventEntity via EventProvidersTable
}
```

## Existing Provider-Event Relationship

### EventProvidersTable (Enhanced)
```kotlin
object EventProvidersTable : UUIDTable("event_providers") {
    val eventId = reference("event_id", EventsTable)
    val providerId = reference("provider_id", ProvidersTable)
    val createdAt = datetime("created_at").clientDefault {  // NEW: Track relationship creation
        Clock.System.now().toLocalDateTime(TimeZone.UTC)
    }

    init {
        // Create unique index to prevent duplicate entries
        uniqueIndex(eventId, providerId)
    }
}
```

**Enhancement**: Add `createdAt` datetime column to track when provider-event relationships are established.

## Domain Models

### Provider (Response Model)
```kotlin
@Serializable
data class Provider(
    val id: String,  // UUID serialized as string
    val name: String,
    val type: String,
    val website: String?,
    val phone: String?,
    val email: String?,
    val org_slug: String,  // For response serialization
    val created_at: LocalDateTime
)
```

## JSON Schema Files

### Existing Enhanced Schemas
- `provider.schema.json` - Enhanced with `org_slug` field for organisation association
- `create_provider.schema.json` - Existing schema for provider creation (already good)
- `update_provider.schema.json` - New schema for partial provider updates
- `paginated_provider.schema.json` - Enhanced with `providers` and `pagination` structure
- `create_by_identifiers.schema.json` - Reused for provider attachment/detachment (array of UUIDs)

### Key Schema Enhancements
- Added `org_slug` field to provider response schema
- Updated pagination response structure to match project conventions
- Provider creation/update schemas follow existing patterns
- Bulk operations use existing `create_by_identifiers.schema.json` pattern

## Validation Rules

### Provider Validation
- **name**: Required, 1-255 characters, non-blank
- **type**: Required, 1-100 characters, non-blank, free-text allowed
- **website**: Optional, valid URL format if provided
- **phone**: Optional, 1-30 characters if provided
- **email**: Optional, valid email format if provided
- **organisation_id**: Required, must reference existing organisation

### Business Rules
- Provider must belong to exactly one organisation
- Provider can be attached to multiple events within the same organisation
- Provider cannot be attached to events from different organisations
- Provider deletion requires detachment from all events first (only providers can be deleted)
- Only organisation members with edit permissions can manage providers
- Events and organisations are permanent entities and cannot be deleted

## Database Migration

### Migration Script
```sql
-- Step 1: Remove existing providers (breaking change acceptable)
DELETE FROM providers;

-- Step 2: Remove existing provider-event relationships (if any)
DELETE FROM event_providers;

-- Step 3: Add organisation_id column to providers table
ALTER TABLE providers 
ADD COLUMN organisation_id UUID NOT NULL 
REFERENCES organisations(id) ON DELETE RESTRICT;

-- Step 4: Add created_at column to event_providers table
ALTER TABLE event_providers
ADD COLUMN created_at TIMESTAMP NOT NULL DEFAULT NOW();

-- Step 5: Add performance indexes
CREATE INDEX idx_providers_organisation_id ON providers(organisation_id);
CREATE INDEX idx_event_providers_created_at ON event_providers(created_at);
```

**Enhancements**:
- Add `organisation_id` to providers table
- Add `created_at` timestamp to event_providers table for relationship tracking
- Add appropriate indexes for performance

## Entity Relationships

```
Organisation (1) ──────┬─ (n) Provider (n) ─────── (n) Event
                       │                    └─ via EventProviders ─┘
                       └─ (n) Event
```

### Relationship Constraints
- **Organisation → Provider**: One-to-many (RESTRICT DELETE - organisation deletion not allowed)
- **Provider ↔ Event**: Many-to-many via existing EventProvidersTable (CASCADE DELETE on provider only)
- **Organisation → Event**: One-to-many (RESTRICT DELETE - event deletion not allowed)

### Cascade Behavior
- Delete Provider → Delete all its EventProvider relationships  
- Event deletion is not supported (business constraint)
- Organisation deletion is not supported (business constraint)

## Query Patterns

### Organisation-Scoped Provider Queries
```sql
-- List providers for organisation with pagination and filtering
SELECT p.* FROM providers p 
JOIN organisations o ON p.organisation_id = o.id 
WHERE o.slug = ? 
  AND (? IS NULL OR p.name ILIKE ? OR p.type ILIKE ?)
ORDER BY p.name ASC 
LIMIT ? OFFSET ?;

-- Get provider by ID within organisation scope
SELECT p.* FROM providers p
JOIN organisations o ON p.organisation_id = o.id
WHERE p.id = ? AND o.slug = ?;
```

### Event Provider Queries  
```sql
-- List providers attached to specific event
SELECT p.* FROM providers p
JOIN event_providers ep ON p.id = ep.provider_id
JOIN events e ON ep.event_id = e.id
WHERE e.slug = ? AND e.org_slug = ?;

-- Check if provider is attached to event
SELECT COUNT(*) FROM event_providers ep
JOIN providers p ON ep.provider_id = p.id
JOIN events e ON ep.event_id = e.id
JOIN organisations o ON p.organisation_id = o.id
WHERE ep.provider_id = ? AND ep.event_id = ? AND o.slug = ?;
```

### Public Provider Queries
```sql
-- Public provider listing with optional organisation filtering
SELECT p.*, o.slug as org_slug FROM providers p
JOIN organisations o ON p.organisation_id = o.id
WHERE (? IS NULL OR o.slug = ?)
  AND (? IS NULL OR p.name ILIKE ? OR p.type ILIKE ?)
ORDER BY p.name ASC
LIMIT ? OFFSET ?;
```