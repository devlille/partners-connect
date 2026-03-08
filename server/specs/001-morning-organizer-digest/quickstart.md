# Quickstart: Morning Organiser Daily Digest

**Branch**: `001-morning-organizer-digest` | **Date**: 2026-03-05

## Prerequisites

- Docker running (PostgreSQL via `docker-compose up`)
- Java 21 installed
- An event with a configured Slack integration (set via the integrations API)
- At least one partnership with data that satisfies a readiness condition

## Running the Application Locally

```bash
cd server
./gradlew run --no-daemon
```

The application starts on port 8080. The digest does **not** run automatically — it runs only when the trigger endpoint is called.

## Triggering the Digest (Development)

Call the trigger endpoint with the org slug and event ID:

```bash
# French (default)
curl -X POST http://localhost:8080/orgs/devlille/events/{eventId}/jobs/digest \
  -H "Accept-Language: fr"

# English
curl -X POST http://localhost:8080/orgs/devlille/events/{eventId}/jobs/digest \
  -H "Accept-Language: en"
```

The `Accept-Language` header is optional — omitting it defaults to `fr`.

## Functional Validation Checklist

Follow these steps to confirm the digest is working end-to-end:

### Step 1 — Create a test event with a Slack integration

1. Create an organisation and an event with `start_time` set to a future date.
2. Configure a Slack integration on the event pointing to a test Slack channel (incoming webhook URL).

### Step 2 — Agreement readiness

1. Create a partnership for the event with a company that has: `name`, `siret`, `address`, `zip_code`, `city`, `country`, `contact_name`, `contact_role` all set.
2. Set `validated_at` to a non-null datetime on the partnership.
3. Leave `agreement_url` null.
4. Call the trigger endpoint (see above).
5. **Expected**: Slack message includes the company in the "Conventions à générer" section.

### Step 3 — Agreement already generated (negative case)

1. Set `agreement_url` to any non-null string on the same partnership.
2. Call the trigger endpoint (see above).
3. **Expected**: Company does NOT appear in the agreement section.

### Step 4 — Quote readiness

1. Create a partnership with a company having `name`, `siret`, `address`, `zip_code`, `city`, `country` set.
2. Set `validated_at` non-null. Assign a `selected_pack_id` referencing a pack with `base_price > 0`.
3. Ensure no `billings` row exists for this partnership with a non-null `quote_pdf_url`.
4. Call the trigger endpoint (see above).
5. **Expected**: Company appears in the "Devis à générer" section.

### Step 5 — Social media reminder

1. Create a partnership and set `communication_publication_date` to today's date (any time of day is fine, only the date portion is checked).
2. Call the trigger endpoint (see above).
3. **Expected**: Company appears in the "Communication prévue aujourd'hui" section.

### Step 6 — No message when nothing to do

1. Ensure all partnerships either have all documents generated or are declined or have future `communication_publication_date`.
2. Call the trigger endpoint (see above).
3. **Expected**: No Slack message is posted to the channel.

### Step 7 — Declined partnerships excluded

1. Set `declined_at` to a non-null datetime on a partnership that would otherwise qualify.
2. Call the trigger endpoint (see above).
3. **Expected**: Company does NOT appear in any section.

## Running Tests

```bash
cd server
./gradlew test --no-daemon
```

Key test classes:
- `DigestJobRoutePostTest` — contract test: HTTP status codes, schema validation
- `DigestJobRoutesTest` — integration test: end-to-end readiness logic with H2 data + mocked Slack gateway

## Running Quality Gates

```bash
cd server
./gradlew ktlintCheck --no-daemon    # formatting
./gradlew detekt --no-daemon          # static analysis
./gradlew check --no-daemon           # everything (2-3 min)
```

## Environment Variables

No new environment variables are required. The digest uses the existing Slack webhook configured per event via the integrations API.

| Variable | Purpose | Required |
|----------|---------|----------|
| `EXPOSED_DB_*` | Database connection | Existing — already required |
| Slack webhook URL | Configured per-event via Integration API, stored in DB | Existing — no new env var |

## Known Limitations

- No retry: if Slack is temporarily unavailable when the endpoint is called, that day's digest for that event is lost. This is intentional per the spec (FR-011). Retry logic should be implemented in the external scheduler if needed.
- The date used for readiness checks is always the current UTC date at the time the endpoint is called. Ensure the external scheduler fires at 08:00 UTC to avoid an off-by-one date at midnight.
