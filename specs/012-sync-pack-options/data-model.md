# Data Model: Synchronize Pack Options

**Feature**: 012-sync-pack-options  
**Date**: November 24, 2025

## Overview

This feature modifies the behavior of pack-option synchronization but does **not change any database schema**. This document describes the existing data model that the feature operates on.

---

## Existing Database Tables

### PackOptionsTable

**Location**: `server/application/src/main/kotlin/fr/devlille/partners/connect/sponsoring/infrastructure/db/PackOptionsTable.kt`

**Purpose**: Junction table managing many-to-many relationship between sponsoring packs and sponsoring options with an additional `required` flag.

**Schema**:
```kotlin
object PackOptionsTable : Table("pack_options") {
    val pack = reference("pack", SponsoringPacksTable, onDelete = ReferenceOption.CASCADE)
    val option = reference("option", SponsoringOptionsTable, onDelete = ReferenceOption.CASCADE)
    val required = bool("required")
    
    override val primaryKey = PrimaryKey(pack, option)
}
```

**Columns**:
- `pack` (UUID, FK → SponsoringPacksTable): Reference to the sponsoring pack
- `option` (UUID, FK → SponsoringOptionsTable): Reference to the sponsoring option
- `required` (BOOLEAN): Whether this option is required (true) or optional (false) for this pack

**Constraints**:
- Primary key: Composite (pack, option) - ensures one attachment per pack-option pair
- Foreign keys: CASCADE delete - removing pack or option removes attachments
- NOT NULL: All columns are required

**Indexes**: Composite primary key provides index on (pack, option)

---

### SponsoringPacksTable

**Location**: `server/application/src/main/kotlin/fr/devlille/partners/connect/sponsoring/infrastructure/db/SponsoringPacksTable.kt`

**Purpose**: Stores sponsoring pack configurations (Bronze, Silver, Gold tiers, etc.)

**Relevant Schema** (partial - only fields relevant to this feature):
```kotlin
object SponsoringPacksTable : UUIDTable("sponsoring_packs") {
    val eventId = reference("event_id", EventsTable, onDelete = ReferenceOption.CASCADE)
    val name = varchar("name", 255)
    val price = integer("price")
    // ... additional translation and configuration fields
}
```

**Relationship**: One pack has many pack-option attachments via PackOptionsTable

---

### SponsoringOptionsTable

**Location**: `server/application/src/main/kotlin/fr/devlille/partners/connect/sponsoring/infrastructure/db/SponsoringOptionsTable.kt`

**Purpose**: Stores individual sponsoring benefits/options (logo placement, speaking slot, etc.)

**Relevant Schema** (partial):
```kotlin
object SponsoringOptionsTable : UUIDTable("sponsoring_options") {
    val eventId = reference("event_id", EventsTable, onDelete = ReferenceOption.CASCADE)
    val name = varchar("name", 255)
    // ... additional translation and configuration fields
}
```

**Relationship**: One option can be attached to many packs via PackOptionsTable

---

## Entity Relationships

```
EventEntity (1) ──────┬──────> (N) SponsoringPackEntity
                      │
                      └──────> (N) SponsoringOptionEntity
                      
SponsoringPackEntity (N) <────> (N) SponsoringOptionEntity
                     [via PackOptionsTable with required flag]
```

**Key Constraints**:
1. Packs and options must belong to the same event
2. One pack-option pair can only exist once (enforced by composite PK)
3. Each attachment has a boolean flag indicating required vs optional

---

## Data Validation Rules

### From Feature Specification

**FR-005**: Option IDs must belong to the same event as the pack
- **Validation**: Query both packs and options filtered by event_id
- **Error**: 403 Forbidden if any option belongs to different event

**FR-006**: Same option cannot be in both required and optional lists
- **Validation**: Check intersection of required and optional arrays
- **Error**: 409 Conflict with duplicate option IDs listed

**FR-007**: Option IDs must exist in the system
- **Validation**: Query count must equal submitted ID count
- **Error**: 404 Not Found with non-existent option IDs listed

**FR-008**: Pack must exist and belong to specified event
- **Validation**: Query pack by ID filtered by event
- **Error**: 404 Not Found if pack doesn't exist for this event

---

## State Transitions

### Current State: Add-Only Behavior

```
Initial: Pack has options [A(req), B(opt)]

Request: Add [C(req), D(opt)]

Validation:
  - Check C and D not already attached → Fail if attached
  
Result: Pack has [A(req), B(opt), C(req), D(opt)]
```

### New State: Synchronization Behavior

```
Initial: Pack has options [A(req), B(opt), C(opt)]

Request: Sync to [B(req), D(opt)]

Operations:
  1. Delete: Remove A and C (not in submitted list)
  2. Update: B remains but changes from optional to required
  3. Insert: Add D as optional
  
Result: Pack has [B(req), D(opt)]
```

**Key Difference**: Final state always matches submitted configuration exactly.

---

## Data Integrity Guarantees

### Transaction Boundaries

All synchronization operations occur within a single Exposed `transaction {}` block:

```kotlin
override fun attachOptionsToPack(...) = transaction {
    // 1. Validate pack and options exist for event
    // 2. Validate no duplicates in required + optional
    // 3. Delete removed options
    // 4. Update changed requirement status
    // 5. Insert new options
    // All steps succeed or all fail (ACID)
}
```

**Guarantees**:
- **Atomicity**: All changes commit together or roll back together (FR-009)
- **Consistency**: Foreign key constraints prevent orphaned attachments
- **Isolation**: Database transaction isolation handles concurrent requests
- **Durability**: PostgreSQL ensures committed changes persist

### Cascade Behavior

**If Pack Deleted**: All pack-option attachments automatically deleted (CASCADE)  
**If Option Deleted**: All pack-option attachments for that option automatically deleted (CASCADE)  
**If Event Deleted**: All packs, options, and attachments cascade delete

---

## Query Patterns

### Existing Queries (from current implementation)

```kotlin
// Fetch options for validation
val requiredOptions = SponsoringOptionEntity.find {
    (SponsoringOptionsTable.eventId eq eventId) and
    (SponsoringOptionsTable.id inList options.required.map(UUID::fromString))
}

// Check for existing attachments
val alreadyAttached = PackOptionsTable
    .selectAll()
    .where { 
        (PackOptionsTable.pack eq packId) and 
        (PackOptionsTable.option inList allOptionIds) 
    }
```

### New Queries (for synchronization)

```kotlin
// Delete attachments not in submitted lists
PackOptionsTable.deleteWhere {
    (pack eq packId) and (option notInList submittedOptionIds)
}

// Update requirement status for options that changed
PackOptionsTable.update({ 
    (pack eq packId) and (option inList changedOptionIds) 
}) {
    it[required] = newRequiredStatus
}

// Insert new attachments (same as current implementation)
PackOptionsTable.insert {
    it[pack] = packId
    it[option] = optionId
    it[required] = isRequired
}
```

---

## Performance Characteristics

### Database Operations Count

**Current Implementation** (add-only):
- 2 SELECT queries (fetch required + optional options)
- 1 SELECT query (check if already attached)
- N INSERT queries (N = number of new options)
- **Total**: 3 + N queries

**New Implementation** (synchronization):
- 2 SELECT queries (fetch required + optional options for validation)
- 1 DELETE query (remove unmatched attachments - bulk operation)
- 1 UPDATE query (change requirement status - bulk operation, conditional)
- M INSERT queries (M = number of new options to add)
- **Total**: 3 + M queries (+ 1 UPDATE if status changes exist)

**Performance Impact**: Minimal - bulk DELETE/UPDATE are efficient operations. Target of 500ms for 50 options easily achievable.

---

## Schema Migration

**Required**: None

**Rationale**: This feature uses existing schema without modifications. The `PackOptionsTable` already supports the required/optional flag and composite primary key needed for synchronization.

---

## Summary

- **No schema changes** - feature uses existing tables
- **Composite PK** ensures one attachment per pack-option pair
- **CASCADE deletes** maintain referential integrity
- **Single transaction** guarantees atomic synchronization
- **Bulk operations** provide efficient synchronization for large option lists
