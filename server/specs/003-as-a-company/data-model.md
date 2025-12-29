# Data Model: Job Offers Management

## Core Entities

### JobOffer Entity
**Purpose**: Represents an employment opportunity posted by a company

**Fields**:
- `id: UUID` - Unique identifier (primary key)
- `companyId: UUID` - Foreign key to Companies table (non-null)
- `url: String` - Link to detailed job posting (non-null, validated as URI)
- `title: String` - Position name (non-null, text)
- `location: String` - Work location (non-null, text)  
- `publicationDate: DateTime` - When job was posted (non-null, not future)
- `endDate: DateTime?` - Application deadline (nullable, must be after publication)
- `experienceYears: Int?` - Minimum experience required (nullable, 1-20 range)
- `salary: String?` - Salary information (nullable, max 100 chars)
- `createdAt: DateTime` - Record creation timestamp (auto-generated)
- `updatedAt: DateTime` - Record modification timestamp (auto-updated)

**Relationships**:
- Belongs to one Company (many-to-one via companyId)
- Can be promoted through multiple Partnerships (implied, not stored directly)

**Validation Rules**:
- URL must be valid URI format
- Title and location cannot be empty/blank
- Publication date cannot be in the future
- End date (if provided) must be after publication date
- Experience years (if provided) must be between 1 and 20
- All dates use system timezone consistently

**State Transitions**:
- Created → Active (default state)
- Active → Updated (when modified)
- Active → Deleted (soft or hard delete based on business requirements)

## Domain Models

### CreateJobOffer (Request Model)
```kotlin
data class CreateJobOffer(
    val url: String,
    val title: String,
    val location: String,
    val publicationDate: LocalDateTime,
    val endDate: LocalDateTime? = null,
    val experienceYears: Int? = null,
    val salary: String? = null
)
```

### UpdateJobOffer (Request Model)  
```kotlin
data class UpdateJobOffer(
    val url: String? = null,
    val title: String? = null,
    val location: String? = null,
    val publicationDate: LocalDateTime? = null,
    val endDate: LocalDateTime? = null,
    val experienceYears: Int? = null,
    val salary: String? = null
)
```

### JobOfferResponse (Response Model)
```kotlin
data class JobOfferResponse(
    val id: UUID,
    val companyId: UUID,
    val url: String,
    val title: String,
    val location: String,
    val publicationDate: LocalDateTime,
    val endDate: LocalDateTime?,
    val experienceYears: Int?,
    val salary: String?,
    val createdAt: LocalDateTime,
    val updatedAt: LocalDateTime
)
```

## Database Schema

### CompanyJobOfferTable (Exposed)
```kotlin
object CompanyJobOfferTable : UUIDTable("company_job_offers") {
    val companyId = reference("company_id", CompaniesTable.id)
    val url = varchar("url", 500)
    val title = text("title")
    val location = text("location")
    val publicationDate = datetime("publication_date")
    val endDate = datetime("end_date").nullable()
    val experienceYears = integer("experience_years").nullable()
    val salary = varchar("salary", 100).nullable()
    val createdAt = datetime("created_at").clientDefault {
        Clock.System.now().toLocalDateTime(TimeZone.UTC)
    }
    val createdAt = datetime("updated_at").clientDefault {
        Clock.System.now().toLocalDateTime(TimeZone.UTC)
    }

    init {
        index(false, companyId) // For efficient company-based queries
        check("valid_experience_years") { experienceYears.isNotNull() implies (experienceYears greaterEq 1 and (experienceYears lessEq 20)) }
        check("end_date_after_publication") { endDate.isNotNull() implies (endDate greater publicationDate) }
    }
}
```

### CompanyJobOfferEntity (Exposed)
```kotlin
class CompanyJobOfferEntity(id: EntityID<UUID>) : UUIDEntity(id) {
    companion object : UUIDEntityClass<CompanyJobOfferEntity>(CompanyJobOfferTable)

    var companyId by CompanyJobOfferTable.companyId
    var url by CompanyJobOfferTable.url
    var title by CompanyJobOfferTable.title
    var location by CompanyJobOfferTable.location
    var publicationDate by CompanyJobOfferTable.publicationDate
    var endDate by CompanyJobOfferTable.endDate
    var experienceYears by CompanyJobOfferTable.experienceYears
    var salary by CompanyJobOfferTable.salary
    val createdAt by CompanyJobOfferTable.createdAt
    var updatedAt by CompanyJobOfferTable.updatedAt

    // Relationship to Company
    val company by CompanyEntity referencedOn CompanyJobOfferTable.companyId
}
```

## Repository Interface

### CompanyJobOfferRepository
```kotlin
interface CompanyJobOfferRepository {
    suspend fun create(companyId: UUID, jobOffer: CreateJobOffer): UUID
    suspend fun findById(jobOfferId: UUID): JobOfferResponse
    suspend fun findByCompany(companyId: UUID, page: Int = 1, pageSize: Int = DEFAULT_PAGE_SIZE): PaginatedResult<JobOfferResponse>
    suspend fun update(jobOfferId: UUID, jobOffer: UpdateJobOffer, companyId: UUID): Boolean
    suspend fun delete(jobOfferId: UUID, companyId: UUID): Boolean
    suspend fun existsByCompanyAndId(companyId: UUID, jobOfferId: UUID): Boolean
}
```

## Migration Strategy

### Database Migration
1. Create new `company_job_offers` table with foreign key to existing `companies` table
2. Add database indexes for performance optimization
3. No changes to existing tables required
4. Migration is backwards-compatible

### API Evolution
1. New endpoints added under existing `/companies/{companyId}/` namespace
2. No breaking changes to existing endpoints
3. OpenAPI specification updated with new schemas
4. Client libraries can be updated incrementally

## Data Flow

### Create Job Offer Flow
1. Validate company ownership and authentication
2. Validate job offer data against schema
3. Apply business rules (date validation, experience range)
4. Create database record with timestamps
5. Return created job offer with generated ID

### Update Job Offer Flow  
1. Validate company ownership of both company and job offer
2. Retrieve existing job offer
3. Apply partial updates (non-null fields only)
4. Validate updated state against business rules
5. Update database record with new timestamp
6. Return updated job offer

### List Job Offers Flow
1. Validate company ownership and authentication
2. Query job offers by company ID with pagination
3. Apply sorting (most recent first)
4. Return paginated results with metadata

### Delete Job Offer Flow
1. Validate company ownership of both company and job offer
2. Check if job offer exists
3. Remove database record
4. Return success/failure status

## Security Considerations

### Access Control
- Job offers can only be managed by authenticated company owners
- Company ownership validated for all operations
- Job offer ownership implicitly derived from company ownership

### Data Validation
- All input validated against JSON schemas
- Business rule validation at domain layer
- SQL injection prevention through parameterized queries

### Audit Trail
- Creation and modification timestamps maintained
- Company ID preserved for ownership tracking
- Deletion can be soft delete if audit requirements exist