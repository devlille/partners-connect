# GET /orgs/{orgSlug}/events/{eventSlug}/communication (updated)

Retrieve the communication planning view for an event. **After this feature**, data is sourced exclusively from the `communication_plans` table instead of `PartnershipsTable`. The response shape is backwards-compatible with the addition of new fields (`id`, `title`, `standalone`); `company_name` is now nullable.

## Endpoint

```
GET /orgs/{orgSlug}/events/{eventSlug}/communication
```

*(Endpoint path unchanged)*

## Authorization

**Required**: Organiser with permissions for the specified organisation/event  
**Enforced by**: `AuthorizedOrganisationPlugin`

## Path Parameters

| Parameter | Type | Required | Description |
|-----------|------|----------|-------------|
| `orgSlug` | string (UUID) | Yes | Organisation UUID |
| `eventSlug` | string (UUID) | Yes | Event UUID |

## Response: 200 OK

```json
{
  "done": [
    {
      "id": "uuid-1",
      "partnership_id": "partnership-uuid",
      "company_name": "Acme Corp",
      "title": "Acme Corp",
      "publication_date": "2026-05-01T09:00:00",
      "support_url": "https://storage.example.com/acme-support.png",
      "standalone": false
    }
  ],
  "planned": [
    {
      "id": "uuid-2",
      "partnership_id": null,
      "company_name": null,
      "title": "Welcome sponsors email",
      "publication_date": "2026-06-15T09:00:00",
      "support_url": null,
      "standalone": true
    }
  ],
  "unplanned": [
    {
      "id": "uuid-3",
      "partnership_id": "partnership-uuid-2",
      "company_name": "Beta Inc",
      "title": "Beta Inc",
      "publication_date": null,
      "support_url": null,
      "standalone": false
    }
  ]
}
```

## Grouping Logic

| Group | Condition |
|-------|-----------|
| `done` | `publication_date` is not null AND before `now` (UTC), sorted descending |
| `planned` | `publication_date` is not null AND on/after `now` (UTC), sorted ascending |
| `unplanned` | `publication_date` is null |

## Changed Fields vs Previous Version

| Field | Before | After |
|-------|--------|-------|
| `id` | absent | NEW — `communication_plans.id` UUID |
| `title` | absent | NEW — entry title |
| `standalone` | absent | NEW — `true` when not linked to a partnership |
| `company_name` | non-null string | **nullable** — null for standalone entries |

All existing fields (`partnership_id`, `publication_date`, `support_url`) retain the same semantics.

## Error Responses

| Status | Condition |
|--------|-----------|
| `401 Unauthorized` | No valid Bearer token |
| `403 Forbidden` | User has no permission for this organisation |
| `404 Not Found` | Event not found |
