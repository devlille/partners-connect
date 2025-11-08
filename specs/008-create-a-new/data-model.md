# Data Model: Public Partnership Information Endpoint

## Domain Models

### DetailedPartnershipResponse (NEW)
**Purpose**: Comprehensive response model for public partnership information endpoint using existing domain models for company and event data.

**Kotlin Model**:
```kotlin
@Serializable
data class DetailedPartnershipResponse(
    val partnership: PartnershipDetail,
    val company: Company,  // Reuse existing domain model
    val event: EventWithOrganisation  // Reuse existing domain model
)
```

**JSON Schema**: Nested structure leveraging existing schemas
```json
{
  "partnership": { /* Partnership details */ },
  "company": { /* Existing Company model */ },
  "event": { /* Existing EventWithOrganisation model */ }
}
```

### PartnershipDetail (NEW)
**Purpose**: Enhanced partnership information with process status and timestamps - only part that needs new modeling.

**Fields**:
| Field Name | Type | Description |
|------------|------|-------------|
| id | String | Partnership UUID identifier |
| phone | String? | Partnership contact phone number |
| contactName | String | Partnership contact person name |
| contactRole | String | Partnership contact person role |
| language | String | Partnership language preference |
| emails | List<String> | Partnership contact email addresses |
| selectedPack | PartnershipPack? | Currently selected sponsoring package |
| suggestionPack | PartnershipPack? | Suggested sponsoring package |
| validatedPack | PartnershipPack? | Validated sponsoring package (uses existing PartnershipEntity.validatedPack extension) |
| processStatus | PartnershipProcessStatus | Detailed workflow status with timestamps |
| createdAt | String | Partnership creation timestamp |
| updatedAt | String | Partnership last update timestamp |

### CompanyDetail (REUSE EXISTING)
**Purpose**: Use existing `Company` domain model from `companies/domain/Company.kt`

**Benefits**:
- Already contains all required fields: name, siret, vat, siteUrl, description, headOffice, medias, status
- Proper serialization annotations already defined
- Consistent with other API responses across the application
- No duplication of model definitions

### EventDetail (REUSE EXISTING)
**Purpose**: Use existing `EventWithOrganisation` domain model from `events/domain/EventWithOrganisation.kt`

**Benefits**:
- Contains `EventDisplay` with all event details and `OrganisationItem` with organization info
- Already includes dates, address, contact information, external links, providers
- Proper serialization and field naming already implemented
- Maintains consistency across event-related endpoints

### PartnershipProcessStatus (NEW)
**Purpose**: Detailed workflow status tracking with timestamps for all process phases.

**Fields**:
| Field Name | Type | Description |
|------------|------|-------------|
| suggestionSentAt | String? | When sponsoring package suggestion was sent |
| suggestionApprovedAt | String? | When suggestion was approved by company |
| suggestionDeclinedAt | String? | When suggestion was declined by company |
| validatedAt | String? | When partnership was validated by organizer |
| declinedAt | String? | When partnership was declined by organizer |
| agreementUrl | String? | Generated partnership agreement document URL |
| agreementSignedUrl | String? | Signed partnership agreement document URL |
| communicationPublicationDate | String? | When partnership communication was published |
| communicationSupportUrl | String? | Partnership communication support materials URL |
| billingStatus | String? | Current billing/payment status from BillingsTable (PENDING, SENT, PAID) |
| currentStage | String | Derived current workflow stage |

### Supporting Detail Models (EXISTING)

**PartnershipPack** (reuse existing from partnership domain):
- id: String
- name: String
- basePrice: Int
- options: List<SponsoringOption>

**Address** (from companies/domain/Company.kt):
- address: String
- city: String
- zipCode: String  
- country: String

**Contact** (from events domain):
- displayName: String
- role: String

## Repository Architecture (UPDATED)

### Multi-Repository Approach
The detailed response will be assembled using three separate repositories, respecting SOLID principles:

**PartnershipRepository**:
- `getByIdDetailed(eventSlug: String, partnershipId: UUID): Partnership` 
- Returns enhanced Partnership model with process status
- Only responsible for partnership-specific data

**CompanyRepository** (existing):
- `getById(companyId: UUID): Company`
- Returns complete Company domain model
- Handles all company-related data and relationships

**EventRepository** (existing):
- `getBySlug(eventSlug: String): EventWithOrganisation`
- Returns complete EventWithOrganisation model  
- Includes event details and organization information

### Billing Integration
The partnership payment status is determined by the billing system:

**BillingsTable Integration**:
- Partnerships are linked to billing records via foreign key relationship
- `billingStatus` field populated from `BillingsTable.status` column
- Status values defined by `InvoiceStatus` enum:
  - `PENDING`: Invoice created but not yet sent
  - `SENT`: Invoice sent to company but payment not received  
  - `PAID`: Payment received and processed

**Query Strategy**:
- Partnership repository will join with BillingsTable to retrieve current billing status
- If no billing record exists, `billingStatus` will be null
- Multiple billing records possible - use most recent status

### Route Handler Orchestration
The route handler will coordinate the three repositories:

```kotlin
get("/events/{eventSlug}/partnerships/{partnershipId}") {
    val eventSlug = call.parameters.eventSlug
    val partnershipId = call.parameters.partnershipId.toUUID()
    
    // Fetch from separate repositories
    val partnership = partnershipRepository.getByIdDetailed(eventSlug, partnershipId)
    val company = companyRepository.getById(partnership.companyId)  
    val event = eventRepository.getBySlug(eventSlug)
    
    // Assemble response
    val response = DetailedPartnershipResponse(
        partnership = partnership.toPartnershipDetail(),
        company = company,
        event = event
    )
    
    call.respond(HttpStatusCode.OK, response)
}
```

### Mapper Responsibilities (SIMPLIFIED)

**PartnershipEntityDetailMapper**:
- Maps `PartnershipEntity` to `PartnershipDetail`
- Constructs `PartnershipProcessStatus` from timestamp fields
- Handles selected/suggestion pack mapping
- Uses existing `PartnershipEntity.validatedPack` extension to populate `validatedPack` field
- Only responsibility - partnership-specific data transformation

**No Company/Event Mappers Needed**:
- Company data uses existing `CompanyRepository.getById()` → `Company` model
- Event data uses existing `EventRepository.getBySlug()` → `EventWithOrganisation` model
- Eliminates duplicate mapping logic and maintains consistency

## Validation Rules

### Request Validation
- **eventSlug**: Must be non-empty, alphanumeric with hyphens
- **partnershipId**: Must be valid UUID format (handled by `toUUID()` extension)

### Response Validation
- **All timestamps**: ISO 8601 format (UTC timezone)
- **URLs**: Valid HTTP/HTTPS format where applicable
- **Email addresses**: Valid email format in lists
- **Phone numbers**: Non-empty strings when present

### Business Rules
- **Partnership-Event Association**: Partnership must belong to specified event
- **Entity Existence**: All referenced entities (partnership, company, event) must exist
- **Data Completeness**: All required fields must be present (no null safety violations)
- **Validated Pack Logic**: `validatedPack` field uses existing `PartnershipEntity.validatedPack` extension to determine which pack (selected or suggestion) has been validated, returns null if no pack is validated yet

## State Transitions

### Partnership Process Workflow
The `currentStage` field in `PartnershipProcessStatus` derives from timestamp combinations and billing status:

1. **CREATED**: Partnership exists, no suggestion sent
2. **SUGGESTION_SENT**: `suggestionSentAt` populated
3. **SUGGESTION_APPROVED**: `suggestionApprovedAt` populated
4. **SUGGESTION_DECLINED**: `suggestionDeclinedAt` populated  
5. **VALIDATED**: `validatedAt` populated
6. **DECLINED**: `declinedAt` populated
7. **AGREEMENT_GENERATED**: `agreementUrl` populated
8. **AGREEMENT_SIGNED**: `agreementSignedUrl` populated
9. **PAID**: `billingStatus` equals "PAID" (from BillingsTable.status)
10. **COMMUNICATION_PUBLISHED**: `communicationPublicationDate` populated

### Terminal States
- **SUGGESTION_DECLINED**: Partnership suggestion rejected by company
- **DECLINED**: Partnership rejected by organizer
- **COMMUNICATION_PUBLISHED**: Partnership fully processed and public

## Performance Considerations

### Database Access Patterns (UPDATED)
- **Partnership Query**: Single query on `PartnershipEntity` with JOIN to `BillingsTable` for billing status
- **Company Query**: Separate query via existing `CompanyRepository` (already optimized)
- **Event Query**: Separate query via existing `EventRepository` (already optimized)  
- **Total Queries**: 3 optimized queries leveraging existing repository patterns
- **Benefits**: Reuses existing query optimizations, caching strategies, and relationship mappings

### Data Volume Expectations
- **Single Record Response**: One partnership with related company and event
- **Payload Size**: Estimated 2-5KB JSON response per request
- **Query Complexity**: Simple joins leveraging existing foreign key indexes

### Caching Strategy (Future)
- **Entity-Level**: Partnership data changes infrequently
- **Response-Level**: Full response suitable for HTTP caching headers
- **Cache Invalidation**: On partnership updates (not implemented initially)