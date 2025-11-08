# Quickstart: Public Partnership Information Endpoint

## Overview
This quickstart validates the implementation of the public GET endpoint that retrieves comprehensive partnership details including company, event, and process status information.

## Prerequisites
- Server running on `http://localhost:8080`
- Test partnership data available in database
- Partnership must be associated with a valid event

## Setup Test Data

### Required Test Entities
Create the following test entities through existing API endpoints or database seeding:

**Test Organization**:
```json
{
  "name": "DevLille Test Org",
  "slug": "devlille-test",
  "headOffice": "Lille, France"
}
```

**Test Event** (EventWithOrganisation structure):
```json
{
  "event": {
    "name": "DevLille 2025 Conference", 
    "start_time": "2025-06-15T09:00:00Z",
    "end_time": "2025-06-15T18:00:00Z",
    "submission_start_time": "2025-02-01T00:00:00Z",
    "submission_end_time": "2025-05-01T23:59:59Z",
    "address": "EuraTechnologies, Lille",
    "contact": {
      "email": "contact@devlille.fr",
      "phone": "+33123000000"
    }
  },
  "organisation": {
    "name": "DevLille Test Org",
    "slug": "devlille-test",
    "head_office": "Lille, France"
  }
}

**Test Company**:
```json
{
  "name": "TechCorp SARL",
  "siret": "12345678901234", 
  "vat": "FR12345678901",
  "site_url": "https://techcorp.example.com",
  "head_office": {
    "address": "123 Tech Street",
    "city": "Lille", 
    "zip_code": "59000",
    "country": "France"
  },
  "description": null,
  "socials": []
}

**Test Partnership** (with process status):
```json
{
  "contactName": "Jean Dupont",
  "contactRole": "CTO",
  "phone": "+33123456789",
  "language": "fr",
  "suggestionSentAt": "2025-11-01T10:00:00Z",
  "suggestionApprovedAt": "2025-11-02T14:30:00Z", 
  "validatedAt": "2025-11-03T09:15:00Z"
}
```

## Test Scenarios

### Scenario 1: Successful Partnership Retrieval

**Request**:
```bash
curl -X GET "http://localhost:8080/events/devlille-2025/partnerships/{partnership-uuid}" \
  -H "Accept: application/json"
```

**Expected Response** (200 OK):
```json
{
  "partnership": {
    "id": "partnership-uuid",
    "phone": "+33123456789",
    "contactName": "Jean Dupont",
    "contactRole": "CTO", 
    "language": "fr",
    "emails": [],
    "selectedPack": null,
    "suggestionPack": null,
    "validatedPack": null,
    "processStatus": {
      "suggestionSentAt": "2025-11-01T10:00:00Z",
      "suggestionApprovedAt": "2025-11-02T14:30:00Z",
      "suggestionDeclinedAt": null,
      "validatedAt": "2025-11-03T09:15:00Z",
      "declinedAt": null,
      "agreementUrl": null,
      "agreementSignedUrl": null, 
      "communicationPublicationDate": null,
      "communicationSupportUrl": null,
      "billingStatus": null,
      "currentStage": "VALIDATED"
    },
    "createdAt": "2025-11-01T09:30:00Z",
    "updatedAt": "2025-11-03T09:15:00Z"
  },
  "company": {
    "id": "company-uuid",
    "name": "TechCorp SARL", 
    "siret": "12345678901234",
    "vat": "FR12345678901",
    "site_url": "https://techcorp.example.com",
    "description": null,
    "head_office": {
      "address": "123 Tech Street",
      "city": "Lille",
      "zip_code": "59000", 
      "country": "France"
    },
    "medias": null,
    "status": "active"
  },
  "event": {
    "event": {
      "slug": "devlille-2025", 
      "name": "DevLille 2025 Conference",
      "start_time": "2025-06-15T09:00:00Z",
      "end_time": "2025-06-15T18:00:00Z",
      "submission_start_time": "2025-02-01T00:00:00Z",
      "submission_end_time": "2025-05-01T23:59:59Z",
      "address": "EuraTechnologies, Lille",
      "contact": {
        "email": "contact@devlille.fr",
        "phone": "+33123000000"
      },
      "external_links": [],
      "providers": []
    },
    "organisation": {
      "name": "DevLille Test Org",
      "slug": "devlille-test",
      "head_office": "Lille, France",
      "owner": null
    }
  }
}
```

**Validation Steps**:
1. ✅ Response status is 200 OK
2. ✅ Response has nested structure (partnership, company, event)
3. ✅ All required fields are present and non-null
4. ✅ Partnership process status shows correct stage progression
5. ✅ Timestamps are in ISO 8601 format
6. ✅ Company and event data is complete

### Scenario 2: Invalid Partnership UUID

**Request**:
```bash
curl -X GET "http://localhost:8080/events/devlille-2025/partnerships/invalid-uuid" \
  -H "Accept: application/json"
```

**Expected Response** (400 Bad Request):
```json
{
  "message": "Bad Request", 
  "stack": null
}
```

**Validation Steps**:
1. ✅ Response status is 400 Bad Request
2. ✅ Error response follows standard format
3. ✅ Invalid UUID properly rejected

### Scenario 3: Non-existent Partnership

**Request**:
```bash
curl -X GET "http://localhost:8080/events/devlille-2025/partnerships/00000000-0000-0000-0000-000000000000" \
  -H "Accept: application/json"
```

**Expected Response** (404 Not Found):
```json
{
  "message": "404 Not Found",
  "stack": null
}
```

**Validation Steps**:
1. ✅ Response status is 404 Not Found
2. ✅ Non-existent entity properly handled
3. ✅ Error response consistent with other 404s

### Scenario 4: Invalid Event Slug

**Request**:
```bash
curl -X GET "http://localhost:8080/events/non-existent-event/partnerships/{partnership-uuid}" \
  -H "Accept: application/json"
```

**Expected Response** (404 Not Found):
```json
{
  "message": "404 Not Found",
  "stack": null
}
```

**Validation Steps**:
1. ✅ Response status is 404 Not Found  
2. ✅ Invalid event slug properly rejected
3. ✅ Error handling consistent

### Scenario 5: Partnership-Event Mismatch

**Request**:
```bash
# Use partnership from different event
curl -X GET "http://localhost:8080/events/different-event/partnerships/{partnership-uuid}" \
  -H "Accept: application/json"
```

**Expected Response** (404 Not Found):
```json
{
  "message": "404 Not Found",
  "stack": null
}
```

**Validation Steps**:
1. ✅ Response status is 404 Not Found
2. ✅ Partnership-event association properly validated
3. ✅ Security through association checking

## Data Integrity Validation

### Partnership Process Status Logic
Test partnerships in different workflow stages:

**CREATED** (no suggestion sent):
- `suggestionSentAt`: null
- `currentStage`: "CREATED"

**SUGGESTION_SENT**:
- `suggestionSentAt`: timestamp
- `suggestionApprovedAt`: null
- `currentStage`: "SUGGESTION_SENT"

**SUGGESTION_APPROVED**:
- `suggestionApprovedAt`: timestamp  
- `validatedAt`: null
- `currentStage`: "SUGGESTION_APPROVED"

**VALIDATED**:
- `validatedAt`: timestamp
- `currentStage`: "VALIDATED"

**PAID** (billing integration):
- `billingStatus`: "PAID" (from BillingsTable.status)
- `currentStage`: "PAID"

### Billing Status Integration
Test partnerships with different billing states:

**No Billing Record**:
- `billingStatus`: null
- Partnership not yet in billing system

**PENDING Invoice**:
- `billingStatus`: "PENDING"
- Invoice created but not sent

**SENT Invoice**:
- `billingStatus`: "SENT"  
- Invoice sent but payment not received

**PAID Invoice**:
- `billingStatus`: "PAID"
- Payment received and processed

### Validated Pack Logic
Test the `validatedPack` field business logic:

**No Packs**:
- `selectedPack`: null, `suggestionPack`: null
- `validatedPack`: null

**Only Selected Pack**:
- `selectedPack`: present, `suggestionPack`: null
- `validatedPack`: same as selectedPack (uses PartnershipEntity.validatedPack extension)

**Only Suggestion Pack**:
- `selectedPack`: null, `suggestionPack`: present
- `validatedPack`: same as suggestionPack

**Both Packs Present**:
- `selectedPack`: present, `suggestionPack`: present
- `validatedPack`: determined by existing business logic in PartnershipEntity.validatedPack

### Data Completeness Checks
1. ✅ All required partnership fields present
2. ✅ Company business information complete (SIRET, VAT) 
3. ✅ Event dates and contact information included
4. ✅ Organization details nested properly
5. ✅ Nullable fields handle null values correctly
6. ✅ `validatedPack` uses existing business logic (PartnershipEntity.validatedPack)
7. ✅ `billingStatus` properly integrated from BillingsTable.status

## Security Validation

### Public Access Verification
1. ✅ No authentication required (completely public)
2. ✅ No authorization headers needed
3. ✅ Works from any IP address
4. ✅ No rate limiting applied

### Data Exposure Validation  
1. ✅ All partnership contact details exposed (as specified)
2. ✅ Company business details accessible
3. ✅ Event contact information available
4. ✅ Process status timestamps visible

## Troubleshooting

### Common Issues

**"404 Not Found" for valid partnership**:
- Verify partnership exists in database
- Check partnership belongs to specified event
- Confirm event slug is correct

**"400 Bad Request" on valid UUID**:
- Verify UUID format (8-4-4-4-12 hex digits)
- Check for extra characters or spaces
- Confirm `.toUUID()` extension works

**Missing nested data in response**:
- Verify entity relationships are properly loaded
- Check mapper implementations
- Confirm all entity fields are accessible

**Slow response times**:
- Check database indexes on partnership ID
- Verify foreign key relationships optimized
- Monitor for N+1 query patterns

### Debugging Commands

**Check partnership exists**:
```sql
SELECT id, event_id FROM partnerships WHERE id = 'partnership-uuid';
```

**Verify event association**:
```sql
SELECT p.id, e.slug 
FROM partnerships p 
JOIN events e ON p.event_id = e.id 
WHERE p.id = 'partnership-uuid';
```

**Check billing status**:
```sql
SELECT p.id, b.status as billing_status
FROM partnerships p
LEFT JOIN billings b ON p.id = b.partnership_id
WHERE p.id = 'partnership-uuid';
```

**Test validatedPack logic**:
```sql
SELECT p.id, p.selected_pack_id, p.suggestion_pack_id
FROM partnerships p
WHERE p.id = 'partnership-uuid';
```

**Monitor query performance**:
```sql
EXPLAIN ANALYZE SELECT * FROM partnerships p
JOIN companies c ON p.company_id = c.id  
JOIN events e ON p.event_id = e.id
LEFT JOIN billings b ON p.id = b.partnership_id
WHERE p.id = 'partnership-uuid' AND e.slug = 'event-slug';
```

## Success Criteria

### Functional Requirements Met
- ✅ Public GET endpoint implemented
- ✅ Nested JSON response structure
- ✅ Complete company and event details
- ✅ Detailed process status with timestamps  
- ✅ Proper error handling for edge cases
- ✅ Partnership-event association validation

### Non-Functional Requirements Met
- ✅ Response times under 2 seconds
- ✅ No authentication required
- ✅ Unlimited access allowed  
- ✅ Consistent error response format
- ✅ OpenAPI documentation updated

### Technical Requirements Met
- ✅ ktlint and detekt compliance
- ✅ Contract tests passing
- ✅ JSON schema validation working
- ✅ Repository layer separation maintained
- ✅ Mapper pattern implemented

This quickstart serves as both validation tool and integration test suite for the public partnership information endpoint implementation.