# Data Model: Event Sponsoring Packs Public API

**Date**: 2025-10-02  
**Feature**: Public GET endpoint for event sponsoring packages

## Domain Entities

### Event
**Purpose**: Represents an event identified by its unique slug
**Source**: Existing `EventEntity` from events domain module
**Key Fields**:
- `id: UUID` - Primary key
- `slug: String` - Unique identifier for public access
- `name: String` - Event display name

**Relationships**:
- One-to-many with SponsoringPack (via `eventId` foreign key)

**Validation Rules**:
- Slug must be unique across all events
- Slug format validation to prevent special character issues

### SponsoringPack
**Purpose**: Represents a sponsorship tier/package with pricing and benefits
**Source**: Existing `SponsoringPack` data class in sponsoring domain
**Key Fields**:
- `id: String` - Unique identifier (UUID as string)
- `name: String` - Package display name
- `basePrice: Int` - Base cost in cents
- `maxQuantity: Int?` - Maximum number available (nullable for unlimited)
- `requiredOptions: List<SponsoringOption>` - Automatically included options
- `optionalOptions: List<SponsoringOption>` - Available add-on options

**Business Rules**:
- Base price must be non-negative
- Max quantity null indicates unlimited availability
- Required options are always included in the package
- Optional options can be selected individually by sponsors

**JSON Serialization**:
```kotlin
@Serializable
data class SponsoringPack(
    val id: String,
    val name: String,
    @SerialName("base_price")
    val basePrice: Int,
    @SerialName("max_quantity")
    val maxQuantity: Int?,
    @SerialName("required_options")
    val requiredOptions: List<SponsoringOption>,
    @SerialName("optional_options")
    val optionalOptions: List<SponsoringOption>,
)
```

### SponsoringOption
**Purpose**: Represents additional benefits or services that can be included with packages
**Source**: Existing `SponsoringOption` data class in sponsoring domain
**Key Fields**:
- `id: String` - Unique identifier (UUID as string)
- `name: String` - Localized option name
- `description: String?` - Localized option description (nullable)
- `price: Int?` - Additional cost in cents (nullable for free options)

**Business Rules**:
- Name must be provided (translated based on Accept-Language header)
- Description is optional
- Price null indicates free option
- Translations retrieved from `OptionTranslationsTable`

**JSON Serialization**:
```kotlin
@Serializable
data class SponsoringOption(
    val id: String,
    val name: String,
    val description: String?,
    val price: Int?,
)
```

## Repository Interfaces

### EventPackRepository (New)
**Purpose**: Provides public access to sponsoring packages for events
**Location**: `fr.devlille.partners.connect.sponsoring.domain.EventPackRepository`

```kotlin
interface EventPackRepository {
    /**
     * Retrieves all sponsoring packages for a public event by slug
     * @param eventSlug The unique event identifier
     * @param language Language code from Accept-Language header for translations
     * @return List of sponsoring packages with localized options
     * @throws NotFoundException if event does not exist
     */
    fun findPublicPacksByEvent(eventSlug: String, language: String): List<SponsoringPack>
}
```

**Implementation**: `EventPackRepositoryExposed` in application layer
- Reuses existing database queries from `PackRepositoryExposed`
- Leverages `EventEntity.findBySlug()` for event resolution
- Uses `SponsoringPackEntity.toDomain()` for translation and mapping

## Database Schema

### Existing Tables (Reused)
- `events` - Event master data with slug index
- `sponsoring_packs` - Package definitions with event foreign key
- `sponsoring_options` - Option definitions with event foreign key  
- `pack_options` - Many-to-many relationship with required flag
- `option_translations` - Localized option names and descriptions

### Key Relationships
```sql
events.id → sponsoring_packs.event_id (one-to-many)
events.id → sponsoring_options.event_id (one-to-many)
sponsoring_packs.id → pack_options.pack_id (many-to-many)
sponsoring_options.id → pack_options.option_id (many-to-many)
sponsoring_options.id → option_translations.option_id (one-to-many)
```

### Indexes
- `events.slug` - Unique index for event lookup
- `sponsoring_packs.event_id` - Foreign key index
- `pack_options(pack_id, option_id)` - Composite primary key
- `option_translations(option_id, language)` - Unique composite index

## State Transitions

### API Response States
1. **Success (200 OK)**: Event exists, returns packages (may be empty list)
2. **Not Found (404)**: Event slug does not exist
3. **Server Error (500)**: Database or translation lookup failure

### Data Consistency
- No state mutations in this read-only endpoint
- Data consistency maintained by existing database constraints
- Translation fallbacks handled in existing `toDomain()` mapper

## Validation Rules

### Input Validation
- `eventSlug`: Required, non-empty string
- `Accept-Language`: Optional header, defaults handled by existing translation logic

### Output Validation  
- All SponsoringPack objects must have valid UUIDs as string IDs
- Base prices must be non-negative integers (cents)
- Option prices must be non-negative when present
- Localized names must be non-empty strings

### Business Rules
- Public endpoint requires no authentication
- Event must exist and be accessible
- Packages returned regardless of availability or max quantity
- Options include both required (embedded) and optional (add-on) types