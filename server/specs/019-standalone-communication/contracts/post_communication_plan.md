# POST /orgs/{orgSlug}/events/{eventSlug}/communication-plan

Create a new standalone communication plan entry for an event (not linked to any partnership).

## Endpoint

```
POST /orgs/{orgSlug}/events/{eventSlug}/communication-plan
```

## Authorization

**Required**: Organiser with permissions for the specified organisation/event  
**Enforced by**: `AuthorizedOrganisationPlugin`

## Path Parameters

| Parameter | Type | Required | Description |
|-----------|------|----------|-------------|
| `orgSlug` | string (UUID) | Yes | Organisation UUID |
| `eventSlug` | string (UUID) | Yes | Event UUID |

## Request Headers

| Header | Required | Value |
|--------|----------|-------|
| `Authorization` | Yes | Bearer token from Google OAuth |
| `Content-Type` | Yes | `application/json` |

## Request Body

**Schema**: `communication_plan_request.schema.json`

```json
{
  "title": "Welcome sponsors email",
  "scheduled_date": "2026-06-15T09:00:00",
  "description": "General welcome message sent to all confirmed sponsors.",
  "support_url": "https://storage.example.com/assets/welcome-brief.pdf"
}
```

### Fields

| Field | Type | Required | Description |
|-------|------|----------|-------------|
| `title` | string | **Yes** | Label for the communication entry. Non-blank, max 255 chars. |
| `scheduled_date` | string (ISO 8601 local datetime) | **Yes** | When communication is planned. Past dates accepted. |
| `description` | string | No | Free-text notes about content or intent. |
| `support_url` | string | No | URL to visual support material. |

## Response: 201 Created

```json
{
  "id": "550e8400-e29b-41d4-a716-446655440000",
  "event_id": "event-uuid",
  "partnership_id": null,
  "company_name": null,
  "title": "Welcome sponsors email",
  "scheduled_date": "2026-06-15T09:00:00",
  "description": "General welcome message sent to all confirmed sponsors.",
  "support_url": "https://storage.example.com/assets/welcome-brief.pdf",
  "standalone": true,
  "created_at": "2026-03-16T10:00:00"
}
```

## Error Responses

| Status | Condition |
|--------|-----------|
| `400 Bad Request` | `title` is missing or blank; `scheduled_date` is missing or not a valid ISO 8601 datetime |
| `401 Unauthorized` | No valid Bearer token |
| `403 Forbidden` | Authenticated user has no permission for this organisation |
| `404 Not Found` | Event not found |
