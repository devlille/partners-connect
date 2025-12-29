# Research: Enhanced Sponsoring Options Implementation

## Current System Analysis

### Existing Sponsoring Option Structure
**Current Implementation**: Text-based options with multilingual translations
- `SponsoringOption` entity with id, name, description, price
- `OptionTranslationEntity` for multilingual support (name/description per language)
- Pack-option associations via `PackOptionsTable` (required/optional)
- Partnership selections via `PartnershipOptionEntity`

**Extension Points Identified**:
- Domain models can accommodate additional metadata without breaking changes
- Translation system already supports all needed languages
- Pack-option relationship structure supports all four option types
- Partnership creation flow allows for selection input handling

### Database Schema Extension Strategy

**Decision**: Extend existing Exposed ORM dual-layer structure
**Rationale**: 
- Constitution requires Exposed ORM with Table/Entity pattern
- Existing `SponsoringOptionsTable` can be extended with optional columns
- `PartnershipOptionsTable` can store partner selections (quantity, selected values)
- Backward compatibility maintained through nullable columns

**Schema Changes Required**:
```kotlin
// Extend SponsoringOptionsTable
val optionType = enumerationByName<OptionType>("option_type").default(OptionType.TEXT)
val typeDescriptor = varchar("type_descriptor", 255).nullable()
val fixedQuantity = integer("fixed_quantity").nullable()

// New table for selectable values
object SelectableValuesTable : UUIDTable("selectable_values") {
    val optionId = reference("option_id", SponsoringOptionsTable, onDelete = ReferenceOption.CASCADE)
    val value = varchar("value", 255)
}

// Extend PartnershipOptionsTable  
val selectedQuantity = integer("selected_quantity").nullable()
val selectedValue = varchar("selected_value", 255).nullable()
```

### Option Type Implementation Strategy

**Decision**: Polymorphic sealed classes with @JsonClassDiscriminator and enum-based type descriptors
**Rationale**: 
- Clean polymorphic serialization with discriminated unions
- Type safety for business logic with sealed class hierarchy
- Enum-based type descriptors provide validation and extensibility
- @SerialName annotations ensure snake_case JSON serialization
- Consistent with kotlinx-serialization patterns

**Implementation Pattern**:
```kotlin
@Serializable
@JsonClassDiscriminator("type")
sealed class SponsoringOption {
    @Serializable
    @SerialName("text")
    data class Text(...) : SponsoringOption()
    
    @Serializable
    @SerialName("typed_quantitative")
    data class TypedQuantitative(
        @SerialName("type_descriptor") val typeDescriptor: QuantitativeDescriptor,
        ...
    ) : SponsoringOption()
    
    @Serializable
    @SerialName("typed_number")
    data class TypedNumber(
        @SerialName("type_descriptor") val typeDescriptor: NumberDescriptor,
        @SerialName("fixed_quantity") val fixedQuantity: Int,
        ...
    ) : SponsoringOption()
    
    @Serializable
    @SerialName("typed_selectable")
    data class TypedSelectable(
        @SerialName("type_descriptor") val typeDescriptor: SelectableDescriptor,
        @SerialName("selectable_values") val selectableValues: List<String>,
        ...
    ) : SponsoringOption()
}

enum class QuantitativeDescriptor { @SerialName("job_offer") JOB_OFFER }
enum class NumberDescriptor { @SerialName("nb_ticket") NB_TICKET }  
enum class SelectableDescriptor { @SerialName("booth") BOOTH }
```

### Pricing Strategy for Quantitative Options

**Decision**: Linear scaling with base price multiplication
**Rationale**: 
- Clarification confirmed: base_price × selected_quantity
- Simple calculation, predictable for partners
- Avoids complex tier-based pricing systems
- Consistent with existing price calculations

**Implementation**: 
- Option price remains per-unit cost
- Partnership total = sum(option.price × selectedQuantity) for all selected options
- Zero quantity treated as "option not selected" (excluded from partnership)

### API Response Strategy

**Decision**: Polymorphic JSON responses with discriminated unions
**Rationale**: 
- Frontend can consume polymorphic responses without changes
- Type discrimination through "type" field enables conditional rendering
- Backward compatibility maintained for existing text options
- Clean separation between API contract and UI implementation

**API Response Format**:
- Discriminated unions with @JsonClassDiscriminator("type")
- Snake_case field naming for consistency
- Enum-based type descriptors for validation
- Existing frontend can ignore new fields gracefully

### Validation Strategy

**Decision**: Server-side domain model validation with comprehensive rules
**Rationale**: 
- Backend: Domain model validation in repository layer
- Sealed class constraints enforce type safety
- Consistent error handling via StatusPages
- Frontend validation can be added later without API changes

**Validation Rules**:
- Typed Quantitative: Positive integers only, zero excluded from partnership
- Typed Number: Fixed quantity > 0 at creation
- Typed Selectable: At least one selectable value required, prevent deletion of used values
- All types: Require type descriptor when not TEXT

### Migration Strategy

**Decision**: Database backward compatibility with nullable columns, breaking model changes allowed
**Rationale**: 
- Clarification confirmed: Model backward compatibility not required
- Database schema must maintain compatibility through nullable columns
- Polymorphic sealed classes provide cleaner design than compatibility branches
- Zero-downtime deployment through additive schema changes

**Migration Steps**:
1. Add nullable columns to existing tables (type_descriptor, fixed_quantity, etc.)
2. Deploy application code with polymorphic sealed classes
3. Existing records treated as Text type (type field = "text")
4. New options use appropriate sealed class implementations

### Performance Considerations

**Decision**: Maintain existing query patterns with minimal joins
**Rationale**: 
- Option metadata stored in same table (avoid additional joins)
- Selectable values in separate table (normalized, supports indexing)
- Partnership queries unchanged (selections stored in existing relationship table)
- Meets <2 second response time requirement

### Testing Strategy

**Decision**: HTTP route integration tests covering all option types
**Rationale**: 
- Constitutional requirement: No repository unit tests
- End-to-end validation of serialization, validation, database operations
- Each option type requires creation, selection, and pricing test scenarios
- Edge case coverage (zero quantity, missing values, deletion prevention)

**Test Coverage Areas**:
- Option creation for each type with validation
- Partnership creation with selections for each type
- Pricing calculations for quantitative options
- Error handling for invalid selections
- Backward compatibility with existing text options

## Risk Assessment

**Low Risk**: 
- Extends well-established patterns
- Maintains backward compatibility
- No breaking API changes
- Uses existing infrastructure

**Medium Risk**: 
- Database migration coordination
- Validation rule complexity
- Polymorphic serialization edge cases

**Mitigation Strategies**:
- Comprehensive integration tests prevent regressions
- Gradual rollout with backward compatible API responses
- Clear documentation for API consumers
- Fallback serialization for unknown types

## Implementation Dependencies

**Existing Infrastructure**:
- Exposed ORM dual-layer structure
- Ktor routing and StatusPages
- Koin dependency injection
- kotlinx-serialization with polymorphic support

**No Additional Dependencies**: All functionality achievable with current tech stack

**Alternative Approaches Considered**

**Single Model with Optional Fields**: Rejected - leads to complex validation logic and unclear contracts
**Separate Tables per Option Type**: Rejected - increases complexity, breaks existing patterns
**JSON Column for Metadata**: Rejected - loses type safety, harder to query/validate
**Repository Pattern Changes**: Rejected - constitutional violation
**Model Backward Compatibility**: Rejected - clarification confirmed breaking changes allowed for cleaner design

## Performance Validation

**Query Impact**: Minimal - single table extension with optional joins for selectable values
**Memory Impact**: Negligible - metadata fields are small
**Response Time**: Expected <2 seconds maintained through existing query optimization patterns