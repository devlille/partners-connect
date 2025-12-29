# Data Model: Enhanced Sponsoring Options

## Entity Overview

### Core Entities (Extended)

#### SponsoringOption (Extended)
**Purpose**: Represents sponsoring options with four distinct types
**Location**: `server/application/src/main/kotlin/fr/devlille/partners/connect/sponsoring/domain/SponsoringPack.kt`

```kotlin
@Serializable
@JsonClassDiscriminator("type")
sealed class SponsoringOption {
    abstract val id: String
    abstract val name: String
    abstract val description: String?
    abstract val price: Int?
    
    @Serializable
    @SerialName("TEXT")
    data class Text(
        override val id: String,
        override val name: String,
        override val description: String?,
        override val price: Int?
    ) : SponsoringOption()
    
    @Serializable
    @SerialName("TYPED_QUANTITATIVE")
    data class TypedQuantitative(
        override val id: String,
        override val name: String,
        override val description: String?,
        override val price: Int?,
        @SerialName("type_descriptor")
        val typeDescriptor: QuantitativeDescriptor
    ) : SponsoringOption()
    
    @Serializable
    @SerialName("TYPED_NUMBER")
    data class TypedNumber(
        override val id: String,
        override val name: String,
        override val description: String?,
        override val price: Int?,
        @SerialName("type_descriptor")
        val typeDescriptor: NumberDescriptor,
        @SerialName("fixed_quantity")
        val fixedQuantity: Int
    ) : SponsoringOption()
    
    @Serializable
    @SerialName("TYPED_SELECTABLE")
    data class TypedSelectable(
        override val id: String,
        override val name: String,
        override val description: String?,
        override val price: Int?,
        @SerialName("type_descriptor")
        val typeDescriptor: SelectableDescriptor,
        @SerialName("selectable_values")
        val selectableValues: List<String>
    ) : SponsoringOption()
}

@Serializable
enum class OptionType {
    @SerialName("text")
    TEXT,
    @SerialName("typed_quantitative")
    TYPED_QUANTITATIVE, 
    @SerialName("typed_number")
    TYPED_NUMBER,
    @SerialName("typed_selectable")
    TYPED_SELECTABLE
}

@Serializable
enum class QuantitativeDescriptor {
    @SerialName("job_offer")
    JOB_OFFER
}

@Serializable
enum class NumberDescriptor {
    @SerialName("nb_ticket")
    NB_TICKET
}

@Serializable
enum class SelectableDescriptor {
    @SerialName("booth")
    BOOTH
}
```

**Polymorphic Structure**:
- **Sealed Class**: `SponsoringOption` with `@JsonClassDiscriminator("type")`
- **Common Fields**: `id`, `name`, `description`, `price` (inherited by all subtypes)
- **Type-Specific Fields**: Only present in relevant subtypes

**Subtypes**:
- **Text**: Basic option with only common fields (current behavior)
- **TypedQuantitative**: Adds `typeDescriptor` for category description
- **TypedNumber**: Adds `typeDescriptor` and `fixedQuantity` for pre-set amounts
- **TypedSelectable**: Adds `typeDescriptor` and `selectableValues` for choice options

**Validation Rules**:
- TEXT: Only common fields required (name/description from translations)
- TYPED_QUANTITATIVE: Must have valid QuantitativeDescriptor enum value
- TYPED_NUMBER: Must have valid NumberDescriptor enum value and fixedQuantity > 0
- TYPED_SELECTABLE: Must have valid SelectableDescriptor enum value and at least one selectableValue

**Serialization Benefits**:
- Type-safe deserialization on client side
- No null fields in JSON (only relevant fields per type)
- Clear discriminator for client-side type handling
- Prevents invalid field combinations

#### PartnershipOption (New Domain Class)
**Purpose**: Represents selected options within partnerships with type-specific selections
**Location**: `server/application/src/main/kotlin/fr/devlille/partners/connect/partnership/domain/PartnershipOption.kt` *(NEW FILE)*

```kotlin
@Serializable
@JsonClassDiscriminator("type")
sealed class PartnershipOption {
    abstract val id: String
    abstract val optionId: String
    abstract val name: String
    abstract val description: String?
    abstract val price: Int?
    
    @Serializable
    @SerialName("TEXT")
    data class Text(
        override val id: String,
        @SerialName("option_id")
        override val optionId: String,
        override val name: String,
        override val description: String?,
        override val price: Int?
    ) : PartnershipOption()
    
    @Serializable
    @SerialName("TYPED_QUANTITATIVE")
    data class TypedQuantitative(
        override val id: String,
        @SerialName("option_id")
        override val optionId: String,
        override val name: String,
        override val description: String?,
        override val price: Int?,
        @SerialName("type_descriptor")
        val typeDescriptor: QuantitativeDescriptor,
        @SerialName("selected_quantity")
        val selectedQuantity: Int,
        @SerialName("calculated_price")
        val calculatedPrice: Int
    ) : PartnershipOption()
    
    @Serializable
    @SerialName("TYPED_NUMBER")
    data class TypedNumber(
        override val id: String,
        @SerialName("option_id")
        override val optionId: String,
        override val name: String,
        override val description: String?,
        override val price: Int?,
        @SerialName("type_descriptor")
        val typeDescriptor: NumberDescriptor,
        @SerialName("fixed_quantity")
        val fixedQuantity: Int
    ) : PartnershipOption()
    
    @Serializable
    @SerialName("TYPED_SELECTABLE")
    data class TypedSelectable(
        override val id: String,
        @SerialName("option_id")
        override val optionId: String,
        override val name: String,
        override val description: String?,
        override val price: Int?,
        @SerialName("type_descriptor")
        val typeDescriptor: SelectableDescriptor,
        @SerialName("selected_value")
        val selectedValue: String
    ) : PartnershipOption()
}
```

**Purpose**: This is a **new domain class** that represents a partner's specific selection/configuration of a `SponsoringOption`. It stores the partner's choices (quantities, values) along with a reference to the original sponsoring option template.

**Relationship Clarification**:
- **`SponsoringOption`**: Template/definition created by organizers (e.g., "Job Offers" option with price €500)
- **`PartnershipOption`**: Partner's specific selection (e.g., "5 job offers" with calculated price €2,500)
- **`optionId`**: Foreign key linking back to the original `SponsoringOption` template

**Breaking Change**:
- **Current**: Simple `SponsoringOption` data class in partnerships
- **New**: `PartnershipOption` sealed class with type-specific selection data
- **Database**: Extended with nullable columns to preserve existing data

**Selection Rules**:
- TEXT: No additional selections required (same as current behavior)
- TYPED_QUANTITATIVE: Must have selectedQuantity > 0, calculatedPrice = price × selectedQuantity
- TYPED_NUMBER: selectedQuantity automatically set to fixedQuantity from original option
- TYPED_SELECTABLE: Must have selectedValue from original option's selectableValues

### Database Schema (Extended)

#### SponsoringOptionsTable (Extended)
**Location**: `server/application/src/main/kotlin/fr/devlille/partners/connect/sponsoring/infrastructure/db/SponsoringOptionsTable.kt`

```kotlin
object SponsoringOptionsTable : UUIDTable("sponsoring_options") {
    val eventId = reference("event_id", EventsTable)
    val price = integer("price").nullable()
    // NEW COLUMNS
    val optionType = enumerationByName<OptionType>("option_type").default(OptionType.TEXT)
    val quantitativeDescriptor = enumerationByName<QuantitativeDescriptor>("quantitative_descriptor").nullable()
    val numberDescriptor = enumerationByName<NumberDescriptor>("number_descriptor").nullable()
    val selectableDescriptor = enumerationByName<SelectableDescriptor>("selectable_descriptor").nullable()
    val fixedQuantity = integer("fixed_quantity").nullable()
}
```

#### SelectableValuesTable (New)
**Purpose**: Stores available values for TYPED_SELECTABLE options
**Location**: `server/application/src/main/kotlin/fr/devlille/partners/connect/sponsoring/infrastructure/db/SelectableValuesTable.kt`

```kotlin
object SelectableValuesTable : UUIDTable("selectable_values") {
    val optionId = reference("option_id", SponsoringOptionsTable, onDelete = ReferenceOption.CASCADE)
    val value = varchar("value", 255)
    val createdAt = datetime("created_at").clientDefault { Clock.System.now().toLocalDateTime(TimeZone.UTC) }
    
    init {
        index(true, optionId, value) // Unique constraint per option
    }
}
```

#### PartnershipOptionsTable (Extended)
**Location**: `server/application/src/main/kotlin/fr/devlille/partners/connect/partnership/infrastructure/db/PartnershipOptionsTable.kt`

**Current Structure**:
```kotlin
object PartnershipOptionsTable : UUIDTable("partnership_options") {
    val partnershipId = reference("partnership_id", PartnershipsTable)
    val packId = reference("pack_id", SponsoringPacksTable)
    val optionId = reference("option_id", SponsoringOptionsTable)
}
```

**Extended Structure** (with new columns for enhanced options):
```kotlin
object PartnershipOptionsTable : UUIDTable("partnership_options") {
    val partnershipId = reference("partnership_id", PartnershipsTable)
    val packId = reference("pack_id", SponsoringPacksTable)
    val optionId = reference("option_id", SponsoringOptionsTable)
    // NEW COLUMNS for enhanced options
    val selectedQuantity = integer("selected_quantity").nullable()
    val selectedValue = varchar("selected_value", 255).nullable()
}
```

**Migration Notes**:
- New columns are nullable for backward compatibility
- Existing partnerships will have null values (valid for TEXT options)
- Primary key remains `id` (UUID) as per existing UUIDTable pattern
```

### Entity Relationships

```
Event 1---* SponsoringOption
SponsoringOption 1---* SelectableValue (for TYPED_SELECTABLE)
SponsoringOption *---* SponsoringPack (via PackOptionsTable)
Partnership *---* SponsoringOption (via PartnershipOptionsTable)
```

### State Transitions

#### Option Lifecycle
1. **Created**: Option defined with type and metadata
2. **Attached**: Associated with one or more sponsoring packs
3. **Selected**: Partner chooses option with appropriate selections
4. **Active**: Part of approved partnership
5. **Completed**: Partnership fulfilled

#### Partner Selection Flow
1. **Browse Packs**: View available options by type
2. **Select Options**: Choose from required/optional options
3. **Configure**: Provide quantities/values based on option type
4. **Validate**: System validates selections against option rules
5. **Submit**: Partnership created with selections

### Validation Rules

#### Creation Validation
- **All Types**: Must have valid translations (name required)
- **TYPED_QUANTITATIVE**: Must have valid QuantitativeDescriptor enum value
- **TYPED_NUMBER**: Must have valid NumberDescriptor enum value and fixedQuantity > 0
- **TYPED_SELECTABLE**: Must have valid SelectableDescriptor enum value and ≥1 selectableValue

#### Selection Validation
- **TYPED_QUANTITATIVE**: selectedQuantity must be positive integer
- **TYPED_SELECTABLE**: selectedValue must exist in option's selectableValues
- **Zero Quantity Rule**: quantity = 0 excludes option from partnership

#### Deletion Rules
- **Options**: Cannot delete if referenced by existing partnerships
- **Selectable Values**: Cannot delete if chosen by existing partnerships
- **Cascade Deletes**: SelectableValues deleted when parent option deleted

### Migration Strategy

#### Database Backward Compatibility (Required)
- **New columns are nullable** for backward compatibility with existing data
- **Existing options** automatically assigned `OptionType.TEXT` via migration
- **Existing partnerships** will have null values in new columns (valid for TEXT options)
- **No data loss** during migration

#### Model Breaking Changes (Allowed)
- **SponsoringOption**: data class → sealed class (API breaking change)
- **SponsoringOptionWithTranslations**: data class → sealed class (API breaking change)
- **PartnershipOption**: New sealed class replaces simple relationship
- **All APIs** return new polymorphic structures immediately

#### Data Migration Steps
1. Add new nullable columns with appropriate defaults
2. Update existing records via migration: `option_type = 'TEXT'` for all existing options
3. Deploy new API with breaking model changes
4. Existing database data remains functional through nullable columns

### Performance Considerations

#### Query Optimization
- Index on `(optionId, value)` for SelectableValuesTable uniqueness
- Existing indexes on SponsoringOptionsTable sufficient
- Partner selection queries use existing PartnershipOptionsTable indexes
- No N+1 query issues (selectableValues loaded efficiently)

#### Memory Usage
- Minimal metadata overhead per option
- SelectableValues normalized (no JSON storage)
- Existing caching strategies still applicable

### API Schema Changes

The API contracts are defined in OpenAPI format (see `contracts/api-contracts.yaml`) with discriminated unions for polymorphic option types. The server uses kotlinx-serialization with @JsonClassDiscriminator("type") to handle polymorphic serialization automatically.

#### SponsoringOptionWithTranslations (Extended for Organizers)
**Purpose**: Enhanced organizer-facing sponsoring option with complete translation data and type support
**Location**: `server/application/src/main/kotlin/fr/devlille/partners/connect/sponsoring/domain/SponsoringOptionWithTranslations.kt` *(UPDATED)*

**Current Structure**:
```kotlin
@Serializable
data class SponsoringOptionWithTranslations(
    val id: String,
    val translations: Map<String, OptionTranslation>,
    val price: Int?,
)
```

**Enhanced Structure** (supports new option types):
```kotlin
@Serializable
@JsonClassDiscriminator("type")
sealed class SponsoringOptionWithTranslations {
    abstract val id: String
    abstract val translations: Map<String, OptionTranslation>
    abstract val price: Int?
    
    @Serializable
    @SerialName("TEXT")
    data class Text(
        override val id: String,
        override val translations: Map<String, OptionTranslation>,
        override val price: Int?
    ) : SponsoringOptionWithTranslations()
    
    @Serializable
    @SerialName("TYPED_QUANTITATIVE")
    data class TypedQuantitative(
        override val id: String,
        override val translations: Map<String, OptionTranslation>,
        override val price: Int?,
        @SerialName("type_descriptor")
        val typeDescriptor: QuantitativeDescriptor
    ) : SponsoringOptionWithTranslations()
    
    @Serializable
    @SerialName("TYPED_NUMBER")
    data class TypedNumber(
        override val id: String,
        override val translations: Map<String, OptionTranslation>,
        override val price: Int?,
        @SerialName("type_descriptor")
        val typeDescriptor: NumberDescriptor,
        @SerialName("fixed_quantity")
        val fixedQuantity: Int
    ) : SponsoringOptionWithTranslations()
    
    @Serializable
    @SerialName("TYPED_SELECTABLE")
    data class TypedSelectable(
        override val id: String,
        override val translations: Map<String, OptionTranslation>,
        override val price: Int?,
        @SerialName("type_descriptor")
        val typeDescriptor: SelectableDescriptor,
        @SerialName("selectable_values")
        val selectableValues: List<String>
    ) : SponsoringOptionWithTranslations()
}
```

**Breaking Changes**:
- **Polymorphic Structure**: data class → sealed class (API breaking change)
- **Type-Specific Data**: Enhanced options include descriptors and configuration  
- **Translation Support**: All types maintain complete translation data
- **Organizer Focus**: Used exclusively for organizer management endpoints
- **No Model Compatibility**: Clients must handle new discriminated union structure

**Breaking Changes** (No Backward Compatibility Required):
- Existing `SponsoringOptionWithTranslations` data class → sealed class (breaking change)
- Existing `SponsoringOption` data class → sealed class (breaking change)  
- New `PartnershipOption` sealed class replaces simple option selection
- All organizer and partnership endpoints will return new polymorphic structures

### Testing Data Scenarios

#### Test Option Configurations
1. **Text Option**: Traditional name/description only
2. **Quantitative**: `JOB_OFFER` descriptor, partners select 1-10
3. **Fixed Number**: `NB_TICKET` descriptor, organizer sets 5
4. **Selectable**: `BOOTH` descriptor, options ["3x3m", "3x6m", "6x6m"]

#### Test Partnership Scenarios
1. **Mixed Selection**: Pack with all four option types
2. **Pricing Validation**: Quantitative option with quantity 3
3. **Edge Cases**: Zero quantity, invalid selectable value
4. **Backward Compatibility**: Existing text-only partnerships