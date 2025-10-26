# Phase 1: Data Model Design

## Database Schema Changes

### 1. SponsoringPacksTable Modifications

**Current Schema** (to be modified):
```kotlin
object SponsoringPacksTable : UUIDTable("sponsoring_packs") {
    val eventId = reference("event_id", EventsTable)
    val name = varchar("name", 255)
    val basePrice = integer("base_price")
    val withBooth = bool("with_booth").default(false)  // ❌ REMOVE THIS
    val nbTickets = integer("nb_ticket")
    val maxQuantity = integer("max_quantity").nullable()
}
```

**New Schema**:
```kotlin
object SponsoringPacksTable : UUIDTable("sponsoring_packs") {
    val eventId = reference("event_id", EventsTable)
    val name = varchar("name", 255)
    val basePrice = integer("base_price")
    val boothSize = text("booth_size").nullable()  // ✅ ADD THIS - e.g., "3x3m", "6x3m", null
    val nbTickets = integer("nb_ticket")
    val maxQuantity = integer("max_quantity").nullable()
}
```

**Migration Impact**:
- **Breaking**: Removes `withBooth` boolean column
- **Safe**: Adds nullable `boothSize` text column  
- **Backward Compatible**: Existing packs will have `boothSize = null` (valid state)
- **Data Loss**: Old boolean booth flag not preserved (acceptable - was just true/false, new model is explicit sizes)

**Rationale**: String-based booth size (e.g., "3x3m") is more expressive than boolean and prepares for future floor plan feature where partners select location based on booth dimensions.

---

### 2. PartnershipsTable Additions

**Current Schema** (to be extended):
```kotlin
object PartnershipsTable : UUIDTable("partnerships") {
    val eventId = reference("event_id", EventsTable)
    val companyId = reference("company_id", CompaniesTable)
    val selectedPackId = reference("selected_pack_id", SponsoringPacksTable).nullable()
    val validatedAt = datetime("validated_at").nullable()
    val agreementSignedUrl = text("agreement_signed_url").nullable()
    // ... other fields
}
```

**New Schema**:
```kotlin
object PartnershipsTable : UUIDTable("partnerships") {
    val eventId = reference("event_id", EventsTable)
    val companyId = reference("company_id", CompaniesTable)
    val selectedPackId = reference("selected_pack_id", SponsoringPacksTable).nullable()
    val validatedAt = datetime("validated_at").nullable()
    val agreementSignedUrl = text("agreement_signed_url").nullable()
    
    // ✅ NEW VALIDATED FIELDS
    val validatedNbTickets = integer("validated_nb_tickets").nullable()
    val validatedNbJobOffers = integer("validated_nb_job_offers").nullable()
    val validatedBoothSize = text("validated_booth_size").nullable()
    
    // ... other existing fields
}
```

**Column Details**:
| Column | Type | Nullable | Default | Description |
|--------|------|----------|---------|-------------|
| `validated_nb_tickets` | INTEGER | YES | NULL | Ticket count at validation time (overrides pack default) |
| `validated_nb_job_offers` | INTEGER | YES | NULL | Job offer count at validation time (required on validation) |
| `validated_booth_size` | TEXT | YES | NULL | Booth size at validation time (overrides pack default) |

**Migration Impact**:
- **Safe**: All new columns are nullable
- **Backward Compatible**: Existing partnerships have `null` for validated fields (handled in application logic)
- **No Data Migration**: Legacy partnerships remain functional with null values

**Rationale**: Separate validated fields preserve pack defaults while allowing organizer customization. Nullable design supports legacy partnerships and phased rollout.

---

## Entity Model Changes

### 1. SponsoringPackEntity

**Current Entity**:
```kotlin
class SponsoringPackEntity(id: EntityID<UUID>) : UUIDEntity(id) {
    companion object : UUIDEntityClass<SponsoringPackEntity>(SponsoringPacksTable)
    
    var event by EventEntity referencedOn SponsoringPacksTable.eventId
    var name by SponsoringPacksTable.name
    var basePrice by SponsoringPacksTable.basePrice
    var withBooth by SponsoringPacksTable.withBooth  // ❌ REMOVE
    var nbTickets by SponsoringPacksTable.nbTickets
    var maxQuantity by SponsoringPacksTable.maxQuantity
}
```

**New Entity**:
```kotlin
class SponsoringPackEntity(id: EntityID<UUID>) : UUIDEntity(id) {
    companion object : UUIDEntityClass<SponsoringPackEntity>(SponsoringPacksTable)
    
    var event by EventEntity referencedOn SponsoringPacksTable.eventId
    var name by SponsoringPacksTable.name
    var basePrice by SponsoringPacksTable.basePrice
    var boothSize by SponsoringPacksTable.boothSize  // ✅ ADD - nullable String
    var nbTickets by SponsoringPacksTable.nbTickets
    var maxQuantity by SponsoringPacksTable.maxQuantity
}
```

---

### 2. PartnershipEntity

**Current Entity** (to be extended):
```kotlin
class PartnershipEntity(id: EntityID<UUID>) : UUIDEntity(id) {
    companion object : UUIDEntityClass<PartnershipEntity>(PartnershipsTable)
    
    var event by EventEntity referencedOn PartnershipsTable.eventId
    var company by CompanyEntity referencedOn PartnershipsTable.companyId
    var selectedPack by SponsoringPackEntity optionalReferencedOn PartnershipsTable.selectedPackId
    var validatedAt by PartnershipsTable.validatedAt
    var agreementSignedUrl by PartnershipsTable.agreementSignedUrl
    // ... other properties
}
```

**New Entity**:
```kotlin
class PartnershipEntity(id: EntityID<UUID>) : UUIDEntity(id) {
    companion object : UUIDEntityClass<PartnershipEntity>(PartnershipsTable)
    
    var event by EventEntity referencedOn PartnershipsTable.eventId
    var company by CompanyEntity referencedOn PartnershipsTable.companyId
    var selectedPack by SponsoringPackEntity optionalReferencedOn PartnershipsTable.selectedPackId
    var validatedAt by PartnershipsTable.validatedAt
    var agreementSignedUrl by PartnershipsTable.agreementSignedUrl
    
    // ✅ NEW VALIDATED PROPERTIES
    var validatedNbTickets by PartnershipsTable.validatedNbTickets
    var validatedNbJobOffers by PartnershipsTable.validatedNbJobOffers
    var validatedBoothSize by PartnershipsTable.validatedBoothSize
    
    // ... other existing properties
}
```

---

## Domain Model Changes

### 1. CreateSponsoringPack (Request Model)

**Current**:
```kotlin
@Serializable
class CreateSponsoringPack(
    val name: String,
    val price: Int,
    @SerialName("with_booth")
    val withBooth: Boolean = false,  // ❌ REMOVE
    @SerialName("nb_tickets")
    val nbTickets: Int,
    @SerialName("max_quantity")
    val maxQuantity: Int? = null,
)
```

**New**:
```kotlin
@Serializable
class CreateSponsoringPack(
    val name: String,
    val price: Int,
    @SerialName("booth_size")
    val boothSize: String? = null,  // ✅ ADD - nullable, e.g., "3x3m"
    @SerialName("nb_tickets")
    val nbTickets: Int,
    @SerialName("max_quantity")
    val maxQuantity: Int? = null,
)
```

---

### 2. ValidatePartnershipRequest (NEW Request Model)

**Purpose**: Accept optional validation parameters from organizer

```kotlin
package fr.devlille.partners.connect.partnership.domain

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

/**
 * Request to validate a partnership with customizable package details.
 * 
 * Organizers can override default pack values at validation time to accommodate
 * special agreements or negotiations. If fields are omitted, pack defaults are used.
 * 
 * @property nbTickets Optional ticket count override (defaults to pack's nbTickets)
 * @property nbJobOffers Required job offer count (no default, must be specified)
 * @property boothSize Optional booth size override (defaults to pack's boothSize)
 */
@Serializable
data class ValidatePartnershipRequest(
    @SerialName("nb_tickets")
    val nbTickets: Int? = null,
    
    @SerialName("nb_job_offers")
    val nbJobOffers: Int,  // Required field - no default
    
    @SerialName("booth_size")
    val boothSize: String? = null
)
```

**Validation Rules** (enforced in repository):
- `nbTickets` if provided: must be >= 0
- `nbJobOffers`: required, must be >= 0
- `boothSize` if provided: must exist in at least one pack for the event

---

### 3. Partnership (Response Model Extension)

**Current Partnership domain model** needs these additional fields in response:

```kotlin
data class Partnership(
    val id: String,
    val language: String,
    val phone: String?,
    val emails: List<String>,
    val selectedPack: SponsoringPack?,
    val suggestionPack: SponsoringPack?,
    
    // ✅ ADD THESE NEW FIELDS
    val validatedNbTickets: Int? = null,
    val validatedNbJobOffers: Int? = null,
    val validatedBoothSize: String? = null
)
```

**Usage**: Frontend displays validated values in partnership details view

---

## Relationship Model

```
┌─────────────────┐
│ SponsoringPack  │
├─────────────────┤
│ id: UUID        │
│ eventId: UUID   │──┐
│ name: String    │  │
│ basePrice: Int  │  │
│ boothSize: ?Str │  │ (NEW - replaces withBooth boolean)
│ nbTickets: Int  │  │
│ maxQuantity: ?  │  │
└─────────────────┘  │
         │           │
         │ selected  │
         │ pack      │
         ▼           │
┌─────────────────┐  │
│  Partnership    │  │
├─────────────────┤  │
│ id: UUID        │  │
│ eventId: UUID   │──┘
│ companyId: UUID │
│ selectedPackId  │──references SponsoringPack
│ validatedAt: ?  │
│                 │
│ ✅ NEW FIELDS  │
│ validatedNbTkt  │──┐
│ validatedNbJobs │  │ Snapshot at validation time
│ validatedBooth  │  │ (overrides pack defaults)
└─────────────────┘  │
                     ▼
              Used for ticket
              generation & display
```

**Key Relationships**:
1. Partnership references SponsoringPack via `selectedPackId` (FK)
2. Validated fields are denormalized snapshot (not FK references)
3. Pack's `boothSize` provides default; partnership's `validatedBoothSize` stores actual value used
4. Ticket generation uses `partnership.validatedNbTickets ?? partnership.selectedPack.nbTickets`

---

## Data Validation Rules

### At Pack Creation/Update
- `boothSize`: Optional, any string format (e.g., "3x3m", "6x3m", "standard", null)
- `nbTickets`: Required, must be >= 0
- No cross-validation needed (pack defines available booth sizes independently)

### At Partnership Validation
1. **Ticket Count Validation**:
   ```
   IF nbTickets provided:
     ASSERT nbTickets >= 0
     USE nbTickets
   ELSE:
     USE selectedPack.nbTickets
   ```

2. **Job Offers Validation**:
   ```
   ASSERT nbJobOffers provided (required field)
   ASSERT nbJobOffers >= 0
   USE nbJobOffers
   ```

3. **Booth Size Validation**:
   ```
   IF boothSize provided:
     ASSERT boothSize exists in ANY pack for this event
     USE boothSize
   ELSE:
     USE selectedPack.boothSize (may be null)
   ```

4. **Re-Validation Check**:
   ```
   IF agreementSignedUrl IS NOT NULL:
     THROW ForbiddenException("Cannot re-validate signed partnership")
   ```

---

## Database Indexes

**Existing Indexes** (no changes needed):
- Primary key: `partnerships.id` (UUID, auto-indexed)
- Foreign keys: `partnerships.event_id`, `partnerships.selected_pack_id` (auto-indexed by Exposed)
- Foreign keys: `sponsoring_packs.event_id` (auto-indexed)

**New Indexes** (not required):
- `partnerships.validated_booth_size` - Low cardinality, not queried independently
- `sponsoring_packs.booth_size` - Used in validation query but combined with `event_id` (already indexed)

**Query Performance**:
- Booth size validation: `O(1)` with index on `event_id` (small result set per event)
- Partnership lookup: `O(1)` via primary key
- Ticket generation: `O(1)` via primary key + FK navigation

---

## Migration Strategy

### Phase 1: Schema Addition (Safe, Backward Compatible)
```sql
-- Auto-generated by Exposed SchemaUtils
ALTER TABLE sponsoring_packs ADD COLUMN booth_size TEXT NULL;
ALTER TABLE partnerships ADD COLUMN validated_nb_tickets INTEGER NULL;
ALTER TABLE partnerships ADD COLUMN validated_nb_job_offers INTEGER NULL;
ALTER TABLE partnerships ADD COLUMN validated_booth_size TEXT NULL;
```

### Phase 2: Schema Removal (Breaking, Requires Code Deployment First)
```sql
-- After all code updated to use boothSize
ALTER TABLE sponsoring_packs DROP COLUMN with_booth;
```

**Deployment Order**:
1. Deploy code that reads both `withBooth` and `boothSize`
2. Run migration to add `boothSize` column
3. Deploy code that only uses `boothSize` (remove `withBooth` references)
4. Run migration to drop `withBooth` column (optional, can defer)

---

## Testing Data Model

### Test Pack Fixtures
```kotlin
fun createPackWithBoothSize(size: String? = "3x3m") = transaction {
    SponsoringPackEntity.new {
        event = testEvent
        name = "Test Pack"
        basePrice = 1000
        boothSize = size
        nbTickets = 5
        maxQuantity = 10
    }
}
```

### Test Partnership Fixtures
```kotlin
fun createValidatedPartnership(
    nbTickets: Int? = null,
    nbJobOffers: Int = 2,
    boothSize: String? = null
) = transaction {
    val partnership = PartnershipEntity.new {
        event = testEvent
        company = testCompany
        selectedPack = testPack
        validatedAt = Clock.System.now().toLocalDateTime(TimeZone.UTC)
    }
    
    partnership.validatedNbTickets = nbTickets
    partnership.validatedNbJobOffers = nbJobOffers
    partnership.validatedBoothSize = boothSize
    
    partnership
}
```

---

## Summary

**Database Changes**:
- SponsoringPacksTable: Replace `withBooth` (boolean) with `boothSize` (nullable text)
- PartnershipsTable: Add 3 nullable columns for validated snapshot

**Entity Changes**:
- SponsoringPackEntity: Update property delegation
- PartnershipEntity: Add 3 new properties

**Domain Model Changes**:
- CreateSponsoringPack: Replace withBooth parameter
- ValidatePartnershipRequest: New request model (NEW FILE)
- Partnership: Add validated fields to response

**Key Design Decisions**:
- Nullable columns for backward compatibility
- Denormalized snapshot (no FK to pack values)
- String-based booth size (flexible, future-proof)
- Cross-pack validation for booth size overrides
- Agreement signature check prevents re-validation abuse
