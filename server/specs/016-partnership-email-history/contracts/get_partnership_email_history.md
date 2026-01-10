# GET /orgs/{orgSlug}/events/{eventSlug}/partnerships/{id}/email-history

Retrieve the complete email history for a specific partnership.

## Endpoint

```
GET /orgs/{orgSlug}/events/{eventSlug}/partnerships/{id}/email-history
```

## Authorization

**Required**: Organiser with permissions for the specified event

**Enforced by**: `AuthorizedOrganisationPlugin`

## Path Parameters

| Parameter | Type | Required | Description |
|-----------|------|----------|-------------|
| `orgSlug` | string | Yes | Organization slug identifier |
| `eventSlug` | string | Yes | Event slug identifier |
| `id` | UUID | Yes | Partnership UUID |

## Query Parameters

| Parameter | Type | Required | Default | Description |
|-----------|------|----------|---------|-------------|
| `page` | integer | No | 0 | Zero-based page number |
| `page_size` | integer | No | 20 | Records per page (1-100) |

**Validation**:
- `page` ≥ 0
- `page_size` must be 1-100 (inclusive)
- Invalid values return 400 Bad Request

## Request Headers

| Header | Required | Value |
|--------|----------|-------|
| `Authorization` | Yes | Bearer token from Google OAuth |

## Response: 200 OK

**Content-Type**: `application/json`

### Response Schema

```json
{
  "items": [
    {
      "id": "uuid",
      "partnershipId": "uuid",
      "sentAt": "2026-01-10T14:30:00",
      "senderEmail": "organiser@example.com",
      "subject": "[Event Name][Company] Partnership Approval",
      "bodyPlainText": "Dear Partner, your partnership has been approved...",
      "overallStatus": "sent",
      "triggeredBy": "user-uuid-or-system",
      "recipients": [
        {
          "email": "partner@company.com",
          "status": "sent"
        },
        {
          "email": "cc@company.com",
          "status": "sent"
        }
      ]
    }
  ],
  "page": 0,
  "page_size": 20,
  "total": 123
}
```

### Field Descriptions

**Email History Object**:
- `id`: Unique identifier for this email history record
- `partnershipId`: UUID of the partnership this email was sent to
- `sentAt`: ISO 8601 datetime (UTC) when email was sent
- `senderEmail`: Email address used as "From" address
- `subject`: Email subject line
- `bodyPlainText`: Email body content (plain text, max 50,000 chars)
- `overallStatus`: Overall delivery status
  - `"sent"` - All recipients succeeded
  - `"failed"` - All recipients failed
  - `"partial"` - Mixed success/failure
- `triggeredBy`: User UUID who triggered the email, or `"system"` for automated emails
- `recipients`: Array of per-recipient delivery status

**Recipient Object**:
- `email`: Recipient email address
- `status`: Delivery status for this recipient
  - `"sent"` - Successfully delivered
  - `"failed"` - Delivery failed

**Pagination Fields**:
- `page`: Current page number (zero-based)
- `page_size`: Records per page
- `total`: Total number of email records for this partnership

### Example Response

```json
{
  "items": [
    {
      "id": "f47ac10b-58cc-4372-a567-0e02b2c3d479",
      "partnershipId": "a3bb189e-8bf9-3888-9912-ace4e6543002",
      "sentAt": "2026-01-10T14:30:00",
      "senderEmail": "organiser@devlille.fr",
      "subject": "[DevFest Lille 2026][Acme Corp] Partnership Approved",
      "bodyPlainText": "Dear Acme Corp,\n\nYour partnership application has been approved! We're excited to have you as a Gold sponsor.\n\nBest regards,\nDevFest Lille Team",
      "overallStatus": "sent",
      "triggeredBy": "9b1deb4d-3b7d-4bad-9bdd-2b0d7b3dcb6d",
      "recipients": [
        {
          "email": "partnerships@acmecorp.com",
          "status": "sent"
        },
        {
          "email": "ceo@acmecorp.com",
          "status": "sent"
        },
        {
          "email": "organiser@devlille.fr",
          "status": "sent"
        }
      ]
    },
    {
      "id": "550e8400-e29b-41d4-a716-446655440000",
      "partnershipId": "a3bb189e-8bf9-3888-9912-ace4e6543002",
      "sentAt": "2026-01-09T10:15:00",
      "senderEmail": "system@devlille.fr",
      "subject": "[DevFest Lille 2026][Acme Corp] Partnership Registered",
      "bodyPlainText": "Dear Acme Corp,\n\nThank you for registering your partnership. We will review your application shortly.",
      "overallStatus": "partial",
      "triggeredBy": "system",
      "recipients": [
        {
          "email": "partnerships@acmecorp.com",
          "status": "sent"
        },
        {
          "email": "invalid@",
          "status": "failed"
        }
      ]
    }
  ],
  "page": 0,
  "page_size": 20,
  "total": 2
}
```

## Response: 400 Bad Request

Invalid query parameters.

```json
{
  "message": "Invalid query parameter: page_size must be between 1 and 100"
}
```

**Possible Causes**:
- `page` < 0
- `page_size` < 1 or > 100

## Response: 401 Unauthorized

User not authenticated.

```json
{
  "message": "Unauthorized"
}
```

## Response: 403 Forbidden

User doesn't have permissions for this event.

```json
{
  "message": "Forbidden: User does not have permission for this event"
}
```

## Response: 404 Not Found

Partnership or event not found.

```json
{
  "message": "Partnership with ID {id} not found"
}
```

**Possible Causes**:
- Partnership doesn't exist
- Event doesn't exist
- Organization doesn't exist

## Response: 200 OK (Empty History)

Partnership has no email history.

```json
{
  "items": [],
  "page": 0,
  "page_size": 20,
  "total": 0
}
```

## Implementation Notes

### Ordering
- Emails MUST be returned in **reverse chronological order** (newest first)
- Uses `ORDER BY sent_at DESC`

### Performance
- Response time target: <2 seconds (per SC-001)
- Database index on (partnership_id, sent_at) ensures efficient queries
- Pagination prevents performance degradation with large histories

### Data Integrity
- Email history records are **immutable** (no updates/deletes)
- History persists even if partnership is deleted (per FR-005)
- Per-recipient status provides complete audit trail

### Testing Requirements

**Contract Test** (`PartnershipEmailHistoryRouteGetTest`):
- Test 200 with data
- Test 200 with empty history
- Test 400 with invalid query params
- Test 401 without authentication
- Test 403 without permissions
- Test 404 with non-existent partnership

**Integration Test** (`PartnershipEmailHistoryRoutesTest`):
- Send email via existing notification endpoint
- Retrieve history via this endpoint
- Verify email details match what was sent
- Test pagination with >50 records

## OpenAPI Schema References

This endpoint will be documented in `openapi.yaml` with schema references:

```yaml
paths:
  /orgs/{orgSlug}/events/{eventSlug}/partnerships/{id}/email-history:
    get:
      summary: Get partnership email history
      operationId: getPartnershipEmailHistory
      tags: [Partnerships]
      parameters:
        - $ref: '#/components/parameters/OrgSlug'
        - $ref: '#/components/parameters/EventSlug'
        - $ref: '#/components/parameters/PartnershipId'
        - name: page
          in: query
          schema:
            type: integer
            minimum: 0
            default: 0
        - name: page_size
          in: query
          schema:
            type: integer
            minimum: 1
            maximum: 100
            default: 20
      responses:
        '200':
          description: Email history retrieved successfully
          content:
            application/json:
              schema:
                $ref: '#/components/schemas/PartnershipEmailHistoryListResponse'
        '400':
          $ref: '#/components/responses/BadRequest'
        '401':
          $ref: '#/components/responses/Unauthorized'
        '403':
          $ref: '#/components/responses/Forbidden'
        '404':
          $ref: '#/components/responses/NotFound'
```

## JSON Schema Files to Create

1. `schemas/partnership_email_history_response.schema.json` - Single email history object
2. `schemas/partnership_email_history_list_response.schema.json` - Paginated list response
3. `schemas/recipient_delivery_status.schema.json` - Recipient status object
