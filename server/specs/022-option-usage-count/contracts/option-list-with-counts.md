# API Contract: List Options with Partnership Counts

**Feature**: 022-option-usage-count
**Date**: 2026-03-25

## Endpoint

```
GET /orgs/{orgSlug}/events/{eventSlug}/options
```

**Authentication**: Bearer token (organizer)
**Authorization**: `AuthorizedOrganisationPlugin`

## Changes from Current Behavior

**Before** (current): Returns `List<SponsoringOptionWithTranslations>` — a flat JSON array of polymorphic option objects.

**After** (this feature): Returns `List<SponsoringOptionWithCount>` — a JSON array of wrapper objects, each containing an `option` field and a `partnership_count` field.

This is a **breaking change** to the response shape.

## Request

No changes to request parameters.

### Parameters (unchanged)

| Parameter | In | Type | Required | Description |
|-----------|----|------|----------|-------------|
| `orgSlug` | path | string | Yes | Organization slug |
| `eventSlug` | path | string | Yes | Event slug |
| `Authorization` | header | string | Yes | Bearer token |

## Response

### 200 OK

```json
[
  {
    "option": {
      "id": "550e8400-e29b-41d4-a716-446655440000",
      "type": "text",
      "translations": {
        "fr": { "name": "Logo sur le site", "description": "Votre logo sera affiché" },
        "en": { "name": "Logo on website", "description": "Your logo will be displayed" }
      },
      "price": 500
    },
    "partnership_count": 3
  },
  {
    "option": {
      "id": "660e8400-e29b-41d4-a716-446655440000",
      "type": "typed_quantitative",
      "translations": {
        "fr": { "name": "Offres d'emploi", "description": "Nombre d'offres" }
      },
      "price": 200,
      "type_descriptor": "job_offers"
    },
    "partnership_count": 0
  }
]
```

### 401 Unauthorized

```json
{
  "error": "Unauthorized"
}
```

### 400 Bad Request

```json
{
  "error": "Bad Request"
}
```

## JSON Schema

**File**: `schemas/sponsoring_option_with_count.schema.json`

```json
{
  "$id": "sponsoring_option_with_count.schema.json",
  "type": "object",
  "properties": {
    "option": {
      "$ref": "sponsoring_option_with_translations.schema.json"
    },
    "partnership_count": {
      "type": "integer",
      "minimum": 0,
      "description": "Number of validated partnerships whose validated pack contains this option"
    }
  },
  "required": [
    "option",
    "partnership_count"
  ]
}
```

## OpenAPI Changes

Update the `GET /orgs/{orgSlug}/events/{eventSlug}/options` operation's `200` response:

**Before**:
```yaml
"200":
  description: "OK"
  content:
    application/json:
      schema:
        type: "array"
        items:
          $ref: "#/components/schemas/SponsoringOption"
```

**After**:
```yaml
"200":
  description: "OK"
  content:
    application/json:
      schema:
        type: "array"
        items:
          $ref: "#/components/schemas/SponsoringOptionWithCount"
```

Add new component schema:
```yaml
SponsoringOptionWithCount:
  $ref: "./schemas/sponsoring_option_with_count.schema.json"
```

## Contract Test Coverage

| Test | Status Code | Scenario |
|------|-------------|----------|
| Empty list — no options | 200 | Returns `[]` |
| Options with no partnerships | 200 | Each wrapper has `partnership_count: 0` |
| Options with validated partnerships | 200 | Correct count per option |
| Unauthorized | 401 | No bearer token |
