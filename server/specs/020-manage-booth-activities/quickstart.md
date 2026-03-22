# Quickstart: Manage Booth Activities

**Feature**: `020-manage-booth-activities`  
**Date**: 2026-03-21

This guide walks through validating the booth activity endpoints end-to-end using the local development stack.

---

## Prerequisites

- Docker running (PostgreSQL via `docker-compose up`)
- Server running: `./gradlew run --no-daemon` from `/server`
- A test partnership that has a booth sponsoring option linked

---

## 1. Seed a partnership with a booth option

You need a partnership whose `partnership_options` row references a `sponsoring_options` row with `selectable_descriptor = 'booth'`. Use the existing event/sponsoring admin UI or a direct DB insert.

---

## 2. Create an activity

```bash
curl -X POST http://localhost:8080/events/{eventSlug}/partnerships/{partnershipId}/activities \
  -H "Content-Type: application/json" \
  -d '{
    "title": "Live CI/CD demo",
    "description": "A hands-on CI/CD walkthrough.",
    "startTime": "2026-06-14T10:00:00",
    "endTime": "2026-06-14T10:30:00"
  }'
```

**Expected**: `201 Created` with activity JSON including `id` and `createdAt`.

---

## 3. Create an activity without times

```bash
curl -X POST http://localhost:8080/events/{eventSlug}/partnerships/{partnershipId}/activities \
  -H "Content-Type: application/json" \
  -d '{
    "title": "Open booth",
    "description": "Come talk to us anytime."
  }'
```

**Expected**: `201 Created` with `"startTime": null` and `"endTime": null`.

---

## 4. List activities

```bash
curl http://localhost:8080/events/{eventSlug}/partnerships/{partnershipId}/activities
```

**Expected**: `200 OK` with array sorted by `startTime` ascending, null-start activities last.

---

## 5. Update an activity

```bash
curl -X PUT http://localhost:8080/events/{eventSlug}/partnerships/{partnershipId}/activities/{activityId} \
  -H "Content-Type: application/json" \
  -d '{
    "title": "Updated title",
    "description": "Updated description.",
    "startTime": null,
    "endTime": null
  }'
```

**Expected**: `200 OK` with updated activity fields.

---

## 6. Delete an activity

```bash
curl -X DELETE http://localhost:8080/events/{eventSlug}/partnerships/{partnershipId}/activities/{activityId}
```

**Expected**: `204 No Content`.

---

## 7. Verify 403 on no-booth partnership

```bash
curl -X POST http://localhost:8080/events/{eventSlug}/partnerships/{nonBoothPartnershipId}/activities \
  -H "Content-Type: application/json" \
  -d '{"title": "Test", "description": "Test"}'
```

**Expected**: `403 Forbidden`.

---

## 8. Verify 400 on missing required field

```bash
curl -X POST http://localhost:8080/events/{eventSlug}/partnerships/{partnershipId}/activities \
  -H "Content-Type: application/json" \
  -d '{"description": "No title here"}'
```

**Expected**: `400 Bad Request`.

---

## 9. Verify 400 on startTime after endTime

```bash
curl -X POST http://localhost:8080/events/{eventSlug}/partnerships/{partnershipId}/activities \
  -H "Content-Type: application/json" \
  -d '{
    "title": "Bad times",
    "description": "End before start.",
    "startTime": "2026-06-14T11:00:00",
    "endTime": "2026-06-14T10:00:00"
  }'
```

**Expected**: `400 Bad Request`.

---

## Running Tests

```bash
cd server

# All tests
./gradlew test --no-daemon

# Specific test class
./gradlew test --tests "fr.devlille.partners.connect.partnership.infrastructure.api.BoothActivityRoutePostTest" --no-daemon

# Integration tests
./gradlew test --tests "fr.devlille.partners.connect.partnership.BoothActivityRoutesTest" --no-daemon

# Quality gates
./gradlew ktlintCheck --no-daemon
./gradlew detekt --no-daemon
```
