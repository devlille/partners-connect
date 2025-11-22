# Research Findings: Partnership Organiser Assignment

**Date**: 2025-11-22  
**Feature**: Assign Organiser to Partnership  
**Status**: Complete

## Executive Summary

This research documents the exact patterns needed to implement optional organiser assignment to partnerships in the partners-connect codebase. All patterns are validated against existing code and constitutional requirements.

---

## 1. Exposed ORM Optional Foreign Key Pattern

### Pattern: `optionalReferencedOn` with Nullable Reference

**Reference Implementation**: `OrganisationsTable.representativeUser`

**Table Definition** (`OrganisationsTable.kt`):
```kotlin
object OrganisationsTable : UUIDTable("organisations") {
    // ... other fields
    val representativeUser = reference("representative_user", UsersTable).nullable()
    val representativeRole = varchar("representative_role", length = 255).nullable()
}
```

**Entity Mapping** (`OrganisationEntity.kt`):
```kotlin
class OrganisationEntity(id: EntityID<UUID>) : UUIDEntity(id) {
    companion object : UUIDEntityClass<OrganisationEntity>(OrganisationsTable)
    
    // ... other properties
    var representativeUser by UserEntity optionalReferencedOn OrganisationsTable.representativeUser
    var representativeRole by OrganisationsTable.representativeRole
}
```

**Key Observations**:
- Table column: `.reference("column_name", TargetTable).nullable()`
- Entity property: `by TargetEntity optionalReferencedOn TableName.columnName`
- No import needed for `optionalReferencedOn` - it's a standard Exposed operator
- Returns `TargetEntity?` (nullable type)

**Application to Partnerships**:
```kotlin
// PartnershipsTable.kt
object PartnershipsTable : UUIDTable("partnerships") {
    // ... existing fields
    val organiserId = reference("organiser_id", UsersTable).nullable()
}

// PartnershipEntity.kt
class PartnershipEntity(id: EntityID<UUID>) : UUIDEntity(id) {
    // ... existing properties
    var organiser by UserEntity optionalReferencedOn PartnershipsTable.organiserId
}
```

---

## 2. Database Migration Pattern

### Pattern: `SchemaUtils.createMissingTablesAndColumns()`

**Reference Implementations**:
- `AddBoothManagementFieldsMigration` (ID: `20250803_add_booth_management_fields`)
- `AddPartnershipCommunicationFieldsMigration` (ID: `20250804_add_partnership_communication_fields`)

**Migration Structure** (`AddBoothManagementFieldsMigration.kt`):
```kotlin
import fr.devlille.partners.connect.internal.infrastructure.migrations.Migration
import fr.devlille.partners.connect.partnership.infrastructure.db.PartnershipsTable
import org.jetbrains.exposed.v1.jdbc.SchemaUtils

object AddBoothManagementFieldsMigration : Migration {
    override val id = "20250803_add_booth_management_fields"
    override val description = "Add booth management fields (booth_plan_image_url to events, booth_location to partnerships)"

    override fun up() {
        // Update the table schemas to include the new nullable columns
        SchemaUtils.createMissingTablesAndColumns(EventsTable, PartnershipsTable)
    }

    override fun down() {
        throw UnsupportedOperationException(
            "Rollback not supported for this migration - would require dropping columns with potential data loss",
        )
    }
}
```

**Key Observations**:
- Migration ID format: `YYYYMMDD_snake_case_description`
- Use `object` declaration (singleton)
- `up()` calls `SchemaUtils.createMissingTablesAndColumns()` with affected tables
- `down()` throws `UnsupportedOperationException` - rollback NOT supported for additive changes
- Automatically detects new nullable columns in table definition and adds them
- No manual SQL required

**Migration Registry** (`MigrationRegistry.kt`):
```kotlin
object MigrationRegistry {
    val allMigrations: List<Migration> = listOf(
        // ... existing migrations
        AddBoothManagementFieldsMigration,
        AddPartnershipCommunicationFieldsMigration,
        // Add new migration here
    )
}
```

**Application to Partnership Organiser**:
```kotlin
// File: AddPartnershipOrganiserFieldMigration.kt
object AddPartnershipOrganiserFieldMigration : Migration {
    override val id = "20251122_add_partnership_organiser_field"
    override val description = "Add organiser_id to partnerships table"

    override fun up() {
        SchemaUtils.createMissingTablesAndColumns(PartnershipsTable)
    }

    override fun down() {
        throw UnsupportedOperationException(
            "Rollback not supported for this migration - would require dropping column which could cause data loss",
        )
    }
}
```

---

## 3. Organization Membership Validation

### Pattern: `AuthorizedOrganisationPlugin` + Manual User Lookup

**Automatic Validation**: `AuthorizedOrganisationPlugin`

**Reference Implementation** (`AuthorizedOrganisationPlugin.kt`):
```kotlin
val AuthorizedOrganisationPlugin = createRouteScopedPlugin(name = "AuthorizedOrganisationPlugin") {
    val authRepository by application.inject<AuthRepository>()
    val userRepository by application.inject<UserRepository>()

    onCall { call ->
        val token = call.token
        val orgSlug = call.parameters.orgSlug
        val userInfo = authRepository.getUserInfo(token)
        val canEdit = userRepository.hasEditPermissionByEmail(userInfo.email, orgSlug)
        if (!canEdit) throw UnauthorizedException("You are not allowed to edit this event")
    }
}
```

**Permission Checking Function** (`UserRepository.kt`):
```kotlin
override fun hasEditPermissionByEmail(email: String, orgSlug: String): Boolean = transaction {
    val user = UserEntity.singleUserByEmail(email)
        ?: throw NotFoundException("User with email $email not found")
    val organisation = OrganisationEntity.findBySlug(orgSlug)
        ?: throw NotFoundException("Organisation with slug: $orgSlug not found")
    OrganisationPermissionEntity.hasPermission(
        organisationId = organisation.id.value, 
        userId = user.id.value
    )
}
```

**Permission Entity Helper** (`OrganisationPermissionEntity.kt`):
```kotlin
fun UUIDEntityClass<OrganisationPermissionEntity>.hasPermission(
    organisationId: UUID,
    userId: UUID
): Boolean = this.find {
    (OrganisationPermissionsTable.organisationId eq organisationId) and
    (OrganisationPermissionsTable.canEdit eq true) and
    (OrganisationPermissionsTable.userId eq userId)
}.empty().not()
```

**Key Observations**:
- `AuthorizedOrganisationPlugin` handles ALL permission validation automatically
- Plugin extracts JWT token via `call.token`
- Plugin validates user has `canEdit=true` for the organization
- Throws `UnauthorizedException` (HTTP 401) if unauthorized
- NO manual permission checking in route handlers

**Application to Organiser Assignment**:

Since assignment happens within org-protected routes, the ASSIGNER is already validated.
Need to validate the TARGET user (email in request body) is a member:

```kotlin
// In repository method
fun assignOrganiser(partnershipId: UUID, organiserEmail: String, orgSlug: String) = transaction {
    // Find partnership and validate it belongs to event in this org
    val partnership = PartnershipEntity[partnershipId]
    val event = partnership.event
    if (event.organisation.slug != orgSlug) {
        throw NotFoundException("Partnership not found in this organisation")
    }
    
    // Validate organiser is a member of the organisation
    val organiserUser = UserEntity.singleUserByEmail(organiserEmail)
        ?: throw NotFoundException("User with email $organiserEmail not found")
    
    val hasMembership = OrganisationPermissionEntity.hasPermission(
        organisationId = event.organisation.id.value,
        userId = organiserUser.id.value
    )
    
    if (!hasMembership) {
        throw ConflictException("User $organiserEmail is not a member of this organisation")
    }
    
    // Assign organiser
    partnership.organiser = organiserUser
    partnership.toDomain()
}
```

**Constitutional Requirement**:
- Routes MUST use `AuthorizedOrganisationPlugin` for authorization
- Repository methods receive validated user context
- NO manual JWT extraction or permission checking in routes

---

## 4. Existing Mock Factory Functions

### Available Test Factories

**User Factories** (`users/factories/UserEntityFactory.kt`):
```kotlin
fun insertMockedAdminUser(
    id: UUID = UUID.randomUUID(),
    email: String = mockedAdminUser.email,
    name: String = mockedAdminUser.givenName ?: "Admin User",
    pictureUrl: String? = mockedAdminUser.picture,
): UserEntity

fun insertMockedUser(
    id: UUID = UUID.randomUUID(),
    email: String = "$id@mail.com",
    name: String? = "John Doe",
    pictureUrl: String = "https://example.com/picture.jpg",
): UserEntity
```

**Organisation Factories** (`organisations/factories/OrganisationEntityFactory.kt`):
```kotlin
fun insertMockedOrganisationEntity(
    id: UUID = UUID.randomUUID(),
    name: String = id.toString(),
    // ... many other optional parameters
    representativeUser: UserEntity = insertMockedUser(),
    representativeRole: String = "Test Representative Role",
): OrganisationEntity
```

**Event Factories** (`events/factories/EventEntityFactory.kt`):
```kotlin
fun insertMockedEvent(
    id: UUID = UUID.randomUUID(),
    name: String = "Test Event",
    slug: String? = null,
    // ... other parameters
    orgId: UUID = UUID.randomUUID(),
): EventEntity

fun insertMockedEventWithOrga(
    id: UUID = UUID.randomUUID(),
    // ... parameters
    organisation: OrganisationEntity = insertMockedOrganisationEntity(),
): EventEntity
```

**Event with Admin User** (`users/factories/UserEntityFactory.kt`):
```kotlin
fun insertMockedEventWithAdminUser(
    eventId: UUID = UUID.randomUUID(),
    orgId: UUID = UUID.randomUUID(),
    slug: String? = null,
): EventEntity {
    val mockedEvent = insertMockedEvent(eventId, slug = slug, orgId = orgId)
    insertMockedOrgaPermission(orgId = orgId, user = insertMockedAdminUser())
    return mockedEvent
}
```

**Permission Factory** (`users/factories/EventPermissionEntityFactory.kt`):
```kotlin
fun insertMockedOrgaPermission(
    orgId: UUID = UUID.randomUUID(),
    user: UserEntity,
    canEdit: Boolean = true,
): OrganisationPermissionEntity
```

**Company Factories** (`companies/factories/CompanyMock.kt`):
```kotlin
fun insertMockedCompany(
    id: UUID = UUID.randomUUID(),
    name: String = "Mock Company",
    address: String = "123 Mock St",
    // ... many optional parameters
    status: CompanyStatus = CompanyStatus.ACTIVE,
): CompanyEntity
```

**Partnership Factories** (`partnership/factories/PartnershipFactory.kt`):
```kotlin
fun insertMockedPartnership(
    id: UUID = UUID.randomUUID(),
    eventId: UUID = UUID.randomUUID(),
    companyId: UUID = UUID.randomUUID(),
    phone: String? = null,
    contactName: String = "John Doe",
    contactRole: String = "Developer",
    language: String = "en",
    // ... many optional parameters including communication fields
    communicationPublicationDate: LocalDateTime? = null,
    communicationSupportUrl: String? = null,
): PartnershipEntity
```

### Test Setup Pattern for Partnership Organiser

**Recommended Test Factory Combination**:
```kotlin
@Test
fun `assign organiser to partnership successfully`() = testApplication {
    val orgId = UUID.randomUUID()
    val eventId = UUID.randomUUID()
    val companyId = UUID.randomUUID()
    val partnershipId = UUID.randomUUID()
    val organiserId = UUID.randomUUID()
    
    application {
        moduleMocked()
        
        // Setup organization with admin user (for route authorization)
        val adminUser = insertMockedAdminUser(email = "admin@test.com")
        insertMockedOrganisationEntity(orgId, name = "Test Org")
        insertMockedOrgaPermission(orgId = orgId, user = adminUser, canEdit = true)
        
        // Setup event, company, and partnership
        insertMockedEvent(eventId, orgId = orgId, slug = "test-event")
        insertMockedCompany(companyId)
        insertMockedPartnership(
            id = partnershipId,
            eventId = eventId,
            companyId = companyId
        )
        
        // Create potential organiser (member of the org)
        val organiserUser = insertMockedUser(
            id = organiserId,
            email = "organiser@test.com",
            name = "Jane Organiser"
        )
        insertMockedOrgaPermission(orgId = orgId, user = organiserUser, canEdit = true)
    }
    
    // Test assignment...
}
```

**Key Observations**:
- ✅ `insertMockedUser()` already exists - no need to create
- ✅ `insertMockedPartnership()` supports all communication fields
- ✅ `insertMockedOrgaPermission()` creates org membership
- ✅ `insertMockedEventWithAdminUser()` combines event + admin setup
- Pattern: Create org → Create admin → Grant permission → Create resources

---

## 5. Database Column Naming Convention

### Pattern: Snake Case with `_id` Suffix for Foreign Keys

**Reference Examples**:
```kotlin
// PartnershipsTable.kt (existing)
val eventId = reference("event_id", EventsTable)
val companyId = reference("company_id", CompaniesTable)
val selectedPackId = reference("selected_pack_id", SponsoringPacksTable).nullable()
val suggestionPackId = reference("suggestion_pack_id", SponsoringPacksTable).nullable()

// OrganisationsTable.kt (existing)
val representativeUser = reference("representative_user", UsersTable).nullable()

// OrganisationPermissionsTable.kt
val organisationId = reference("organisation_id", OrganisationsTable)
val userId = reference("user_id", UsersTable)
```

**Application to Partnership Organiser**:
```kotlin
// CORRECT: Matches existing pattern (foreign key ends with _id)
val organiserId = reference("organiser_id", UsersTable).nullable()

// ALTERNATIVE (if following representativeUser pattern): 
// val organiser = reference("organiser", UsersTable).nullable()
// BUT organiserId is more consistent with other partnership FKs (eventId, companyId, selectedPackId)
```

**Decision**: Use `organiserId` to match the existing partnership table FK naming convention.

---

## 6. Entity Property Naming (Kotlin Side)

### Pattern: Camel Case, Omit `Id` Suffix for Entity References

**Reference Examples**:
```kotlin
// PartnershipEntity.kt (existing)
var event by EventEntity referencedOn PartnershipsTable.eventId
var company by CompanyEntity referencedOn PartnershipsTable.companyId
var selectedPack by SponsoringPackEntity optionalReferencedOn PartnershipsTable.selectedPackId
var suggestionPack by SponsoringPackEntity optionalReferencedOn PartnershipsTable.suggestionPackId

// OrganisationEntity.kt (existing)
var representativeUser by UserEntity optionalReferencedOn OrganisationsTable.representativeUser

// OrganisationPermissionEntity.kt
var organisation by OrganisationEntity referencedOn OrganisationPermissionsTable.organisationId
var user by UserEntity referencedOn OrganisationPermissionsTable.userId
```

**Key Pattern**:
- Database column: `selected_pack_id` → Entity property: `selectedPack` (not `selectedPackId`)
- Database column: `organiser_id` → Entity property: `organiser` (not `organiserId`)
- The `by` delegation returns the entity object, not the ID

**Application to Partnership Organiser**:
```kotlin
// PartnershipEntity.kt
var organiser by UserEntity optionalReferencedOn PartnershipsTable.organiserId
```

---

## 7. Table Definition Pattern (CRITICAL)

### Pattern: MUST Extend `UUIDTable`, Use `datetime()`, Use `enumerationByName<>()`

**Reference from Constitution** (Section III.1):
```kotlin
// CORRECT Pattern
object PartnershipsTable : UUIDTable("partnerships") {
    val eventId = reference("event_id", EventsTable)
    val createdAt = datetime("created_at").clientDefault {
        Clock.System.now().toLocalDateTime(TimeZone.UTC)
    }
    val suggestionSentAt = datetime("suggestion_sent_at").nullable()
}
```

**Key Requirements**:
- ✅ Extend `UUIDTable` (NOT `IdTable` or `IntIdTable`)
- ✅ Use `datetime()` for temporal fields (NEVER `timestamp()`)
- ✅ Use `enumerationByName<EnumType>()` for enums (NEVER `varchar()` with manual mapping)
- ✅ Use `clientDefault { Clock.System.now()... }` for creation timestamps

**Application to Partnership Organiser** (No temporal fields needed, but pattern for reference):
```kotlin
object PartnershipsTable : UUIDTable("partnerships") {
    // ... existing fields
    val organiserId = reference("organiser_id", UsersTable).nullable()
    // If we had an assignment timestamp:
    // val assignedAt = datetime("assigned_at").nullable()
}
```

---

## 8. Entity Pattern (CRITICAL)

### Pattern: Extend `UUIDEntity`, Companion Object, Property Delegation

**Reference from Constitution** (Section III.2):
```kotlin
class PartnershipEntity(id: EntityID<UUID>) : UUIDEntity(id) {
    companion object : UUIDEntityClass<PartnershipEntity>(PartnershipsTable)
    
    var event by EventEntity referencedOn PartnershipsTable.eventId
    var company by CompanyEntity referencedOn PartnershipsTable.companyId
    var contactName by PartnershipsTable.contactName
    var createdAt by PartnershipsTable.createdAt
}
```

**Key Requirements**:
- ✅ Constructor: `(id: EntityID<UUID>)`
- ✅ Extend: `UUIDEntity(id)`
- ✅ Companion: `companion object : UUIDEntityClass<EntityClassName>(TableName)`
- ✅ Properties: `by TableName.columnName` delegation (NOT manual getters/setters)

**Application to Partnership Organiser**:
```kotlin
class PartnershipEntity(id: EntityID<UUID>) : UUIDEntity(id) {
    companion object : UUIDEntityClass<PartnershipEntity>(PartnershipsTable)
    
    // ... existing properties
    var organiser by UserEntity optionalReferencedOn PartnershipsTable.organiserId
}
```

---

## 9. Repository Pattern (NON-NEGOTIABLE)

### Pattern: Repositories MUST NOT Depend on Other Repositories

**Reference from Constitution** (Section IV):
> Repository implementations MUST NOT depend on other repositories.  
> Notification sending happens in route layer, NEVER in repositories.  
> Repositories return data; routes orchestrate cross-cutting concerns.

**Anti-Pattern** (FORBIDDEN):
```kotlin
// ❌ WRONG - repository depending on another repository
class PartnershipRepository(
    private val eventRepository: EventRepository,  // FORBIDDEN
    private val userRepository: UserRepository     // FORBIDDEN
) {
    fun assignOrganiser(partnershipId: UUID, organiserEmail: String) = transaction {
        val user = userRepository.getByEmail(organiserEmail)  // ❌ Calling another repo
        // ...
    }
}
```

**Correct Pattern**:
```kotlin
// ✅ CORRECT - repository only accesses entities/tables directly
class PartnershipRepositoryExposed : PartnershipRepository {
    fun assignOrganiser(
        partnershipId: UUID,
        organiserEmail: String,
        orgSlug: String
    ): Partnership = transaction {
        // Direct entity access, no repository dependencies
        val partnership = PartnershipEntity[partnershipId]
        val event = partnership.event
        
        // Validate event belongs to organization
        if (event.organisation.slug != orgSlug) {
            throw NotFoundException("Partnership not found in this organisation")
        }
        
        // Direct UserEntity access
        val organiserUser = UserEntity.singleUserByEmail(organiserEmail)
            ?: throw NotFoundException("User with email $organiserEmail not found")
        
        // Direct permission check
        val hasMembership = OrganisationPermissionEntity.hasPermission(
            organisationId = event.organisation.id.value,
            userId = organiserUser.id.value
        )
        
        if (!hasMembership) {
            throw ConflictException(
                "User $organiserEmail is not a member of this organisation"
            )
        }
        
        partnership.organiser = organiserUser
        partnership.toDomain()
    }
}
```

---

## 10. Gotchas and Special Considerations

### 10.1 Migration Ordering
- Migrations are executed in list order from `MigrationRegistry.allMigrations`
- New migration MUST be added at the END of the list
- Migration IDs MUST be unique and follow `YYYYMMDD_description` format

### 10.2 Nullable Foreign Keys
- Use `.nullable()` on the table column definition
- Use `optionalReferencedOn` (NOT `referencedOn`) in entity
- Returns nullable type (`UserEntity?`)
- No need to handle null explicitly in entity - delegation handles it

### 10.3 Authorization vs Validation
- **Authorization** (who can perform action): Handled by `AuthorizedOrganisationPlugin`
- **Validation** (is data valid): Handled in repository/route with exceptions
- For organiser assignment:
  - Authorization: Plugin ensures ASSIGNER has `canEdit` permission (automatic)
  - Validation: Repository ensures TARGET user exists and is org member (manual)

### 10.4 Exception Handling
- `NotFoundException`: Entity/user not found (HTTP 404)
- `ConflictException`: Business rule violation (HTTP 409)
- `UnauthorizedException`: Permission denied (HTTP 401) - thrown by plugin
- Let StatusPages handle HTTP mapping (NO try-catch in routes)

### 10.5 Transaction Scope
- Repository methods wrap operations in `transaction { ... }`
- Entity lookups MUST be inside transaction scope
- Permission checks MUST be inside transaction scope
- Use `UserEntity.singleUserByEmail()` NOT `UserEntity.findByEmail()`

### 10.6 Test Database Isolation
- Tests use H2 in-memory database (NOT PostgreSQL)
- Each test gets transaction rollback for isolation
- No need to clean up test data manually
- Factory functions automatically use transactions

### 10.7 Domain Model Updates
- If adding nullable field to existing entity, NO change to existing domain constructor
- Add as optional parameter with default `null`
- Existing code continues to work without modification
- Example:
  ```kotlin
  // Domain model
  data class Partnership(
      val id: UUID,
      // ... existing fields
      val organiserEmail: String? = null  // New optional field
  )
  ```

---

## Summary Table

| Topic | Pattern | File Reference |
|-------|---------|----------------|
| Optional FK (Table) | `.reference("col", Table).nullable()` | `OrganisationsTable.kt:25` |
| Optional FK (Entity) | `by Entity optionalReferencedOn Table.col` | `OrganisationEntity.kt:29` |
| Migration (Add Column) | `SchemaUtils.createMissingTablesAndColumns()` | `AddBoothManagementFieldsMigration.kt:20` |
| Migration ID Format | `YYYYMMDD_snake_case_description` | `AddPartnershipCommunicationFieldsMigration.kt:12` |
| Org Membership Check | `OrganisationPermissionEntity.hasPermission()` | `OrganisationPermissionEntity.kt:36` |
| User Lookup | `UserEntity.singleUserByEmail(email)` | `UserRepositoryExposed.kt:42` |
| Authorization Plugin | `install(AuthorizedOrganisationPlugin)` | `AuthorizedOrganisationPlugin.kt:9` |
| Mock User Factory | `insertMockedUser(id, email, name)` | `UserEntityFactory.kt:31` |
| Mock Partnership | `insertMockedPartnership(...)` | `PartnershipFactory.kt:11` |
| Mock Org Permission | `insertMockedOrgaPermission(orgId, user)` | `EventPermissionEntityFactory.kt:8` |
| Repository Independence | Direct entity access, no repo injection | Constitution Section IV |
| Table Extension | `object Table : UUIDTable("name")` | `PartnershipsTable.kt:10` |
| Entity Extension | `class Entity(...) : UUIDEntity(id)` | `PartnershipEntity.kt:18` |
| DateTime Fields | `datetime("col").nullable()` | `PartnershipsTable.kt:28` |
| Column Naming | Snake case with `_id` for FKs | `PartnershipsTable.kt:11-13` |
| Property Naming | Camel case, omit `Id` suffix | `PartnershipEntity.kt:69-71` |

---

## Implementation Checklist

Based on research findings, the implementation requires:

- [ ] **Schema Change**: Add `organiserId` column to `PartnershipsTable`
- [ ] **Entity Update**: Add `organiser` property to `PartnershipEntity`
- [ ] **Migration**: Create `AddPartnershipOrganiserFieldMigration`
- [ ] **Domain Model**: Add optional `organiserEmail` to `Partnership` domain class
- [ ] **Repository Method**: Implement `assignOrganiser()` with org membership validation
- [ ] **Route Handler**: Create POST endpoint with `AuthorizedOrganisationPlugin`
- [ ] **Test Factories**: Use existing `insertMockedUser()` and `insertMockedOrgaPermission()`
- [ ] **Contract Tests**: Validate request/response schemas
- [ ] **Integration Tests**: Test org membership validation, authorization, edge cases

---

## References

- Constitution Section III: Database Patterns (Tables & Entities)
- Constitution Section IV: Repository Architecture  
- Constitution Section V: Authorization Pattern
- Existing implementation: `OrganisationsTable.representativeUser`
- Migration pattern: `AddBoothManagementFieldsMigration`
- Test patterns: `users/factories/`, `partnership/factories/`
