# API Contracts: Complete CRUD Operations for Companies

## Overview
Extends the existing `/companies` resource with PUT (update) and DELETE (soft delete) operations, plus enhanced GET operation with status filtering.

## Endpoints

### 1. Update Company Information
**PUT** `/companies/{companyId}`

**Security**: Public endpoint (no authentication required)

**Request** (partial update, all fields optional):
```json
{
  "name": "Updated Company Name",
  "site_url": "https://updated-company.com",
  "head_office": {
    "address": "456 New Street",
    "city": "New City",
    "zip_code": "54321",
    "country": "FR"
  },
  "siret": "98765432109876",
  "vat": "FR98765432109",
  "description": "Updated company description",
  "socials": [
    {
      "type": "TWITTER",
      "url": "https://twitter.com/updated_company"
    }
  ]
}
```

**Response** (200 OK):
```json
{
  "id": "123e4567-e89b-12d3-a456-426614174000",
  "name": "Updated Company Name",
  "site_url": "https://updated-company.com",
  "head_office": {
    "address": "456 New Street",
    "city": "New City",
    "zip_code": "54321", 
    "country": "FR"
  },
  "siret": "98765432109876",
  "vat": "FR98765432109",
  "description": "Updated company description",
  "medias": {
    "original": "https://storage.example.com/original.svg",
    "png_1000": "https://storage.example.com/1000.png",
    "png_500": "https://storage.example.com/500.png",
    "png_250": "https://storage.example.com/250.png"
  },
  "status": "active"
}
```

**Error Responses**:
- **400 Bad Request**: Invalid data format or validation errors
- **404 Not Found**: Company with specified ID does not exist
- **409 Conflict**: SIRET or VAT conflict with another company

### 2. Soft Delete Company
**DELETE** `/companies/{companyId}`

**Security**: Public endpoint (no authentication required)

**Request**: No body required

**Response** (204 No Content): Empty response body

**Error Responses**:
- **404 Not Found**: Company with specified ID does not exist

### 3. List Companies with Status Filtering (Enhanced)
**GET** `/companies`

**Security**: Public endpoint (no authentication required)

**Query Parameters**:
- `query` (optional): Search term for company name
- `status` (optional): Filter by status (`active`, `inactive`). Default: show all companies
- `page` (optional): Page number (default: 1)
- `page_size` (optional): Items per page (default: 20, max: 100)

**Request Examples**:
```
GET /companies                           # All companies (default)
GET /companies?status=active             # Active companies only  
GET /companies?status=inactive           # Inactive companies only
GET /companies?query=tech&status=active  # Active companies matching "tech"
```

**Response** (200 OK):
```json
{
  "items": [
    {
      "id": "123e4567-e89b-12d3-a456-426614174000",
      "name": "Active Tech Company",
      "site_url": "https://active-tech.com",
      "head_office": {
        "address": "123 Tech Street",
        "city": "Paris",
        "zip_code": "75001",
        "country": "FR"
      },
      "siret": "12345678901234",
      "vat": "FR12345678901",
      "description": "Leading technology company",
      "medias": null,
      "status": "active"
    },
    {
      "id": "456e7890-e89b-12d3-a456-426614174001", 
      "name": "Inactive Company",
      "site_url": "https://inactive-company.com",
      "head_office": {
        "address": "789 Old Street",
        "city": "Lyon",
        "zip_code": "69001",
        "country": "FR"
      },
      "siret": "98765432109876",
      "vat": "FR98765432109",
      "description": "Company no longer active",
      "medias": null,
      "status": "inactive"
    }
  ],
  "page": 1,
  "page_size": 20,
  "total": 2
}
```

### 4. Get Company by ID (Unchanged)
**GET** `/companies/{companyId}`

**Enhancement**: Response now includes `status` field in company object.

## OpenAPI Schema Updates

### Path Parameters
```yaml
parameters:
  - name: companyId
    in: path
    required: true
    schema:
      type: string
      format: uuid
    description: Company unique identifier
```

### Query Parameters (Enhanced GET)
```yaml
parameters:
  - name: status
    in: query
    required: false
    schema:
      type: string
      enum: [active, inactive]
    description: Filter companies by status
  - name: query
    in: query  
    required: false
    schema:
      type: string
    description: Search term for company name
  - name: page
    in: query
    required: false
    schema:
      type: integer
      minimum: 1
      default: 1
    description: Page number
  - name: page_size
    in: query
    required: false
    schema:
      type: integer
      minimum: 1
      maximum: 100
      default: 20
    description: Items per page
```

### Request/Response Schemas

#### UpdateCompanySchema
```yaml
UpdateCompanySchema:
  type: object
  properties:
    name:
      type: string
      minLength: 1
      maxLength: 255
    site_url:
      type: string
      format: uri
    head_office:
      $ref: '#/components/schemas/AddressSchema'
    siret:
      type: string
      pattern: '^[0-9]{14}$'
    vat:
      type: string
      pattern: '^[A-Z]{2}[0-9A-Z]+$'
    description:
      type: [string, null]
      maxLength: 1000
    socials:
      type: array
      items:
        $ref: '#/components/schemas/SocialSchema'
  additionalProperties: false
```

#### CompanySchema (Enhanced)
```yaml
CompanySchema:
  type: object
  required: [id, name, head_office, siret, vat, site_url, status]
  properties:
    id:
      type: string
      format: uuid
    name:
      type: string
    site_url:
      type: string
      format: uri
    head_office:
      $ref: '#/components/schemas/AddressSchema'
    siret:
      type: string
    vat:
      type: string
    description:
      type: [string, null]
    medias:
      anyOf:
        - $ref: '#/components/schemas/MediaSchema'
        - type: null
    status:
      type: string
      enum: [active, inactive]
      description: Company lifecycle status
```

## Error Response Format

All endpoints return consistent error responses:

```json
{
  "error": "validation_failed",
  "message": "Request validation failed",
  "details": {
    "siret": "Invalid SIRET format - must be 14 digits",
    "vat": "Invalid VAT format for country FR"
  }
}
```

**Common Error Codes**:
- `validation_failed`: Request data validation errors
- `not_found`: Company ID not found
- `conflict`: Business rule violation (duplicate SIRET/VAT)
- `internal_error`: Server-side processing error

## Status Code Summary

| Endpoint | Success | Client Error | Server Error |
|----------|---------|--------------|--------------|
| PUT /companies/{id} | 200 OK | 400, 404, 409 | 500 |
| DELETE /companies/{id} | 204 No Content | 404 | 500 |
| GET /companies | 200 OK | 400 | 500 |
| GET /companies/{id} | 200 OK | 404 | 500 |

## Backwards Compatibility

### API Changes
- **GET /companies**: Returns companies with new `status` field (non-breaking addition)
- **GET /companies/{id}**: Returns company with new `status` field (non-breaking addition)
- **New endpoints**: PUT and DELETE operations are new functionality

### Client Impact
- Existing clients receive additional `status` field in responses (can be ignored)
- No breaking changes to existing functionality
- Optional new filtering capability available via query parameter

### Migration Path
1. Deploy backend changes with schema migration
2. Existing API calls continue to work unchanged
3. Clients can optionally adopt status filtering and update/delete operations
4. Frontend can display status information and implement management UI