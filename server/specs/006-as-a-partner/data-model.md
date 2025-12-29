# Data Model: Complete CRUD Operations for Companies

## Overview
Extends the existing Company entity with status management for soft deletion and partial update capabilities. Maintains all existing relationships while adding new functionality for complete CRUD operations.

## Domain Models

### UpdateCompany (Request Model)
```kotlin
@Serializable
data class UpdateCompany(
    val name: String? = null,
    @SerialName("site_url")
    val siteUrl: String? = null,
    @SerialName("head_office")
    val headOffice: Address? = null,
    val siret: String? = null,
    val vat: String? = null,
    val description: String? = null,
    val socials: List<Social>? = null,
)
```

**Purpose**: Partial update model for company information
**Null Semantics**: `null` values indicate "no change", non-null values replace existing data
**Validation**: Same business rules as `CreateCompany` but all fields optional

### CompanyStatus (Enum)
```kotlin
@Serializable
enum class CompanyStatus {
    @SerialName("active")
    ACTIVE,
    @SerialName("inactive") 
    INACTIVE
}
```

**Purpose**: Represents company lifecycle state for soft deletion
**Values**: 
- `ACTIVE`: Normal operational company (default for existing records)
- `INACTIVE`: Soft-deleted company (preserved for historical integrity)

### Enhanced Company (Response Model)
```kotlin
@Serializable
data class Company(
    val id: String,
    val name: String,
    @SerialName("head_office")
    val headOffice: Address,
    val siret: String,
    val vat: String,
    val description: String?,
    @SerialName("site_url")
    val siteUrl: String,
    val medias: Media?,
    val status: CompanyStatus, // NEW FIELD
)
```

**Changes**: Added `status` field to existing Company model
**Backwards Compatibility**: Existing companies automatically receive `ACTIVE` status

## Database Schema

### CompaniesTable (Updated)
```kotlin
object CompaniesTable : UUIDTable("companies") {
    // Existing columns...
    val name = text("name")
    val siteUrl = text("site_url")
    val address = text("address")
    val city = text("city")
    val zipCode = text("zip_code")
    val country = varchar("country", 2)
    val siret = text("siret")
    val vat = text("vat")
    val description = text("description").nullable()
    val logoUrlOriginal = text("logo_url_original").nullable()
    val logoUrl1000 = text("logo_url_1000").nullable()
    val logoUrl500 = text("logo_url_500").nullable()
    val logoUrl250 = text("logo_url_250").nullable()
    val createdAt = datetime("created_at").clientDefault { Clock.System.now().toLocalDateTime(TimeZone.UTC) }
    val updatedAt = datetime("updated_at").clientDefault { Clock.System.now().toLocalDateTime(TimeZone.UTC) }
    
    // NEW COLUMN
    val status = enumerationByName<CompanyStatus>("status", 10).default(CompanyStatus.ACTIVE)
    
    init {
        // NEW INDEX for filtering performance
        index(isUnique = false, status)
    }
}
```

**Schema Changes**:
- Added `status` column with `ACTIVE` default for backwards compatibility
- Added index on `status` column for efficient filtering queries
- Uses `enumerationByName` for readable enum storage

### CompanyEntity (Updated)
```kotlin
class CompanyEntity(id: EntityID<UUID>) : UUIDEntity(id) {
    companion object : UUIDEntityClass<CompanyEntity>(CompaniesTable)

    // Existing properties...
    var name by CompaniesTable.name
    var description by CompaniesTable.description
    var address by CompaniesTable.address
    var city by CompaniesTable.city
    var zipCode by CompaniesTable.zipCode
    var country by CompaniesTable.country
    var siret by CompaniesTable.siret
    var vat by CompaniesTable.vat
    var siteUrl by CompaniesTable.siteUrl
    var logoUrlOriginal by CompaniesTable.logoUrlOriginal
    var logoUrl1000 by CompaniesTable.logoUrl1000
    var logoUrl500 by CompaniesTable.logoUrl500
    var logoUrl250 by CompaniesTable.logoUrl250
    val createdAt by CompaniesTable.createdAt
    var updatedAt by CompaniesTable.updatedAt
    
    // NEW PROPERTY
    var status by CompaniesTable.status
    
    // Existing relationships preserved
    val socials by CompanySocialEntity referrersOn CompanyId
}
```

**Changes**: Added `status` property mapped to database column
**Relationships**: All existing relationships (socials, partnerships, job offers) remain unchanged

## Repository Interface Extensions

### CompanyRepository (Updated)
```kotlin
interface CompanyRepository {
    // UPDATED EXISTING METHOD - Added status parameter
    /**
     * Lists companies with optional search and status filtering.
     * 
     * @param query Search query (optional)
     * @param status Filter by company status (optional, defaults to showing all)
     * @param page Page number (1-based)
     * @param pageSize Items per page
     * @return Paginated company results with status information
     */
    fun listPaginated(
        query: String?, 
        status: CompanyStatus?, 
        page: Int, 
        pageSize: Int
    ): PaginatedResponse<Company>
    
    // Existing methods...
    fun getById(id: UUID): Company
    fun createOrUpdate(input: CreateCompany): UUID
    fun updateLogoUrls(companyId: UUID, uploaded: Media): UUID
    
    // NEW METHODS
    /**
     * Updates an existing company with partial data.
     * Only non-null fields in UpdateCompany are applied.
     * 
     * @param id Company UUID to update
     * @param input Partial update data
     * @return Updated company information
     * @throws NotFoundException if company does not exist
     */
    fun update(id: UUID, input: UpdateCompany): Company
    
    /**
     * Soft deletes a company by marking it as INACTIVE.
     * Preserves all relationships and data integrity.
     * 
     * @param id Company UUID to soft delete
     * @return Company UUID (for consistency with other methods)
     * @throws NotFoundException if company does not exist
     */
    fun softDelete(id: UUID): UUID
}
```

**Method Enhancement**: Extended existing `listPaginated` method signature with status parameter for filtering while maintaining backwards compatibility

## Data Relationships

### Relationship Preservation
- **Partnerships**: Remain linked to companies regardless of status
- **Job Offers**: Continue to reference parent company even when inactive
- **Social Links**: Preserved through soft deletion
- **Media Assets**: Logo URLs maintained for historical integrity

### Filtering Behavior
- **Default Listing**: Shows ALL companies (active + inactive) as per user clarification
- **Active Filter**: `GET /companies?status=active` shows only ACTIVE companies
- **Inactive Filter**: `GET /companies?status=inactive` shows only INACTIVE companies
- **Status Display**: All listings clearly indicate company status in response

## Validation Rules

### UpdateCompany Validation
- **Optional Fields**: All fields nullable, validation only applied to non-null values
- **Business Rules**: Same validation as CreateCompany when fields provided:
  - `siret`: 14 digits, valid French SIRET format
  - `vat`: Valid European VAT format (e.g., "FR12345678901")
  - `country`: ISO 3166-1 alpha-2 country code
  - `name`: Non-empty string, trimmed
  - `siteUrl`: Valid HTTP/HTTPS URL format
  - `headOffice.zipCode`: Valid postal code for country
  - `socials`: Valid social platform URLs

### Business Constraints
- **Unique Identifiers**: SIRET and VAT remain unique across all companies (active + inactive)
- **Required Fields**: Cannot update required fields to null/empty values
- **Relationship Integrity**: Updates cannot break existing partnership or job offer references

## State Transitions

### Company Lifecycle
```
[CREATED] → ACTIVE → INACTIVE
           ↑         ↓
           └─ REACTIVATE (future extension)
```

**Current Implementation**: Only supports ACTIVE → INACTIVE transition (soft delete)
**Future Extensions**: Could support INACTIVE → ACTIVE (reactivation) without schema changes

### Status Change Effects
- **ACTIVE → INACTIVE**: Company marked as deleted, preserved in all relationships
- **Listing Behavior**: Inactive companies shown with status indicator by default
- **Search Impact**: Status filter allows excluding inactive companies when desired

## Performance Considerations

### Database Optimization
- **Status Index**: Enables efficient filtering queries: `WHERE status = 'ACTIVE'`
- **Composite Queries**: Status + name search optimized: `WHERE status = ? AND name ILIKE ?`
- **Pagination**: Existing pagination logic works with status filtering

### Query Patterns
```sql
-- Default: All companies
SELECT * FROM companies ORDER BY created_at DESC LIMIT 20;

-- Active only
SELECT * FROM companies WHERE status = 'ACTIVE' ORDER BY created_at DESC LIMIT 20;

-- Search active companies
SELECT * FROM companies 
WHERE status = 'ACTIVE' AND name ILIKE '%search%' 
ORDER BY created_at DESC LIMIT 20;
```

## Migration Strategy

### Backwards Compatibility
1. **Schema Migration**: Add `status` column with `ACTIVE` default
2. **Existing Data**: All current companies automatically become `ACTIVE`
3. **API Compatibility**: Existing GET endpoints return companies with new `status` field
4. **Client Impact**: Frontend can optionally use status information, no breaking changes

### Database Migration
```sql
-- Add status column with default value and index
ALTER TABLE companies 
ADD COLUMN status VARCHAR(10) NOT NULL DEFAULT 'ACTIVE';

CREATE INDEX idx_companies_status ON companies(status);
```

**Migration Safety**: 
- Non-breaking change (adds column with default)
- Existing queries continue to work
- New functionality available immediately
- Rollback possible by dropping column and index