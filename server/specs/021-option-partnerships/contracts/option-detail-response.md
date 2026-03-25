# API Contract: Option Detail with Partnerships

**Feature**: 021-option-partnerships
**Date**: 2026-03-24

## Endpoint

**Method**: `GET`
**Path**: `/orgs/{orgSlug}/events/{eventSlug}/options/{optionId}`
**Auth**: Bearer token (AuthorizedOrganisationPlugin)

### Parameters

| Name | In | Required | Type | Description |
|------|-----|----------|------|-------------|
| orgSlug | path | yes | string | Organisation slug |
| eventSlug | path | yes | string | Event slug |
| optionId | path | yes | string (UUID) | Sponsoring option ID |

### Response: 200 OK

Content-Type: `application/json`

```json
{
  "option": {
    "type": "text",
    "id": "550e8400-e29b-41d4-a716-446655440000",
    "translations": {
      "en": {
        "language": "en",
        "name": "Logo on website",
        "description": "Your logo displayed on the event website"
      },
      "fr": {
        "language": "fr",
        "name": "Logo sur le site",
        "description": "Votre logo affiché sur le site de l'événement"
      }
    },
    "price": 500
  },
  "partnerships": [
    {
      "id": "650e8400-e29b-41d4-a716-446655440000",
      "contact": {
        "display_name": "Jane Doe",
        "role": "CTO"
      },
      "company_name": "Acme Corp",
      "event_name": "DevLille 2026",
      "selected_pack_id": "750e8400-e29b-41d4-a716-446655440000",
      "selected_pack_name": "Gold",
      "suggested_pack_id": null,
      "suggested_pack_name": null,
      "validated_pack_id": "750e8400-e29b-41d4-a716-446655440000",
      "language": "en",
      "phone": "+33612345678",
      "emails": ["jane@acme.com"],
      "organiser": null,
      "created_at": "2026-03-01T10:00:00"
    }
  ]
}
```

### Response: 401 Unauthorized

Returned when the bearer token is missing or invalid.

### Response: 404 Not Found

Returned when the event, organisation, or option does not exist.

---

## JSON Schema: `sponsoring_option_with_partnerships.schema.json`

```json
{
  "$id": "sponsoring_option_with_partnerships.schema.json",
  "type": "object",
  "properties": {
    "option": {
      "$ref": "sponsoring_option_with_translations.schema.json"
    },
    "partnerships": {
      "type": "array",
      "items": {
        "$ref": "partnership_item.schema.json"
      },
      "description": "List of validated partnerships associated with this option"
    }
  },
  "required": [
    "option",
    "partnerships"
  ]
}
```

---

## OpenAPI Addition

Add a `get` operation to the existing `/orgs/{orgSlug}/events/{eventSlug}/options/{optionId}` path:

```yaml
get:
  summary: "Get sponsoring option detail with partnerships"
  operationId: "getOrgsEventsOptionsById"
  description: "Get a single sponsoring option with all translations and the list of validated partnerships"
  security:
    - bearerAuth: []
  parameters:
    - name: "eventSlug"
      in: "path"
      required: true
      schema:
        type: "string"
    - name: "optionId"
      in: "path"
      required: true
      schema:
        type: "string"
    - name: "orgSlug"
      in: "path"
      required: true
      schema:
        type: "string"
  responses:
    "200":
      description: "OK"
      content:
        application/json:
          schema:
            $ref: "#/components/schemas/SponsoringOptionDetailWithPartners"
    "401":
      description: "Unauthorized"
      content:
        application/json:
          schema:
            $ref: "#/components/schemas/ErrorResponse"
    "404":
      description: "Not Found"
      content:
        application/json:
          schema:
            $ref: "#/components/schemas/ErrorResponse"
```
