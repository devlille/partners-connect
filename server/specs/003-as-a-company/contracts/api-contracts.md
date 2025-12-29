# API Contracts: Job Offers Management

## Overview
All endpoints follow the existing authentication and authorization patterns. Company ownership validation is required for all operations.

## Base Path
All job offer endpoints are namespaced under the company resource:
```
/companies/{companyId}/job-offers
```

## Endpoints

### 1. Create Job Offer
**POST** `/companies/{companyId}/job-offers`

**Request**:
```json
{
  "url": "https://example.com/jobs/senior-developer",
  "title": "Senior Software Developer",
  "location": "Paris, France",
  "publicationDate": "2025-10-15",
  "endDate": "2025-12-15",
  "experienceYears": 5,
  "salary": "60000-80000 EUR"
}
```

**Response** (201 Created):
```json
{
  "id": "550e8400-e29b-41d4-a716-446655440000"
}
```

**Error Responses**:
- 400 Bad Request: Invalid request data
- 401 Unauthorized: Authentication required
- 403 Forbidden: Not authorized for this company
- 404 Not Found: Company not found

### 2. List Job Offers for Company
**GET** `/companies/{companyId}/job-offers`

**Query Parameters**:
- `page` (optional): Page number, default 1
- `page_size` (optional): Items per page, default 20

**Response** (200 OK):
```json
{
  "items": [
    {
      "id": "550e8400-e29b-41d4-a716-446655440000",
      "companyId": "123e4567-e89b-12d3-a456-426614174000",
      "url": "https://example.com/jobs/senior-developer",
      "title": "Senior Software Developer",
      "location": "Paris, France",
      "publicationDate": "2025-10-15",
      "endDate": "2025-12-15",
      "experienceYears": 5,
      "salary": "60000-80000 EUR",
      "createdAt": "2025-10-15T10:30:00.000Z",
      "updatedAt": "2025-10-15T10:30:00.000Z"
    }
  ],
  "pagination": {
    "page": 1,
    "pageSize": 20,
    "totalItems": 1,
    "totalPages": 1
  }
}
```

### 3. Get Job Offer by ID
**GET** `/companies/{companyId}/job-offers/{jobOfferId}`

**Response** (200 OK):
```json
{
  "id": "550e8400-e29b-41d4-a716-446655440000",
  "companyId": "123e4567-e89b-12d3-a456-426614174000",
  "url": "https://example.com/jobs/senior-developer",
  "title": "Senior Software Developer",
  "location": "Paris, France",
  "publicationDate": "2025-10-15",
  "endDate": "2025-12-15",
  "experienceYears": 5,
  "salary": "60000-80000 EUR",
  "createdAt": "2025-10-15T10:30:00.000Z",
  "updatedAt": "2025-10-15T10:30:00.000Z"
}
```

**Error Responses**:
- 401 Unauthorized: Authentication required
- 403 Forbidden: Not authorized for this company
- 404 Not Found: Company or job offer not found

### 4. Update Job Offer
**PUT** `/companies/{companyId}/job-offers/{jobOfferId}`

**Request** (partial update, all fields optional):
```json
{
  "title": "Lead Software Developer",
  "salary": "70000-90000 EUR",
  "experienceYears": 7
}
```

**Response** (200 OK):
```json
{
  "id": "550e8400-e29b-41d4-a716-446655440000",
  "companyId": "123e4567-e89b-12d3-a456-426614174000",
  "url": "https://example.com/jobs/senior-developer",
  "title": "Lead Software Developer",
  "location": "Paris, France",
  "publicationDate": "2025-10-15",
  "endDate": "2025-12-15",
  "experienceYears": 7,
  "salary": "70000-90000 EUR",
  "createdAt": "2025-10-15T10:30:00.000Z",
  "updatedAt": "2025-10-16T14:20:00.000Z"
}
```

### 5. Delete Job Offer
**DELETE** `/companies/{companyId}/job-offers/{jobOfferId}`

**Response** (204 No Content): Empty body on successful deletion

**Error Responses**:
- 401 Unauthorized: Authentication required
- 403 Forbidden: Not authorized for this company
- 404 Not Found: Company or job offer not found

## Validation Rules

### Create Job Offer Validation
- `url`: Required, must be valid URI format, max 500 characters
- `title`: Required, non-empty, max 200 characters
- `location`: Required, non-empty, max 100 characters
- `publicationDate`: Required, valid date format (YYYY-MM-DD), not in future
- `endDate`: Optional, valid date format, must be after publicationDate
- `experienceYears`: Optional, integer between 1 and 20
- `salary`: Optional, string max 100 characters

### Update Job Offer Validation
- All fields optional (partial update)
- Same validation rules as create for provided fields
- Date consistency maintained (endDate after publicationDate)
- Cannot update `companyId`, `id`, `createdAt`

## Error Response Format
All errors follow the standard format:
```json
{
  "error": {
    "code": "VALIDATION_ERROR",
    "message": "Invalid request data",
    "details": [
      {
        "field": "publicationDate",
        "message": "Publication date cannot be in the future"
      }
    ]
  }
}
```

## Authentication & Authorization
- All endpoints require valid JWT authentication
- Company ownership validated by checking user permissions for the specified company
- Job offer ownership implicitly validated through company ownership
- Return 403 Forbidden if user doesn't own the company
- Return 404 Not Found if company doesn't exist or user has no access

## Rate Limiting
- Standard rate limiting applies (consistent with other company endpoints)
- 100 requests per minute per authenticated user
- 1000 requests per hour per company

## Caching Headers
- `Cache-Control: private, max-age=300` for GET responses
- `ETag` header included for individual job offer responses
- `If-None-Match` conditional requests supported