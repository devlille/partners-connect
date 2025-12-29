# Research: Update Partnership Contact Information

**Date**: 2025-11-29  
**Feature**: Update Partnership Contact Information  
**Branch**: 013-update-partnership

## Research Questions & Findings

### 1. Email Validation Pattern in Kotlin/Ktor

**Decision**: Use kotlinx.serialization with JSON schema validation via `call.receive<T>(schema)` pattern

**Rationale**: 
- Project already uses JSON schema validation pattern for all API endpoints (per Constitution IV)
- Email validation defined in JSON schema using RFC 5322 format pattern
- Automatic 400 Bad Request responses for validation failures
- Consistent with existing partnership endpoints (RegisterPartnership, etc.)

**Alternatives Considered**:
- Manual regex validation in Kotlin code - REJECTED: duplicates validation logic, harder to maintain
- Apache Commons Validator - REJECTED: adds dependency, doesn't integrate with OpenAPI documentation
- Ktor's built-in validation - REJECTED: less declarative than schema-based approach

**Implementation**: 
```json
// In update_partnership_request.schema.json
{
  "emails": {
    "type": "array",
    "items": {
      "type": "string",
      "format": "email"
    }
  }
}
```

### 2. ISO 639-1 Language Code Validation

**Decision**: Use JSON schema enum validation with supported language codes

**Rationale**:
- Clarification confirmed ISO 639-1 (2-letter codes: en, fr, de)
- JSON schema supports enum validation natively
- Provides clear error messages for invalid language codes
- Existing codebase stores language as text field (varchar 50)

**Alternatives Considered**:
- Runtime validation in repository layer - REJECTED: validation should happen at API boundary
- Database CHECK constraint - REJECTED: harder to update supported languages, less flexible
- Custom validator class - REJECTED: JSON schema provides same functionality

**Implementation**:
```json
{
  "language": {
    "type": ["string", "null"],
    "enum": ["en", "fr", "de", "nl", "es", null]
  }
}
```

**Note**: Initial language support includes common European languages. Can be expanded based on event requirements.

### 3. Partial Update Pattern in Exposed ORM

**Decision**: Use kotlinx.serialization optionals with explicit null handling in repository

**Rationale**:
- Existing codebase pattern: all fields in UpdatePartnershipContactInfo are nullable
- Repository checks if field is provided (not null) before updating entity property
- Supports clearing optional fields (phone) by sending explicit null
- Aligns with REST PUT semantics for partial updates

**Alternatives Considered**:
- Separate update DTOs per field - REJECTED: too many endpoints, reduces flexibility
- JSON Merge Patch (RFC 7396) - REJECTED: more complex, overkill for this use case
- PATCH for partial updates - REJECTED: Using PUT with optional fields for simplicity

**Implementation Pattern**:
```kotlin
// DTO with all nullable fields
@Serializable
data class UpdatePartnershipContactInfo(
    @SerialName("contact_name") val contactName: String? = null,
    @SerialName("contact_role") val contactRole: String? = null,
    val language: String? = null,
    val phone: String? = null,
    val emails: List<String>? = null
)

// Repository applies only provided fields
fun updateContactInfo(eventSlug: String, partnershipId: UUID, update: UpdatePartnershipContactInfo): Partnership {
    val entity = findEntityOrThrow(eventSlug, partnershipId)
    update.contactName?.let { entity.contactName = it }
    update.contactRole?.let { entity.contactRole = it }
    update.language?.let { entity.language = it }
    update.phone?.let { entity.phone = it }
    update.emails?.let { /* handle emails separately */ }
    return entity.toDomain()
}
```

### 4. Email Storage Pattern

**Decision**: Research existing email storage in partnerships

**Finding**: Partnerships currently don't store emails in PartnershipsTable. Need to check if:
- Emails should be stored in partnership record
- Emails are stored in separate table (partnership_emails)
- Emails are company-level data

**Research Result**: Examined PartnershipsTable and PartnershipEntity:
- No `emails` column exists in current schema
- RegisterPartnership accepts `emails: List<String>` but they're not persisted to partnerships table
- Emails likely stored elsewhere or not currently implemented

**Decision**: Add `emails` column to PartnershipsTable as text array (PostgreSQL supports array types)

**Rationale**:
- Spec explicitly requires emails to be updatable partnership contact information
- Storing as JSON text allows multiple emails per partnership
- Exposed supports PostgreSQL array columns via `array<String>()` function
- Aligns with contact information being partnership-specific (not company-wide)

**Implementation**:
```kotlin
// In PartnershipsTable
val emails = array<String>("emails").default(emptyList())

// In PartnershipEntity  
var emails by PartnershipsTable.emails
```

**Migration Required**: Add emails column to partnerships table

### 5. Phone Number Validation (30 char limit)

**Decision**: Use JSON schema maxLength validation

**Rationale**:
- Clarification confirmed free-form text with 1-30 character limit
- PartnershipsTable already has `phone = text("phone").nullable()`
- JSON schema provides maxLength validation
- No format enforcement needed (free-form)

**Implementation**:
```json
{
  "phone": {
    "type": ["string", "null"],
    "minLength": 1,
    "maxLength": 30
  }
}
```

### 6. Concurrent Update Handling

**Decision**: Database-level optimistic locking not required for this feature

**Rationale**:
- Edge case identified in spec but rare in practice for contact updates
- PostgreSQL transaction isolation provides basic protection
- No version field in PartnershipsTable currently
- Adding optimistic locking would require schema changes and increased complexity

**Alternative**: Document as known limitation, address in future if needed

**Risk Mitigation**: Last-write-wins is acceptable for contact information updates

### 7. Public Endpoint Security Considerations

**Decision**: Completely public endpoint with no authentication (per clarification)

**Security Analysis**:
- **Risk**: Anyone can update any partnership contact info with partnership ID
- **Mitigation**: Partnership IDs are UUIDs (non-guessable)
- **Trade-off**: Simplicity vs security - spec explicitly requires public access
- **Future Enhancement**: Consider token-based update links (emailed to partner)

**Current Implementation**: No security checks, rely on UUID obscurity

## Best Practices Applied

### Kotlin/Ktor Patterns
- Use `call.receive<T>(schema)` for JSON schema validation
- Use `call.parameters.eventSlug` extension for parameter extraction
- Throw `NotFoundException` for missing resources (StatusPages handles HTTP 404)
- Follow existing publicPartnershipRoutes pattern for route definition

### Exposed ORM Patterns
- Use existing PartnershipEntity delegation pattern
- Use `PartnershipEntity.singleByEventAndPartnership()` for lookup
- Transaction handling via Exposed's transaction {} blocks
- Return domain objects from repository, not entities

### Testing Patterns
- Contract tests validate request/response schemas only
- Integration tests validate end-to-end HTTP behavior with H2 database
- Use `insertMockedPartnership()` factory for test data setup
- Test partial updates separately (each field combination)

## Technology Decisions Summary

| Aspect | Technology | Justification |
|--------|-----------|---------------|
| Validation | JSON Schema + kotlinx.serialization | Constitution requirement, automatic validation |
| Email Format | RFC 5322 (schema format: email) | Standard email validation |
| Language Codes | ISO 639-1 enum in schema | Clarification, simple validation |
| Partial Updates | Nullable fields in DTO | REST PATCH semantics, flexible |
| Email Storage | PostgreSQL text array | Multiple emails support, queryable |
| Phone Validation | Schema maxLength (30) | Clarification, free-form with limit |
| Concurrency | PostgreSQL transaction isolation | Good enough, avoid complexity |
| Security | None (public endpoint) | Per clarification, UUID obscurity |

## Open Questions Resolved

1. ✅ How to validate emails? → JSON schema format: email
2. ✅ Which language codes? → ISO 639-1 enum in schema  
3. ✅ How to handle partial updates? → Nullable DTO fields with let {} checks
4. ✅ Where to store emails? → Add array column to PartnershipsTable
5. ✅ Phone validation format? → maxLength 30, minLength 1
6. ✅ Authentication required? → No, completely public endpoint

## Implementation Risks

| Risk | Impact | Mitigation |
|------|--------|------------|
| Anyone can update partnerships | HIGH | Document UUID obscurity, consider future token system |
| Concurrent updates (last-write-wins) | LOW | Accept for MVP, monitor in production |
| Email array column migration | MEDIUM | Test migration thoroughly, ensure backwards compatibility |
| Invalid language code handling | LOW | Schema validation prevents, clear error messages |

## Next Steps (Phase 1)

1. Create data-model.md documenting PartnershipEntity changes
2. Generate JSON schema in contracts/update_partnership_request.schema.json
3. Update OpenAPI specification with PATCH endpoint
4. Create quickstart.md for testing the update endpoint
5. Run agent context update script
