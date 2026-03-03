# API Contract Changes: Filter Partnerships by Declined Status

**Feature Branch**: `018-filter-declined-partnerships`

This document describes the exact changes to be applied to `openapi/openapi.yaml`.
No new endpoints are introduced. Two existing endpoints gain a new query parameter.

---

## Change 1: GET /orgs/{orgSlug}/events/{eventSlug}/partnerships

**Add after the existing `filter[organiser]` parameter block:**

```yaml
        - name: "filter[declined]"
          in: "query"
          required: false
          schema:
            type: "boolean"
          description: >
            Filter by declined status. When absent or false (default), declined partnerships
            (those with a non-null declinedAt timestamp) are excluded from results.
            When true, declined partnerships are included alongside active ones.
            NOTE: This changes the default behaviour — previously declined partnerships
            were returned by default. Existing callers that need the old behaviour must
            pass filter[declined]=true.
```

**Updated `400` response description** (already present — extend description):

```yaml
        "400":
          description: "Bad Request — invalid filter parameter value (e.g., non-boolean for filter[declined])"
```

---

## Change 2: POST /orgs/{orgSlug}/events/{eventSlug}/partnerships/email

**Add after the existing `filter[organiser]` parameter block:**

```yaml
        - name: "filter[declined]"
          in: "query"
          required: false
          schema:
            type: "boolean"
          description: >
            Filter by declined status for email recipients. When absent or false (default),
            declined partnerships are excluded from the email recipient list.
            When true, declined partnerships are included as recipients.
```

---

## Response contract: PaginationMetadata filters array

The `GET /orgs/{orgSlug}/events/{eventSlug}/partnerships` response body includes a `metadata.filters` array. After this feature, the array will contain one additional entry:

```json
{
  "name": "declined",
  "type": "boolean"
}
```

This entry has no `values` array (it is a free boolean, not an enum-constrained field).

The full updated filters array order:
1. `pack_id` (string, with values)
2. `validated` (boolean)
3. `suggestion` (boolean)
4. `paid` (boolean)
5. `agreement-generated` (boolean)
6. `agreement-signed` (boolean)
7. `organiser` (string, with values)
8. `declined` (boolean) ← **NEW**

---

## No schema file changes

No new JSON schema files are needed. The new parameter is a simple boolean query parameter, not part of a request body schema.
