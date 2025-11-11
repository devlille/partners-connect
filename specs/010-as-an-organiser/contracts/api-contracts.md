# API Contracts

## Enhanced Agenda Import Endpoint

### POST /orgs/{orgSlug}/events/{eventSlug}/agenda
**Purpose**: Import agenda data from OpenPlanner (enhanced error handling)

**Request**:
- Method: POST
- Path: `/orgs/{orgSlug}/events/{eventSlug}/agenda`
- Headers: `Authorization: Bearer {jwt_token}`
- Body: Empty

**Responses**:
- **201 Created**: Agenda imported successfully
- **503 Service Unavailable**: OpenPlanner API error (no database impact)
- **404 Not Found**: Event not found or no OpenPlanner integration
- **401 Unauthorized**: Invalid or missing JWT token
- **403 Forbidden**: User lacks edit permission for organization

### GET /orgs/{orgSlug}/events/{eventSlug}/agenda
**Purpose**: Retrieve imported agenda data (sessions and speakers)

**Request**:
- Method: GET
- Path: `/orgs/{orgSlug}/events/{eventSlug}/agenda`
- Headers: `Authorization: Bearer {jwt_token}` (optional - public endpoint)
- Body: None

**Response Schemas**:
- **200 OK**: Agenda data retrieved successfully
  ```json
  {
    "sessions": [
      {
        "id": "uuid",
        "name": "Introduction to Kotlin",
        "abstract": "Getting started with Kotlin programming",
        "start_time": "2025-11-11T10:00:00Z",
        "end_time": "2025-11-11T11:00:00Z",
        "track_name": "Development Track",
        "language": "en"
      }
    ],
    "speakers": [
      {
        "id": "speaker-uuid-1",
        "name": "Jane Doe",
        "biography": "Kotlin expert with 5 years experience",
        "job_title": "Senior Developer",
        "photo_url": "https://example.com/photo.jpg",
        "pronouns": "she/her"
      }
    ]
  }
  ```
- **404 Not Found**: Event not found or no agenda imported
- **503 Service Unavailable**: Database error

## New Partnership Speaker Endpoints

### POST /partnerships/{partnershipId}/speakers/{speakerId}
**Purpose**: Attach a speaker to a partnership

**Request**:
- Method: POST  
- Path: `/partnerships/{partnershipId}/speakers/{speakerId}`
- Headers: `Content-Type: application/json`
- Body: `{}` (empty JSON object)

**Response Schemas**:
- **201 Created**: Speaker attached successfully
  ```json
  {
    "id": "uuid",
    "speaker_id": "uuid", 
    "partnership_id": "uuid",
    "created_at": "2025-11-11T10:30:00Z"
  }
  ```
- **409 Conflict**: Speaker already attached to this partnership
- **403 Forbidden**: Partnership not approved by organizers
- **404 Not Found**: Speaker or partnership not found
- **400 Bad Request**: Speaker not from partnership's event

### DELETE /partnerships/{partnershipId}/speakers/{speakerId}
**Purpose**: Remove speaker from partnership

**Request**:
- Method: DELETE
- Path: `/partnerships/{partnershipId}/speakers/{speakerId}`
- Headers: None required

**Responses**:
- **204 No Content**: Speaker detached successfully
- **404 Not Found**: Association not found
- **404 Not Found**: Partnership not found

## OpenAPI Schema References

### Request Schemas
- No additional request schemas needed (POST uses empty body, DELETE uses no body)

### Response Schemas  
- `speaker_partnership_response.schema.json`: Speaker attachment response (201 Created)
- `partnership_detail.schema.json`: Enhanced partnership details including speakers (for future implementation)
- `agenda_response.schema.json`: Complete agenda response with sessions and speakers (200 OK)
- `session.schema.json`: Individual session schema (used in agenda response)
- `speaker.schema.json`: Individual speaker schema (used in agenda response)
- `error_response.schema.json`: Standard error format (existing)

## HTTP Status Code Patterns

### Success Responses
- **201 Created**: Resource created (speaker attached)
- **200 OK**: Resource retrieved (lists, details)
- **204 No Content**: Resource deleted (speaker detached)

### Client Error Responses
- **400 Bad Request**: Invalid request format or business rule violation
- **401 Unauthorized**: Missing or invalid authentication
- **403 Forbidden**: Valid auth but insufficient permissions  
- **404 Not Found**: Resource does not exist
- **409 Conflict**: Resource already exists (duplicate attachment)

### Server Error Responses
- **503 Service Unavailable**: External service failure (OpenPlanner API)