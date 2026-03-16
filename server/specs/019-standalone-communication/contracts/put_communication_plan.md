# PUT /orgs/{orgSlug}/events/{eventSlug}/communication-plan/{id}

Update an existing communication plan entry (standalone or partnership-linked).

## Endpoint

```
PUT /orgs/{orgSlug}/events/{eventSlug}/communication-plan/{id}
```

## Authorization

**Required**: Organiser with permissions for the specified organisation/event  
**Enforced by**: `AuthorizedOrganisationPlugin`

## Path Parameters

| Parameter | Type | Required | Description |
|-----------|------|----------|-------------|
| `orgSlug` | string (UUID) | Yes | Organisation UUID |
| `eventSlug` | string (UUID) | Yes | Event UUID |
| `id` | UUID | Yes | Communication plan entry UUID |

## Request Headers

| Header | Required | Value |
|--------|----------|-------|
| `Authorization` | Yes | Bearer token from Google OAuth |
| `Content-Type` | Yes | `application/json` |

## Request Body

**Schema**: `communication_plan_request.schema.json` (same as POST)

```json
{
  "title": "Updated title",
  "scheduled_date": "2026-06-20T10:00:00",
  "description": "Updated description.",
  "support_url": "https://storage.example.com/assets/updated-brief.pdf"
}
```

### Fields

| Field | Type | Required | Description |
|-------|------|----------|-------------|
| `title` | string | **Yes** | Non-blank, max 255 chars. |
| `scheduled_date` | string (ISO 8601 local datetime) | No | Updated scheduled date. Send `null` to clear the date and move entry to `unplanned` group. |
| `description` | string | No | Updated description. Send `null` to clear. |
| `support_url` | string | No | Updated support URL. Send `null` to clear. |

## Response: 200 OK

```json
{
  "id": "550e8400-e29b-41d4-a716-446655440000",
  "event_id": "event-uuid",
  "partnership_id": "partnership-uuid-or-null",
  "company_name": "Acme Corp",
  "title": "Updated title",
  "scheduled_date": "2026-06-20T10:00:00",
  "description": "Updated description.",
  "support_url": "https://storage.example.com/assets/updated-brief.pdf",
  "standalone": false,
  "created_at": "2026-03-16T09:00:00"
}
```

## Error Responses

| Status | Condition |
|--------|-----------|
| `400 Bad Request` | `title` is missing or blank; `scheduled_date` is present but not a valid datetime |
| `401 Unauthorized` | No valid Bearer token |
| `403 Forbidden` | User has no permission for this organisation |
| `404 Not Found` | Entry not found, or entry belongs to a different event |
