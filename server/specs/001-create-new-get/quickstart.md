# Quickstart: Event Sponsoring Packs Public API

**Date**: 2025-10-02  
**Feature**: Public GET endpoint for event sponsoring packages

## Prerequisites

- Java 21 (Amazon Corretto recommended)
- PostgreSQL database running (or use H2 for testing)
- Gradle 8.13+ (wrapper included)

## Quick Start

### 1. Start the Server
```bash
cd server
./gradlew run --no-daemon
```
Server will start on `http://localhost:8080`

### 2. Test the Endpoint

#### Get sponsoring packages for an event
```bash
curl -X GET \
  "http://localhost:8080/events/devlille-2025/sponsoring/packs" \
  -H "Accept: application/json" \
  -H "Accept-Language: en"
```

#### Test with French translations
```bash
curl -X GET \
  "http://localhost:8080/events/devlille-2025/sponsoring/packs" \
  -H "Accept: application/json" \
  -H "Accept-Language: fr"
```

#### Test non-existent event (should return 404)
```bash
curl -X GET \
  "http://localhost:8080/events/invalid-event/sponsoring/packs" \
  -H "Accept: application/json"
```

### 3. Expected Responses

#### Success Response (200 OK)
```json
[
  {
    "id": "123e4567-e89b-12d3-a456-426614174000",
    "name": "Gold Sponsor",
    "base_price": 50000,
    "max_quantity": 5,
    "required_options": [
      {
        "id": "456e7890-e89b-12d3-a456-426614174000",
        "name": "Logo on website",
        "description": "Company logo displayed on event website",
        "price": null
      }
    ],
    "optional_options": [
      {
        "id": "789e0123-e89b-12d3-a456-426614174000",
        "name": "Additional booth space",
        "description": "Extra 2x2m booth space",
        "price": 10000
      }
    ]
  }
]
```

#### Empty Response (200 OK)
```json
[]
```

#### Error Response (404 Not Found)
```json
{
  "error": "Event with slug 'invalid-event' not found"
}
```

## Testing Scenarios

### Scenario 1: Event with Multiple Packages
1. Create an event with slug "tech-conference-2025"
2. Create 3 sponsoring packages (Gold, Silver, Bronze)
3. Add required and optional options to each package
4. Call endpoint and verify all packages returned with correct structure

### Scenario 2: Event with No Packages
1. Create an event with slug "small-meetup-2025"
2. Do not create any sponsoring packages
3. Call endpoint and verify empty array returned with 200 status

### Scenario 3: Language Translations
1. Create options with translations in French and English
2. Call endpoint with `Accept-Language: fr`
3. Verify option names and descriptions are in French
4. Call same endpoint with `Accept-Language: en`
5. Verify option names and descriptions are in English

### Scenario 4: Error Handling
1. Call endpoint with non-existent event slug
2. Verify 404 status code and appropriate error message
3. Call endpoint with malformed event slug
4. Verify appropriate error handling

## Development Workflow

### Run Tests
```bash
cd server
./gradlew test --no-daemon
```

### Check Code Quality
```bash
cd server
./gradlew ktlintCheck detekt --no-daemon
```

### Fix Code Formatting
```bash
cd server
./gradlew ktlintFormat --no-daemon
```

### Full Build with All Checks
```bash
cd server
./gradlew check --no-daemon
```

## Database Setup for Testing

### Create Test Data
```sql
-- Insert test event
INSERT INTO events (id, name, slug, start_time, end_time, submission_start_time, submission_end_time, address, organisation_id) 
VALUES ('12345678-1234-1234-1234-123456789012', 'DevLille 2025', 'devlille-2025', '2025-06-15 09:00:00', '2025-06-15 18:00:00', '2025-01-01 00:00:00', '2025-05-01 23:59:59', '123 Tech Street, Lille', '87654321-4321-4321-4321-210987654321');

-- Insert test sponsoring pack
INSERT INTO sponsoring_packs (id, event_id, name, base_price, with_booth, nb_tickets, max_quantity)
VALUES ('11111111-1111-1111-1111-111111111111', '12345678-1234-1234-1234-123456789012', 'Gold Sponsor', 50000, true, 10, 5);

-- Insert test sponsoring option
INSERT INTO sponsoring_options (id, event_id, price)
VALUES ('22222222-2222-2222-2222-222222222222', '12345678-1234-1234-1234-123456789012', 10000);

-- Insert option translation
INSERT INTO option_translations (id, option_id, language, name, description)
VALUES ('33333333-3333-3333-3333-333333333333', '22222222-2222-2222-2222-222222222222', 'en', 'Additional booth space', 'Extra 2x2m booth space');

-- Link option to pack as optional
INSERT INTO pack_options (pack_id, option_id, required)
VALUES ('11111111-1111-1111-1111-111111111111', '22222222-2222-2222-2222-222222222222', false);
```

## Troubleshooting

### Common Issues

#### Server won't start
- Check Java 21 is installed: `java -version`
- Verify PostgreSQL is running and accessible
- Check for port conflicts on 8080

#### Tests failing
- Run with `--no-daemon` flag to avoid timeout issues
- Verify test data is clean with H2 in-memory database

#### 404 errors for valid events
- Verify event exists in database with correct slug
- Check case sensitivity of event slug
- Confirm foreign key relationships are correct

#### Missing translations
- Verify `option_translations` table has entries for requested language
- Check `Accept-Language` header format (e.g., "en", "fr")
- Confirm fallback language handling in `toDomain()` mapper

## Performance Validation

### Response Time Testing
```bash
# Use Apache Bench to test response times
ab -n 100 -c 10 "http://localhost:8080/events/devlille-2025/sponsoring/packs"
```

Target: All requests should complete under 2 seconds (constitutional requirement)

### Load Testing
```bash
# Test with multiple concurrent requests
ab -n 1000 -c 50 "http://localhost:8080/events/devlille-2025/sponsoring/packs"
```

Monitor database connection pool and memory usage during load testing.