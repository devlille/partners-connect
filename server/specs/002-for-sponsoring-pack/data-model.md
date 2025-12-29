# Data Model: Multi-Language Sponsoring Pack and Option Management for Organizers

**Feature**: Remove Accept-Language header dependency from organizer-facing sponsoring endpoints  
**Date**: October 4, 2025  
**Context**: Domain entities and relationships for multi-language organizer management

## Domain Entities

### SponsoringPackWithTranslations (New)
**Purpose**: Organizer-facing sponsoring package with complete translation data  
**Source**: Extension of existing `SponsoringPack` domain entity  
**Key Fields**:
- `id: String` - Unique identifier (UUID as string)
- `name: String` - Package name (single language, typically default)
- `basePrice: Int` - Base price in cents
- `maxQuantity: Int?` - Maximum quantity available (nullable)
- `requiredOptions: List<SponsoringOptionWithTranslations>` - Required options with all translations
- `optionalOptions: List<SponsoringOptionWithTranslations>` - Optional add-ons with all translations

**Business Rules**:
- Contains same core data as public `SponsoringPack` but with enhanced option translation data
- Used exclusively for organizer management endpoints under `/orgs/{orgSlug}` paths
- Backward compatible with existing package management workflows

**JSON Serialization**:
```kotlin
@Serializable
data class SponsoringPackWithTranslations(
    val id: String,
    val name: String,
    val basePrice: Int,
    val maxQuantity: Int?,
    val requiredOptions: List<SponsoringOptionWithTranslations>,
    val optionalOptions: List<SponsoringOptionWithTranslations>,
)
```

### SponsoringOptionWithTranslations (New)
**Purpose**: Organizer-facing sponsoring option with complete translation data for all available languages  
**Source**: Extension of existing `SponsoringOption` domain entity  
**Key Fields**:
- `id: String` - Unique identifier (UUID as string)
- `translations: Map<String, OptionTranslation>` - All available translations keyed by language code
- `price: Int?` - Additional cost in cents (nullable for free options)

**Business Rules**:
- Contains all translation data for organizer review and management
- Language codes follow ISO 639-1 standard (e.g., "en", "fr", "de")
- Translations map may be empty if no translations exist (graceful degradation)
- Used exclusively for organizer management endpoints

**JSON Serialization**:
```kotlin
@Serializable
data class SponsoringOptionWithTranslations(
    val id: String,
    val translations: Map<String, OptionTranslation>,
    val price: Int?,
)
```

### OptionTranslation (New)
**Purpose**: Individual language-specific translation data for an option  
**Source**: Maps to existing `OptionTranslationsTable` database records  
**Key Fields**:
- `language: String` - ISO 639-1 language code (e.g., "en", "fr")
- `name: String` - Translated option name
- `description: String?` - Translated option description (nullable)

**Business Rules**:
- Language code must be valid ISO 639-1 format
- Name is required for each translation
- Description is optional but recommended for user clarity
- Represents single translation entry from existing database infrastructure

**JSON Serialization**:
```kotlin
@Serializable
data class OptionTranslation(
    val language: String,
    val name: String,
    val description: String?,
)
```

## Repository Interface Changes

### PackRepository (Modified)
**New Methods**:
```kotlin
// For organizer management - returns all translation data
fun findPacksByEventWithAllTranslations(eventSlug: String): List<SponsoringPackWithTranslations>
fun getByIdWithAllTranslations(eventSlug: String, packId: UUID): SponsoringPackWithTranslations

// Existing methods preserved for public endpoints
fun findPacksByEvent(eventSlug: String, language: String): List<SponsoringPack>
fun getById(eventSlug: String, packId: UUID, language: String): SponsoringPack
// ... other existing methods unchanged
```

### OptionRepository (Modified)  
**New Methods**:
```kotlin
// For organizer management - returns all translation data
fun listOptionsByEventWithAllTranslations(eventSlug: String): List<SponsoringOptionWithTranslations>

// Existing methods preserved for public endpoints  
fun listOptionsByEvent(eventSlug: String, language: String): List<SponsoringOption>
// ... other existing methods unchanged
```

## Database Schema (No Changes)

### Existing Tables Used
- `SponsoringPacksTable` - Package core data (unchanged)
- `SponsoringOptionsTable` - Option core data (unchanged)  
- `OptionTranslationsTable` - Translation data (unchanged)
- `PackOptionsTable` - Pack-option relationships (unchanged)

**Note**: No database schema changes required. All new functionality leverages existing translation infrastructure through modified queries and data mapping.

## Entity Relationships

```
SponsoringPackWithTranslations
├── requiredOptions: List<SponsoringOptionWithTranslations>
└── optionalOptions: List<SponsoringOptionWithTranslations>

SponsoringOptionWithTranslations  
└── translations: Map<String, OptionTranslation>
    ├── "en" -> OptionTranslation(language="en", name="...", description="...")
    ├── "fr" -> OptionTranslation(language="fr", name="...", description="...")
    └── "de" -> OptionTranslation(language="de", name="...", description="...")
```

## Mapping Strategy

### From Database to Domain
```kotlin
// PackRepositoryExposed implementation
fun findPacksByEventWithAllTranslations(eventSlug: String): List<SponsoringPackWithTranslations> {
    // 1. Load pack entities (existing logic)
    // 2. For each pack, load associated options
    // 3. For each option, load ALL translations (not filtered by language)
    // 4. Map to SponsoringPackWithTranslations using new extension function
}

// New extension function
fun SponsoringPackEntity.toDomainWithAllTranslations(): SponsoringPackWithTranslations {
    // Map core pack data + call option.toDomainWithAllTranslations() for each option
}

fun SponsoringOptionEntity.toDomainWithAllTranslations(): SponsoringOptionWithTranslations {
    // Load all translations for this option from OptionTranslationsTable
    // Build translations map keyed by language code
}
```

## Validation Rules

### Request Validation
- No new request validation required (removing Accept-Language header requirement)
- Existing authentication and authorization rules apply unchanged
- Route parameters (orgSlug, eventSlug) validation unchanged

### Response Validation  
- Translation maps may be empty (graceful degradation)
- Language codes in translation maps must be valid ISO 639-1 format
- Each translation must have non-empty name field
- Price fields follow existing validation rules (non-negative integers)

## Error Handling

### Translation Missing Scenarios
- **No translations exist**: Return empty translations map, do not fail
- **Partial translations**: Return available translations only, do not require all languages
- **Invalid language codes**: Skip invalid entries, include valid ones
- **Missing translation names**: Skip incomplete translation entries

### Backward Compatibility
- Existing public endpoints continue to work unchanged
- Existing error codes and messages preserved for public APIs
- New organizer endpoints follow same error response format as existing organizer APIs

## Performance Considerations

### Query Optimization
- Leverage existing optimized queries in OptionTranslationsTable
- Use existing entity relationships and joins
- No new N+1 query patterns introduced
- Translation loading batched per option (not per language)

### Response Size Impact
- Organizer responses will be larger due to complete translation data
- Acceptable trade-off for organizer management use case
- No impact on public endpoint performance
- Client-side caching can mitigate repeated large responses

## Migration Strategy

### Backward Compatibility
- No database migrations required
- Existing data structures fully compatible
- Existing public endpoints unchanged
- Existing frontend integrations unaffected

### Rollout Plan
1. Deploy new repository methods (no breaking changes)
2. Update organizer routes to use new methods
3. Update OpenAPI documentation
4. Update frontend types if needed
5. Monitor performance and error rates

This data model design ensures clean separation between organizer management needs and public API requirements while leveraging existing, proven infrastructure.