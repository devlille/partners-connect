# Quickstart: Filter Partnerships by Declined Status

**Feature Branch**: `018-filter-declined-partnerships`

---

## Prerequisites

- Kotlin/Gradle toolchain configured (JVM 21)
- Local PostgreSQL running (or use H2 for tests)
- Authentication token for an organiser with edit permissions on the target event

---

## Key Validation Steps

### 1. Verify default behaviour excludes declined (US1 — SC-001, SC-002)

```bash
# No filter[declined] parameter — declined partnerships must NOT appear
curl -s -H "Authorization: Bearer $TOKEN" \
  "http://localhost:8080/orgs/{orgSlug}/events/{eventSlug}/partnerships"
```

Expected: response `items` array contains no partnerships where `declinedAt` is set.

---

### 2. Verify filter[declined]=true includes declined (US1 — FR-003)

```bash
curl -s -H "Authorization: Bearer $TOKEN" \
  "http://localhost:8080/orgs/{orgSlug}/events/{eventSlug}/partnerships?filter[declined]=true"
```

Expected: response `items` array includes declined partnerships alongside active ones.

---

### 3. Verify filter[declined]=false explicitly excludes declined (US1 — FR-002)

```bash
curl -s -H "Authorization: Bearer $TOKEN" \
  "http://localhost:8080/orgs/{orgSlug}/events/{eventSlug}/partnerships?filter[declined]=false"
```

Expected: same result as no filter — declined partnerships excluded.

---

### 4. Verify invalid value returns 400 (FR-005)

```bash
curl -s -o /dev/null -w "%{http_code}" -H "Authorization: Bearer $TOKEN" \
  "http://localhost:8080/orgs/{orgSlug}/events/{eventSlug}/partnerships?filter[declined]=maybe"
```

Expected: `400`

---

### 5. Verify AND logic with other filters (FR-004)

```bash
curl -s -H "Authorization: Bearer $TOKEN" \
  "http://localhost:8080/orgs/{orgSlug}/events/{eventSlug}/partnerships?filter[declined]=true&filter[validated]=true"
```

Expected: only partnerships that are both declined AND validated are returned.

---

### 6. Verify metadata includes declined filter (FR-011)

```bash
curl -s -H "Authorization: Bearer $TOKEN" \
  "http://localhost:8080/orgs/{orgSlug}/events/{eventSlug}/partnerships" \
  | jq '.metadata.filters[] | select(.name == "declined")'
```

Expected output:
```json
{
  "name": "declined",
  "type": "boolean"
}
```

---

### 7. Verify email endpoint excludes declined by default (US2 — FR-007)

```bash
curl -s -X POST \
  -H "Authorization: Bearer $TOKEN" \
  -H "Content-Type: application/json" \
  -d '{"subject":"Test","body":"<p>Hello</p>"}' \
  "http://localhost:8080/orgs/{orgSlug}/events/{eventSlug}/partnerships/email"
```

Expected: Email sent only to non-declined partnership contacts.

---

### 8. Verify email endpoint with filter[declined]=true (US2 — FR-003, FR-006)

```bash
curl -s -X POST \
  -H "Authorization: Bearer $TOKEN" \
  -H "Content-Type: application/json" \
  -d '{"subject":"Test","body":"<p>Hello</p>"}' \
  "http://localhost:8080/orgs/{orgSlug}/events/{eventSlug}/partnerships/email?filter[declined]=true"
```

Expected: Email sent to all partnership contacts including declined ones.

---

## Running Tests

```bash
cd server
./gradlew test --no-daemon --tests "*.PartnershipDeclinedFilterRoutesTest"
./gradlew test --no-daemon --tests "*.PartnershipListDeclinedFilterRouteGetTest"
```

Run full suite to confirm zero regression:

```bash
./gradlew check --no-daemon
```

---

## Quality Gates

```bash
./gradlew ktlintCheck --no-daemon
./gradlew detekt --no-daemon
cd server && npm run validate
```
