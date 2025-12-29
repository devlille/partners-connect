# Data Model: Assign Organiser to Partnership

**Feature**: Organiser assignment to partnerships  
**Date**: November 22, 2025

## Overview

Extends the existing Partnership entity with an optional reference to a User who acts as the designated organiser for that partnership. This provides partners with a direct contact person within the event organization team.

## Domain Models

### Partnership (Extended)

**Location**: `server/application/src/main/kotlin/fr/devlille/partners/connect/partnership/domain/Partnership.kt`

```kotlin
data class Partnership(
    val id: UUID,
    val eventId: UUID,
    val companyId: UUID,
    val phone: String?,
    val contactName: String,
    val contactRole: String,
    val language: String,
    val agreementUrl: String?,
    val agreementSignedUrl: String?,
    val selectedPackId: UUID?,
    val suggestionPackId: UUID?,
    val suggestionSentAt: LocalDateTime?,
    val suggestionApprovedAt: LocalDateTime?,
    val suggestionDeclinedAt: LocalDateTime?,
    val declinedAt: LocalDateTime?,
    val validatedAt: LocalDateTime?,
    val boothLocation: String?,
    val communicationPublicationDate: LocalDateTime?,
    val communicationSupportUrl: String?,
    val createdAt: LocalDateTime,
    // NEW: Optional organiser assignment
    val organiser: User? = null
)
```

**Changes**:
- Added `organiser: User? = null` - Assigned organiser user object
- Field is optional (nullable) per FR-011
- Uses `User` domain model from `users/domain/User.kt` with `displayName`, `pictureUrl`, and `email` properties

### Request/Response DTOs

#### AssignOrganiserRequest

**Location**: `server/application/src/main/kotlin/fr/devlille/partners/connect/partnership/infrastructure/api/AssignOrganiserRequest.kt`

```kotlin
@Serializable
data class AssignOrganiserRequest(
    val email: String
)
```

**Validation** (via JSON schema):
- `email`: Required, must be valid email format
- Schema file: `assign_organiser_request.schema.json`

#### PartnershipOrganiserResponse

**Location**: `server/application/src/main/kotlin/fr/devlille/partners/connect/partnership/infrastructure/api/PartnershipOrganiserResponse.kt`

```kotlin
import fr.devlille.partners.connect.users.domain.User
import kotlinx.serialization.SerialName

@Serializable
data class PartnershipOrganiserResponse(
    @SerialName("partnership_id")
    val partnershipId: UUID,
    val organiser: User?
)
```

**Purpose**: 
- Returns current organiser information for a partnership
- `organiser` is null if no organiser assigned, otherwise contains `User` object with `displayName`, `pictureUrl`, and `email`
- Schema file: `partnership_organiser_response.schema.json` (references existing `user.schema.json`)

## Database Schema

### PartnershipsTable (Extended)

**Location**: `server/application/src/main/kotlin/fr/devlille/partners/connect/partnership/infrastructure/db/PartnershipsTable.kt`

```kotlin
object PartnershipsTable : UUIDTable("partnerships") {
    val eventId = reference("event_id", EventsTable)
    val companyId = reference("company_id", CompaniesTable)
    val phone = text("phone").nullable()
    val contactName = text("contact_name")
    val contactRole = text("contact_role")
    val language = text("language")
    val agreementUrl = text("agreement_url").nullable()
    val agreementSignedUrl = text("agreement_signed_url").nullable()
    val selectedPackId = reference("selected_pack_id", SponsoringPacksTable).nullable()
    val suggestionPackId = reference("suggestion_pack_id", SponsoringPacksTable).nullable()
    val suggestionSentAt = datetime("suggestion_sent_at").nullable()
    val suggestionApprovedAt = datetime("suggestion_approved_at").nullable()
    val suggestionDeclinedAt = datetime("suggestion_declined_at").nullable()
    val declinedAt = datetime("declined_at").nullable()
    val validatedAt = datetime("validated_at").nullable()
    val boothLocation = text("booth_location").nullable()
    val communicationPublicationDate = datetime("communication_publication_date").nullable()
    val communicationSupportUrl = text("communication_support_url").nullable()
    val createdAt = datetime("created_at").clientDefault {
        Clock.System.now().toLocalDateTime(TimeZone.UTC)
    }
    // NEW: Optional organiser assignment
    val organiserId = reference("organiser_id", UsersTable).nullable()
}
```

**Changes**:
- Added `organiserId` column: Optional foreign key to UsersTable
- Follows naming convention: snake_case with `_id` suffix for foreign keys
- Nullable to support partnerships without assigned organisers (FR-011)
- No timestamp tracking per FR-015 and clarification session

**SQL Migration** (via SchemaUtils):
```sql
ALTER TABLE partnerships 
ADD COLUMN organiser_id UUID NULL 
REFERENCES users(id);
```

### PartnershipEntity (Extended)

**Location**: `server/application/src/main/kotlin/fr/devlille/partners/connect/partnership/infrastructure/db/PartnershipEntity.kt`

```kotlin
class PartnershipEntity(id: EntityID<UUID>) : UUIDEntity(id) {
    companion object : UUIDEntityClass<PartnershipEntity>(PartnershipsTable) {
        // ... existing companion object methods
    }

    var event by EventEntity referencedOn PartnershipsTable.eventId
    var company by CompanyEntity referencedOn PartnershipsTable.companyId
    var phone by PartnershipsTable.phone
    var contactName by PartnershipsTable.contactName
    var contactRole by PartnershipsTable.contactRole
    var language by PartnershipsTable.language
    var agreementUrl by PartnershipsTable.agreementUrl
    var agreementSignedUrl by PartnershipsTable.agreementSignedUrl
    var selectedPack by SponsoringPackEntity optionalReferencedOn PartnershipsTable.selectedPackId
    var suggestionPack by SponsoringPackEntity optionalReferencedOn PartnershipsTable.suggestionPackId
    var suggestionSentAt by PartnershipsTable.suggestionSentAt
    var suggestionApprovedAt by PartnershipsTable.suggestionApprovedAt
    var suggestionDeclinedAt by PartnershipsTable.suggestionDeclinedAt
    var declinedAt by PartnershipsTable.declinedAt
    var validatedAt by PartnershipsTable.validatedAt
    var boothLocation by PartnershipsTable.boothLocation
    var communicationPublicationDate by PartnershipsTable.communicationPublicationDate
    var communicationSupportUrl by PartnershipsTable.communicationSupportUrl
    var createdAt by PartnershipsTable.createdAt
    // NEW: Optional organiser relationship
    var organiser by UserEntity optionalReferencedOn PartnershipsTable.organiserId
}
```

**Changes**:
- Added `organiser` property: Optional reference to UserEntity
- Uses `optionalReferencedOn` delegation pattern per Exposed ORM standards
- Property name is `organiser` (not `organiserId`) - follows entity naming convention
- Returns `UserEntity?` (nullable UserEntity)

**Entity to Domain Mapping Extension**:

```kotlin
fun PartnershipEntity.toPartnership(): Partnership = Partnership(
    id = id.value,
    eventId = event.id.value,
    companyId = company.id.value,
    phone = phone,
    contactName = contactName,
    contactRole = contactRole,
    language = language,
    agreementUrl = agreementUrl,
    agreementSignedUrl = agreementSignedUrl,
    selectedPackId = selectedPack?.id?.value,
    suggestionPackId = suggestionPack?.id?.value,
    suggestionSentAt = suggestionSentAt,
    suggestionApprovedAt = suggestionApprovedAt,
    suggestionDeclinedAt = suggestionDeclinedAt,
    declinedAt = declinedAt,
    validatedAt = validatedAt,
    boothLocation = boothLocation,
    communicationPublicationDate = communicationPublicationDate,
    communicationSupportUrl = communicationSupportUrl,
    createdAt = createdAt,
    // NEW: Map organiser information
    organiser = organiser?.let { User(
        displayName = it.name,
        pictureUrl = it.pictureUrl,
        email = it.email
    ) }
)
```

## Entity Relationships

```
Partnership *---0..1 User (organiser)
    └─ organiserId (nullable FK) → UsersTable.id
    
User 1---* OrganisationPermission
    └─ Validates user is member of organization

Event 1---* Partnership
Organisation 1---* Event
    └─ Transitively validates: organiser must be member of partnership's event's organisation
```

**Relationship Constraints**:
- **Partnership → User (organiser)**: Zero-or-one (optional assignment)
- **User → Organisation**: Via OrganisationPermissionsTable (validation only, no FK from Partnership)
- **No cascade delete**: User accounts cannot be deleted (clarification session answer)
- **Validation layer**: Repository ensures organiser is member of event's organisation

**Key Validation Rules**:
1. Assigned organiser MUST be a member of the organisation that owns the partnership's event
2. Assigned organiser MUST have edit permission (`canEdit=true`) for the organisation
3. Checked via `OrganisationPermissionEntity.hasPermission(userId, organisationId)` for membership and `canEdit` flag
4. Validation happens in repository layer before assignment
5. Throws `ForbiddenException` if organiser lacks organisation membership or edit permission

## Validation Rules

### Functional Requirements Mapping

**FR-001**: System MUST allow administrators with edit permissions to assign organisers
- Enforced by: `AuthorizedOrganisationPlugin` on route

**FR-002**: System MUST store organiser assignment as relationship between partnership and user
- Implemented by: `PartnershipsTable.organiserId` FK column

**FR-003**: System MUST validate assigned organiser is member of organisation owning the event
- Implemented by: Repository validation using `OrganisationPermissionEntity.hasPermission()` and checking `canEdit=true`

**FR-011**: System MUST support optional organiser assignment
- Implemented by: Nullable FK column and entity property

**FR-014**: System MUST maintain valid FK references - users cannot be deleted
- Guaranteed by: System constraint (users cannot be deleted per clarification session)

**FR-015**: System MUST store only current state without timestamps
- Implemented by: No `assignedAt` or `updatedAt` columns for organiser

**FR-016**: System MUST use last-write-wins strategy for concurrent modifications
- Implemented by: Standard transaction isolation, no optimistic locking

### Request Validation

**AssignOrganiserRequest**:
- `email`: Required, non-empty, valid email format
- Validated via JSON schema before reaching repository

**Business Logic Validation** (in repository):
1. Partnership exists and belongs to organisation (via event)
2. Target user exists in system
3. Target user has membership in partnership's event's organisation
4. Target user has edit permission (`canEdit=true`) for the organisation
5. Throws appropriate exceptions if validation fails

### Error Scenarios

| Scenario | Exception | HTTP Status |
|----------|-----------|-------------|
| Partnership not found | `NotFoundException` | 404 |
| User email not found | `NotFoundException` | 404 |
| User not org member | `ForbiddenException` | 403 |
| User lacks edit permission | `ForbiddenException` | 403 |
| Invalid email format | Validation error via schema | 400 |
| Missing auth token | `UnauthorizedException` (plugin) | 401 |
| No edit permission | `UnauthorizedException` (plugin) | 401 |

## Repository Interface Extensions

### PartnershipRepository

**Location**: `server/application/src/main/kotlin/fr/devlille/partners/connect/partnership/domain/PartnershipRepository.kt`

```kotlin
interface PartnershipRepository {
    // ... existing methods ...
    
    /**
     * Assigns an organiser to a partnership.
     *
     * @param partnershipId UUID of the partnership
     * @param email Email of the user to assign as organiser
     * @return Updated partnership with organiser assigned
     * @throws NotFoundException if partnership or user not found
     * @throws ForbiddenException if user is not a member of the organisation or lacks edit permission
     */
    fun assignOrganiser(
        partnershipId: UUID,
        email: String
    ): Partnership
    
    /**
     * Removes the organiser assignment from a partnership.
     *
     * @param partnershipId UUID of the partnership
     * @return Updated partnership without organiser
     * @throws NotFoundException if partnership not found
     */
    fun removeOrganiser(
        partnershipId: UUID
    ): Partnership
}
```

**Implementation Notes**:
- No repository dependencies - uses direct entity access
- Validation happens before mutation operations
- Returns domain model objects, not entities
- Organisation is obtained directly from partnership's event (no need to pass orgSlug)
- Organiser information is automatically included in Partnership via `toPartnership()` mapping when using existing methods like `getById()`

## Migration Strategy

### Migration File

**Location**: `server/application/src/main/kotlin/fr/devlille/partners/connect/internal/infrastructure/migrations/versions/AddPartnershipOrganiserMigration.kt`

```kotlin
package fr.devlille.partners.connect.internal.infrastructure.migrations.versions

import fr.devlille.partners.connect.internal.infrastructure.migrations.Migration
import fr.devlille.partners.connect.partnership.infrastructure.db.PartnershipsTable
import org.jetbrains.exposed.v1.jdbc.SchemaUtils

/**
 * Migration to add organiser assignment capability to partnerships.
 * Adds nullable organiser_id foreign key to partnerships table.
 */
object AddPartnershipOrganiserMigration : Migration {
    override val id = "20251122_add_partnership_organiser"
    override val description = "Add organiser_id nullable column to partnerships table"

    override fun up() {
        // Add missing columns to PartnershipsTable - this will add the new organiserId column
        SchemaUtils.createMissingTablesAndColumns(PartnershipsTable)
    }

    override fun down() {
        throw UnsupportedOperationException(
            "Rollback not supported - would require dropping column which could cause data loss"
        )
    }
}
```

**Registration** (update `MigrationRegistry.kt`):
```kotlin
val allMigrations: List<Migration> = listOf(
    // ... existing migrations ...
    AddPartnershipOrganiserMigration,  // Add at end of list
)
```

**Migration Characteristics**:
- **Type**: Additive (adds nullable column)
- **Backwards Compatible**: Yes (existing partnerships will have null organiser)
- **Data Loss Risk**: None
- **Rollback**: Not supported (standard for additive migrations)
- **Execution**: Automatic on application startup via MigrationManager

## Performance Considerations

### Database Indexes

**Existing Indexes**:
- Primary key on `partnerships.id` (automatic via UUIDTable)
- Foreign key on `partnerships.event_id` (existing)
- Foreign key on `partnerships.company_id` (existing)

**New Index**:
- Foreign key on `partnerships.organiser_id` (automatic via Exposed reference)
- No additional composite index needed - organiser lookups are by partnership ID (already indexed)

**Query Patterns**:
- Assignment/removal: Single row update by partition ID (indexed)
- View organiser: Single row select by partnership ID (indexed)
- No complex joins or aggregations required

### Scalability

**Expected Load**:
- Organiser assignment is infrequent (happens once per partnership, occasionally updated)
- View operations happen when partners/administrators view partnership details
- No high-frequency updates or bulk operations

**Optimization Notes**:
- No caching needed (data rarely changes)
- Single-row operations only (no pagination concerns)
- Nullable FK has minimal performance impact
- No timestamp tracking reduces write overhead

## Data Access Patterns

### Query Examples

**Fetch Partnership with Organiser**:
```kotlin
// In repository implementation
fun getById(id: UUID): Partnership = transaction {
    val partnership = PartnershipEntity[id]
    partnership.toPartnership()  // Includes organiser via delegation
}
```

**Assign Organiser**:
```kotlin
fun assignOrganiser(
    partnershipId: UUID, 
    email: String
): Partnership = transaction {
    val partnership = PartnershipEntity[partnershipId]
    val event = partnership.event
    val organisation = event.organisation
    
    // Find and validate user
    val organiserUser = UserEntity.singleUserByEmail(email)
        ?: throw NotFoundException("User with email $email not found")
    
    // Validate user has organisation permission with edit access
    val permission = OrganisationPermissionEntity.find {
        (OrganisationPermissionsTable.userId eq organiserUser.id) and
        (OrganisationPermissionsTable.organisationId eq organisation.id)
    }.singleOrNull()
    
    if (permission == null) {
        throw ForbiddenException(
            "User $email is not a member of this organisation"
        )
    }
    
    if (!permission.canEdit) {
        throw ForbiddenException(
            "User $email does not have edit permission for this organisation"
        )
    }
    
    // Assign organiser
    partnership.organiser = organiserUser
    partnership.toPartnership()
}
```

**Remove Organiser**:
```kotlin
fun removeOrganiser(partnershipId: UUID): Partnership = transaction {
    val partnership = PartnershipEntity[partnershipId]
    
    partnership.organiser = null
    partnership.toPartnership()
}
```

## Constitutional Compliance

✅ **Database Schema Standards**:
- Uses `UUIDTable` extension
- Foreign key with `.nullable()` modifier
- Column naming: snake_case with `_id` suffix
- No `datetime()` columns (no timestamp tracking per FR-015)

✅ **Entity Standards**:
- Extends `UUIDEntity` with proper constructor
- Companion object pattern
- Property delegation with `optionalReferencedOn`
- Property naming: camelCase without `Id` suffix

✅ **Repository Architecture**:
- No repository dependencies
- Direct entity access for validation
- Throws domain exceptions
- Transaction-scoped operations

✅ **Migration Standards**:
- Uses `SchemaUtils.createMissingTablesAndColumns()`
- Migration ID format: `YYYYMMDD_description`
- No rollback for additive changes
- Registered in MigrationRegistry

## Summary

This data model extends the Partnership entity with optional organiser assignment while maintaining constitutional compliance and architectural consistency. The implementation:

- Adds nullable FK column to existing PartnershipsTable
- Extends PartnershipEntity with optional UserEntity reference
- Provides repository methods for assignment, removal, and viewing
- Validates organisation membership before assignment
- Uses established patterns from existing codebase
- Requires zero data migration (additive change only)
- Maintains backwards compatibility with existing partnerships
