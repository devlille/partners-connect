# API Contract: Partnership List with Organiser Filter

**Endpoint**: `GET /orgs/{orgSlug}/events/{eventSlug}/partnerships`  
**Feature**: Filter Partnerships by Assigned Organiser  
**Date**: December 29, 2025

---

## Overview

This contract specifies the partnership list endpoint with the new `filter[organiser]` query parameter and enhanced pagination metadata containing available filters (including organisers list) and sorts arrays.

---

## Request Specification

### HTTP Method
`GET`

### URL Pattern
`/orgs/{orgSlug}/events/{eventSlug}/partnerships`

### Path Parameters

| Parameter | Type | Required | Description |
|-----------|------|----------|-------------|
| `orgSlug` | string | Yes | Organisation identifier |
| `eventSlug` | string | Yes | Event identifier |

### Query Parameters

| Parameter | Type | Required | Description | Example |
|-----------|------|----------|-------------|---------|
| `filter[pack_id]` | string (UUID) | No | Filter by sponsoring pack ID | `123e4567-e89b-12d3-a456-426614174000` |
| `filter[validated]` | boolean | No | Filter by validation status | `true`, `false` |
| `filter[suggestion]` | boolean | No | Filter by suggestion status | `true`, `false` |
| `filter[paid]` | boolean | No | Filter by payment status | `true`, `false` |
| `filter[agreement-generated]` | boolean | No | Filter by agreement generation status | `true`, `false` |
| `filter[agreement-signed]` | boolean | No | Filter by agreement signature status | `true`, `false` |
| **`filter[organiser]`** | string | No | **NEW**: Filter by assigned organiser email (case-insensitive) | `john.doe@example.com` |
| `direction` | string | No | Sort direction: "asc" or "desc" | `asc` (default) |
| `page` | integer | No | Page number (1-based) | `1` (default) |
| `page_size` | integer | No | Items per page | `20` (default) |

### Headers

```http
Authorization: Bearer <jwt_token>
Accept: application/json
```

### Request Example

```http
GET /orgs/devlille/events/2025/partnerships?filter[organiser]=john.doe@example.com&filter[validated]=true&page=1&page_size=20 HTTP/1.1
Host: api.partners-connect.com
Authorization: Bearer eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9...
Accept: application/json
```

---

## Response Specification

### Success Response (200 OK)

**Content-Type**: `application/json`

#### Response Body Structure

```json
{
  "items": [
    {
      "id": "uuid",
      "company": {
        "id": "uuid",
        "name": "string",
        "address": "string",
        "city": "string",
        "postal_code": "string"
      },
      "organiser": {
        "email": "string",
        "display_name": "string",
        "picture_url": "string"
      },
      "validated_at": "datetime",
      "created_at": "datetime",
      "suggestion_pack": { ... },
      "validated_pack": { ... }
    }
  ],
  "page": 1,
  "page_size": 20,
  "total": 5,
  "metadata": {
    "filters": [
      {
        "name": "pack_id",
        "type": "string"
      },
      {
        "name": "validated",
        "type": "boolean"
      },
      {
        "name": "suggestion",
        "type": "boolean"
      },
      {
        "name": "paid",
        "type": "boolean"
      },
      {
        "name": "agreement-generated",
        "type": "boolean"
      },
      {
        "name": "agreement-signed",
        "type": "boolean"
      },
      {
        "name": "organiser",
        "type": "string",
        "values": [
          {
            "value": "john.doe@example.com",
            "display_value": "John Doe"
          },
          {
            "value": "jane.smith@example.com",
            "display_value": "Jane Smith"
          }
        ]
      }
    ],
    "sorts": ["created", "validated"]
  }
}
```

#### Field Descriptions

**Root Level**:
- `items`: Array of partnership objects matching filter criteria
- `page`: Current page number (1-based)
- `page_size`: Number of items per page
- `total`: Total count of partnerships matching all filters
- `metadata`: **NEW** - Pagination metadata with available filters and sorts

**Metadata Object**:
- `filters`: Array of filter definitions describing all available filters
- `sorts`: Array of sortable field names

**FilterDefinition Object**:
- `name`: Filter parameter name (without "filter[]" prefix)
- `type`: Data type ("string" or "boolean")
- `values`: Optional array of valid options (only for "organiser" filter)

**FilterValue Object** (organiser filter only):
- `value`: Filter value (e.g., organiser's email address)
- `display_value`: Human-readable display name (e.g., full name)

### Response Examples

#### Example 1: Successful Filter with Results

**Request**:
```http
GET /orgs/devlille/events/2025/partnerships?filter[organiser]=john.doe@example.com
```

**Response** (200 OK):
```json
{
  "items": [
    {
      "id": "123e4567-e89b-12d3-a456-426614174000",
      "company": {
        "id": "456e4567-e89b-12d3-a456-426614174000",
        "name": "Acme Corp",
        "address": "123 Main St",
        "city": "Lille",
        "postal_code": "59000"
      },
      "organiser": {
        "email": "john.doe@example.com",
        "display_name": "John Doe",
        "picture_url": "https://example.com/john.jpg"
      },
      "validated_at": "2025-12-01T10:00:00Z",
      "created_at": "2025-11-15T09:30:00Z",
      "suggestion_pack": null,
      "validated_pack": {
        "id": "789e4567-e89b-12d3-a456-426614174000",
        "name": "Gold Sponsor",
        "price": 5000
      }
    },
    {
      "id": "234e4567-e89b-12d3-a456-426614174000",
      "company": {
        "id": "567e4567-e89b-12d3-a456-426614174000",
        "name": "TechStartup Inc",
        "address": "456 Tech Ave",
        "city": "Lille",
        "postal_code": "59100"
      },
      "organiser": {
        "email": "john.doe@example.com",
        "display_name": "John Doe",
        "picture_url": "https://example.com/john.jpg"
      },
      "validated_at": null,
      "created_at": "2025-11-20T14:15:00Z",
      "suggestion_pack": {
        "id": "891e4567-e89b-12d3-a456-426614174000",
        "name": "Silver Sponsor",
        "price": 2500
      },
      "validated_pack": null
    }
  ],
  "page": 1,
  "page_size": 20,
  "total": 2,
  "metadata": {
    "filters": [
      { "name": "pack_id", "type": "string" },
      { "name": "validated", "type": "boolean" },
      { "name": "suggestion", "type": "boolean" },
      { "name": "paid", "type": "boolean" },
      { "name": "agreement-generated", "type": "boolean" },
      { "name": "agreement-signed", "type": "boolean" },
      {
        "name": "organiser",
        "type": "string",
        "values": [
          {
            "value": "john.doe@example.com",
            "display_value": "John Doe"
          },
          {
            "value": "jane.smith@example.com",
            "display_value": "Jane Smith"
          },
          {
            "value": "admin@example.com",
            "display_value": "Admin User"
          }
        ]
      }
    ],
    "sorts": ["created", "validated"]
  }
}
```

#### Example 2: Empty Result (No Matches)

**Request**:
```http
GET /orgs/devlille/events/2025/partnerships?filter[organiser]=nonexistent@example.com
```

**Response** (200 OK):
```json
{
  "items": [],
  "page": 1,
  "page_size": 20,
  "total": 0,
  "metadata": {
    "filters": [
      { "name": "pack_id", "type": "string" },
      { "name": "validated", "type": "boolean" },
      { "name": "suggestion", "type": "boolean" },
      { "name": "paid", "type": "boolean" },
      { "name": "agreement-generated", "type": "boolean" },
      { "name": "agreement-signed", "type": "boolean" },
      {
        "name": "organiser",
        "type": "string",
        "values": [
          {
            "value": "john.doe@example.com",
            "display_value": "John Doe"
          },
          {
            "value": "jane.smith@example.com",
            "display_value": "Jane Smith"
          }
        ]
      }
    ],
    "sorts": ["created", "validated"]
  }
}
```

#### Example 3: Combined Filters (AND Logic)

**Request**:
```http
GET /orgs/devlille/events/2025/partnerships?filter[organiser]=jane.smith@example.com&filter[validated]=true&filter[paid]=true
```

**Response** (200 OK):
```json
{
  "items": [
    {
      "id": "345e4567-e89b-12d3-a456-426614174000",
      "company": {
        "id": "678e4567-e89b-12d3-a456-426614174000",
        "name": "Enterprise Solutions Ltd",
        "address": "789 Business Blvd",
        "city": "Lille",
        "postal_code": "59000"
      },
      "organiser": {
        "email": "jane.smith@example.com",
        "display_name": "Jane Smith",
        "picture_url": "https://example.com/jane.jpg"
      },
      "validated_at": "2025-12-05T11:30:00Z",
      "created_at": "2025-11-10T08:00:00Z",
      "validated_pack": {
        "id": "912e4567-e89b-12d3-a456-426614174000",
        "name": "Platinum Sponsor",
        "price": 10000
      }
    }
  ],
  "page": 1,
  "page_size": 20,
  "total": 1,
  "metadata": {
    "filters": [
      { "name": "pack_id", "type": "string" },
      { "name": "validated", "type": "boolean" },
      { "name": "suggestion", "type": "boolean" },
      { "name": "paid", "type": "boolean" },
      { "name": "agreement-generated", "type": "boolean" },
      { "name": "agreement-signed", "type": "boolean" },
      {
        "name": "organiser",
        "type": "string",
        "values": [
          {
            "value": "john.doe@example.com",
            "display_value": "John Doe"
          },
          {
            "value": "jane.smith@example.com",
            "display_value": "Jane Smith"
          }
        ]
      }
    ],
    "sorts": ["created", "validated"]
  }
}
```

#### Example 4: No Available Organisers (Empty Values)

**Request**:
```http
GET /orgs/neworg/events/firstevent/partnerships
```

**Response** (200 OK):
```json
{
  "items": [
    {
      "id": "456e4567-e89b-12d3-a456-426614174000",
      "company": {
        "id": "789e4567-e89b-12d3-a456-426614174000",
        "name": "Solo Sponsor",
        "address": "101 Lone St",
        "city": "Lille",
        "postal_code": "59000"
      },
      "organiser": null,
      "validated_at": null,
      "created_at": "2025-12-01T10:00:00Z"
    }
  ],
  "page": 1,
  "page_size": 20,
  "total": 1,
  "metadata": {
    "filters": [
      { "name": "pack_id", "type": "string" },
      { "name": "validated", "type": "boolean" },
      { "name": "suggestion", "type": "boolean" },
      { "name": "paid", "type": "boolean" },
      { "name": "agreement-generated", "type": "boolean" },
      { "name": "agreement-signed", "type": "boolean" },
      {
        "name": "organiser",
        "type": "string",
        "values": []
      }
    ],
    "sorts": ["created", "validated"]
  }
}
```

---

## Error Responses

### 400 Bad Request

**Scenario**: Invalid query parameter format

```json
{
  "error": "Invalid filter parameter",
  "message": "filter[validated] must be a boolean value"
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

**Scenario**: User lacks view permissions for organisation

```json
{
  "error": "Forbidden",
  "message": "User does not have view permission for this organisation"
}
```

### 404 Not Found

**Scenario**: Organisation or event does not exist

```json
{
  "error": "Not Found",
  "message": "Event with slug '2025' not found"
}
```

---

## Business Rules

### Filter Application (AND Logic)

- All active filters combined with AND logic
- Only partnerships matching **ALL** criteria returned
- Organiser filter excludes partnerships with `null` organiser (FR-004)

### Case-Insensitive Email Matching

- Email comparison case-insensitive (FR-006)
- `filter[organiser]=John.Doe@example.com` matches `john.doe@example.com`

### Metadata Always Included

- Metadata present in every response (FR-005)
- Includes available organisers regardless of current filter

### Available Organisers List

- Only users with edit permissions on organisation (FR-009)
- Includes users with zero assigned partnerships (FR-010)
- Deduplicated by email address

---

## OpenAPI 3.1.0 Specification

```yaml
/orgs/{orgSlug}/events/{eventSlug}/partnerships:
  get:
    summary: Get partnership list
    operationId: getOrgsEventsPartnership
    description: List partnerships for an event with filtering options and pagination metadata
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
      
      - name: direction
        in: query
        required: false
        schema:
          type: string
          default: "asc"
          enum: ["asc", "desc"]
        description: Sort direction
      
      - name: page
        in: query
        required: false
        schema:
          type: integer
          default: 1
          minimum: 1
        description: Page number (1-based)
      
      - name: page_size
        in: query
        required: false
        schema:
          type: integer
          default: 20
          minimum: 1
          maximum: 100
        description: Items per page
    
    security:
      - bearerAuth: []
    
    responses:
      '200':
        description: OK - Partnership list with pagination metadata
        content:
          application/json:
            schema:
              $ref: '#/components/schemas/partnership_list_response.schema'
      
      '400':
        description: Bad Request - Invalid query parameter
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
        description: Not Found - Organisation or event not found
        content:
          application/json:
            schema:
              $ref: '#/components/schemas/error_response.schema'
```

---

## Validation Rules

### Query Parameter Validation

| Parameter | Validation Rule | Error Message |
|-----------|----------------|---------------|
| `filter[pack_id]` | Must be valid UUID format | "filter[pack_id] must be a valid UUID" |
| `filter[validated]` | Must be "true" or "false" | "filter[validated] must be a boolean value" |
| `filter[suggestion]` | Must be "true" or "false" | "filter[suggestion] must be a boolean value" |
| `filter[paid]` | Must be "true" or "false" | "filter[paid] must be a boolean value" |
| `filter[agreement-generated]` | Must be "true" or "false" | "filter[agreement-generated] must be a boolean value" |
| `filter[agreement-signed]` | Must be "true" or "false" | "filter[agreement-signed] must be a boolean value" |
| `filter[organiser]` | Any string (no format validation) | N/A |
| `direction` | Must be "asc" or "desc" | "direction must be 'asc' or 'desc'" |
| `page` | Must be positive integer | "page must be a positive integer" |
| `page_size` | Must be 1-100 | "page_size must be between 1 and 100" |

---

## Testing Scenarios

### Scenario 1: Filter by Organiser with Results

**Test**: Verify organiser filter returns only matching partnerships

```gherkin
Given a partnership assigned to "john.doe@example.com"
When I request partnerships with filter[organiser]=john.doe@example.com
Then response status is 200
And items array contains only partnerships assigned to that organiser
And metadata includes organiser in filters with values array
```

### Scenario 2: Case-Insensitive Email Matching

**Test**: Verify email comparison is case-insensitive

```gherkin
Given a partnership assigned to "john.doe@example.com"
When I request partnerships with filter[organiser]=John.Doe@Example.com
Then response status is 200
And items array contains the partnership
```

### Scenario 3: Combined Filters (AND Logic)

**Test**: Verify filters combined with AND logic

```gherkin
Given partnerships:
  | organiser | validated |
  | john@example.com | true |
  | john@example.com | false |
  | jane@example.com | true |
When I request partnerships with filter[organiser]=john@example.com&filter[validated]=true
Then response status is 200
And items array contains only the first partnership
```

### Scenario 4: No Matches (Empty Result)

**Test**: Verify empty result when no partnerships match

```gherkin
Given no partnerships assigned to "ghost@example.com"
When I request partnerships with filter[organiser]=ghost@example.com
Then response status is 200
And items array is empty
And total is 0
And metadata is still included
```

### Scenario 5: Metadata Always Included

**Test**: Verify metadata present in every response

```gherkin
Given a partnership list request (with or without filters)
When I receive the response
Then response includes metadata object
And metadata contains filters array
And metadata contains sorts array
```

### Scenario 6: Available Organisers List

**Test**: Verify organisers list populated correctly

```gherkin
Given organisation "devlille" has users:
  | email | name | canEdit |
  | john@example.com | John Doe | true |
  | jane@example.com | Jane Smith | true |
  | viewer@example.com | Viewer Only | false |
When I request partnerships for devlille event
Then metadata.filters includes organiser filter
And organiser.values contains 2 items (john and jane)
And viewer is excluded (canEdit=false)
```

### Scenario 7: Unassigned Partnerships Excluded

**Test**: Verify partnerships without organiser excluded when filter applied

```gherkin
Given partnerships:
  | id | organiser |
  | p1 | john@example.com |
  | p2 | null |
When I request partnerships with filter[organiser]=john@example.com
Then response status is 200
And items contains only p1
And p2 is excluded
```

---

## Performance Requirements

- **Response Time**: Sub-2-second for typical datasets (up to 1000 partnerships, 10-100 organisers)
- **Query Optimization**: Indexed FK on `organiser_user_id`, organisation permissions indexed
- **Metadata Overhead**: ~50-100ms additional query time for available organisers

---

## Changelog

| Version | Date | Changes |
|---------|------|---------|
| 1.0 | 2025-12-29 | Initial contract specification with organiser filter and metadata |
