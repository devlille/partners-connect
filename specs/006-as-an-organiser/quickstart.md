# Quickstart: Partnership Validation with Customization

## Overview

This quickstart guide provides executable test scenarios for validating partnerships with customized package details (tickets, job offers, booth size). Each scenario maps directly to acceptance criteria defined in `spec.md`.

## Prerequisites

- Server running on `http://localhost:8080`
- PostgreSQL database initialized with test data
- Valid Google OAuth token with organizer permissions
- Bruno CLI installed or use Bruno GUI

## Test Data Setup

Before running scenarios, ensure test data exists:

```bash
# From /bruno directory
bruno run companies/Create.bru --env Local
bruno run events/Create.bru --env Local  
bruno run organisations/Create.bru --env Local
```

**Required Test Entities:**
- Organization: `devlille` (slug)
- Event: `devlille-2026` (slug)
- Sponsoring Pack: Gold tier with `nbTickets=10, nbJobOffers=2, boothSize="3x3m"`
- Company: Partner company requesting sponsorship
- Partnership: Created partnership in "pending validation" state

## Test Scenarios

### Scenario 1: Validate with Pack Defaults

**User Story:** Organizer validates partnership without customization

**Test Steps:**
```bash
POST /organisations/devlille/events/devlille-2026/partnership/{partnershipId}/validate
Headers:
  Authorization: Bearer {organizer_token}
Body: {} or omit entirely
```

**Expected Result:**
- HTTP 200 OK
- Response contains:
  - `validatedAt`: Current timestamp
  - `validatedNbTickets`: 10 (from pack)
  - `validatedNbJobOffers`: 2 (from pack)
  - `validatedBoothSize`: "3x3m" (from pack)
  - `agreementSignedUrl`: null

**Validation:**
```bash
# Query partnership and verify validated fields match pack defaults
GET /organisations/devlille/events/devlille-2026/partnership/{partnershipId}
```

---

### Scenario 2: Validate with Custom Ticket Count

**User Story:** Organizer reduces tickets from pack default

**Test Steps:**
```bash
POST /organisations/devlille/events/devlille-2026/partnership/{partnershipId}/validate
Body:
{
  "nbTickets": 5,
  "nbJobOffers": 2
}
```

**Expected Result:**
- HTTP 200 OK
- Response contains:
  - `validatedNbTickets`: 5 (custom value)
  - `validatedNbJobOffers`: 2 (required field)
  - `validatedBoothSize`: "3x3m" (pack default)

**Validation:**
```sql
-- Query database to verify denormalized snapshot
SELECT validated_nb_tickets, validated_nb_job_offers, validated_booth_size 
FROM partnerships 
WHERE id = '{partnershipId}';
-- Expected: 5, 2, "3x3m"
```

---

### Scenario 3: Validate with Custom Booth Size

**User Story:** Organizer assigns different booth size than pack default

**Pre-conditions:**
- Event has multiple packs: Gold ("3x3m"), Silver ("6x2m")
- Partnership is for Gold pack (default "3x3m")

**Test Steps:**
```bash
POST /organisations/devlille/events/devlille-2026/partnership/{partnershipId}/validate
Body:
{
  "nbJobOffers": 2,
  "boothSize": "6x2m"
}
```

**Expected Result:**
- HTTP 200 OK
- Response contains:
  - `validatedNbTickets`: 10 (pack default)
  - `validatedNbJobOffers`: 2 (required)
  - `validatedBoothSize`: "6x2m" (custom, from Silver pack)

**Validation:**
- Booth size "6x2m" exists in Silver pack → validation passes
- Partnership stores "6x2m" even though assigned to Gold pack

---

### Scenario 4: Validate with Invalid Booth Size

**User Story:** System rejects booth size not in any event pack

**Pre-conditions:**
- Event packs only have: "3x3m", "6x2m"

**Test Steps:**
```bash
POST /organisations/devlille/events/devlille-2026/partnership/{partnershipId}/validate
Body:
{
  "nbJobOffers": 2,
  "boothSize": "10x10m"
}
```

**Expected Result:**
- HTTP 400 Bad Request
- Error response:
```json
{
  "error": "Bad Request",
  "message": "Booth size '10x10m' is not available in any sponsoring pack for this event",
  "status": 400
}
```

**Validation:**
- Partnership remains unvalidated (`validatedAt` is null)
- No changes to database

---

### Scenario 5: Validate with Zero Tickets

**User Story:** Organizer explicitly sets no tickets

**Test Steps:**
```bash
POST /organisations/devlille/events/devlille-2026/partnership/{partnershipId}/validate
Body:
{
  "nbTickets": 0,
  "nbJobOffers": 1
}
```

**Expected Result:**
- HTTP 200 OK
- Response contains:
  - `validatedNbTickets`: 0 (explicit zero allowed)
  - `validatedNbJobOffers`: 1

**Validation:**
- Zero is valid (non-negative constraint satisfied)
- Different from null (pack default)

---

### Scenario 6: Re-validate Before Agreement Signature

**User Story:** Organizer corrects validation details before agreement signed

**Pre-conditions:**
- Partnership validated with `nbTickets=10, nbJobOffers=2`
- `agreementSignedUrl` is null

**Test Steps:**
```bash
# Re-validate with different values
POST /organisations/devlille/events/devlille-2026/partnership/{partnershipId}/validate
Body:
{
  "nbTickets": 15,
  "nbJobOffers": 3,
  "boothSize": "6x2m"
}
```

**Expected Result:**
- HTTP 200 OK
- Response shows updated values:
  - `validatedNbTickets`: 15 (updated)
  - `validatedNbJobOffers`: 3 (updated)
  - `validatedBoothSize`: "6x2m" (updated)
  - `validatedAt`: New timestamp (re-validation time)

**Validation:**
- Original values overwritten, not appended
- Only most recent validation stored

---

### Scenario 7: Block Re-validation After Agreement Signed

**User Story:** System prevents changes after agreement signed

**Pre-conditions:**
- Partnership validated
- Agreement uploaded: `agreementSignedUrl` is `"https://storage.googleapis.com/agreements/signed.pdf"`

**Test Steps:**
```bash
POST /organisations/devlille/events/devlille-2026/partnership/{partnershipId}/validate
Body:
{
  "nbTickets": 20,
  "nbJobOffers": 5
}
```

**Expected Result:**
- HTTP 400 Bad Request
- Error response:
```json
{
  "error": "Conflict",
  "message": "Cannot re-validate partnership: agreement already signed",
  "status": 400
}
```

**Validation:**
- Original validated values unchanged
- `validatedAt` timestamp unchanged

---

### Scenario 8: Validate with Negative Ticket Count

**User Story:** System rejects negative values

**Test Steps:**
```bash
POST /organisations/devlille/events/devlille-2026/partnership/{partnershipId}/validate
Body:
{
  "nbTickets": -5,
  "nbJobOffers": 2
}
```

**Expected Result:**
- HTTP 400 Bad Request
- Error message: `"nbTickets must be >= 0"`

---

### Scenario 9: Validate Without Job Offers (Missing Required Field)

**User Story:** System rejects validation missing required field

**Test Steps:**
```bash
POST /organisations/devlille/events/devlille-2026/partnership/{partnershipId}/validate
Body:
{
  "nbTickets": 10
}
```

**Expected Result:**
- HTTP 400 Bad Request
- Error message: `"nbJobOffers is required"`

---

### Scenario 10: Validate with Extremely Large Counts

**User Story:** System accepts unlimited ticket/job offer counts

**Test Steps:**
```bash
POST /organisations/devlille/events/devlille-2026/partnership/{partnershipId}/validate
Body:
{
  "nbTickets": 999999,
  "nbJobOffers": 100
}
```

**Expected Result:**
- HTTP 200 OK
- No upper limit validation error
- Values stored as-is

**Validation:**
- Business decision: No artificial limits enforced
- Database column type: INTEGER (max 2,147,483,647)

---

### Scenario 11: Concurrent Validation (First-Wins)

**User Story:** System handles concurrent validation requests consistently

**Test Steps:**
```bash
# Terminal 1: Start validation
POST /organisations/devlille/events/devlille-2026/partnership/{partnershipId}/validate
Body:
{
  "nbTickets": 10,
  "nbJobOffers": 2
}

# Terminal 2: Simultaneously validate (within same second)
POST /organisations/devlille/events/devlille-2026/partnership/{partnershipId}/validate
Body:
{
  "nbTickets": 15,
  "nbJobOffers": 3
}
```

**Expected Result:**
- Both requests return HTTP 200 OK
- Transaction isolation (READ COMMITTED) ensures consistency
- Final database state reflects one of the two validations (first transaction to commit wins)
- No partial updates (all validated_* columns updated atomically)

**Validation:**
```bash
# Query partnership immediately after
GET /organisations/devlille/events/devlille-2026/partnership/{partnershipId}
# Verify either (10, 2, pack_booth) OR (15, 3, pack_booth) - no mixed state
```

---

### Scenario 12: Legacy Partnership Backward Compatibility

**User Story:** System handles partnerships validated before feature release

**Pre-conditions:**
- Partnership validated via old code (before this feature)
- Database columns: `validated_nb_tickets=NULL, validated_nb_job_offers=NULL, validated_booth_size=NULL`

**Test Steps:**
```bash
# Query legacy partnership
GET /organisations/devlille/events/devlille-2026/partnership/{legacyPartnershipId}
```

**Expected Result:**
- HTTP 200 OK
- Response gracefully handles nulls:
  - `validatedNbTickets`: null (or fallback to pack value in UI)
  - `validatedNbJobOffers`: null
  - `validatedBoothSize`: null
- No application errors from null values

**Validation:**
- Application code handles nullable fields
- Optional: UI displays pack defaults when validated values are null

---

### Scenario 13: Validate with Empty Booth Size String

**User Story:** System rejects empty/whitespace-only booth size

**Test Steps:**
```bash
POST /organisations/devlille/events/devlille-2026/partnership/{partnershipId}/validate
Body:
{
  "nbJobOffers": 2,
  "boothSize": ""
}
```

**Expected Result:**
- HTTP 400 Bad Request
- Error message: `"boothSize must not be empty"`

**Validation:**
- JSON schema `minLength: 1` enforced
- Whitespace-only strings also rejected

---

### Scenario 14: Unauthorized User Validation Attempt

**User Story:** System restricts validation to authorized organizers

**Pre-conditions:**
- User authenticated but NOT organizer of event

**Test Steps:**
```bash
POST /organisations/devlille/events/devlille-2026/partnership/{partnershipId}/validate
Headers:
  Authorization: Bearer {non_organizer_token}
Body:
{
  "nbJobOffers": 2
}
```

**Expected Result:**
- HTTP 403 Forbidden
- Error response:
```json
{
  "error": "Forbidden",
  "message": "User must be an organizer of this event to validate partnerships",
  "status": 403
}
```

---

### Scenario 15: Validate Non-Existent Partnership

**User Story:** System returns 404 for invalid partnership ID

**Test Steps:**
```bash
POST /organisations/devlille/events/devlille-2026/partnership/00000000-0000-0000-0000-000000000000/validate
Body:
{
  "nbJobOffers": 2
}
```

**Expected Result:**
- HTTP 404 Not Found
- Error message: `"Partnership with id '00000000-0000-0000-0000-000000000000' not found"`

---

## Integration Test Execution

### Using Bruno CLI

```bash
# Run all validation scenarios
cd bruno
bruno run events/partnership/Validate\ Partnership.bru --env Local

# Run specific scenario with variables
bruno run events/partnership/Validate\ Partnership.bru \
  --env Local \
  --var partnershipId=550e8400-e29b-41d4-a716-446655440000 \
  --var nbTickets=10
```

### Using curl

```bash
# Scenario 1: Pack defaults
curl -X POST http://localhost:8080/organisations/devlille/events/devlille-2026/partnership/550e8400-e29b-41d4-a716-446655440000/validate \
  -H "Authorization: Bearer ${TOKEN}" \
  -H "Content-Type: application/json" \
  -d '{"nbJobOffers": 2}'

# Scenario 3: Custom booth size
curl -X POST http://localhost:8080/organisations/devlille/events/devlille-2026/partnership/550e8400-e29b-41d4-a716-446655440000/validate \
  -H "Authorization: Bearer ${TOKEN}" \
  -H "Content-Type: application/json" \
  -d '{"nbJobOffers": 2, "boothSize": "6x2m"}'

# Scenario 4: Invalid booth size (expect 400)
curl -X POST http://localhost:8080/organisations/devlille/events/devlille-2026/partnership/550e8400-e29b-41d4-a716-446655440000/validate \
  -H "Authorization: Bearer ${TOKEN}" \
  -H "Content-Type: application/json" \
  -d '{"nbJobOffers": 2, "boothSize": "10x10m"}' \
  -w "\nHTTP Status: %{http_code}\n"
```

### Automated Test Suite

```kotlin
// Kotlin integration test structure
class ValidatePartnershipIntegrationTest : ApplicationTest() {
    
    @Test
    fun `test scenario 1 - validate with pack defaults`() = testApp {
        // Arrange: Create test partnership
        val partnershipId = createTestPartnership()
        
        // Act: Validate without body
        val response = client.post("/organisations/devlille/events/devlille-2026/partnership/$partnershipId/validate") {
            bearerAuth(organizerToken)
            contentType(ContentType.Application.Json)
            setBody("""{"nbJobOffers": 2}""")
        }
        
        // Assert: Verify response
        response.status shouldBe HttpStatusCode.OK
        val body = response.body<ValidatePartnershipResponse>()
        body.validatedNbTickets shouldBe 10 // Pack default
        body.validatedBoothSize shouldBe "3x3m" // Pack default
    }
    
    @Test
    fun `test scenario 4 - reject invalid booth size`() = testApp {
        // Act: Validate with booth size not in any pack
        val response = client.post("/organisations/devlille/events/devlille-2026/partnership/$partnershipId/validate") {
            bearerAuth(organizerToken)
            contentType(ContentType.Application.Json)
            setBody("""{"nbJobOffers": 2, "boothSize": "10x10m"}""")
        }
        
        // Assert: Verify 400 error
        response.status shouldBe HttpStatusCode.BadRequest
        response.bodyAsText() shouldContain "not available in any sponsoring pack"
    }
    
    @Test
    fun `test scenario 7 - block re-validation after signature`() = testApp {
        // Arrange: Partnership with signed agreement
        val partnershipId = createValidatedPartnershipWithSignedAgreement()
        
        // Act: Attempt re-validation
        val response = client.post("/organisations/devlille/events/devlille-2026/partnership/$partnershipId/validate") {
            bearerAuth(organizerToken)
            contentType(ContentType.Application.Json)
            setBody("""{"nbJobOffers": 5}""")
        }
        
        // Assert: Verify 400 conflict
        response.status shouldBe HttpStatusCode.BadRequest
        response.bodyAsText() shouldContain "agreement already signed"
    }
}
```

## Database Verification Queries

```sql
-- Verify validated values stored correctly
SELECT 
    id,
    event_slug,
    validated_at,
    validated_nb_tickets,
    validated_nb_job_offers,
    validated_booth_size,
    agreement_signed_url
FROM partnerships
WHERE id = '550e8400-e29b-41d4-a716-446655440000';

-- Verify booth size cross-pack validation
SELECT DISTINCT booth_size 
FROM sponsoring_packs 
WHERE event_slug = 'devlille-2026';
-- Expected: "3x3m", "6x2m" (only these values allowed in validatedBoothSize)

-- Check legacy partnerships (nullable columns)
SELECT id, validated_nb_tickets, validated_nb_job_offers, validated_booth_size
FROM partnerships
WHERE validated_at IS NOT NULL 
  AND validated_nb_tickets IS NULL;
-- Expected: Partnerships validated before feature release
```

## Performance Benchmarks

**Expected Response Times:**
- Validation with pack defaults: < 100ms
- Validation with booth size cross-pack query: < 150ms
- Concurrent validation (10 simultaneous requests): < 200ms per request

**Load Test:**
```bash
# Using Apache Bench
ab -n 100 -c 10 -T 'application/json' \
   -H "Authorization: Bearer ${TOKEN}" \
   -p validate-body.json \
   http://localhost:8080/organisations/devlille/events/devlille-2026/partnership/550e8400-e29b-41d4-a716-446655440000/validate
```

## Troubleshooting

**Issue: 401 Unauthorized**
- Verify Google OAuth token is valid: `curl -H "Authorization: Bearer ${TOKEN}" http://localhost:8080/users/me`
- Check token expiry and refresh if needed

**Issue: 403 Forbidden**
- Verify user is organizer: `GET /organisations/devlille/events/devlille-2026`
- Check `AuthorizedOrganisationPlugin` configuration

**Issue: 404 Partnership Not Found**
- Verify partnership exists: `GET /organisations/devlille/events/devlille-2026/partnership/{partnershipId}`
- Check event slug and partnership ID are correct

**Issue: 400 Booth Size Validation Fails**
- Query event packs: `SELECT booth_size FROM sponsoring_packs WHERE event_slug = 'devlille-2026'`
- Ensure booth size matches exactly (case-sensitive)

**Issue: Database NULL Constraint Violation**
- Check migration applied: `validated_nb_tickets`, `validated_nb_job_offers`, `validated_booth_size` columns should be NULLABLE
- Verify test database schema matches production

---

## Next Steps

After completing quickstart scenarios:

1. **Phase 2 Implementation:**
   - Create database migration for new columns
   - Implement `ValidatePartnershipRequest` domain model
   - Update `PartnershipRepositoryExposed.validate()` logic
   - Add booth size cross-pack validation
   - Write Kotlin integration tests for all scenarios

2. **Frontend Updates:**
   - Add validation form with ticket/job offer/booth inputs
   - Display validated values in partnership details
   - Handle validation errors (booth size, re-validation blocked)
   - Show pack defaults as placeholder values

3. **Documentation:**
   - Update API documentation (OpenAPI spec)
   - Document booth size format conventions
   - Add deployment migration notes
   - Update user guide for organizers

---

**Document Version:** 1.0  
**Last Updated:** 2025-01-15  
**Related Specs:** `spec.md`, `data-model.md`, `contracts/openapi-endpoint.yaml`
