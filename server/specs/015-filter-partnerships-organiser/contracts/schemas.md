# JSON Schemas: Pagination Metadata

**Feature**: 015-filter-partnerships-organiser  
**Date**: December 29, 2025  
**Status**: Draft

---

## Overview

This document specifies the JSON schemas for pagination metadata structures added to the PaginatedResponse model.

---

## Schema Files

### 1. pagination_metadata.schema.json

**Location**: `application/src/main/resources/schemas/pagination_metadata.schema.json`

**Purpose**: Defines the structure of pagination metadata containing filters and sorts arrays.

```json
{
  "$schema": "http://json-schema.org/draft-07/schema#",
  "type": "object",
  "required": ["filters", "sorts"],
  "properties": {
    "filters": {
      "type": "array",
      "description": "Array of filter definitions describing all available filters for the endpoint",
      "items": {
        "$ref": "filter_definition.schema.json"
      }
    },
    "sorts": {
      "type": "array",
      "description": "Array of field names that can be used for sorting",
      "items": {
        "type": "string"
      },
      "examples": [
        ["created", "validated"],
        ["name", "created_at"]
      ]
    }
  },
  "additionalProperties": false
}
```

---

### 2. filter_definition.schema.json

**Location**: `application/src/main/resources/schemas/filter_definition.schema.json`

**Purpose**: Describes a single filter parameter with its type and optional values.

```json
{
  "$schema": "http://json-schema.org/draft-07/schema#",
  "type": "object",
  "required": ["name", "type"],
  "properties": {
    "name": {
      "type": "string",
      "description": "Filter parameter name (without 'filter[]' prefix)",
      "examples": ["pack_id", "validated", "organiser"]
    },
    "type": {
      "type": "string",
      "enum": ["string", "boolean"],
      "description": "Data type of the filter parameter (enum: 'string' or 'boolean')"
    },
    "values": {
      "oneOf": [
        {
          "type": "array",
          "description": "Optional array of valid options for the filter (only populated for organiser filter)",
          "items": {
            "$ref": "filter_value.schema.json"
          }
        },
        {
          "type": "null",
          "description": "No predefined values (user provides arbitrary value)"
        }
      ]
    }
  },
  "additionalProperties": false,
  "examples": [
    {
      "name": "validated",
      "type": "boolean"
    },
    {
      "name": "organiser",
      "type": "string",
      "values": [
        {
          "value": "john.doe@example.com",
          "display_value": "John Doe"
        }
      ]
    }
  ]
}
```

**Notes**:
- `values` is optional (can be `null` or omitted)
- Only the "organiser" filter includes a `values` array (per FR-010)
- Other filters (validated, paid, etc.) have `values: null`

---

### 3. filter_value.schema.json

**Location**: `application/src/main/resources/schemas/filter_value.schema.json`

**Purpose**: Represents a valid option for the organiser filter (user with edit permissions).

```json
{
  "$schema": "http://json-schema.org/draft-07/schema#",
  "type": "object",
  "required": ["value", "display_value"],
  "properties": {
    "value": {
      "type": "string",
      "description": "Filter value (e.g., email address for organiser filter)",
      "examples": ["john.doe@example.com", "jane.smith@example.com"]
    },
    "display_value": {
      "type": "string",
      "description": "Human-readable display name (e.g., full name for organiser filter)",
      "minLength": 1,
      "examples": ["John Doe", "Jane Smith"]
    }
  },
  "additionalProperties": false,
  "examples": [
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
```

---

### 4. partnership_list_response.schema.json (Updated)

**Location**: `application/src/main/resources/schemas/partnership_list_response.schema.json`

**Purpose**: Extends PaginatedResponse to include metadata field for partnership list endpoint.

**Note**: This may be a modification to existing `paginated_response.schema.json` or a new schema specific to partnership list.

```json
{
  "$schema": "http://json-schema.org/draft-07/schema#",
  "type": "object",
  "required": ["items", "page", "page_size", "total"],
  "properties": {
    "items": {
      "type": "array",
      "description": "Array of partnership items matching filter criteria",
      "items": {
        "$ref": "partnership_item.schema.json"
      }
    },
    "page": {
      "type": "integer",
      "minimum": 1,
      "description": "Current page number (1-based)"
    },
    "page_size": {
      "type": "integer",
      "minimum": 1,
      "description": "Number of items per page"
    },
    "total": {
      "type": "integer",
      "minimum": 0,
      "description": "Total count of items matching all filters"
    },
    "metadata": {
      "oneOf": [
        {
          "$ref": "pagination_metadata.schema.json"
        },
        {
          "type": "null"
        }
      ],
      "description": "Pagination metadata containing available filters and sorts (always populated but nullable for backwards compatibility)"
    }
  },
  "additionalProperties": false
}
```

**Changes from base PaginatedResponse**:
- Added `metadata` field with reference to `pagination_metadata.schema.json`
- Field is nullable for type safety but always populated in responses

---

## Usage in Routes

### Partnership List Endpoint

```kotlin
import io.ktor.server.request.receive

val schema = Schema.from(this::class.java.getResource("/schemas/partnership_list_response.schema.json"))

get("/partnerships") {
    val partnerships = repository.listByEvent(eventSlug, filters, direction)
    
    // Response automatically validated against schema
    call.respond(HttpStatusCode.OK, partnerships)
}
```

**Note**: Response validation happens automatically if configured in Ktor's serialization plugin.

---

## Schema Validation Rules

### Pagination Metadata

- **filters**: Must be an array (can be empty)
- **sorts**: Must be an array (can be empty)
- **filters[].name**: Required, non-empty string
- **filters[].type**: Required, must be "string" or "boolean"
- **filters[].values**: Optional, can be null or array of FilterValue

### Filter Value

- **email**: Required, valid email format
- **display_name**: Required, non-empty string (minimum length 1)

### Partnership List Response

- **items**: Required array (can be empty)
- **page**: Required positive integer (minimum 1)
- **page_size**: Required positive integer (minimum 1)
- **total**: Required non-negative integer (minimum 0)
- **metadata**: Optional (nullable) but always populated in actual responses

---

## Example JSON Documents

### Complete Response with Metadata

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
      "created_at": "2025-11-15T09:30:00Z"
    }
  ],
  "page": 1,
  "page_size": 20,
  "total": 1,
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

### Empty Organiser Values

```json
{
  "items": [],
  "page": 1,
  "page_size": 20,
  "total": 0,
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
        "name": "organiser",
        "type": "string",
        "values": []
      }
    ],
    "sorts": ["created"]
  }
}
```

### Null Metadata (Backwards Compatibility)

**Note**: In practice, metadata is always populated, but schema allows null for type safety.

```json
{
  "items": [
    {
      "id": "123e4567-e89b-12d3-a456-426614174000",
      "company": { ... },
      "organiser": null
    }
  ],
  "page": 1,
  "page_size": 20,
  "total": 1,
  "metadata": null
}
```

---

## OpenAPI Components Reference

### Adding to openapi.yaml

```yaml
components:
  schemas:
    PaginationMetadata:
      $ref: '../schemas/pagination_metadata.schema.json'
    
    FilterDefinition:
      $ref: '../schemas/filter_definition.schema.json'
    
    FilterValue:
      $ref: '../schemas/filter_value.schema.json'
    
    PartnershipListResponse:
      $ref: '../schemas/partnership_list_response.schema.json'
```

### Using in Endpoint Definition

```yaml
/orgs/{orgSlug}/events/{eventSlug}/partnerships:
  get:
    # ... parameters
    responses:
      '200':
        description: OK - Partnership list with pagination metadata
        content:
          application/json:
            schema:
              $ref: '#/components/schemas/PartnershipListResponse'
```

---

## Kotlin Data Class Mapping

### PaginatedResponse (Enhanced)

```kotlin
@Serializable
data class PaginatedResponse<T>(
    val items: List<T>,
    val page: Int,
    @SerialName("page_size")
    val pageSize: Int,
    val total: Long,
    val metadata: PaginationMetadata? = null,
)
```

### PaginationMetadata

```kotlin
@Serializable
data class PaginationMetadata(
    val filters: List<FilterDefinition>,
    val sorts: List<String>,
)
```

### FilterDefinition

```kotlin
@Serializable
data class FilterDefinition(
    val name: String,
    val type: String,
    val values: List<FilterValue>? = null,
)
```

### FilterValue

```kotlin
@Serializable
data class FilterValue(
    val value: String,
    @SerialName("display_value")
    val displayValue: String,
)
```

---

## Testing with JSON Schema

### Validation in Contract Tests

```kotlin
@Test
fun `GET partnerships returns response matching schema`() = testApplication {
    val userId = UUID.randomUUID()
    val orgId = UUID.randomUUID()
    val eventId = UUID.randomUUID()
    
    application {
        moduleSharedDb(userId = userId)
        transaction {
            insertMockedOrganisationEntity(orgId)
            insertMockedFutureEvent(eventId, orgId = orgId)
            insertMockedUser(userId, email = "john@example.com", name = "John Doe")
            insertMockedOrgaPermission(orgId, userId, canEdit = true)
            insertMockedPartnership(UUID.randomUUID(), eventId, organiserUserId = userId)
        }
    }
    
    val response = client.get("/orgs/${orgId}/events/${eventId}/partnerships") {
        header(HttpHeaders.Authorization, "Bearer valid")
    }
    
    assertEquals(HttpStatusCode.OK, response.status)
    
    val json = Json.parseToJsonElement(response.bodyAsText()).jsonObject
    
    // Verify metadata structure
    assertTrue(json.containsKey("metadata"))
    val metadata = json["metadata"]!!.jsonObject
    assertTrue(metadata.containsKey("filters"))
    assertTrue(metadata.containsKey("sorts"))
    
    // Verify filters array
    val filters = metadata["filters"]!!.jsonArray
    assertTrue(filters.isNotEmpty())
    
    // Verify organiser filter has values
    val organiserFilter = filters.first { 
        it.jsonObject["name"]?.jsonPrimitive?.content == "organiser" 
    }.jsonObject
    assertEquals("string", organiserFilter["type"]?.jsonPrimitive?.content)
    assertTrue(organiserFilter.containsKey("values"))
    
    val values = organiserFilter["values"]!!.jsonArray
    assertEquals(1, values.size)
    assertEquals("john@example.com", values[0].jsonObject["email"]?.jsonPrimitive?.content)
    assertEquals("John Doe", values[0].jsonObject["display_name"]?.jsonPrimitive?.content)
}
```

---

## Schema Maintenance

### Version Control

- All schemas tracked in `application/src/main/resources/schemas/`
- Schema changes require:
  1. Update JSON schema file
  2. Update Kotlin data class
  3. Update OpenAPI spec
  4. Update contract tests

### Backwards Compatibility

- **metadata** field nullable for type safety
- Existing API consumers can ignore new field
- Frontend can check for metadata presence and adapt UI

### Schema Validation

```bash
cd server
npm run validate  # Validates all schemas in openapi.yaml
```

---

## Changelog

| Version | Date | Schema File | Changes |
|---------|------|-------------|---------|
| 1.0 | 2025-12-29 | pagination_metadata.schema.json | Initial creation |
| 1.0 | 2025-12-29 | filter_definition.schema.json | Initial creation |
| 1.0 | 2025-12-29 | filter_value.schema.json | Initial creation |
| 1.0 | 2025-12-29 | partnership_list_response.schema.json | Added metadata field |
