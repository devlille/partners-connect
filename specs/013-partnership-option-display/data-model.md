# Data Model: Display Partnership-Specific Options

**Date**: November 30, 2025  
**Phase**: 1 (Design & Contracts)

## Overview

This document defines the enhanced data model for partnership details, focusing on partnership-specific options with complete descriptions and pricing breakdown. The model extends existing domain structures without requiring database schema changes.

---

## Domain Models

### Enhanced PartnershipDetail

**Purpose**: Extended partnership detail with complete option and pricing information.

**Location**: `server/application/src/main/kotlin/fr/devlille/partners/connect/partnership/domain/PartnershipDetail.kt`

```kotlin
@Serializable
data class PartnershipDetail(
    val id: String,
    val phone: String? = null,
    @SerialName("contact_name")
    val contactName: String,
    @SerialName("contact_role")
    val contactRole: String,
    val language: String,
    val emails: List<String> = emptyList(),
    @SerialName("selected_pack")
    val selectedPack: PartnershipPack? = null,
    @SerialName("suggestion_pack")
    val suggestionPack: PartnershipPack? = null,
    @SerialName("validated_pack")
    val validatedPack: PartnershipPack? = null,
    val organiser: User? = null,
    @SerialName("process_status")
    val processStatus: PartnershipProcessStatus,
    @SerialName("created_at")
    val createdAt: String,
    
    // NEW FIELD
    val currency: String = "EUR",  // Currency code
)
```

**Changes from Existing**:
- Added `currency`: Currency code (always "EUR" per assumptions)

**Rationale**:
- Currency field enables invoice/quote generation without hardcoding
- Total amount moved to PartnershipPack level (see below)
- Maintains backward compatibility by adding fields (not removing/changing existing ones)

---

### Enhanced PartnershipPack

**Purpose**: Partnership pack with complete option details and pricing.

**Location**: `server/application/src/main/kotlin/fr/devlille/partners/connect/partnership/domain/PartnershipPack.kt`

```kotlin
@Serializable
class PartnershipPack(
    val id: String,
    val name: String,
    @SerialName("base_price")
    val basePrice: Int,
    @SerialName("required_options")
    val requiredOptions: List<PartnershipOption>,  // Options included in base pack
    @SerialName("optional_options")
    val optionalOptions: List<PartnershipOption>,  // Additional options selected by partner
    @SerialName("total_price")
    val totalPrice: Int,  // NEW: Total pack price (base + optional options) in cents
)
```

**Changes from Existing**:
- Replaced single `options` field with two separate lists: `requiredOptions` and `optionalOptions`
- Added `totalPrice`: Computed total for this pack (base price + sum of optional option amounts)
- Both lists contain `PartnershipOption` (was `List<SponsoringOption>`)
- Each option includes complete description and pricing details

**Rationale**:
- Separate lists make it explicit which options are included in base price vs additional cost
- Eliminates need for `required` boolean flag on each option
- Simplifies UI rendering (can display required and optional sections separately)
- Total amount at pack level allows comparing costs across selected/suggestion/validated packs
- Complete descriptions ready for document generation per FR-024
- Breaking change acceptable per user requirement ("don't worry about backward compatibility")

---

### NEW: PartnershipOption

**Purpose**: Partnership-specific option with complete description, selection data, and pricing.

**Location**: `server/application/src/main/kotlin/fr/devlille/partners/connect/partnership/domain/PartnershipOption.kt`

```kotlin
/**
 * Polymorphic sealed class for partnership-specific options.
 * Includes complete formatted descriptions ready for document generation
 * and pricing details for invoice/quote/agreement workflows.
 */
@OptIn(ExperimentalSerializationApi::class)
@Serializable
@JsonClassDiscriminator("type")
sealed class PartnershipOption {
    abstract val id: String
    abstract val name: String
    abstract val description: String  // Original description from translation
    abstract val completeDescription: String  // Formatted description with value
    abstract val price: Int  // Unit price for this option in cents
    abstract val quantity: Int  // Quantity for this partnership
    abstract val totalPrice: Int  // Total cost for this option (price × quantity or specific logic)
}

/**
 * Text-based partnership option (no quantity or selectable value).
 */
@Serializable
@SerialName("text")
data class TextPartnershipOption(
    override val id: String,
    override val name: String,
    override val description: String,
    @SerialName("complete_description")
    override val completeDescription: String,  // Same as description
    override val price: Int,
    override val quantity: Int = 1,
    @SerialName("total_price")
    override val totalPrice: Int,
) : PartnershipOption()

/**
 * Quantitative partnership option with user-selected quantity.
 */
@Serializable
@SerialName("typed_quantitative")
data class QuantitativePartnershipOption(
    override val id: String,
    override val name: String,
    override val description: String,
    @SerialName("complete_description")
    override val completeDescription: String,  // "Description (quantity)"
    override val price: Int,
    override val quantity: Int,  // User-selected quantity
    @SerialName("total_price")
    override val totalPrice: Int,  // price × quantity
    @SerialName("type_descriptor")
    val typeDescriptor: QuantitativeDescriptor,
) : PartnershipOption()

/**
 * Number-based partnership option with fixed quantity.
 */
@Serializable
@SerialName("typed_number")
data class NumberPartnershipOption(
    override val id: String,
    override val name: String,
    override val description: String,
    @SerialName("complete_description")
    override val completeDescription: String,  // "Description (fixedQuantity)"
    override val price: Int,
    override val quantity: Int,  // Fixed quantity from option definition
    @SerialName("total_price")
    override val totalPrice: Int,  // price × fixedQuantity
    @SerialName("type_descriptor")
    val typeDescriptor: NumberDescriptor,
) : PartnershipOption()

/**
 * Selectable partnership option with user-chosen value.
 */
@Serializable
@SerialName("typed_selectable")
data class SelectablePartnershipOption(
    override val id: String,
    override val name: String,
    override val description: String,
    @SerialName("complete_description")
    override val completeDescription: String,  // "Description (selectedValueName)"
    override val price: Int,
    override val quantity: Int = 1,
    @SerialName("total_price")
    override val totalPrice: Int,  // Selected value's price
    @SerialName("type_descriptor")
    val typeDescriptor: SelectableDescriptor,
    @SerialName("selected_value")
    val selectedValue: SelectedValue,  // The value chosen by user
) : PartnershipOption()

/**
 * Selected value for selectable options.
 */
@Serializable
data class SelectedValue(
    val id: String,
    val value: String,  // Display name of selected value
    val price: Int,  // Price for this specific value
)
```

**Design Decisions**:
- **Sealed class hierarchy**: Follows existing `SponsoringOption` pattern for type safety
- **Complete description**: Separate field from original description enables both structured and formatted access
- **Pricing fields**: Include price (unit price), quantity, total_price for all types (enables consistent invoice generation)
- **No required flag**: Required vs optional distinction handled by separate lists in PartnershipPack
- **Type descriptors**: Maintain compatibility with existing option type system

**Calculation Logic**:
- **Text**: `totalPrice = price` (quantity always 1)
- **Quantitative**: `totalPrice = price × selectedQuantity`
- **Number**: `totalPrice = price × fixedQuantity`
- **Selectable**: `totalPrice = selectedValue.price` (quantity always 1)lected value)

**Note**: Required options are in `requiredOptions` list (cost included in base price), optional options are in `optionalOptions` list (additional cost).

---

## Entity-to-Domain Mapping

### PartnershipOptionEntity Extension

**Purpose**: Map database entities to partnership option domain models with complete descriptions.

**Location**: `server/application/src/main/kotlin/fr/devlille/partners/connect/partnership/application/mappers/PartnershipOptionEntity.ext.kt` (NEW FILE)

```kotlin
/**
 * Maps PartnershipOptionEntity to PartnershipOption domain model.
 * Generates complete description by merging original description with selected value.
 */
internal fun PartnershipOptionEntity.toDomain(
    language: String,
): PartnershipOption {
    val translation = option.translations.firstOrNull { it.language == language }
        ?: throw ForbiddenException(
            "Option ${option.id} does not have a translation for language $language"
        )
    
    val originalDescription = translation.description ?: ""
    
    return when (option.optionType) {
        OptionType.TEXT -> TextPartnershipOption(
            id = option.id.value.toString(),
            name = translation.name,
            description = originalDescription,
            completeDescription = originalDescription,  // No value to append
            price = option.price ?: 0,
            quantity = 1,
            totalPrice = option.price ?: 0,
        )
        
        OptionType.TYPED_QUANTITATIVE -> {
            val quantity = selectedQuantity ?: 0
            val price = option.price ?: 0
            val completeDesc = if (quantity > 0) {
                "$originalDescription ($quantity)"
            } else {
                originalDescription
            }
            
            QuantitativePartnershipOption(
                id = option.id.value.toString(),
                name = translation.name,
                description = originalDescription,
                completeDescription = completeDesc,
                price = price,
                quantity = quantity,
                totalPrice = price * quantity,
                typeDescriptor = option.quantitativeDescriptor!!,
            )
        }
        
        OptionType.TYPED_NUMBER -> {
            val fixedQty = option.fixedQuantity ?: 0
            val price = option.price ?: 0
            val completeDesc = if (fixedQty > 0) {
                "$originalDescription ($fixedQty)"
            } else {
                originalDescription
            }
            
            NumberPartnershipOption(
                id = option.id.value.toString(),
                name = translation.name,
                description = originalDescription,
                completeDescription = completeDesc,
                price = price,
                quantity = fixedQty,
                totalPrice = price * fixedQty,
                typeDescriptor = option.numberDescriptor!!,
            )
        }
        
        OptionType.TYPED_SELECTABLE -> {
            val selectedVal = selectedValue
                ?: throw NotFoundException("Selected value not found for selectable option ${option.id}")
            val completeDesc = "$originalDescription (${selectedVal.value})"
            
            SelectablePartnershipOption(
                id = option.id.value.toString(),
                name = translation.name,
                description = originalDescription,
                completeDescription = completeDesc,
                price = selectedVal.price,
                quantity = 1,
                totalPrice = selectedVal.price,
                typeDescriptor = option.selectableDescriptor!!,
                selectedValue = SelectedValue(
                    id = selectedVal.id.value.toString(),
                    value = selectedVal.value,
                    price = selectedVal.price,
                ),
            )
        }
    }
}
```

**Key Points**:
- Translation validation matches existing pattern in `PartnershipRepositoryExposed`
- Complete description format follows research decision #1 (parentheses separator)
- Pricing calculations match `PartnershipBillingRepositoryExposed.computePricing()` logic
- Edge case handling: zero quantities omit the quantity suffix
- Caller determines whether option goes in requiredOptions or optionalOptions list based on PackOptionsTable
- All options have actual price values (no null prices)

---

## Repository Interface Changes

### PartnershipRepository.getByIdDetailed()

**Location**: `server/application/src/main/kotlin/fr/devlille/partners/connect/partnership/domain/PartnershipRepository.kt`

```kotlin
interface PartnershipRepository {
    // ... existing methods ...
    
    /**
     * Get detailed partnership information including company, event, organization, speakers,
     * partnership-specific options with complete descriptions, and pricing breakdown.
     *
     * @param eventSlug The event slug
     * @param partnershipId The partnership UUID
     * @return Partnership detail with enhanced option and pricing data
     * @throws NotFoundException if partnership or event not found
     * @throws ForbiddenException if option translations missing for partnership language
     */
    fun getByIdDetailed(eventSlug: String, partnershipId: UUID): PartnershipDetail
}
```

**Changes**:
- Return type unchanged (`PartnershipDetail`) but model fields enhanced
- Additional exception documented: `ForbiddenException` for missing translations
- KDoc updated to reflect enhanced data (options, pricing)

---

## Database Entities (Unchanged)

No database schema changes required. Existing entities used:

### PartnershipEntity
- Location: `server/application/src/main/kotlin/fr/devlille/partners/connect/partnership/infrastructure/db/PartnershipEntity.kt`
- Tables: `partnerships`
- Usage: Core partnership data (language, contact info, pack references)

### PartnershipOptionEntity  
- Location: `server/application/src/main/kotlin/fr/devlille/partners/connect/partnership/infrastructure/db/PartnershipOptionEntity.kt`
- Tables: `partnership_options`
- Usage: Selection data (selectedQuantity, selectedValue reference)

### SponsoringOptionEntity
- Location: `server/application/src/main/kotlin/fr/devlille/partners/connect/sponsoring/infrastructure/db/SponsoringOptionEntity.kt`
- Tables: `sponsoring_options`, `option_translations`
- Usage: Option definitions, translations, pricing

### PackOptionsTable
- Location: `server/application/src/main/kotlin/fr/devlille/partners/connect/sponsoring/infrastructure/db/PackOptionsTable.kt`
- Tables: `pack_options`
- Usage: Required vs. optional option distinction

---

## Entity Relationships

```
PartnershipEntity
  ├── selectedPack → SponsoringPackEntity
  ├── suggestionPack → SponsoringPackEntity
  └── options via PartnershipOptionEntity.listByPartnershipAndPack()
        └── option → SponsoringOptionEntity
              ├── translations → OptionTranslationEntity (language filter)
              └── selectableValues → SelectableValueEntity (for typed_selectable)

SponsoringPackEntity
  └── options via PackOptionsTable.listOptionsByPack()
        ├── required = true → included in base pack
        └── required = false → optional (additional cost)
```

**Query Strategy**:
1. Fetch `PartnershipEntity` by ID
2. Fetch `PartnershipOptionEntity` list filtered by partnership + pack
3. For each option:
   - Get `SponsoringOptionEntity` and filter translations by partnership language
   - Get selected value entity for TYPED_SELECTABLE options
   - Determine required/optional from `PackOptionsTable`
   - Map to `PartnershipOption` subclass with complete description
   - Add to `requiredOptions` or `optionalOptions` list based on PackOptionsTable flag

---

## Validation Rules

### Required Data
- Partnership language MUST match available option translations (FR-027)
- Selected pack MUST exist for pricing calculations
- Option type MUST be one of: TEXT, TYPED_QUANTITATIVE, TYPED_NUMBER, TYPED_SELECTABLE

### Pricing Constraints
- Pack base price MUST be >= 0
- All option prices MUST be >= 0
- Total price = base price + sum(optionalOptions totalPrice)
- Required options appear in `requiredOptions` list (cost included in base price)
- Optional options appear in `optionalOptions` list (additional cost beyond base price)

### Complete Description Format (FR-019 through FR-022)
- Quantitative: `"{description} ({quantity})"` 
- Selectable: `"{description} ({selectedValue})"`
- Number: `"{description} ({fixedQuantity})"`
- Text: `"{description}"` (unchanged)
- Unlimited length (FR-028)

---

## Migration Notes

**Database**: No migrations required - using existing schema.

**Code Migration**:
1. Update `PartnershipDetail` domain model with new fields
2. Create `PartnershipOption` sealed class hierarchy
3. Create `PartnershipOptionEntity.toDomain()` extension
4. Update `PartnershipRepositoryExposed.getByIdDetailed()` implementation
5. Update invoice/quote/agreement routes to consume enhanced partnership detail
6. Update JSON schemas and OpenAPI documentation

**Test Migration**:
1. Update `PartnershipDetailedGetRouteTest` assertions for new fields
2. Update mock factories to include `packOptions` data
3. Update billing/agreement tests to expect single partnership detail call

---

## Data Flow Diagram

```
GET /events/{eventSlug}/partnerships/{partnershipId}
  ↓
PartnershipRoutes.kt (unchanged route logic)
  ↓
PartnershipRepositoryExposed.getByIdDetailed()
  ├─→ Fetch PartnershipEntity
  ├─→ Fetch PartnershipOptionEntity list (partnership + pack filter)
  ├─→ For each option:
  │     ├─→ Get SponsoringOptionEntity + translations (language filter)
  │     ├─→ Get SelectableValueEntity (if TYPED_SELECTABLE)
  │     ├─→ Determine required flag (PackOptionsTable)
  │     └─→ Map to PartnershipOption with complete description + pricing
  ├─→ For each pack (selected/suggestion/validated):
  │     ├─→ Separate options into requiredOptions and optionalOptions lists
  │     └─→ Calculate totalPrice (basePrice + sum of optionalOptions totalPrice)
  └─→ Return enhanced PartnershipDetail
  ↓
DetailedPartnershipResponse envelope
  ├─→ partnership (enhanced)
  ├─→ company
  ├─→ event
  ├─→ organisation
  └─→ speakers
  ↓
JSON response to client
```

**Invoice/Quote/Agreement Flow**:
```
POST /orgs/{orgSlug}/events/{eventSlug}/partnerships/{partnershipId}/invoice
  ↓
BillingRoutes.kt
  ├─→ partnershipRepository.getByIdDetailed() (enhanced)
  ├─→ Extract validated pack pricing (basePrice, options with amounts, totalPrice)
  ├─→ Map to PartnershipPricing structure (for BillingRepository interface compatibility)
  └─→ billingRepository.createInvoice(pricing)
  ↓
Qonto/External billing system
```

---

## Performance Considerations

**Query Optimization** (already exists):
- Partnership options fetched with single query via `PartnershipOptionEntity.listByPartnershipAndPack()`
- Translations filtered in application layer (acceptable for <100 options per partnership)
- No N+1 query problems - batch fetching via Exposed

**Response Size**:
- Typical partnership: 5-8 options (per spec)
- Each option: ~200-500 bytes JSON
- Total response size increase: ~2-4 KB (negligible)

**Caching** (deferred):
- Not required for initial implementation per constitution (performance testing excluded)
- Partnership details change infrequently (suitable for future caching if needed)

---

## Summary

This data model enhances existing partnership detail responses with:
- ✅ Partnership-specific options only (required + selected optional)
- ✅ Complete formatted descriptions ready for documents
- ✅ Full pricing breakdown (base, per-option, total)
- ✅ No database schema changes
- ✅ Backward compatible envelope structure
- ✅ Reusable for invoice/quote/agreement workflows
