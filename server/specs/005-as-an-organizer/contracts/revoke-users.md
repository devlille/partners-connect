# HTTP Contract: Revoke User Permissions

**Endpoint**: `POST /orgs/{orgSlug}/users/revoke`  
**Purpose**: Revoke edit permissions for users from an organisation  
**Authentication**: Required (Bearer JWT token)  
**Authorization**: Requesting user must have `canEdit=true` for the organisation

## Request Specification

### Endpoint
```
POST /orgs/{orgSlug}/users/revoke
```

### Path Parameters
| Parameter | Type | Required | Description |
|-----------|------|----------|-------------|
| `orgSlug` | string | Yes | Unique organisation identifier (e.g., "devlille-2025") |

### Headers
```http
Authorization: Bearer {JWT_TOKEN}
Content-Type: application/json
```

### Request Body Schema
```json
{
  "user_emails": ["string"]
}
```

#### JSON Schema Definition
File: `server/application/src/main/resources/schemas/revoke_permission_request.schema.json`

```json
{
  "$schema": "http://json-schema.org/draft-07/schema#",
  "type": "object",
  "required": ["user_emails"],
  "properties": {
    "user_emails": {
      "type": "array",
      "items": {
        "type": "string",
        "format": "email"
      },
      "description": "List of user email addresses to revoke access from"
    }
  },
  "additionalProperties": false
}
```

### Example Requests

#### Successful Revocation (Full Success)
```http
POST /orgs/devlille-2025/users/revoke HTTP/1.1
Host: api.partners-connect.example.com
Authorization: Bearer eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9...
Content-Type: application/json

{
  "user_emails": [
    "alice@example.com",
    "bob@example.com"
  ]
}
```

#### Revocation with Non-Existent Users (Partial Success)
```http
POST /orgs/devlille-2025/users/revoke HTTP/1.1
Authorization: Bearer eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9...
Content-Type: application/json

{
  "user_emails": [
    "alice@example.com",
    "nonexistent@example.com"
  ]
}
```

#### Empty Revocation List
```http
POST /orgs/devlille-2025/users/revoke HTTP/1.1
Authorization: Bearer eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9...
Content-Type: application/json

{
  "user_emails": []
}
```

## Response Specification

### Success Response (200 OK)

#### Full Success
```http
HTTP/1.1 200 OK
Content-Type: application/json

{
  "revoked_count": 2,
  "not_found_emails": []
}
```

#### Partial Success
```http
HTTP/1.1 200 OK
Content-Type: application/json

{
  "revoked_count": 1,
  "not_found_emails": [
    "nonexistent@example.com"
  ]
}
```

#### Empty List (No-Op)
```http
HTTP/1.1 200 OK
Content-Type: application/json

{
  "revoked_count": 0,
  "not_found_emails": []
}
```

### Response Body Schema
```json
{
  "revoked_count": "integer",
  "not_found_emails": ["string"]
}
```

**Fields**:
- `revoked_count`: Number of users whose permissions were successfully revoked (≥ 0)
- `not_found_emails`: List of email addresses that were not found in the system

### Error Responses

#### 400 Bad Request - Invalid JSON or Schema Violation
```http
HTTP/1.1 400 Bad Request
Content-Type: application/json

{
  "error": "Validation failed",
  "details": "user_emails: must be a valid email address"
}
```

**Causes**:
- Malformed JSON
- Missing required field `user_emails`
- Invalid email format
- Additional properties in request body

#### 401 Unauthorized - Missing or Invalid Token
```http
HTTP/1.1 401 Unauthorized
Content-Type: application/json

{
  "error": "Missing Authorization header"
}
```

**Causes**:
- No `Authorization` header provided
- Invalid JWT token (expired, malformed, invalid signature)

#### 401 Unauthorized - No Edit Permission
```http
HTTP/1.1 401 Unauthorized
Content-Type: application/json

{
  "error": "You do not have permission to revoke users for this organisation"
}
```

**Causes**:
- Requesting user exists but lacks `canEdit=true` permission for organisation
- Handled by `AuthorizedOrganisationPlugin`

#### 404 Not Found - Organisation Not Found
```http
HTTP/1.1 404 Not Found
Content-Type: application/json

{
  "error": "Organisation with slug: devlille-2025 not found"
}
```

**Causes**:
- Organisation with specified slug does not exist in database

#### 404 Not Found - Requesting User Not Found
```http
HTTP/1.1 404 Not Found
Content-Type: application/json

{
  "error": "User with email admin@example.com not found"
}
```

**Causes**:
- Token is valid but user (from token) is not registered in database
- Rare edge case (user deleted after authentication but before request)

#### 409 Conflict - Last Editor Self-Revocation
```http
HTTP/1.1 409 Conflict
Content-Type: application/json

{
  "error": "Cannot revoke your own access as the last editor of this organisation"
}
```

**Causes**:
- Requesting user attempts to revoke their own access
- User is the only remaining user with `canEdit=true` on organisation
- Prevents orphaned organisations (FR-013)

#### 500 Internal Server Error - Database Error
```http
HTTP/1.1 500 Internal Server Error
Content-Type: application/json

{
  "error": "An unexpected error occurred"
}
```

**Causes**:
- Database connection failure
- Transaction rollback due to unexpected error
- Any unhandled exception in repository layer

## Contract Test Scenarios

### Test 1: Successful Full Revocation
**Given**: 
- Organisation "test-org" exists
- Users alice@example.com and bob@example.com have edit permissions
- Requesting user (admin@example.com) has edit permission

**When**: 
```
POST /orgs/test-org/users/revoke
{ "user_emails": ["alice@example.com", "bob@example.com"] }
```

**Then**:
- Status: 200 OK
- Response: `{ "revoked_count": 2, "not_found_emails": [] }`
- Database: OrganisationPermission records deleted for alice and bob

### Test 2: Partial Success with Non-Existent Users
**Given**:
- Organisation "test-org" exists
- User alice@example.com has edit permission
- User nonexistent@example.com does NOT exist

**When**:
```
POST /orgs/test-org/users/revoke
{ "user_emails": ["alice@example.com", "nonexistent@example.com"] }
```

**Then**:
- Status: 200 OK
- Response: `{ "revoked_count": 1, "not_found_emails": ["nonexistent@example.com"] }`
- Database: OrganisationPermission deleted for alice only

### Test 3: Unauthorized - No Edit Permission
**Given**:
- Organisation "test-org" exists
- Requesting user (viewer@example.com) has NO edit permission

**When**:
```
POST /orgs/test-org/users/revoke
{ "user_emails": ["alice@example.com"] }
```

**Then**:
- Status: 401 Unauthorized
- Response: `{ "error": "You do not have permission..." }`
- Database: No changes

### Test 4: Unauthorized - Missing Token
**Given**:
- Organisation "test-org" exists
- No Authorization header provided

**When**:
```
POST /orgs/test-org/users/revoke
{ "user_emails": ["alice@example.com"] }
```

**Then**:
- Status: 401 Unauthorized
- Response: `{ "error": "Missing Authorization header" }`
- Database: No changes

### Test 5: Not Found - Organisation Missing
**Given**:
- Organisation "nonexistent-org" does NOT exist
- Requesting user is authenticated

**When**:
```
POST /orgs/nonexistent-org/users/revoke
{ "user_emails": ["alice@example.com"] }
```

**Then**:
- Status: 404 Not Found
- Response: `{ "error": "Organisation with slug: nonexistent-org not found" }`
- Database: No changes

### Test 6: Conflict - Last Editor Self-Revocation
**Given**:
- Organisation "test-org" exists
- Requesting user (admin@example.com) is the ONLY user with edit permission

**When**:
```
POST /orgs/test-org/users/revoke
{ "user_emails": ["admin@example.com"] }
```

**Then**:
- Status: 409 Conflict
- Response: `{ "error": "Cannot revoke your own access as the last editor..." }`
- Database: No changes

### Test 7: Idempotent - Already Revoked
**Given**:
- Organisation "test-org" exists
- User alice@example.com does NOT have permissions (already revoked)

**When**:
```
POST /orgs/test-org/users/revoke
{ "user_emails": ["alice@example.com"] }
```

**Then**:
- Status: 200 OK
- Response: `{ "revoked_count": 0, "not_found_emails": ["alice@example.com"] }`
- Database: No changes (idempotent)

### Test 8: Empty List
**Given**:
- Organisation "test-org" exists
- Requesting user has edit permission

**When**:
```
POST /orgs/test-org/users/revoke
{ "user_emails": [] }
```

**Then**:
- Status: 200 OK
- Response: `{ "revoked_count": 0, "not_found_emails": [] }`
- Database: No changes

### Test 9: Bad Request - Invalid Email Format
**Given**:
- Organisation "test-org" exists

**When**:
```
POST /orgs/test-org/users/revoke
{ "user_emails": ["not-an-email"] }
```

**Then**:
- Status: 400 Bad Request
- Response: Schema validation error
- Database: No changes

### Test 10: Bad Request - Missing Required Field
**Given**:
- Organisation "test-org" exists

**When**:
```
POST /orgs/test-org/users/revoke
{}
```

**Then**:
- Status: 400 Bad Request
- Response: `{ "error": "Missing required field: user_emails" }`
- Database: No changes

## Performance Requirements

- **Response Time**: < 2 seconds for typical requests (1-50 emails)
- **Throughput**: Minimum 10 req/s per server instance
- **Timeout**: 30 seconds maximum

## Security Considerations

1. **Authentication**: JWT token required for all requests
2. **Authorization**: Automatic via AuthorizedOrganisationPlugin
3. **Input Validation**: JSON schema enforcement prevents injection attacks
4. **Rate Limiting**: Standard API rate limits apply (configured globally)
5. **Audit Trail**: No explicit audit logging per specification clarification

## OpenAPI Specification

This contract should be added to `server/application/src/main/resources/openapi/openapi.yaml`:

```yaml
/orgs/{orgSlug}/users/revoke:
  post:
    summary: Revoke user permissions from organisation
    operationId: revokeOrganisationUsers
    tags:
      - users
    security:
      - bearerAuth: []
    parameters:
      - name: orgSlug
        in: path
        required: true
        schema:
          type: string
        description: Organisation unique identifier
    requestBody:
      required: true
      content:
        application/json:
          schema:
            $ref: '#/components/schemas/RevokePermissionRequest'
    responses:
      '200':
        description: Permissions revoked successfully (full or partial success)
        content:
          application/json:
            schema:
              $ref: '#/components/schemas/RevokeUsersResult'
      '400':
        description: Invalid request body or validation error
      '401':
        description: Unauthorized - missing/invalid token or insufficient permissions
      '404':
        description: Organisation or requesting user not found
      '409':
        description: Conflict - cannot revoke last editor's own access
```

## Comparison with Grant Endpoint

| Aspect | Grant (`/grant`) | Revoke (`/revoke`) |
|--------|------------------|---------------------|
| HTTP Method | POST | POST |
| Path | `/orgs/{orgSlug}/users/grant` | `/orgs/{orgSlug}/users/revoke` |
| Request Body | `GrantPermissionRequest` | `RevokePermissionRequest` |
| Authorization | Manual permission check | AuthorizedOrganisationPlugin |
| Operation | Create/update permissions | Delete permissions |
| Non-existent users | Throws NotFoundException | Returns in `not_found_emails` |
| Response | Plain text "Permissions granted" | JSON with counts |
| Idempotency | Updates existing permissions | No-op for missing permissions |
| Self-operation | Allowed | Blocked if last editor (FR-013) |

**Key Difference**: Revoke handles non-existent users gracefully (partial success) while grant throws errors. This aligns with the specification requirement (FR-008a) and provides better UX for bulk operations.
