# DELETE /orgs/{orgSlug}/events/{eventSlug}/communication-plan/{id}

Delete a communication plan entry (standalone or partnership-linked). Deleting a partnership-linked entry does not affect the partnership itself.

## Endpoint

```
DELETE /orgs/{orgSlug}/events/{eventSlug}/communication-plan/{id}
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

## Response: 204 No Content

Empty body.

## Error Responses

| Status | Condition |
|--------|-----------|
| `401 Unauthorized` | No valid Bearer token |
| `403 Forbidden` | User has no permission for this organisation |
| `404 Not Found` | Entry not found, or entry belongs to a different event |
