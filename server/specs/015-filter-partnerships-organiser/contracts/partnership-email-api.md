# API Contract: Email Partnerships with Organiser Filter

**Endpoint**: `POST /orgs/{orgSlug}/events/{eventSlug}/partnerships/email`  
**Feature**: Filter Partnerships by Assigned Organiser (Email Endpoint)  
**Date**: December 29, 2025

---

## Overview

This contract extends the existing email partnerships endpoint to support the `filter[organiser]` query parameter, enabling organisers to send targeted emails to partnerships assigned to specific team members.

---

## Request Specification

### HTTP Method
`POST`

### URL Pattern
`/orgs/{orgSlug}/events/{eventSlug}/partnerships/email`

### Path Parameters

| Parameter | Type | Required | Description |
|-----------|------|----------|-------------|
| `orgSlug` | string | Yes | Organisation identifier |
| `eventSlug` | string | Yes | Event identifier |

### Query Parameters (Filters)

| Parameter | Type | Required | Description | Example |
|-----------|------|----------|-------------|---------|
| `filter[pack_id]` | string (UUID) | No | Filter by sponsoring pack ID | `123e4567-e89b-12d3-a456-426614174000` |
| `filter[validated]` | boolean | No | Filter by validation status | `true`, `false` |
| `filter[suggestion]` | boolean | No | Filter by suggestion status | `true`, `false` |
| `filter[paid]` | boolean | No | Filter by payment status | `true`, `false` |
| `filter[agreement-generated]` | boolean | No | Filter by agreement generation status | `true`, `false` |
| `filter[agreement-signed]` | boolean | No | Filter by agreement signature status | `true`, `false` |
| **`filter[organiser]`** | string | No | **NEW**: Filter by assigned organiser email (case-insensitive) | `john.doe@example.com` |

### Headers

```http
Authorization: Bearer <jwt_token>
Content-Type: application/json
Accept: application/json
```

### Request Body

```json
{
  "subject": "string",
  "body": "string"
}
```

**Fields**:
- `subject` (required): Email subject line
- `body` (required): HTML email body content

### Request Example

```http
POST /orgs/devlille/events/2025/partnerships/email?filter[organiser]=john.doe@example.com&filter[validated]=true HTTP/1.1
Host: api.partners-connect.com
Authorization: Bearer eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9...
Content-Type: application/json

{
  "subject": "Partnership Update for DevLille 2025",
  "body": "<html><body><p>Dear Partner,</p><p>Thank you for your sponsorship...</p></body></html>"
}
```

---

## Response Specification

### Success Response (204 No Content)

**Scenario**: Emails sent successfully to at least one partnership

**Response Body**: Empty

**Headers**:
```http
HTTP/1.1 204 No Content
Content-Length: 0
```

### Success Response (404 Not Found)

**Scenario**: No partnerships match the provided filters (per FR-016 clarification)

**Response Body**:
```json
{
  "error": "Not Found",
  "message": "No partnerships match the provided filters"
}
```

**Note**: Changed from HTTP 204 to HTTP 404 to provide explicit feedback when no recipients exist. This helps frontend differentiate between successful send and no recipients scenario.

**Alternative Considered**: HTTP 204 No Content (silent success)  
**Decision**: HTTP 404 with message for better UX and debugging

---

## Response Examples

### Example 1: Successful Email Send

**Request**:
```http
POST /orgs/devlille/events/2025/partnerships/email?filter[organiser]=jane.smith@example.com
Content-Type: application/json

{
  "subject": "Urgent: Event Schedule Change",
  "body": "<p>Dear Partner, we need to inform you...</p>"
}
```

**Response**:
```http
HTTP/1.1 204 No Content
```

**Recipients**: All partnerships assigned to jane.smith@example.com
- Partnership contacts: company emails from `company_emails` table
- Organiser contact: jane.smith@example.com included in CC (existing behavior)

---

### Example 2: No Recipients Match Filter

**Request**:
```http
POST /orgs/devlille/events/2025/partnerships/email?filter[organiser]=unassigned@example.com
Content-Type: application/json

{
  "subject": "Partnership Update",
  "body": "<p>Hello...</p>"
}
```

**Response** (404 Not Found):
```http
HTTP/1.1 404 Not Found
Content-Type: application/json

{
  "error": "Not Found",
  "message": "No partnerships match the provided filters"
}
```

---

### Example 3: Combined Filters

**Request**:
```http
POST /orgs/devlille/events/2025/partnerships/email?filter[organiser]=john.doe@example.com&filter[validated]=true&filter[paid]=true
Content-Type: application/json

{
  "subject": "Invoice Reminder",
  "body": "<p>Dear Partner, your payment is due...</p>"
}
```

**Response**:
```http
HTTP/1.1 204 No Content
```

**Recipients**: Only partnerships where:
- `organiser.email = "john.doe@example.com"` (case-insensitive)
- `validated_at IS NOT NULL`
- `billing.status = 'PAID'`

---

## Error Responses

### 400 Bad Request

**Scenario**: Missing required body fields or invalid format

```json
{
  "error": "Bad Request",
  "message": "Missing required field: subject"
}
```

### 401 Unauthorized

**Scenario**: Missing or invalid authentication token

```json
{
  "error": "Unauthorized",
  "message": "Authentication token missing or invalid"
}
```

### 403 Forbidden

**Scenario**: User lacks edit permissions for organisation

```json
{
  "error": "Forbidden",
  "message": "User does not have edit permission for this organisation"
}
```

### 404 Not Found (Event/Org)

**Scenario**: Organisation or event does not exist

```json
{
  "error": "Not Found",
  "message": "Event with slug '2025' not found"
}
```

### 404 Not Found (No Recipients)

**Scenario**: No partnerships match filter criteria

```json
{
  "error": "Not Found",
  "message": "No partnerships match the provided filters"
}
```

---

## Business Rules

### Filter Application

- Same filter logic as partnership list endpoint (AND logic)
- Organiser filter excludes partnerships with `null` organiser
- Case-insensitive email matching

### Email Recipients

**Per Partnership**:
1. **Company Contacts**: All emails from `company_emails` table for that partnership
2. **Organiser Contact**: Organiser's email (if assigned) included in CC (existing behavior)

**Example Email Distribution**:
```
Partnership ID: 123
  To: contact1@acme.com, contact2@acme.com (company emails)
  CC: john.doe@example.com (organiser)
  
Partnership ID: 456
  To: ceo@techcorp.com (company email)
  CC: john.doe@example.com (organiser)
```

### Filter Behavior Consistency

- Identical to partnership list endpoint filter logic
- Ensures what users see in list matches what receives email

---

## OpenAPI 3.1.0 Specification

```yaml
/orgs/{orgSlug}/events/{eventSlug}/partnerships/email:
  post:
    summary: Send email to partnership contacts
    operationId: emailPartnershipContacts
    description: Send email to contacts of partnerships matching filter criteria
    parameters:
      - name: orgSlug
        in: path
        required: true
        schema:
          type: string
        description: Organisation identifier
      
      - name: eventSlug
        in: path
        required: true
        schema:
          type: string
        description: Event identifier
      
      - name: filter[pack_id]
        in: query
        required: false
        schema:
          type: string
          format: uuid
        description: Filter by sponsoring pack ID
      
      - name: filter[validated]
        in: query
        required: false
        schema:
          type: boolean
        description: Filter by validation status
      
      - name: filter[suggestion]
        in: query
        required: false
        schema:
          type: boolean
        description: Filter by suggestion status
      
      - name: filter[paid]
        in: query
        required: false
        schema:
          type: boolean
        description: Filter by payment status
      
      - name: filter[agreement-generated]
        in: query
        required: false
        schema:
          type: boolean
        description: Filter by agreement generation status
      
      - name: filter[agreement-signed]
        in: query
        required: false
        schema:
          type: boolean
        description: Filter by agreement signature status
      
      - name: filter[organiser]
        in: query
        required: false
        schema:
          type: string
        description: Filter by assigned organiser email (case-insensitive)
        example: john.doe@example.com
    
    requestBody:
      required: true
      content:
        application/json:
          schema:
            $ref: '#/components/schemas/email_partnership_request.schema'
    
    security:
      - bearerAuth: []
    
    responses:
      '204':
        description: No Content - Emails sent successfully
      
      '400':
        description: Bad Request - Invalid request body
        content:
          application/json:
            schema:
              $ref: '#/components/schemas/error_response.schema'
      
      '401':
        description: Unauthorized - Missing or invalid authentication
        content:
          application/json:
            schema:
              $ref: '#/components/schemas/error_response.schema'
      
      '403':
        description: Forbidden - Insufficient permissions
        content:
          application/json:
            schema:
              $ref: '#/components/schemas/error_response.schema'
      
      '404':
        description: Not Found - Organisation/event not found or no matching partnerships
        content:
          application/json:
            schema:
              $ref: '#/components/schemas/error_response.schema'
```

---

## Testing Scenarios

### Scenario 1: Send Email to Specific Organiser's Partnerships

**Test**: Verify emails sent only to partnerships assigned to specified organiser

```gherkin
Given partnerships:
  | id | organiser | company_emails |
  | p1 | john@example.com | [contact1@acme.com] |
  | p2 | jane@example.com | [ceo@techcorp.com] |
  | p3 | john@example.com | [info@startup.com] |
When I POST email with filter[organiser]=john@example.com
Then response status is 204
And emails sent to 2 partnerships (p1, p3)
And contact1@acme.com receives email
And info@startup.com receives email
And ceo@techcorp.com does NOT receive email
```

### Scenario 2: No Partnerships Match Filter

**Test**: Verify 404 when no recipients exist

```gherkin
Given no partnerships assigned to "ghost@example.com"
When I POST email with filter[organiser]=ghost@example.com
Then response status is 404
And response message is "No partnerships match the provided filters"
And no emails are sent
```

### Scenario 3: Combined Filters with Organiser

**Test**: Verify AND logic with multiple filters

```gherkin
Given partnerships:
  | id | organiser | validated | company_emails |
  | p1 | john@example.com | true | [contact@a.com] |
  | p2 | john@example.com | false | [contact@b.com] |
  | p3 | jane@example.com | true | [contact@c.com] |
When I POST email with filter[organiser]=john@example.com&filter[validated]=true
Then response status is 204
And only contact@a.com receives email
And contact@b.com and contact@c.com do NOT receive email
```

### Scenario 4: Case-Insensitive Email Matching

**Test**: Verify case-insensitive filter application

```gherkin
Given partnership assigned to "john.doe@example.com"
When I POST email with filter[organiser]=John.Doe@Example.com
Then response status is 204
And email sent to partnership contact
```

### Scenario 5: Organisers Included in CC

**Test**: Verify organiser receives copy of email (existing behavior)

```gherkin
Given partnership assigned to john@example.com with contact@acme.com
When I POST email with filter[organiser]=john@example.com
Then response status is 204
And contact@acme.com receives email as TO
And john@example.com receives email as CC
```

---

## Implementation Notes

### Route Handler Pattern

```kotlin
post("/email") {
    val eventSlug = call.parameters.eventSlug
    val request = call.receive<EmailPartnershipRequest>(schema = "email_partnership_request.schema.json")
    
    // Parse filters (same as list endpoint)
    val filters = PartnershipFilters(
        packId = call.request.queryParameters["filter[pack_id]"],
        validated = call.request.queryParameters["filter[validated]"]?.toBoolean(),
        suggestion = call.request.queryParameters["filter[suggestion]"]?.toBoolean(),
        paid = call.request.queryParameters["filter[paid]"]?.toBoolean(),
        agreementGenerated = call.request.queryParameters["filter[agreement-generated]"]?.toBoolean(),
        agreementSigned = call.request.queryParameters["filter[agreement-signed]"]?.toBoolean(),
        organiser = call.request.queryParameters["filter[organiser]"],  // NEW
    )
    
    // Fetch partnerships with emails (same filter logic)
    val destinations = partnershipEmailRepository.getPartnershipDestination(eventSlug, filters)
    
    // Return 404 if no recipients (per FR-016 clarification)
    if (destinations.isEmpty()) {
        throw NotFoundException("No partnerships match the provided filters")
    }
    
    // Send emails (route-layer orchestration)
    destinations.forEach { destination ->
        notificationRepository.sendMessage(
            eventSlug = eventSlug,
            destination = destination,
            subject = request.subject,
            htmlBody = request.body,
        )
    }
    
    call.respond(HttpStatusCode.NoContent)
}
```

### Repository Reuse

**No changes needed to `PartnershipEmailRepository`** - it already accepts `PartnershipFilters` and will automatically apply organiser filter when `filters.organiser` is populated.

---

## Performance Considerations

- Same query patterns as list endpoint
- Email sending happens after filtering (route orchestration)
- No metadata generation needed (only list endpoint returns metadata)

---

## Changelog

| Version | Date | Changes |
|---------|------|---------|
| 1.0 | 2025-12-29 | Added organiser filter support, clarified 404 response for no recipients |
