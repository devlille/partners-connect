# API Contracts: Provider Management Enhancement

## Organisation-Scoped Provider Management

### POST /orgs/{orgSlug}/providers
Create a new provider within organisation scope.

**Request:**
```http
POST /orgs/{orgSlug}/providers
Authorization: Bearer <jwt_token>
Content-Type: application/json

{
  "name": "Traiteur Lillois",
  "type": "catering",
  "website": "https://traiteur-lillois.fr",
  "phone": "+33123456789", 
  "email": "contact@traiteur-lillois.fr"
}
```

**Response 201:**
```json
{
  "id": "550e8400-e29b-41d4-a716-446655440000",
  "name": "TechCorp Solutions",
  "type": "Technology Consulting", 
  "website": "https://techcorp.example.com",
  "phone": "+33 1 23 45 67 89",
  "email": "contact@techcorp.example.com",
  "org_slug": "devlille",
  "created_at": "2025-11-08T15:00:00"
}
```

**Error Responses:**
- 401: Invalid/missing JWT token
- 403: User not member of organisation
- 400: Invalid request body (schema validation)

### PUT /orgs/{orgSlug}/providers/{providerId}
Update provider within organisation scope.

**Request:**
```http
PUT /orgs/{orgSlug}/providers/550e8400-e29b-41d4-a716-446655440000
Authorization: Bearer <jwt_token>
Content-Type: application/json

{
  "name": "Traiteur Lillois Premium",
  "phone": "+33987654321"
}
```

**Response 200:**
```json
{
  "id": "550e8400-e29b-41d4-a716-446655440000",
  "name": "Traiteur Lillois Premium",
  "type": "catering",
  "website": "https://traiteur-lillois.fr",
  "phone": "+33987654321",
  "email": "contact@traiteur-lillois.fr", 
  "org_slug": "devlille",
  "created_at": "2025-11-08T14:30:00"
}
```

**Error Responses:**
- 401: Invalid/missing JWT token
- 403: User not member of organisation
- 404: Provider not found or not owned by organisation
- 400: Invalid request body

### DELETE /orgs/{orgSlug}/providers/{providerId}  
Delete provider after detaching from all events.

**Request:**
```http
DELETE /orgs/{orgSlug}/providers/550e8400-e29b-41d4-a716-446655440000
Authorization: Bearer <jwt_token>
```

**Response 204:** (No content)

**Error Responses:**
- 401: Invalid/missing JWT token
- 403: User not member of organisation
- 404: Provider not found or not owned by organisation
- 409: Provider still attached to events (must detach first)

## Event Provider Attachment

### POST /orgs/{orgSlug}/events/{eventSlug}/providers
Attach provider to event within same organisation.

**Request:**
```http
POST /orgs/{orgSlug}/events/{eventSlug}/providers
Authorization: Bearer <jwt_token>
Content-Type: application/json

["550e8400-e29b-41d4-a716-446655440000", "650e8400-e29b-41d4-a716-446655440001"]
```

**Response 201:**
```json
{
  "attached_providers": [
    {
      "provider_id": "550e8400-e29b-41d4-a716-446655440000",
      "event_slug": "devlille-2025",
      "attached_at": "2025-11-08T15:00:00"
    },
    {
      "provider_id": "650e8400-e29b-41d4-a716-446655440001",
      "event_slug": "devlille-2025", 
      "attached_at": "2025-11-08T15:00:00"
    }
  ]
}
```

**Error Responses:**
- 401: Invalid/missing JWT token
- 403: User not member of organisation
- 404: One or more providers or event not found
- 409: One or more providers already attached to event
- 400: One or more providers not owned by same organisation as event

### GET /orgs/{orgSlug}/events/{eventSlug}/providers
List providers attached to specific event.

**Request:**
```http  
GET /orgs/{orgSlug}/events/{eventSlug}/providers
Authorization: Bearer <jwt_token>
```

**Response 200:**
```json
{
  "items": [
    {
      "id": "550e8400-e29b-41d4-a716-446655440000",
      "name": "Traiteur Lillois Premium",
      "type": "catering",
      "website": "https://traiteur-lillois.fr",
      "phone": "+33987654321",
      "email": "contact@traiteur-lillois.fr",
      "org_slug": "devlille",
      "created_at": "2025-11-08T14:30:00Z"
    },
    {
      "id": "650e8400-e29b-41d4-a716-446655440001",
      "name": "DataFlow Systems",
      "type": "Data Analytics",
      "website": "https://dataflow.example.com",
      "phone": null,
      "email": "info@dataflow.example.com",
      "org_slug": "devlille",
      "created_at": "2025-11-08T14:45:00Z"
    }
  ],
  "page": 1,
  "page_size": 20,
  "total": 45
}

### DELETE /orgs/{orgSlug}/events/{eventSlug}/providers
Detach providers from event.

**Request:**
```http
DELETE /orgs/{orgSlug}/events/{eventSlug}/providers
Authorization: Bearer <jwt_token>
Content-Type: application/json

["550e8400-e29b-41d4-a716-446655440000", "650e8400-e29b-41d4-a716-446655440001"]
```

**Response 204:** (No content)

**Error Responses:**
- 401: Invalid/missing JWT token
- 403: User not member of organisation  
- 404: One or more providers, event, or attachments not found

## Public Provider Directory

### GET /providers
Public listing of providers with optional organisation filtering. **(ENHANCED: Add org_slug parameter)**

**Request:**
```http
GET /providers?org_slug=devlille&query=catering&sort=name&direction=asc&page=1&page_size=20
```

**Response 200:**
```json
{
  "items": [
    {
      "id": "550e8400-e29b-41d4-a716-446655440000", 
      "name": "Traiteur Lillois",
      "type": "catering",
      "website": "https://traiteur-lillois.fr",
      "phone": "+33123456789",
      "email": "contact@traiteur-lillois.fr",
      "org_slug": "devlille",
      "created_at": "2025-11-08T14:30:00"
    }
  ],
  "page": 1,
  "page_size": 20,
  "total": 1
}
```

**Query Parameters:**
- `org_slug` (optional): **NEW** Filter providers by organisation
- `query` (optional): Search in provider name and type (existing)
- `sort` (optional): Sort field (name, type, created_at) (existing)
- `direction` (optional): asc/desc (existing)
- `page` (optional): Page number (default 1) (existing)
- `page_size` (optional): Items per page (default 20) (existing)
