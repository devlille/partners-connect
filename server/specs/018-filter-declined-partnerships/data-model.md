# Data Model: Filter Partnerships by Declined Status

**Feature Branch**: `018-filter-declined-partnerships`
**Phase**: 1 - Design
**Date**: March 2, 2026

---

## No Schema Changes Required

This feature requires **no database schema changes**. The `declinedAt` column already exists on the `partnerships` table:

```
partnerships.declined_at  DATETIME  NULLABLE
```

The filter operates by querying whether this column is NULL (non-declined, default) or NOT NULL (declined).

---

## Domain Model Changes

### PartnershipFilters (modified)

**File**: `partnership/domain/PartnershipItem.kt`

```kotlin
@Serializable
data class PartnershipFilters(
    val packId: String? = null,
    val validated: Boolean? = null,
    val suggestion: Boolean? = null,
    val paid: Boolean? = null,
    val agreementGenerated: Boolean? = null,
    val agreementSigned: Boolean? = null,
    val organiser: String? = null,
    val declined: Boolean = false,   // NEW — non-nullable, default = false (exclude declined)
)
```

**Why non-nullable with `false` default**: Unlike other boolean filters where `null` means "no constraint," `declined = false` must actively add a `WHERE declinedAt IS NULL` constraint. This ensures the breaking-change default (exclude declined) is embedded in the domain model itself rather than being hidden in route parsing logic.

---

## Query Logic Changes

### PartnershipEntity.filters() (modified)

**File**: `partnership/infrastructure/db/PartnershipEntity.kt`

New `declined` parameter and corresponding constraint:

| `declined` value | SQL condition applied |
|---|---|
| `false` (default) | `AND declined_at IS NULL` |
| `true` | *(no constraint — return all regardless of declinedAt)* |

The existing logic pattern maps exactly:

```kotlin
fun filters(
    eventId: UUID,
    packId: UUID?,
    validated: Boolean?,
    suggestion: Boolean?,
    agreementGenerated: Boolean?,
    agreementSigned: Boolean?,
    organiserUserId: UUID?,
    declined: Boolean = false,       // NEW
): SizedIterable<PartnershipEntity> {
    var op = PartnershipsTable.eventId eq eventId
    // ... existing filters ...
    if (!declined) {
        op = op and (PartnershipsTable.declinedAt.isNull())
    }
    return find { op }
}
```

---

## Metadata Changes

### PaginationMetadata filters list (modified)

**File**: `partnership/application/PartnershipRepositoryExposed.kt` — `buildMetadata()`

A new entry is added at the end of the `filters` list:

```kotlin
FilterDefinition("declined", FilterType.BOOLEAN),
```

This keeps the discoverability contract established in spec 015 complete.

---

## Validation Changes

### Boolean query parameter parsing

**File**: `partnership/infrastructure/api/PartnershipRoutes.kt` and `PartnershipEmailRoutes.kt`

A strict boolean parser is needed to satisfy FR-005 (HTTP 400 for invalid values). Kotlin's built-in `String.toBoolean()` silently converts any non-"true" string to `false`.

New helper (added to `StringValues.ext.kt` or inline):

```kotlin
/**
 * Parses a boolean query parameter strictly.
 * Returns [default] if the parameter is absent.
 * Throws [BadRequestException] if the parameter is present but not "true" or "false" (case-insensitive).
 */
fun String?.toBooleanStrict(paramName: String, default: Boolean): Boolean {
    if (this == null) return default
    return when (this.lowercase()) {
        "true" -> true
        "false" -> false
        else -> throw BadRequestException("Invalid boolean value '$this' for parameter '$paramName'. Must be 'true' or 'false'.")
    }
}
```

Usage in routes:
```kotlin
declined = call.request.queryParameters["filter[declined]"]
    .toBooleanStrict("filter[declined]", default = false),
```

---

## Validation Rules Summary

| Rule | Behaviour |
|---|---|
| `filter[declined]` absent | Default `false` → exclude declined partnerships |
| `filter[declined]=false` | Explicitly exclude declined partnerships |
| `filter[declined]=true` | Include declined partnerships alongside others |
| `filter[declined]=anything-else` | HTTP 400 Bad Request |
