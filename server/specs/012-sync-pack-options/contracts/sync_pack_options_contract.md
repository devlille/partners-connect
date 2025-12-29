# API Contract: Synchronize Pack Options

**Endpoint**: `POST /orgs/{orgSlug}/events/{eventSlug}/packs/{packId}/options`  
**Feature**: 012-sync-pack-options  
**Date**: November 24, 2025

## Contract Overview

This document defines the API contract for the pack options synchronization endpoint. The endpoint accepts a complete configuration of required and optional options and ensures the pack's final state exactly matches the submitted configuration.

---

## Request Specification

### HTTP Method & Path

```
POST /orgs/{orgSlug}/events/{eventSlug}/packs/{packId}/options
```

### Path Parameters

| Parameter | Type | Required | Description |
|-----------|------|----------|-------------|
| `orgSlug` | string | Yes | Organization identifier |
| `eventSlug` | string | Yes | Event identifier |
| `packId` | UUID | Yes | Sponsoring pack identifier |

### Headers

| Header | Required | Value |
|--------|----------|-------|
| `Authorization` | Yes | `Bearer {jwt_token}` |
| `Content-Type` | Yes | `application/json` |

### Request Body

**Schema**: `attach_options_to_pack.schema.json` (existing, no changes)

```json
{
  "required": ["array", "of", "option", "uuid", "strings"],
  "optional": ["array", "of", "option", "uuid", "strings"]
}
```

**Field Validation**:
- `required`: Array of UUID strings (can be empty)
- `optional`: Array of UUID strings (can be empty)
- Both lists can be empty simultaneously (removes all options)
- Option UUIDs must be valid UUID format
- Same UUID cannot appear in both lists

**Example Request**:
```json
{
  "required": [
    "550e8400-e29b-41d4-a716-446655440000",
    "650e8400-e29b-41d4-a716-446655440001"
  ],
  "optional": [
    "750e8400-e29b-41d4-a716-446655440002"
  ]
}
```

---

## Response Specifications

### Success Response

**HTTP Status**: `201 Created`

**Body**: Empty object or minimal success indicator

```json
{}
```

**Behavior**: 
- Pack now contains exactly the options from the request
- Options not in request have been removed
- Options in both old and new configuration may have updated requirement status
- Operation is idempotent - submitting same configuration multiple times produces same result

---

### Error Responses

#### 400 Bad Request - Invalid JSON Schema

**Trigger**: Request body doesn't match schema validation

**Response**:
```json
{
  "error": "Validation error",
  "message": "Request body validation failed: [specific validation error]"
}
```

**Examples**:
- Invalid UUID format in option IDs
- Missing required fields
- Invalid JSON structure

---

#### 401 Unauthorized

**Trigger**: Missing or invalid JWT token

**Response**:
```json
{
  "error": "Unauthorized",
  "message": "Authentication required"
}
```

**Handled By**: `AuthorizedOrganisationPlugin` before route handler

---

#### 403 Forbidden - Options Don't Belong to Event

**Trigger**: One or more option IDs don't belong to the specified event (FR-005)

**Response**:
```json
{
  "error": "Forbidden",
  "message": "Some options do not belong to the event"
}
```

**Test Scenario**:
```
Given: Pack belongs to event "devlille-2025"
And: Option A belongs to event "devlille-2025"
And: Option B belongs to event "devlille-2026" (different event)
When: Request includes both Option A and Option B
Then: Return 403 Forbidden
```

---

#### 404 Not Found - Pack Not Found

**Trigger**: Pack ID doesn't exist or doesn't belong to specified event (FR-008)

**Response**:
```json
{
  "error": "Not Found",
  "message": "Pack not found"
}
```

**Test Scenarios**:
```
Scenario 1: Non-existent pack ID
Given: Pack ID "550e8400-e29b-41d4-a716-446655440000" doesn't exist
When: POST to /orgs/{org}/events/{event}/packs/{packId}/options
Then: Return 404 Not Found

Scenario 2: Pack belongs to different event
Given: Pack exists but belongs to event "other-event"
When: POST to /orgs/{org}/events/devlille-2025/packs/{packId}/options
Then: Return 404 Not Found
```

---

#### 404 Not Found - Option Not Found

**Trigger**: One or more option IDs don't exist in the system (FR-007)

**Response**:
```json
{
  "error": "Not Found",
  "message": "Option not found: {option-ids}"
}
```

**Test Scenario**:
```
Given: Option "550e8400-e29b-41d4-a716-446655440000" doesn't exist
When: Request includes non-existent option ID
Then: Return 404 Not Found with option ID in message
```

---

#### 409 Conflict - Duplicate in Required and Optional

**Trigger**: Same option ID appears in both required and optional lists (FR-006)

**Response**:
```json
{
  "error": "Conflict",
  "message": "options {option-ids} cannot be both required and optional"
}
```

**Test Scenario**:
```
Given: Request body:
{
  "required": ["550e8400-e29b-41d4-a716-446655440000"],
  "optional": ["550e8400-e29b-41d4-a716-446655440000"]
}
When: POST request submitted
Then: Return 409 Conflict
And: Message lists the duplicate option ID
```

---

#### 500 Internal Server Error - Database Transaction Failure

**Trigger**: Database error during synchronization operation

**Response**:
```json
{
  "error": "Internal Server Error",
  "message": "An error occurred processing your request"
}
```

**Behavior**: 
- Automatic transaction rollback
- Pack state unchanged
- No partial updates applied

---

## Contract Test Scenarios

### Test Suite Structure

**File**: `server/application/src/test/kotlin/fr/devlille/partners/connect/sponsoring/SponsoringPackRoutesTest.kt`

**Test Organization**:
1. Schema validation tests (contract tests)
2. Business logic tests (integration tests)

### Contract Test Cases (TDD - Write These First)

#### CT-1: Successful Synchronization - Replace All Options

```kotlin
@Test
fun `POST pack options synchronizes by removing old and adding new options`() = testApplication {
    // Given: Pack with options [A(req), B(opt)]
    // When: Sync to [C(req), D(opt)]
    // Then: Pack contains only [C(req), D(opt)]
    // Status: 201 Created
}
```

#### CT-2: Successful Synchronization - Partial Overlap

```kotlin
@Test
fun `POST pack options keeps overlapping options and removes non-overlapping`() = testApplication {
    // Given: Pack with options [A(req), B(opt), C(opt)]
    // When: Sync to [B(opt), D(req)]
    // Then: Pack contains [B(opt), D(req)]
    // Status: 201 Created
}
```

#### CT-3: Successful Synchronization - Change Requirement Status

```kotlin
@Test
fun `POST pack options updates requirement status for existing options`() = testApplication {
    // Given: Pack with option A as required
    // When: Sync with A in optional list
    // Then: Option A is now optional
    // Status: 201 Created
}
```

#### CT-4: Successful Synchronization - Empty Configuration

```kotlin
@Test
fun `POST pack options removes all options when empty lists submitted`() = testApplication {
    // Given: Pack with options [A(req), B(opt)]
    // When: Sync to { required: [], optional: [] }
    // Then: Pack has no options
    // Status: 201 Created
}
```

#### CT-5: Idempotency - No Changes

```kotlin
@Test
fun `POST pack options is idempotent when configuration unchanged`() = testApplication {
    // Given: Pack with options [A(req), B(opt)]
    // When: Sync to [A(req), B(opt)]
    // Then: Pack still has [A(req), B(opt)]
    // Status: 201 Created
}
```

#### CT-6: Validation Error - Duplicate in Both Lists

```kotlin
@Test
fun `POST pack options returns 409 when option in both required and optional`() = testApplication {
    // Given: Valid pack
    // When: Sync with option A in both required and optional
    // Then: Status 409 Conflict
    // And: Error message identifies duplicate option ID
}
```

#### CT-7: Validation Error - Options From Different Event

```kotlin
@Test
fun `POST pack options returns 403 when options belong to different event`() = testApplication {
    // Given: Pack belongs to event "devlille-2025"
    // And: Option A belongs to event "devlille-2026"
    // When: Sync including option A
    // Then: Status 403 Forbidden
}
```

#### CT-8: Validation Error - Pack Not Found

```kotlin
@Test
fun `POST pack options returns 404 when pack does not exist`() = testApplication {
    // Given: Random UUID for pack
    // When: POST to sync options
    // Then: Status 404 Not Found
}
```

#### CT-9: Validation Error - Option Not Found

```kotlin
@Test
fun `POST pack options returns 404 when option does not exist`() = testApplication {
    // Given: Valid pack
    // And: Random UUID for option (doesn't exist)
    // When: Sync including non-existent option
    // Then: Status 404 Not Found
}
```

### Integration Test Cases (Write After Contract Tests Pass)

#### IT-1: Database State Verification

```kotlin
@Test
fun `POST pack options updates database to match submitted configuration exactly`() = testApplication {
    // Given: Pack with 5 options in database
    // When: Sync to 3 different options
    // Then: Verify database contains exactly 3 pack-option records
    // And: Verify old options are deleted
    // And: Verify new options are inserted
}
```

#### IT-2: Transaction Rollback on Error

```kotlin
@Test
fun `POST pack options rolls back all changes on validation failure`() = testApplication {
    // Given: Pack with options [A, B]
    // When: Sync request includes non-existent option C
    // Then: Status 404
    // And: Pack still has options [A, B] (unchanged)
}
```

#### IT-3: Concurrent Modification Handling

```kotlin
@Test
fun `POST pack options applies last-write-wins for concurrent requests`() = testApplication {
    // Given: Pack with options [A]
    // When: Two concurrent sync requests (to [B] and [C])
    // Then: Last request wins
    // And: Pack has either [B] or [C] (whichever completed last)
}
```

---

## Mock Factory Usage

**Existing Factories** (from test utilities):
- `insertMockedOrganisationEntity(orgId: UUID)`
- `insertMockedEventWithAdminUser(eventId: UUID, orgId: UUID, eventSlug: String)`
- `insertMockedSponsoringPack(packId: UUID, eventId: UUID)`
- `insertMockedSponsoringOption(optionId: UUID, eventId: UUID, name: String = "Option")`

**Usage Pattern**:
```kotlin
@Test
fun `test scenario`() = testApplication {
    val orgId = UUID.randomUUID()
    val eventId = UUID.randomUUID()
    val packId = UUID.randomUUID()
    val optionA = UUID.randomUUID()
    val optionB = UUID.randomUUID()
    
    application {
        moduleMocked()
        insertMockedOrganisationEntity(orgId)
        insertMockedEventWithAdminUser(eventId, orgId, "devlille-2025")
        insertMockedSponsoringPack(packId, eventId)
        insertMockedSponsoringOption(optionA, eventId, "Logo")
        insertMockedSponsoringOption(optionB, eventId, "Booth")
    }
    
    // ... test execution
}
```

---

## Breaking Changes Assessment

**Is this a breaking change?** NO

**Rationale**:
- Request schema unchanged (same DTO, same JSON schema)
- Response schema unchanged (still returns 201 Created)
- HTTP method and path unchanged
- Authorization requirements unchanged
- Error response formats unchanged (same status codes, same error structure)

**Behavior Change**: Yes - endpoint now synchronizes instead of only adding
- **Impact**: Clients submitting duplicate options will no longer receive 409 errors (idempotent behavior)
- **Migration**: No client changes required - API contract is compatible

**Backward Compatibility**:
- Clients calling endpoint to add new options: ✅ Works (adds new options)
- Clients calling endpoint multiple times: ✅ Works (idempotent instead of error)
- Clients expecting 409 on duplicate: ⚠️ Behavior change (now returns 201 instead)

---

## OpenAPI Documentation Updates

**File**: `server/application/src/main/resources/openapi/openapi.yaml`

**Changes Required**:

```yaml
/orgs/{orgSlug}/events/{eventSlug}/packs/{packId}/options:
  post:
    summary: "Synchronize sponsoring pack options"  # CHANGED from "Create sponsoring option"
    operationId: "postOrgsEventsPacksOptions"
    security:
      - bearerAuth: []
    description: |  # UPDATED
      Synchronizes the complete set of options for a sponsoring pack. 
      
      This endpoint accepts a complete configuration of required and optional options
      and ensures the pack's final state exactly matches the submitted configuration by:
      - Removing options not included in the request
      - Adding new options from the request
      - Updating requirement status (required ↔ optional) for existing options
      
      The operation is atomic - all changes succeed or all fail. The operation is
      idempotent - submitting the same configuration multiple times produces the same result.
      
      Both required and optional lists can be empty to remove all options from the pack.
    parameters:
      - name: "eventSlug"
        in: "path"
        required: true
        schema:
          type: "string"
      - name: "packId"
        in: "path"
        required: true
        schema:
          type: "string"
      - name: "orgSlug"
        in: "path"
        required: true
        schema:
          type: "string"
    requestBody:
      content:
        application/json:
          schema:
            $ref: "#/components/schemas/AttachOptionsToPack"
      required: true
    responses:
      "201":
        description: "Options synchronized successfully"
        content:
          '*/*':
            schema:
              type: "object"
      "400":
        description: "Bad Request - Invalid request body"
        content:
          application/json:
            schema:
              $ref: "#/components/schemas/ErrorResponse"
      "401":
        description: "Unauthorized - Invalid or missing authentication"
        content:
          application/json:
            schema:
              $ref: "#/components/schemas/ErrorResponse"
      "403":
        description: "Forbidden - Options do not belong to event"
        content:
          application/json:
            schema:
              $ref: "#/components/schemas/ErrorResponse"
      "404":
        description: "Not Found - Pack or options not found"
        content:
          application/json:
            schema:
              $ref: "#/components/schemas/ErrorResponse"
      "409":
        description: "Conflict - Duplicate option in required and optional lists"
        content:
          application/json:
            schema:
              $ref: "#/components/schemas/ErrorResponse"
      "500":
        description: "Internal Server Error - Database transaction failure"
        content:
          application/json:
            schema:
              $ref: "#/components/schemas/ErrorResponse"
```

**Note**: Error response schemas already exist in `components/schemas/ErrorResponse`, no changes needed there.

---

## Summary

This API contract maintains backward compatibility while changing behavior from "add-only" to "synchronize". The contract tests ensure the endpoint correctly handles all success and error scenarios defined in the specification.
