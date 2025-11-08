# API Contract: GET Partnership Details

## Endpoint Specification

**Path**: `GET /events/{eventSlug}/partnerships/{partnershipId}`  
**Method**: GET  
**Authentication**: None (completely public endpoint)  
**Content-Type**: application/json  

## Request Parameters

### Path Parameters
- **eventSlug** (string, required): URL slug of the event (alphanumeric with hyphens)
- **partnershipId** (string, required): UUID of the partnership

### Query Parameters
None

### Headers
- **Accept**: application/json (optional, defaults to JSON)
- **Accept-Language**: Not required (public endpoint)

## Response Specifications

### Success Response (200 OK)

**Content-Type**: application/json

**Schema Structure**:
```json
{
  "partnership": {
    "id": "uuid",
    "phone": "string|null",
    "contact_name": "string", 
    "contact_role": "string",
    "language": "string",
    "emails": ["string"],
    "selected_pack": {
      "id": "string",
      "name": "string", 
      "base_price": "number",
      "max_quantity": "number|null",
      "required_options": [],
      "optional_options": []
    } | null,
    "suggestion_pack": {
      "id": "string",
      "name": "string",
      "base_price": "number", 
      "max_quantity": "number|null",
      "required_options": [],
      "optional_options": []
    } | null,
    "validated_pack": {
      "id": "string",
      "name": "string",
      "base_price": "number", 
      "max_quantity": "number|null",
      "required_options": [],
      "optional_options": []
    } | null,
    "process_status": {
      "suggestion_sent_at": "string|null",
      "suggestion_approved_at": "string|null",
      "suggestion_declined_at": "string|null",
      "validated_at": "string|null",
      "declined_at": "string|null",
      "agreement_url": "string|null",
      "agreement_signed_url": "string|null",
      "communication_publication_date": "string|null",
      "communication_support_url": "string|null",
      "billing_status": "string|null",
      "current_stage": "string"
    },
    "created_at": "string",
    "updated_at": "string"
  },
  "company": {
    "id": "string",
    "name": "string",
    "siret": "string",
    "vat": "string", 
    "site_url": "string",
    "description": "string|null",
    "head_office": {
      "address": "string",
      "city": "string", 
      "zip_code": "string",
      "country": "string"
    },
    "medias": {
      "original": "string",
      "png_1000": "string",
      "png_500": "string",
      "png_250": "string"
    } | null,
    "status": "string"
  },
  "event": {
    "event": {
      "slug": "string",
      "name": "string",
      "start_time": "string",
      "end_time": "string", 
      "submission_start_time": "string",
      "submission_end_time": "string",
      "address": "string",
      "contact": {
        "email": "string",
        "phone": "string|null"
      },
      "external_links": [
        {
          "id": "string",
          "name": "string",
          "url": "string"
        }
      ],
      "providers": [
        {
          "id": "string",
          "name": "string",
          "type": "string",
          "website": "string|null",
          "phone": "string|null",
          "email": "string|null",
          "created_at": "string"
        }
      ]
    },
    "organisation": {
      "name": "string",
      "slug": "string", 
      "head_office": "string",
      "owner": {
        "display_name": "string",
        "email": "string"
      } | null
    }
  }
}
```

### Error Responses

#### 400 Bad Request
Invalid partnership ID format (not a valid UUID)

```json
{
  "message": "Bad Request",
  "stack": null
}
```

#### 404 Not Found  
Partnership not found, event not found, or partnership doesn't belong to event

```json
{
  "message": "404 Not Found", 
  "stack": null
}
```

#### 500 Internal Server Error
Server error during processing

```json
{
  "message": "Internal Server Error",
  "stack": "string|null"
}
```

## OpenAPI Integration

### Schema Reference
The detailed response schema should be defined as external JSON schema with proper component references:
- **File**: `server/application/src/main/resources/schemas/detailed_partnership_response.schema.json`
- **Component Reference**: Uses existing schemas for company, event, and sponsoring models

### Components Definition
```yaml
components:
  schemas:
    DetailedPartnershipResponse:
      $ref: "../schemas/detailed_partnership_response.schema.json"
    Company:
      $ref: "../schemas/company.schema.json"
    EventWithOrganisation:
      $ref: "../schemas/event_with_organisation.schema.json"
    SponsoringPack:
      $ref: "../schemas/sponsoring_pack.schema.json"
    SponsoringOption:
      $ref: "../schemas/sponsoring_option.schema.json"
    ErrorResponse:
      $ref: "../schemas/error_response.schema.json"
```

### Schema Implementation Notes
The `detailed_partnership_response.schema.json` will be created during implementation with:
- **Partnership object**: New schema for PartnershipDetail model
- **Company object**: Reference to existing `company.schema.json`
- **Event object**: Reference to existing `event_with_organisation.schema.json`
- **Sponsoring objects**: Reference to existing `sponsoring_pack.schema.json` and `sponsoring_option.schema.json`

**Kotlin Model Requirements**:
- Use **PascalCase** for Kotlin property names (e.g., `contactName`, `processStatus`)
- Use **@SerialName** annotations to map to snake_case JSON fields (e.g., `@SerialName("contact_name")`)
- Use **@SerialName** on enum values to map to lowercase JSON (e.g., `@SerialName("pending")` for `PENDING`)
- Follow existing patterns in `Company.kt` and `EventWithOrganisation.kt` models

### JSON Schema Content
The following schema should be created at `server/application/src/main/resources/schemas/detailed_partnership_response.schema.json`:

```json
{
  "$schema": "http://json-schema.org/draft-07/schema#",
  "$id": "detailed_partnership_response.schema.json",
  "type": "object",
  "title": "Detailed Partnership Response",
  "description": "Complete partnership information including company, event, and partnership details with process status",
  "required": ["partnership", "company", "event"],
  "properties": {
    "partnership": {
      "type": "object",
      "title": "Partnership Detail",
      "description": "Enhanced partnership information with process status and timestamps",
      "required": [
        "id",
        "contact_name",
        "contact_role", 
        "language",
        "emails",
        "process_status",
        "created_at",
        "updated_at"
      ],
      "properties": {
        "id": {
          "type": "string",
          "format": "uuid",
          "description": "Partnership UUID identifier"
        },
        "phone": {
          "type": ["string", "null"],
          "description": "Partnership contact phone number"
        },
        "contact_name": {
          "type": "string",
          "description": "Partnership contact person name"
        },
        "contact_role": {
          "type": "string", 
          "description": "Partnership contact person role"
        },
        "language": {
          "type": "string",
          "description": "Partnership language preference"
        },
        "emails": {
          "type": "array",
          "items": {
            "type": "string",
            "format": "email"
          },
          "description": "Partnership contact email addresses"
        },
        "selected_pack": {
          "anyOf": [
            {"$ref": "sponsoring_pack.schema.json"},
            {"type": "null"}
          ],
          "description": "Currently selected sponsoring package"
        },
        "suggestion_pack": {
          "anyOf": [
            {"$ref": "sponsoring_pack.schema.json"},
            {"type": "null"}
          ],
          "description": "Suggested sponsoring package"
        },
        "validated_pack": {
          "anyOf": [
            {"$ref": "sponsoring_pack.schema.json"},
            {"type": "null"}
          ],
          "description": "Validated sponsoring package"
        },
        "process_status": {
          "$ref": "#/definitions/PartnershipProcessStatus",
          "description": "Detailed workflow status with timestamps"
        },
        "created_at": {
          "type": "string",
          "format": "date-time",
          "description": "Partnership creation timestamp (ISO 8601)"
        },
        "updated_at": {
          "type": "string", 
          "format": "date-time",
          "description": "Partnership last update timestamp (ISO 8601)"
        }
      }
    },
    "company": {
      "$ref": "company.schema.json",
      "description": "Complete company information using existing Company domain model"
    },
    "event": {
      "$ref": "event_with_organisation.schema.json",
      "description": "Complete event and organisation information using existing EventWithOrganisation domain model"
    }
  },
  "definitions": {
    "PartnershipProcessStatus": {
      "type": "object",
      "title": "Partnership Process Status",
      "description": "Detailed workflow status tracking with timestamps for all process phases",
      "required": ["current_stage"],
      "properties": {
        "suggestion_sent_at": {
          "type": ["string", "null"],
          "format": "date-time",
          "description": "When sponsoring package suggestion was sent (ISO 8601)"
        },
        "suggestion_approved_at": {
          "type": ["string", "null"], 
          "format": "date-time",
          "description": "When suggestion was approved by company (ISO 8601)"
        },
        "suggestion_declined_at": {
          "type": ["string", "null"],
          "format": "date-time", 
          "description": "When suggestion was declined by company (ISO 8601)"
        },
        "validated_at": {
          "type": ["string", "null"],
          "format": "date-time",
          "description": "When partnership was validated by organizer (ISO 8601)"
        },
        "declined_at": {
          "type": ["string", "null"],
          "format": "date-time",
          "description": "When partnership was declined by organizer (ISO 8601)"
        },
        "agreement_url": {
          "type": ["string", "null"],
          "format": "uri",
          "description": "Generated partnership agreement document URL"
        },
        "agreement_signed_url": {
          "type": ["string", "null"],
          "format": "uri",
          "description": "Signed partnership agreement document URL"
        },
        "communication_publication_date": {
          "type": ["string", "null"],
          "format": "date-time",
          "description": "When partnership communication was published (ISO 8601)"
        },
        "communication_support_url": {
          "type": ["string", "null"],
          "format": "uri", 
          "description": "Partnership communication support materials URL"
        },
        "billing_status": {
          "type": ["string", "null"],
          "enum": ["pending", "sent", "paid", null],
          "description": "Current billing/payment status from BillingsTable (pending, sent, paid)"
        },
        "current_stage": {
          "type": "string",
          "enum": [
            "created",
            "suggestion_sent", 
            "suggestion_approved",
            "suggestion_declined",
            "validated",
            "declined",
            "agreement_generated",
            "agreement_signed", 
            "paid",
            "communication_published"
          ],
          "description": "Derived current workflow stage"
        }
      }
    }
  }
}
```

### Operation Definition
```yaml
paths:
  /events/{eventSlug}/partnerships/{partnershipId}:
    get:
      operationId: getPartnershipDetails
      summary: Get detailed partnership information
      description: Retrieve comprehensive partnership details including company, event, and process status information
      security:
        - {} # Public endpoint
      parameters:
        - name: eventSlug
          in: path
          required: true
          schema:
            type: string
            pattern: "^[a-zA-Z0-9-]+$"
          description: Event URL slug
        - name: partnershipId
          in: path
          required: true
          schema:
            type: string
            format: uuid
          description: Partnership UUID
      responses:
        '200':
          description: Partnership details retrieved successfully
          content:
            application/json:
              schema:
                $ref: "#/components/schemas/DetailedPartnershipResponse"
        '400':
          description: Invalid request parameters
          content:
            application/json:
              schema:
                $ref: "#/components/schemas/ErrorResponse"
        '404':
          description: Partnership or event not found
          content:
            application/json:
              schema:
                $ref: "#/components/schemas/ErrorResponse"
        '500':
          description: Internal server error
          content:
            application/json:
              schema:
                $ref: "#/components/schemas/ErrorResponse"
```

## Contract Test Scenarios

### Happy Path Tests
1. **Valid Partnership Retrieval**
   - Given: Valid eventSlug and partnershipId  
   - When: GET request to endpoint
   - Then: 200 response with complete nested structure
   - Validate: All required fields present, proper JSON structure

2. **Partnership with Minimal Data**
   - Given: Partnership with minimal required fields
   - When: GET request to endpoint
   - Then: 200 response with null optional fields
   - Validate: Schema compliance with nullable fields

3. **Partnership with Full Process Status**
   - Given: Partnership with complete workflow timestamps
   - When: GET request to endpoint  
   - Then: 200 response with all process status fields
   - Validate: Timestamp format and currentStage derivation

### Error Path Tests
1. **Invalid Partnership UUID**
   - Given: Malformed partnershipId (not UUID)
   - When: GET request to endpoint
   - Then: 400 Bad Request response
   - Validate: Error message format consistency

2. **Non-existent Partnership**
   - Given: Valid UUID that doesn't exist
   - When: GET request to endpoint
   - Then: 404 Not Found response
   - Validate: Error response schema

3. **Non-existent Event Slug**  
   - Given: Invalid eventSlug
   - When: GET request to endpoint
   - Then: 404 Not Found response
   - Validate: Error handling consistency

4. **Partnership-Event Mismatch**
   - Given: Valid partnership and event that aren't associated
   - When: GET request to endpoint
   - Then: 404 Not Found response
   - Validate: Proper association validation

### Schema Validation Tests
1. **Response Schema Compliance**
   - Validate all responses match JSON schema exactly
   - Test nullable field handling
   - Verify nested object structure
   - Check array field formats

2. **Data Type Validation**
   - UUID format validation for ID fields
   - ISO 8601 datetime format for timestamps
   - URL format for URL fields
   - Email format for contact emails

## Implementation Requirements

### Request Handling
- Use `call.parameters.eventSlug` and `call.parameters.partnershipId.toUUID()` for parameter extraction
- Leverage existing StatusPages for automatic error response formatting
- No manual try-catch blocks needed in route handler

### Response Generation  
- Use new `getByIdDetailed()` repository method
- Apply mappers for entity-to-response transformation
- Return typed response via `call.respond(HttpStatusCode.OK, response)`

### Validation Strategy
- Parameter validation automatic via Ktor parameter extraction
- UUID validation via `.toUUID()` extension (throws on invalid format)
- Business validation in repository layer (entity existence, association)
- JSON schema validation for response structure consistency