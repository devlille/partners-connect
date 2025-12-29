# API Contracts: Assign Organiser to Partnership

**Feature**: Organiser assignment to partnerships  
**Date**: November 22, 2025  
**Base Path**: `/orgs/{orgSlug}/events/{eventSlug}/partnerships/{partnershipId}/organiser`

## Overview

RESTful API endpoints for managing organiser assignments to partnerships. All endpoints require organization-level edit permissions enforced by `AuthorizedOrganisationPlugin`.

**Note**: To retrieve organiser information, use the existing partnership GET endpoint which includes the organiser field in the Partnership response.

## Endpoints

### 1. Assign Organiser to Partnership

**Endpoint**: `POST /orgs/{orgSlug}/events/{eventSlug}/partnerships/{partnershipId}/organiser`

**Description**: Assigns a user as the designated organiser for a partnership. The user must be a member of the organization with edit permission.

**Authorization**: Requires `canEdit=true` permission for the organization (enforced by plugin)

**Path Parameters**:
- `orgSlug` (string, required): Organization slug
- `eventSlug` (string, required): Event slug
- `partnershipId` (UUID, required): Partnership UUID

**Request Body**:
```json
{
  "email": "organiser@example.com"
}
```

**Request Schema**: [`assign_organiser_request.schema.json`](./assign_organiser_request.schema.json)

**Success Response** (200 OK):
```json
{
  "partnership_id": "550e8400-e29b-41d4-a716-446655440000",
  "organiser": {
    "display_name": "Jane Organiser",
    "picture_url": "https://example.com/photos/jane.jpg",
    "email": "organiser@example.com"
  }
}
```

**Response Schema**: [`partnership_organiser_response.schema.json`](./partnership_organiser_response.schema.json)

**Error Responses**:
- `400 Bad Request`: Invalid email format or missing required fields
- `401 Unauthorized`: Missing or invalid authentication token, or no edit permission
- `403 Forbidden`: User is not a member of the organization or lacks edit permission
- `404 Not Found`: Partnership not found, user not found, or partnership not in specified organization

**Example cURL**:
```bash
curl -X POST \
  https://api.example.com/orgs/devlille/events/devlille-2025/partnerships/550e8400-e29b-41d4-a716-446655440000/organiser \
  -H 'Authorization: Bearer YOUR_JWT_TOKEN' \
  -H 'Content-Type: application/json' \
  -d '{"email": "organiser@example.com"}'
```

---

### 2. Remove Partnership Organiser

**Endpoint**: `DELETE /orgs/{orgSlug}/events/{eventSlug}/partnerships/{partnershipId}/organiser`

**Description**: Removes the organiser assignment from a partnership, leaving it without a designated contact.

**Authorization**: Requires `canEdit=true` permission for the organization (enforced by plugin)

**Path Parameters**:
- `orgSlug` (string, required): Organization slug
- `eventSlug` (string, required): Event slug
- `partnershipId` (UUID, required): Partnership UUID

**Success Response** (200 OK):
```json
{
  "partnership_id": "550e8400-e29b-41d4-a716-446655440000",
  "organiser": null
}
```

**Response Schema**: [`partnership_organiser_response.schema.json`](./partnership_organiser_response.schema.json)

**Error Responses**:
- `401 Unauthorized`: Missing or invalid authentication token, or no edit permission
- `404 Not Found`: Partnership not found or partnership not in specified organization

**Example cURL**:
```bash
curl -X DELETE \
  https://api.example.com/orgs/devlille/events/devlille-2025/partnerships/550e8400-e29b-41d4-a716-446655440000/organiser \
  -H 'Authorization: Bearer YOUR_JWT_TOKEN'
```

---

## Common Patterns

### Authentication

All endpoints require JWT bearer token authentication:
```
Authorization: Bearer <JWT_TOKEN>
```

Token is validated by `AuthorizedOrganisationPlugin` which:
1. Extracts user information from token
2. Verifies user has `canEdit=true` permission for the organization
3. Returns 401 Unauthorized if validation fails

### Content Type

**Request**: `Content-Type: application/json`  
**Response**: `Content-Type: application/json`

### Error Response Format

All error responses follow consistent format (handled by StatusPages):
```json
{
  "error": "Error message description"
}
```

### URL Structure

Follows RESTful resource hierarchy:
```
/orgs/{orgSlug}                    # Organization scope
  /events/{eventSlug}              # Event scope
    /partnerships/{partnershipId}  # Partnership scope
      /organiser                   # Organiser resource
```

## Validation Rules

### Request Validation (via JSON Schema)

**AssignOrganiserRequest**:
- `email`: Required, must be valid email format
- Additional properties not allowed
- Validated automatically via `call.receive<T>(schema)` pattern

### Business Logic Validation (in Repository)

**Assign Organiser**:
1. Partnership must exist
2. Partnership must belong to specified organization (via event)
3. User with given email must exist
4. User must have organization membership (validated via OrganisationPermissionsTable)
5. User must have edit permission (`canEdit=true`) for the organization

**Get/Remove Organiser**:
1. Partnership must exist
2. Partnership must belong to specified organization (via event)

**Validation Exceptions**:
- `NotFoundException` → HTTP 404
- `ForbiddenException` → HTTP 403
- `UnauthorizedException` → HTTP 401 (from plugin)

## OpenAPI Specification Updates

### Components/Schemas

Add to `server/application/src/main/resources/openapi/openapi.yaml`:

```yaml
components:
  schemas:
    AssignOrganiserRequest:
      $ref: "schemas/assign_organiser_request.schema.json"
    
    PartnershipOrganiserResponse:
      $ref: "schemas/partnership_organiser_response.schema.json"
```

### Paths

```yaml
paths:
  /orgs/{orgSlug}/events/{eventSlug}/partnerships/{partnershipId}/organiser:
    post:
      operationId: assignPartnershipOrganiser
      summary: Assign organiser to partnership
      security:
        - bearerAuth: []
      parameters:
        - name: orgSlug
          in: path
          required: true
          schema:
            type: string
        - name: eventSlug
          in: path
          required: true
          schema:
            type: string
        - name: partnershipId
          in: path
          required: true
          schema:
            type: string
            format: uuid
      requestBody:
        required: true
        content:
          application/json:
            schema:
              $ref: "#/components/schemas/AssignOrganiserRequest"
      responses:
        '200':
          description: Organiser assigned successfully
          content:
            application/json:
              schema:
                $ref: "#/components/schemas/PartnershipOrganiserResponse"
        '400':
          description: Invalid request body
        '401':
          description: Unauthorized
        '403':
          description: User is not a member of the organization
        '404':
          description: Partnership or user not found
    
    delete:
      operationId: removePartnershipOrganiser
      summary: Remove partnership organiser
      security:
        - bearerAuth: []
      parameters:
        - name: orgSlug
          in: path
          required: true
          schema:
            type: string
        - name: eventSlug
          in: path
          required: true
          schema:
            type: string
        - name: partnershipId
          in: path
          required: true
          schema:
            type: string
            format: uuid
      responses:
        '200':
          description: Organiser removed successfully
          content:
            application/json:
              schema:
                $ref: "#/components/schemas/PartnershipOrganiserResponse"
        '401':
          description: Unauthorized
        '404':
          description: Partnership not found
```

## Testing Checklist

### Contract Tests (Schema Validation)

- [ ] POST endpoint accepts valid AssignOrganiserRequest
- [ ] POST endpoint rejects request with missing email field
- [ ] POST endpoint rejects request with invalid email format
- [ ] POST endpoint returns valid PartnershipOrganiserResponse
- [ ] DELETE endpoint returns valid PartnershipOrganiserResponse with null organiser

### Integration Tests (Business Logic)

- [ ] POST assigns organiser when user is org member with edit permission
- [ ] POST returns 403 when user is not org member
- [ ] POST returns 403 when user lacks edit permission
- [ ] POST returns 404 when partnership not found
- [ ] POST returns 404 when user not found
- [ ] POST returns 401 when user lacks edit permission
- [ ] DELETE removes organiser successfully
- [ ] DELETE returns 404 when partnership not found
- [ ] Concurrent POST operations use last-write-wins strategy
- [ ] Organiser information is included in existing partnership GET endpoint responses

## Constitutional Compliance

✅ **JSON Schema Validation**: All request bodies validated via `call.receive<T>(schema)`  
✅ **Authorization Plugin**: Routes use `AuthorizedOrganisationPlugin`  
✅ **Exception Handling**: Domain exceptions thrown, StatusPages handles HTTP mapping  
✅ **Parameter Extraction**: Uses `call.parameters.partnershipId` extensions  
✅ **OpenAPI Documentation**: All endpoints documented with schema references  
✅ **Security Definitions**: Uses `bearerAuth` security scheme  
✅ **Operation IDs**: Unique camelCase IDs for each operation
