# Research: Job Offers Management Implementation

## Technical Research Findings

### Database Schema Design
**Decision**: Use Exposed ORM with new CompanyJobOfferTable linked to existing CompaniesTable via foreign key  
**Rationale**: Follows existing project patterns, maintains referential integrity, supports efficient queries  
**Alternatives considered**: 
- NoSQL document storage (rejected: inconsistent with existing PostgreSQL usage)
- Embedded job offers in Company entity (rejected: violates normalization, complicates querying)

### REST API Patterns
**Decision**: Extend existing `/companies/{companyId}/` namespace with job offer sub-resources  
**Rationale**: Maintains resource hierarchy, leverages existing authentication/authorization, follows REST conventions  
**Alternatives considered**:
- Separate `/job-offers` namespace (rejected: breaks company ownership context)
- GraphQL (rejected: project uses REST consistently)

### Validation Strategy  
**Decision**: Use JSON Schema validation for request bodies with Ktor integration  
**Rationale**: Consistent with existing project patterns (create_company.schema.json), provides client-side validation docs  
**Alternatives considered**:
- Manual validation in domain (rejected: error-prone, no API documentation benefits)
- Kotlinx.serialization only (rejected: less comprehensive validation rules)

### URL Validation Approach
**Decision**: Validate URL format using Java URI class, check accessibility optional for performance  
**Rationale**: Fast validation, prevents obviously malformed URLs, avoids external network calls in request path  
**Alternatives considered**:
- HTTP HEAD requests to validate accessibility (rejected: adds latency, external dependency)
- Regex-only validation (rejected: less robust than URI parsing)

### Date Handling
**Decision**: Use LocalDateTime for publication/end dates, validate publication date not in future, end date after publication  
**Rationale**: Dates represent calendar dates not timestamps, prevents logical inconsistencies  
**Alternatives considered**:
- Instant/timestamp (rejected: unnecessary precision for job posting dates)
- String dates (rejected: no type safety, manual parsing required)

### Experience Years Validation
**Decision**: Integer validation with range 1-20 years as specified in clarifications  
**Rationale**: Specification provides explicit range, catches data entry errors  
**Alternatives considered**:
- Unbounded positive integer (rejected: allows unrealistic values)
- Float for partial years (rejected: unnecessary complexity)

### Salary Information Structure
**Decision**: Store as optional string field with no format constraints initially  
**Rationale**: Salary can be ranges, exact amounts, currency-specific, or "competitive" - string provides flexibility  
**Alternatives considered**:
- Structured amount + currency (rejected: overly constraining for diverse salary representations)
- Numeric only (rejected: doesn't handle ranges or text descriptions)

### Partnership Promotion Integration
**Decision**: Reference existing PartnershipRepository, validate active partnerships when promoting job offers  
**Rationale**: Leverages existing partnership validation logic, maintains data consistency  
**Alternatives considered**:
- Duplicate partnership status logic (rejected: code duplication)
- Store promotion status in job offer (rejected: denormalization, sync issues)

### Performance Optimization
**Decision**: Add database index on company_id for job offer queries, use pagination for list endpoints  
**Rationale**: Primary query pattern is "all job offers for company", pagination prevents memory issues  
**Alternatives considered**:
- Load all job offers always (rejected: poor performance for companies with many offers)
- Complex caching layer (rejected: premature optimization)

## Integration Points

### Existing Company Domain
- Extends companies module without breaking changes
- Uses existing Company entity validation and security
- Maintains modular architecture boundaries

### Partnership System
- Read-only integration with PartnershipRepository for promotion validation
- No modifications to existing partnership workflows
- Promotes job offers through existing partnership context

### Authentication/Authorization  
- Leverages existing company ownership validation
- Uses same JWT/session handling as other company endpoints
- Maintains principle of least privilege

## Risk Mitigation

### Database Migration Safety
- New table creation is non-breaking change
- Foreign key constraints ensure data integrity
- Backward compatibility maintained for existing endpoints

### API Versioning Compatibility
- All new endpoints under existing company namespace
- No changes to existing endpoint contracts
- OpenAPI documentation updated incrementally

### Performance Impact
- Job offer operations isolated to company owners
- Indexes added for efficient querying
- No impact on existing company operations