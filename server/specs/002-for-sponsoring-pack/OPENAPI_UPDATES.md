# OpenAPI Documentation Updates

**Feature**: Multi-Language Sponsoring Pack and Option Management for Organizers  
**Date**: October 5, 2025  
**File**: `server/application/src/main/resources/openapi/openapi.yaml`

## Changes Made

### 1. Endpoint Parameter Updates

**Organizer Options Endpoint** (`/orgs/{orgSlug}/events/{eventSlug}/options`):
- ❌ **REMOVED**: `Accept-Language` header parameter (was optional)
- ✅ **UPDATED**: Description to "List sponsoring options for an event with all available translations (Organizer)"

**Organizer Packs Endpoint** (`/orgs/{orgSlug}/events/{eventSlug}/packs`):
- ❌ **REMOVED**: `Accept-Language` header parameter (was optional with default "en")
- ✅ **UPDATED**: Description to "List sponsoring packs for an event with all available translations (Organizer)"

### 2. New Schema Definitions

**SponsoringOptionWithTranslations**:
```yaml
type: object
properties:
  id: string (required)
  translations: object with OptionTranslation values (required)
  price: integer, nullable (required)
```

**OptionTranslation**:
```yaml
type: object
properties:
  language: string - ISO 639-1 code (required)
  name: string (required) 
  description: string, nullable (required)
```

**SponsoringPackWithTranslations**:
```yaml
type: object
properties:
  id: string (required)
  name: string (required)
  basePrice: integer (required)
  maxQuantity: integer, nullable (required)
  requiredOptions: array of SponsoringOptionWithTranslations (required)
  optionalOptions: array of SponsoringOptionWithTranslations (required)
```

### 3. Response Schema Updates

**GET /orgs/{orgSlug}/events/{eventSlug}/options**:
- ❌ **OLD**: Returns `array of SponsoringOption`
- ✅ **NEW**: Returns `array of SponsoringOptionWithTranslations`
- ✅ **UPDATED**: Description to "OK - Returns sponsoring options with all available translations"

**GET /orgs/{orgSlug}/events/{eventSlug}/packs**:
- ❌ **OLD**: Returns `array of SponsoringPack`  
- ✅ **NEW**: Returns `array of SponsoringPackWithTranslations`
- ✅ **UPDATED**: Description to "OK - Returns sponsoring packs with all available translations"

## Impact

### For API Consumers
- **Organizer endpoints** no longer require `Accept-Language` header
- Response format now includes complete translation data for all available languages
- **Public endpoints** remain unchanged (backward compatibility maintained)

### For Frontend Applications
- Can remove `Accept-Language` header from organizer endpoint calls
- Must handle new response structure with translations map
- TypeScript types may need updates if using generated API clients

## Validation

- ✅ OpenAPI document validates successfully
- ✅ Build passes with updated documentation
- ✅ All endpoint behaviors match documentation

## Notes

This update completes the OpenAPI documentation for the multi-language organizer feature implementation. The documentation now accurately reflects the API behavior where organizer endpoints return complete translation data without requiring language specification.