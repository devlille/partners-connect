# API Contracts: Job Offer Promotion Endpoints

## Overview
This document defines all REST API endpoints for the job offer promotion workflow, including request/response formats, authentication requirements, and HTTP status codes. Contracts follow OpenAPI 3.1.0 specification and existing partners-connect API patterns.

---

## Company Domain Endpoints

### POST /companies/{companyId}/partnerships/{partnershipId}/promote

**Purpose**: Company owner promotes a job offer to an active partnership with an event.

**Authentication**: None (public endpoint)

**Authorization**: None required - company ownership validation handled internally via companyId match

**Path Parameters**:
| Parameter | Type | Required | Description |
|-----------|------|----------|-------------|
| companyId | UUID | Yes | Company unique identifier |
| partnershipId | UUID | Yes | Partnership unique identifier |

**Request Body**:
```json
{
  "job_offer_id": "550e8400-e29b-41d4-a716-446655440000"
}
```

**Schema**: References `promote_job_offer.schema.json`

**Success Response** (201 Created):
```json
{
  "id": "650e8400-e29b-41d4-a716-446655440000"
}
```

**Error Responses**:
- **400 Bad Request**: Invalid request body or missing required fields
  ```json
  {
    "error": "validation_error",
    "message": "job_offer_id is required",
    "field": "job_offer_id"
  }
  ```

- **403 Forbidden**: Event has ended
  ```json
  {
    "error": "forbidden",
    "message": "Cannot promote job offers after event has ended"
  }
  ```

- **404 Not Found**: Company, partnership, or job offer not found
  ```json
  {
    "error": "not_found",
    "message": "Job offer not found"
  }
  ```

- **409 Conflict**: Job offer already promoted and status is pending/approved (Note: declined can be re-promoted)
  ```json
  {
    "error": "conflict",
    "message": "Job offer already promoted to this partnership with status: pending"
  }
  ```

**Side Effects**:
- Creates new PromotedJobOffer record with status=PENDING
- OR updates existing DECLINED promotion to PENDING (re-promotion)
- Sends Mailjet notification to partnership contact emails
- Sends Slack notification to organization channel

**Operation ID**: `promoteJobOfferToPartnership`

**Tags**: `companies`, `job-offers`, `promotions`

---

### GET /companies/{companyId}/job-offers/{jobOfferId}/promotions

**Purpose**: List all promotions for a specific job offer across all partnerships.

**Authentication**: None (public endpoint)

**Authorization**: None required - company ownership validation handled internally via companyId match

**Path Parameters**:
| Parameter | Type | Required | Description |
|-----------|------|----------|-------------|
| companyId | UUID | Yes | Company unique identifier |
| jobOfferId | UUID | Yes | Job offer unique identifier |

**Query Parameters**:
| Parameter | Type | Required | Default | Description |
|-----------|------|----------|---------|-------------|
| partnership_id | UUID | No | (all) | Filter by partnership ID |
| page | integer | No | 1 | Page number for pagination |
| page_size | integer | No | 20 | Items per page |

**Success Response** (200 OK):
```json
{
  "items": [
    {
      "id": "650e8400-e29b-41d4-a716-446655440000",
      "job_offer_id": "550e8400-e29b-41d4-a716-446655440000",
      "partnership_id": "750e8400-e29b-41d4-a716-446655440000",
      "event_slug": "devlille-2025",
      "status": "pending",
      "promoted_at": "2025-10-18T10:00:00Z",
      "reviewed_at": null,
      "reviewed_by": null,
      "job_offer": {
        "id": "550e8400-e29b-41d4-a716-446655440000",
        "title": "Senior Backend Developer",
        "url": "https://example.com/jobs/senior-backend-dev",
        "location": "Remote",
        "publication_date": "2025-10-15T00:00:00Z",
        "end_date": "2025-11-15T23:59:59Z",
        "experience_years": 5,
        "salary": "70k-90k EUR",
        "company_id": "450e8400-e29b-41d4-a716-446655440000",
        "created_at": "2025-10-15T08:30:00Z",
        "updated_at": "2025-10-15T08:30:00Z"
      },
      "created_at": "2025-10-18T10:00:00Z",
      "updated_at": "2025-10-18T10:00:00Z"
    }
  ],
  "total": 1,
  "page": 1,
  "page_size": 20,
  "total_pages": 1
}
```

**Schema**: PaginatedResponse<JobOfferPromotionResponse>

**Error Responses**:
- **404 Not Found**: Company or job offer not found

**Operation ID**: `listJobOfferPromotions`

**Tags**: `companies`, `job-offers`, `promotions`

---

## Partnership Domain Endpoints

### GET /orgs/{orgSlug}/events/{eventSlug}/partnerships/{partnershipId}/job-offers

**Purpose**: List all promoted job offers for a specific partnership.

**Authentication**: None (public endpoint)

**Authorization**: None required - partnership existence validated, data accessible to anyone with partnership ID

**Path Parameters**:
| Parameter | Type | Required | Description |
|-----------|------|----------|-------------|
| orgSlug | string | Yes | Organization slug |
| eventSlug | string | Yes | Event slug |
| partnershipId | UUID | Yes | Partnership unique identifier |

**Query Parameters**:
| Parameter | Type | Required | Default | Description |
|-----------|------|----------|---------|-------------|
| status | string enum | No | (all) | Filter by status: pending, approved, declined |
| page | integer | No | 1 | Page number |
| page_size | integer | No | 20 | Items per page |

**Success Response** (200 OK):
```json
{
  "items": [...],  // Array of JobOfferPromotionResponse
  "total": 5,
  "page": 1,
  "page_size": 20,
  "total_pages": 1
}
```

**Error Responses**:
- **403 Forbidden**: User doesn't own company
- **404 Not Found**: Organization, event, or partnership not found

**Operation ID**: `listPartnershipJobOffers`

**Tags**: `partnerships`, `job-offers`, `promotions`

---

### GET /orgs/{orgSlug}/events/{eventSlug}/job-offers

**Purpose**: Event organizer lists all promoted job offers for the event across all partnerships.

**Authentication**: Required (bearerAuth - JWT token)

**Authorization**: User must have event organization edit permission (canEdit=true)

**Path Parameters**:
| Parameter | Type | Required | Description |
|-----------|------|----------|-------------|
| orgSlug | string | Yes | Organization slug |
| eventSlug | string | Yes | Event slug |

**Query Parameters**:
| Parameter | Type | Required | Default | Description |
|-----------|------|----------|---------|-------------|
| status | string enum | No | (all) | Filter by status: pending, approved, declined |
| page | integer | No | 1 | Page number |
| page_size | integer | No | 20 | Items per page |

**Success Response** (200 OK):
```json
{
  "items": [
    {
      "id": "650e8400-e29b-41d4-a716-446655440000",
      "job_offer_id": "550e8400-e29b-41d4-a716-446655440000",
      "partnership_id": "750e8400-e29b-41d4-a716-446655440000",
      "event_slug": "devlille-2025",
      "status": "approved",
      "promoted_at": "2025-10-18T10:00:00Z",
      "reviewed_at": "2025-10-18T14:30:00Z",
      "reviewed_by": "950e8400-e29b-41d4-a716-446655440000",
      "job_offer": {
        "id": "550e8400-e29b-41d4-a716-446655440000",
        "title": "Senior Backend Developer",
        "url": "https://example.com/jobs/senior-backend-dev",
        "location": "Remote",
        "publication_date": "2025-10-15T00:00:00Z",
        "end_date": "2025-11-15T23:59:59Z",
        "experience_years": 5,
        "salary": "70k-90k EUR",
        "company_id": "450e8400-e29b-41d4-a716-446655440000",
        "created_at": "2025-10-15T08:30:00Z",
        "updated_at": "2025-10-15T08:30:00Z"
      },
      "created_at": "2025-10-18T10:00:00Z",
      "updated_at": "2025-10-18T14:30:00Z"
    }
  ],
  "total": 10,
  "page": 1,
  "page_size": 20,
  "total_pages": 1
}
```

**Error Responses**:
- **403 Forbidden**: User lacks event edit permission
- **404 Not Found**: Organization or event not found

**Operation ID**: `listEventJobOfferPromotions`

**Tags**: `events`, `job-offers`, `promotions`

---

### POST /orgs/{orgSlug}/events/{eventSlug}/partnerships/{partnershipId}/job-offers/{promotionId}/approve

**Purpose**: Event organizer approves a pending job offer promotion.

**Authentication**: Required (bearerAuth - JWT token)

**Authorization**: AuthorizedOrganisationPlugin automatically validates user has canEdit=true permission for the event's organization

**Route Protection**:
```kotlin
route("/orgs/{orgSlug}/events/{eventSlug}/partnership/{partnershipId}/job-offers/promotions/{promotionId}") {
    install(AuthorizedOrganisationPlugin)  // Automatic JWT + permission validation
    post("/approve") { /* ... */ }
}
```

**Path Parameters**:
| Parameter | Type | Required | Description |
|-----------|------|----------|-------------|
| orgSlug | string | Yes | Organization slug |
| eventSlug | string | Yes | Event slug |
| partnershipId | UUID | Yes | Partnership unique identifier |
| promotionId | UUID | Yes | Promotion unique identifier |

**Request Body**: Empty JSON object `{}`

**Schema**: References `approve_job_offer_promotion.schema.json`

**Success Response** (200 OK):
```json
{
  "id": "650e8400-e29b-41d4-a716-446655440000",
  "job_offer_id": "550e8400-e29b-41d4-a716-446655440000",
  "partnership_id": "750e8400-e29b-41d4-a716-446655440000",
  "event_slug": "devlille-2025",
  "status": "approved",
  "promoted_at": "2025-10-18T10:00:00Z",
  "reviewed_at": "2025-10-18T15:45:00Z",
  "reviewed_by": "950e8400-e29b-41d4-a716-446655440000",
  "job_offer": {
    "id": "550e8400-e29b-41d4-a716-446655440000",
    "title": "Senior Backend Developer",
    "url": "https://example.com/jobs/senior-backend-dev",
    "location": "Remote",
    "publication_date": "2025-10-15T00:00:00Z",
    "end_date": "2025-11-15T23:59:59Z",
    "experience_years": 5,
    "salary": "70k-90k EUR",
    "company_id": "450e8400-e29b-41d4-a716-446655440000",
    "created_at": "2025-10-15T08:30:00Z",
    "updated_at": "2025-10-15T08:30:00Z"
  },
  "created_at": "2025-10-18T10:00:00Z",
  "updated_at": "2025-10-18T15:45:00Z"
}
```

**Error Responses**:
- **403 Forbidden**: User lacks event edit permission
- **404 Not Found**: Promotion not found
- **409 Conflict**: Promotion not in pending status
  ```json
  {
    "error": "conflict",
    "message": "Promotion status must be pending, current status: approved"
  }
  ```

**Side Effects**:
- Updates promotion: status=APPROVED, reviewed_at=now, reviewed_by=currentUser
- Sends Mailjet notification to partnership contact emails (approval message)
- Sends Slack notification to organization channel (approval message)

**Operation ID**: `approveJobOfferPromotion`

**Tags**: `partnerships`, `job-offers`, `promotions`, `moderation`

---

### POST /orgs/{orgSlug}/events/{eventSlug}/partnerships/{partnershipId}/job-offers/{promotionId}/decline

**Purpose**: Event organizer declines a pending job offer promotion with optional reason.

**Authentication**: Required (bearerAuth - JWT token)

**Authorization**: AuthorizedOrganisationPlugin automatically validates user has canEdit=true permission for the event's organization

**Route Protection**:
```kotlin
route("/orgs/{orgSlug}/events/{eventSlug}/partnership/{partnershipId}/job-offers/promotions/{promotionId}") {
    install(AuthorizedOrganisationPlugin)  // Automatic JWT + permission validation
    post("/decline") { /* ... */ }
}
```

**Path Parameters**:
| Parameter | Type | Required | Description |
|-----------|------|----------|-------------|
| orgSlug | string | Yes | Organization slug |
| eventSlug | string | Yes | Event slug |
| partnershipId | UUID | Yes | Partnership unique identifier |
| promotionId | UUID | Yes | Promotion unique identifier |

**Request Body**:
```json
{
  "reason": "Job offer not aligned with event audience"
}
```

**Schema**: References `decline_job_offer_promotion.schema.json`

**Success Response** (200 OK):
```json
{
  "id": "650e8400-e29b-41d4-a716-446655440000",
  "job_offer_id": "550e8400-e29b-41d4-a716-446655440000",
  "partnership_id": "750e8400-e29b-41d4-a716-446655440000",
  "event_slug": "devlille-2025",
  "status": "declined",
  "promoted_at": "2025-10-18T10:00:00Z",
  "reviewed_at": "2025-10-18T15:50:00Z",
  "reviewed_by": "950e8400-e29b-41d4-a716-446655440000",
  "job_offer": {
    "id": "550e8400-e29b-41d4-a716-446655440000",
    "title": "Senior Backend Developer",
    "url": "https://example.com/jobs/senior-backend-dev",
    "location": "Remote",
    "publication_date": "2025-10-15T00:00:00Z",
    "end_date": "2025-11-15T23:59:59Z",
    "experience_years": 5,
    "salary": "70k-90k EUR",
    "company_id": "450e8400-e29b-41d4-a716-446655440000",
    "created_at": "2025-10-15T08:30:00Z",
    "updated_at": "2025-10-15T08:30:00Z"
  },
  "created_at": "2025-10-18T10:00:00Z",
  "updated_at": "2025-10-18T15:50:00Z"
}
```

**Error Responses**:
- **403 Forbidden**: User lacks event edit permission
- **404 Not Found**: Promotion not found
- **409 Conflict**: Promotion not in pending status

**Side Effects**:
- Updates promotion: status=DECLINED, reviewed_at=now, reviewed_by=currentUser
- Sends Mailjet notification to partnership contact emails (decline message with reason if provided)
- Sends Slack notification to organization channel (decline message)

**Operation ID**: `declineJobOfferPromotion`

**Tags**: `partnerships`, `job-offers`, `promotions`, `moderation`

---

## Common Schemas

### JobOfferPromotionResponse

**File**: `job_offer_promotion_response.schema.json`

**Description**: Complete promotion details with embedded job offer.

**Properties**:
- `id`: UUID (required)
- `job_offer_id`: UUID (required)
- `partnership_id`: UUID (required)
- `event_slug`: string (required) - Event slug identifier (e.g., "devlille-2025")
- `status`: enum ["pending", "approved", "declined"] (required)
- `promoted_at`: ISO 8601 datetime (required)
- `reviewed_at`: ISO 8601 datetime or null
- `reviewed_by`: UUID or null
- `job_offer`: JobOfferResponse object (required, embedded)
- `created_at`: ISO 8601 datetime (required)
- `updated_at`: ISO 8601 datetime (required)

---

## Authentication & Authorization Summary

| Endpoint | Auth Required | Authorization Rule |
|----------|---------------|-------------------|
| POST promote | No (public) | Company ownership validated internally via companyId |
| GET job offer promotions | No (public) | Company ownership validated internally via companyId |
| GET partnership job offers | No (public) | Company ownership OR event association validated internally |
| GET event job offers | Yes (JWT) | Event edit permission (canEdit=true) via AuthorizedOrganisationPlugin |
| POST approve | Yes (JWT) | Event edit permission (canEdit=true) via AuthorizedOrganisationPlugin |
| POST decline | Yes (JWT) | Event edit permission (canEdit=true) via AuthorizedOrganisationPlugin |

**Company Endpoints Pattern** (public, no authentication):
```kotlin
route("/companies/{companyId}/...") {
    // No install(AuthorizedOrganisationPlugin)
    // No authentication required
    post {
        val companyId = call.parameters.companyUUID
        // Company ownership validation happens in repository layer
        val result = repository.someOperation(companyId, ...)
        call.respond(HttpStatusCode.OK, result)
    }
}
```

**Partnership Endpoints Pattern** (authenticated, organization-protected):
```kotlin
route("/orgs/{orgSlug}/events/{eventSlug}/...") {
    install(AuthorizedOrganisationPlugin)  // Automatic JWT + canEdit validation
    
    post {
        // User already validated - has canEdit=true permission
        val eventSlug = call.parameters.eventSlug
        val result = repository.someOperation(eventSlug, ...)
        call.respond(HttpStatusCode.OK, result)
    }
}
```

---

## HTTP Status Code Usage

| Code | Usage |
|------|-------|
| 200 OK | Successful retrieval, approval, or decline |
| 201 Created | Successful promotion creation |
| 400 Bad Request | Invalid request body, validation errors |
| 403 Forbidden | Authorization failure (no permission, event ended) |
| 404 Not Found | Resource not found (company, partnership, job offer, promotion) |
| 409 Conflict | Business rule violation (duplicate promotion, invalid status transition) |
| 500 Internal Server Error | Unhandled server error (logged, generic message) |

---

## Rate Limiting

**Policy**: No rate limiting required per specification (FR: unlimited promotions allowed).

**Future Consideration**: If abuse detected, implement rate limiting per company per event (e.g., max 100 promotions per hour).

---

## Pagination Standards

All list endpoints support pagination with consistent query parameters:
- `page`: Page number (1-indexed, default 1)
- `page_size`: Items per page (default 20, max 100)

Response format:
```json
{
  "items": [...],
  "total": <total_count>,
  "page": <current_page>,
  "page_size": <items_per_page>,
  "total_pages": <calculated_pages>
}
```

---

*API contracts complete. All endpoints defined with request/response formats, authentication rules, and error codes. Ready for OpenAPI documentation and test generation.*
