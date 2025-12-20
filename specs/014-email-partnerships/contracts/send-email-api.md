# API Contract: Send Email to Partnership Contacts

**Feature**: 014-email-partnerships  
**Endpoint**: `POST /orgs/{orgSlug}/events/{eventSlug}/partnerships/email`  
**Version**: 1.0  
**Status**: Complete

---

## Endpoint Definition

### HTTP Request

```http
POST /orgs/{orgSlug}/events/{eventSlug}/partnerships/email HTTP/1.1
Host: api.example.com
Content-Type: application/json
Authorization: Bearer <JWT_TOKEN>
```

**Path Parameters**:
- `orgSlug` (string, required): Organization slug identifier (e.g., `"devlille"`)
- `eventSlug` (string, required): Event slug identifier (e.g., `"devlille-2025"`)

**Query Parameters** (all optional):
- `filter[validated]` (boolean): Filter to partnerships with validated pack
- `filter[suggestion]` (boolean): Filter to partnerships with sent suggestion
- `filter[paid]` (boolean): Filter to partnerships with paid invoice
- `filter[agreement-generated]` (boolean): Filter to partnerships with generated agreement
- `filter[agreement-signed]` (boolean): Filter to partnerships with signed agreement
- `filter[pack_id]` (UUID): Filter to partnerships with specific validated pack ID
- `direction` (string): Sort direction (`"asc"` or `"desc"`, default: `"desc"`)

**Request Body** (JSON, validated by schema):
```json
{
  "subject": "Your Partnership Update",
  "body": "<p>Dear Partner,</p><p>Thank you for your support...</p>"
}
```

**JSON Schema Reference**: `send_partnership_email_request.schema.json`

---

### Success Response (200 OK)

**Condition**: Email batch(es) successfully accepted by Mailjet

**Response Body**:
```json
{
  "recipients": 42
}
```

**Fields**:
- `recipients` (integer, minimum 1): Total count of unique email addresses that received the email across all organizer groups

**JSON Schema Reference**: `send_partnership_email_response.schema.json`

**Response Headers**:
```
Content-Type: application/json
```

---

### Error Responses

#### 400 Bad Request

**Condition**: Invalid request body (schema validation failure)

**Response Body**:
```json
{
  "error": "Bad Request",
  "message": "Validation failed: subject must not be empty",
  "status": 400
}
```

**Validation Failures**:
- Missing `subject` or `body` field
- Empty string for `subject` or `body`
- Subject exceeds 500 characters

---

#### 401 Unauthorized

**Condition**: Missing or invalid JWT token, or user lacks edit permission for organization

**Response Body**:
```json
{
  "error": "Unauthorized",
  "message": "You do not have permission to edit this organization",
  "status": 401
}
```

**Trigger Scenarios**:
- No Authorization header
- Invalid/expired JWT token
- User not member of organization
- User has read-only role (lacks edit permission)

---

#### 404 Not Found

**Condition**: One of the following:
1. Event not found for given `eventSlug`
2. Organization not found for given `orgSlug`
3. No partnerships match the applied filters
4. Mailjet integration not configured for organization

**Response Body**:
```json
{
  "error": "Not Found",
  "message": "No partnerships found matching the filters",
  "status": 404
}
```

**Alternative Messages**:
- `"Event not found: devlille-2025"`
- `"Organisation not found: devlille"`
- `"Mailjet integration not configured for organisation"`
- `"No email addresses found for matching partnerships"`

---

#### 503 Service Unavailable

**Condition**: Mailjet API is unavailable or returns error response

**Response Body**:
```json
{
  "error": "Service Unavailable",
  "message": "Email service is currently unavailable. Please try again later.",
  "status": 503
}
```

**Trigger Scenarios**:
- Mailjet API responds with 5xx error
- Network timeout connecting to Mailjet
- Mailjet quota exceeded (API returns specific error)

**Quota Exceeded Variant**:
```json
{
  "error": "Service Unavailable",
  "message": "Email quota exceeded. Please contact support or wait for quota reset.",
  "status": 503
}
```

---

## Request/Response Examples

### Example 1: Send to Validated Partnerships Only

**Request**:
```http
POST /orgs/devlille/events/devlille-2025/partnerships/email?filter[validated]=true HTTP/1.1
Host: api.example.com
Content-Type: application/json
Authorization: Bearer eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9...

{
  "subject": "Partnership Pack Validation",
  "body": "<p>Dear Partner,</p><p>Your sponsorship pack has been validated. Next steps...</p>"
}
```

**Response** (200 OK):
```json
{
  "recipients": 15
}
```

**Explanation**: 15 unique email addresses from validated partnerships received the email (after deduplication and organizer grouping).

---

### Example 2: Send to Paid Partnerships with Signed Agreement

**Request**:
```http
POST /orgs/devlille/events/devlille-2025/partnerships/email?filter[paid]=true&filter[agreement-signed]=true HTTP/1.1
Host: api.example.com
Content-Type: application/json
Authorization: Bearer eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9...

{
  "subject": "Event Logistics Update",
  "body": "<h1>Important: Venue Setup Details</h1><p>As a confirmed sponsor...</p><ul><li>Booth setup: June 1st</li></ul>"
}
```

**Response** (200 OK):
```json
{
  "recipients": 8
}
```

---

### Example 3: No Partnerships Match Filters

**Request**:
```http
POST /orgs/devlille/events/devlille-2025/partnerships/email?filter[validated]=true&filter[pack_id]=00000000-0000-0000-0000-000000000000 HTTP/1.1
Host: api.example.com
Content-Type: application/json
Authorization: Bearer eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9...

{
  "subject": "Test",
  "body": "<p>Test email</p>"
}
```

**Response** (404 Not Found):
```json
{
  "error": "Not Found",
  "message": "No partnerships found matching the filters",
  "status": 404
}
```

---

### Example 4: Mailjet Not Configured

**Request**:
```http
POST /orgs/neworg/events/new-event-2025/partnerships/email HTTP/1.1
Host: api.example.com
Content-Type: application/json
Authorization: Bearer eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9...

{
  "subject": "Welcome",
  "body": "<p>Welcome to our event...</p>"
}
```

**Response** (404 Not Found):
```json
{
  "error": "Not Found",
  "message": "Mailjet integration not configured for organisation",
  "status": 404
}
```

---

### Example 5: Invalid Request Body

**Request**:
```http
POST /orgs/devlille/events/devlille-2025/partnerships/email HTTP/1.1
Host: api.example.com
Content-Type: application/json
Authorization: Bearer eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9...

{
  "subject": "",
  "body": "Plain text body"
}
```

**Response** (400 Bad Request):
```json
{
  "error": "Bad Request",
  "message": "Validation failed: subject must not be empty",
  "status": 400
}
```

---

## Authorization Requirements

### JWT Token Claims

**Required Claims**:
- `userId` (UUID): User identifier for permission lookup
- `exp` (timestamp): Token expiration (must be valid)

### Permission Check Logic

```
1. Extract orgSlug from path parameters
2. Verify user is member of organization (via Users → UserOrganisations table)
3. Verify user has edit permission for organization
4. If any check fails → 401 Unauthorized
```

**Permission Level**: `edit` (read-only access is insufficient)

**Handled By**: `AuthorizedOrganisationPlugin` (automatic enforcement, no manual checks in route)

---

## Rate Limiting

**Current Implementation**: None (not in scope for v1)

**Future Consideration**: 
- Per-organization limit: 10 emails/hour
- Per-user limit: 5 emails/hour
- Would return `429 Too Many Requests` when exceeded

---

## Idempotency

**Behavior**: **Non-idempotent** (each request sends new email batch)

**Retry Safety**: NOT safe to retry automatically
- Multiple identical requests → Multiple emails sent to same recipients
- Clients should implement retry with user confirmation

**Recommended Client Behavior**:
1. On network error: Prompt user "Email may have been sent. Retry?"
2. On 5xx error: Show "Service unavailable, please try later"
3. On 4xx error: Display validation error, allow user to fix and resubmit

---

## Sequence Diagram

```
Client                     API Server                PartnershipEmailRepo   EventRepo   NotificationRepo   IntegrationRepo   MailjetProvider
  |                             |                            |                  |              |                 |                 |
  |-- POST /email ----------->  |                            |                  |              |                 |                 |
  |  + JWT token                |                            |                  |              |                 |                 |
  |  + Body: {subject, body}    |                            |                  |              |                 |                 |
  |                             |                            |                  |              |                 |                 |
  |                             |-- Validate JWT ----------->| AuthPlugin       |              |                 |                 |
  |                             |<- Permissions OK ----------|                  |              |                 |                 |
  |                             |                            |                  |              |                 |                 |
  |                             |-- call.receive<Req>() ---->| JSON Schema      |              |                 |                 |
  |                             |<- Validated Request -------|                  |              |                 |                 |
  |                             |                            |                  |              |                 |                 |
  |                             |-- getPartnershipsWithEmails(eventSlug, filters) -->          |                 |                 |
  |                             |                            |                  |              |                 |                 |
  |                             |                            |-- DB Query ------+              |                 |                 |
  |                             |                            |   (JOIN partnerships,           |                 |                 |
  |                             |                            |    company_emails, users)       |                 |                 |
  |                             |                            |                  |              |                 |                 |
  |                             |<- List<PartnershipWithEmails> -----------------|              |                 |                 |
  |                             |   [P1(org=User1, emails=[a,b]),               |              |                 |                 |
  |                             |    P2(org=null, emails=[c])]                 |              |                 |                 |
  |                             |                            |                  |              |                 |                 |
  |                             |-- findBySlug(eventSlug) ---------------------->|              |                 |                 |
  |                             |<- Event(name, contactEmail) ------------------|              |                 |                 |
  |                             |                            |                  |              |                 |                 |
  |    [Route Logic: Group by Organizer]                    |                  |              |                 |                 |
  |    Group 1: User1 → emails [a, b] (distinct)            |                  |              |                 |                 |
  |    Group 2: null → emails [c]                           |                  |              |                 |                 |
  |                             |                            |                  |              |                 |                 |
  |    [Route loops organizer groups]                       |                  |              |                 |                 |
  |                             |-- sendMail(orgSlug, from=User1, to=[a,b], cc=[event]) ------>|                 |                 |
  |                             |                            |                  |              |                 |                 |
  |                             |                            |                  |              |-- getMailjetConfig(orgSlug) --->|                 |
  |                             |                            |                  |              |<- MailjetConfig -------------|                 |
  |                             |                            |                  |              |                 |                 |
  |                             |                            |                  |              |-- send(Message1, config) -------->|      |
  |                             |                            |                  |              |   From: user1@example.com     |      |
  |                             |                            |                  |              |   CC: event@example.com       |      |
  |                             |                            |                  |              |   To: [a, b]                  |      |
  |                             |                            |                  |              |                 |      |
  |                             |                            |                  |              |                 |      |-- POST /v3.1/send -->| Mailjet API
  |                             |                            |                  |              |                 |      |<- 200 OK ------------|  
  |                             |                            |                  |              |<- true (success) ----------|      |                 |
  |                             |<- true (success) --------------------------------------|                 |      |                 |
  |                             |                            |                  |              |                 |                 |
  |                             |-- sendMail(orgSlug, from=event, to=[c]) -------------------->|                 |                 |
  |                             |                            |                  |              |                 |                 |
  |                             |                            |                  |              |-- send(Message2, config) -------->|      |
  |                             |                            |                  |              |   From: event@example.com     |      |
  |                             |                            |                  |              |   To: [c]                     |      |
  |                             |                            |                  |              |                 |      |
  |                             |                            |                  |              |                 |      |-- POST /v3.1/send -->| Mailjet API
  |                             |                            |                  |              |                 |      |<- 200 OK ------------|  
  |                             |                            |                  |              |<- true (success) ----------|      |                 |
  |                             |<- true (success) --------------------------------------|                 |      |                 |
  |                             |                            |                  |              |                 |                 |
  |<- 200 OK ------------------|                            |                  |              |                 |                 |
  |   {recipients: 3}           |                            |                  |              |                 |                 |
```

---

## Data Transformation Pipeline

### Step 1: Input Validation
```json
// Raw Request Body
{
  "subject": "Update",
  "body": "<p>Content</p>"
}

// Validated via JSON Schema → SendPartnershipEmailRequest
SendPartnershipEmailRequest(
  subject = "Update",
  body = "<p>Content</p>"
)
```

### Step 2: Data Retrieval
```kotlin
// Repository returns list of partnerships with emails
List<PartnershipWithEmails>(
  PartnershipWithEmails(
    partnershipId = UUID(...),
    organiser = User(id=UUID(...), email="alice@example.com", firstname="Alice", lastname="Smith"),
    emails = ["contact1@company.com", "contact2@company.com"]
  ),
  PartnershipWithEmails(
    partnershipId = UUID(...),
    organiser = null,
    emails = ["contact3@company.com"]
  )
)
```

### Step 3: Email Grouping by Organizer
```kotlin
// Group partnerships by assigned organizer
Map<User?, List<PartnershipWithEmails>>(
  User(alice@example.com) → [Partnership1],
  null → [Partnership2]
)
```

### Step 4: Recipient Deduplication per Group
```kotlin
// For each group, collect and deduplicate emails
Group 1 (Alice):
  Emails: ["contact1@company.com", "contact2@company.com"]
  Distinct: ["contact1@company.com", "contact2@company.com"]  // 2 recipients

Group 2 (No organizer):
  Emails: ["contact3@company.com"]
  Distinct: ["contact3@company.com"]  // 1 recipient
```

### Step 5: Mailjet Batch Construction
```json
// Batch 1: Sent by Alice
{
  "Messages": [
    {
      "From": {"Email": "alice@example.com", "Name": "Alice Smith"},
      "Cc": [{"Email": "event@devlille.com"}],
      "To": [
        {"Email": "contact1@company.com"},
        {"Email": "contact2@company.com"}
      ],
      "Subject": "[DevLille 2025] Update",
      "HTMLPart": "<p>Content</p>"
    }
  ]
}

// Batch 2: Sent by Event
{
  "Messages": [
    {
      "From": {"Email": "event@devlille.com", "Name": "DevLille 2025"},
      "To": [{"Email": "contact3@company.com"}],
      "Subject": "[DevLille 2025] Update",
      "HTMLPart": "<p>Content</p>"
    }
  ]
}
```

### Step 6: Response Construction
```kotlin
// Count total unique recipients across all groups
totalRecipients = 2 + 1 = 3

// Return response
SendPartnershipEmailResponse(recipients = 3)
```

---

## Contract Test Scenarios

### Test 1: Valid Request, Multiple Recipients
**Given**: 3 partnerships with assigned organizers and 5 total unique emails  
**When**: POST with subject and body  
**Then**: 200 OK with `{"recipients": 5}`

### Test 2: Valid Request, Single Organizer Group
**Given**: 2 partnerships assigned to same organizer  
**When**: POST request  
**Then**: 200 OK, 1 Mailjet batch sent (not 2)

### Test 3: Missing Subject
**Given**: Request body `{"body": "<p>Content</p>"}`  
**When**: POST request  
**Then**: 400 Bad Request with validation error

### Test 4: Subject Too Long
**Given**: Subject with 501 characters  
**When**: POST request  
**Then**: 400 Bad Request with validation error

### Test 5: No Matching Partnerships
**Given**: Filter `filter[pack_id]=nonexistent-uuid`  
**When**: POST request  
**Then**: 404 Not Found with "No partnerships found matching the filters"

### Test 6: Mailjet Integration Not Configured
**Given**: Organization without Mailjet integration  
**When**: POST request  
**Then**: 404 Not Found with "Mailjet integration not configured"

### Test 7: Unauthorized User
**Given**: JWT token with no edit permission for organization  
**When**: POST request  
**Then**: 401 Unauthorized

### Test 8: Email Deduplication
**Given**: 2 partnerships sharing same contact email  
**When**: POST request  
**Then**: 200 OK, email count reflects deduplicated total (not duplicates)

### Test 9: Mailjet API Failure
**Given**: Mailjet returns 503 Service Unavailable  
**When**: POST request  
**Then**: 503 Service Unavailable with "Email service is currently unavailable"

---

## OpenAPI 3.1.0 Specification

```yaml
/orgs/{orgSlug}/events/{eventSlug}/partnerships/email:
  post:
    summary: Send email to partnership contacts
    description: |
      Send a bulk email to contact addresses of partnerships matching the provided filters.
      Emails are grouped by assigned organizer (if any) for personalized sender information.
      Recipients are deduplicated within each organizer group.
    operationId: sendPartnershipEmail
    tags:
      - Partnerships
    parameters:
      - name: orgSlug
        in: path
        required: true
        schema:
          type: string
        description: Organization slug identifier
      - name: eventSlug
        in: path
        required: true
        schema:
          type: string
        description: Event slug identifier
      - name: filter[validated]
        in: query
        required: false
        schema:
          type: boolean
        description: Filter to partnerships with validated pack
      - name: filter[suggestion]
        in: query
        required: false
        schema:
          type: boolean
        description: Filter to partnerships with sent suggestion
      - name: filter[paid]
        in: query
        required: false
        schema:
          type: boolean
        description: Filter to partnerships with paid invoice
      - name: filter[agreement-generated]
        in: query
        required: false
        schema:
          type: boolean
        description: Filter to partnerships with generated agreement
      - name: filter[agreement-signed]
        in: query
        required: false
        schema:
          type: boolean
        description: Filter to partnerships with signed agreement
      - name: filter[pack_id]
        in: query
        required: false
        schema:
          type: string
          format: uuid
        description: Filter to partnerships with specific validated pack ID
      - name: direction
        in: query
        required: false
        schema:
          type: string
          enum: [asc, desc]
          default: desc
        description: Sort direction for partnerships
    requestBody:
      required: true
      content:
        application/json:
          schema:
            $ref: '#/components/schemas/SendPartnershipEmailRequest'
          examples:
            simpleEmail:
              summary: Simple email notification
              value:
                subject: "Partnership Update"
                body: "<p>Dear Partner,</p><p>Thank you for your support.</p>"
            htmlEmail:
              summary: Rich HTML email
              value:
                subject: "Event Logistics Information"
                body: "<h1>Booth Setup Details</h1><ul><li>Date: June 1st</li><li>Time: 9:00 AM</li></ul>"
    responses:
      '200':
        description: Email successfully sent
        content:
          application/json:
            schema:
              $ref: '#/components/schemas/SendPartnershipEmailResponse'
            examples:
              success:
                summary: Email sent to 15 recipients
                value:
                  recipients: 15
      '400':
        description: Invalid request body
        content:
          application/json:
            schema:
              $ref: '#/components/schemas/Error'
            examples:
              missingSubject:
                summary: Missing subject field
                value:
                  error: "Bad Request"
                  message: "Validation failed: subject must not be empty"
                  status: 400
      '401':
        description: Unauthorized - missing or invalid JWT token, or insufficient permissions
        content:
          application/json:
            schema:
              $ref: '#/components/schemas/Error'
            examples:
              noPermission:
                summary: User lacks edit permission
                value:
                  error: "Unauthorized"
                  message: "You do not have permission to edit this organization"
                  status: 401
      '404':
        description: Not Found - event/org not found, no matching partnerships, or Mailjet not configured
        content:
          application/json:
            schema:
              $ref: '#/components/schemas/Error'
            examples:
              noPartnerships:
                summary: No partnerships match filters
                value:
                  error: "Not Found"
                  message: "No partnerships found matching the filters"
                  status: 404
              noMailjet:
                summary: Mailjet integration missing
                value:
                  error: "Not Found"
                  message: "Mailjet integration not configured for organisation"
                  status: 404
      '503':
        description: Service Unavailable - Mailjet API error or quota exceeded
        content:
          application/json:
            schema:
              $ref: '#/components/schemas/Error'
            examples:
              mailjetDown:
                summary: Mailjet API unavailable
                value:
                  error: "Service Unavailable"
                  message: "Email service is currently unavailable. Please try again later."
                  status: 503
              quotaExceeded:
                summary: Email quota exceeded
                value:
                  error: "Service Unavailable"
                  message: "Email quota exceeded. Please contact support or wait for quota reset."
                  status: 503
    security:
      - bearerAuth: []

components:
  schemas:
    SendPartnershipEmailRequest:
      type: object
      required:
        - subject
        - body
      properties:
        subject:
          type: string
          minLength: 1
          maxLength: 500
          description: Email subject line (will be prefixed with event name)
        body:
          type: string
          minLength: 1
          description: Email body in HTML format
    
    SendPartnershipEmailResponse:
      type: object
      required:
        - recipients
      properties:
        recipients:
          type: integer
          minimum: 1
          description: Total number of unique email recipients
    
    Error:
      type: object
      required:
        - error
        - message
        - status
      properties:
        error:
          type: string
          description: Error type (e.g., "Bad Request", "Unauthorized")
        message:
          type: string
          description: Human-readable error message
        status:
          type: integer
          description: HTTP status code
  
  securitySchemes:
    bearerAuth:
      type: http
      scheme: bearer
      bearerFormat: JWT
```

---

## Summary

**Method**: POST  
**Path**: `/orgs/{orgSlug}/events/{eventSlug}/partnerships/email`  
**Authentication**: Required (JWT Bearer token)  
**Authorization**: User must have edit permission for organization  
**Success Status**: 200 OK  
**Minimum Recipients**: 1 (enforced by business logic, 404 if zero matches)  
**Mailjet Batches**: Variable (1 per unique organizer assignment)  
**Idempotent**: No (each request sends new emails)  
**Rate Limited**: No (v1 scope)
