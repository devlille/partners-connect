---
name: openapi-schemas
description: 'OpenAPI specification and JSON schema management for partners-connect. Use when creating, updating, or validating JSON schema files, editing openapi.yaml, generating documentation.yaml, or running npm validation/bundle commands. Covers schema authoring rules, OpenAPI path patterns, schema registration, and the full validation-to-bundle workflow.'
---

# OpenAPI & JSON Schema Management

## File Locations

| File | Path | Purpose |
|------|------|---------|
| Main spec | `application/src/main/resources/openapi/openapi.yaml` | OpenAPI 3.1.0 source of truth (~5100 lines) |
| Bundled output | `application/src/main/resources/openapi/documentation.yaml` | Generated file — NEVER edit manually |
| JSON schemas | `application/src/main/resources/schemas/*.schema.json` | Individual schema files (~99 files) |
| Schema loader | `internal/infrastructure/ktor/ApplicationCall.ext.kt` | Runtime validation + schema registry |
| NPM config | `package.json` | Redocly CLI scripts |

---

## NPM Commands

Run all commands from the `server/` directory.

```bash
# Install dependencies (first time only)
npm install

# Validate OpenAPI spec — MUST pass with zero errors
npm run validate

# Bundle openapi.yaml → documentation.yaml (resolves all $ref)
npm run bundle

# Validate + bundle in one step
npm run build-docs

# Preview documentation locally in browser
npm run preview
```

### When to run

| Situation | Command |
|-----------|---------|
| After editing `openapi.yaml` | `npm run validate` |
| After adding/modifying a JSON schema file | `npm run validate` |
| After all OpenAPI changes are done (end of implementation) | `npm run build-docs` |
| Before committing | `npm run validate` (at minimum) |
| To generate final `documentation.yaml` | `npm run bundle` |

**CRITICAL**: `documentation.yaml` is auto-generated. Always run `npm run bundle` after changes to regenerate it. Never edit it directly.

---

## JSON Schema Authoring

### File conventions

- **Location**: `application/src/main/resources/schemas/`
- **Naming**: `snake_case.schema.json` — e.g., `create_company.schema.json`, `partnership_item.schema.json`
- **Compatible with**: JSON Schema Draft-7 (runtime validation) and OpenAPI 3.1.0 (documentation)

### Nullable fields

Use union types — **NEVER use `nullable: true`** (not OpenAPI 3.1.0 compliant):

```json
// ✅ CORRECT — simple nullable
"description": {
  "type": ["string", "null"]
}

// ✅ CORRECT — nullable with $ref (use anyOf)
"head_office": {
  "anyOf": [
    { "$ref": "address.schema.json" },
    { "type": "null" }
  ]
}

// ❌ WRONG — not OpenAPI 3.1.0 compliant
"description": {
  "type": "string",
  "nullable": true
}
```

### Cross-references between schemas

Use relative file references (same directory):

```json
"socials": {
  "type": "array",
  "items": {
    "$ref": "social.schema.json"
  }
}
```

### Schema categories & patterns

#### Create request schema

```json
{
  "$id": "create_company.schema.json",
  "type": "object",
  "properties": {
    "name": { "type": "string" },
    "site_url": { "type": ["string", "null"] },
    "socials": {
      "type": "array",
      "items": { "$ref": "social.schema.json" }
    }
  },
  "required": ["name"]
}
```

#### Update request schema (partial update)

```json
{
  "$schema": "http://json-schema.org/draft-07/schema#",
  "type": "object",
  "title": "UpdatePartnershipContactInfo",
  "properties": {
    "contact_name": {
      "type": ["string", "null"],
      "minLength": 1,
      "maxLength": 255
    },
    "language": {
      "type": ["string", "null"],
      "minLength": 2,
      "maxLength": 2
    }
  },
  "additionalProperties": false
}
```

#### Response schema

```json
{
  "$id": "company.schema.json",
  "type": "object",
  "properties": {
    "id": { "type": "string" },
    "name": { "type": "string" },
    "head_office": {
      "anyOf": [
        { "$ref": "address.schema.json" },
        { "type": "null" }
      ]
    },
    "status": { "type": "string" }
  },
  "required": ["id", "name"]
}
```

#### Polymorphic / sealed class schema

Use `oneOf` with a discriminator `const` field:

```json
{
  "$id": "sponsoring_option.schema.json",
  "title": "SponsoringOption",
  "oneOf": [
    { "$ref": "#/definitions/Text" },
    { "$ref": "#/definitions/TypedQuantitative" }
  ],
  "definitions": {
    "Text": {
      "type": "object",
      "required": ["type", "id", "name"],
      "properties": {
        "type": { "const": "text" },
        "id": { "type": "string" },
        "name": { "type": "string" },
        "price": { "type": ["integer", "null"], "minimum": 0 }
      },
      "additionalProperties": false
    },
    "TypedQuantitative": {
      "type": "object",
      "required": ["type", "id", "name", "type_descriptor"],
      "properties": {
        "type": { "const": "typed_quantitative" },
        "id": { "type": "string" },
        "name": { "type": "string" },
        "type_descriptor": { "type": "string", "enum": ["job_offer"] }
      },
      "additionalProperties": false
    }
  }
}
```

#### Error response schema

```json
{
  "$schema": "http://json-schema.org/draft-07/schema#",
  "type": "object",
  "title": "ErrorResponse",
  "properties": {
    "message": { "type": "string" },
    "errors": {
      "type": "array",
      "items": { "type": "string" },
      "default": []
    }
  },
  "required": ["message"],
  "additionalProperties": false
}
```

---

## OpenAPI Path Patterns

### POST endpoint with request body

```yaml
/companies:
  post:
    summary: "Create company"
    operationId: "postCompanies"
    description: "Create a new company"
    security:
      - {}
    requestBody:
      required: true
      content:
        application/json:
          schema:
            $ref: "#/components/schemas/CreateCompany"
    responses:
      "201":
        description: "Created"
        content:
          '*/*':
            schema:
              $ref: "#/components/schemas/Identifier"
```

### GET endpoint with query parameters

```yaml
/companies:
  get:
    summary: "List companies"
    operationId: "getCompanies"
    description: "List companies with paging support"
    security:
      - {}
    parameters:
      - name: "query"
        in: "query"
        required: false
        schema:
          type: "string"
        description: "Search companies by name"
      - name: "page"
        in: "query"
        required: false
        schema:
          type: "integer"
          minimum: 1
          default: 1
    responses:
      "200":
        description: "OK"
        content:
          application/json:
            schema:
              $ref: "#/components/schemas/PaginatedCompany"
```

### Security declarations

```yaml
# Public endpoint — no auth required
security:
  - {}

# Authenticated endpoint — JWT bearer token
security:
  - bearerAuth: []
```

### Operation ID conventions

- Format: `<verb><Resource>` in camelCase
- Examples: `getCompanies`, `postCompanies`, `putCompanyById`, `deleteCompanyById`
- Every operation MUST have a unique `operationId`

---

## Components / Schemas Section

Located at the bottom of `openapi.yaml` (~line 4946). All entries reference external JSON files:

```yaml
components:
  securitySchemes:
    bearerAuth:
      type: http
      scheme: bearer
      bearerFormat: JWT
      description: "Authentication token obtained through OAuth flow"
  schemas:
    Company:
      $ref: "../schemas/company.schema.json"
    CreateCompany:
      $ref: "../schemas/create_company.schema.json"
    UpdateCompany:
      $ref: "../schemas/update_company.schema.json"
    # ... one entry per schema file
```

**Path convention**: `$ref: "../schemas/<name>.schema.json"` (relative from `openapi/` to `schemas/`).

---

## Schema Registration (Runtime Validation)

When adding a new schema, it must be registered in the lazy-loaded schema cache:

**File**: `internal/infrastructure/ktor/ApplicationCall.ext.kt`

```kotlin
val schemas by lazy {
    JsonSchemaLoader.create()
        .register(readResourceFile("/schemas/address.schema.json"), SchemaType.DRAFT_7)
        .register(readResourceFile("/schemas/create_company.schema.json"), SchemaType.DRAFT_7)
        // ... add new schemas here
        .register(readResourceFile("/schemas/your_new_schema.schema.json"), SchemaType.DRAFT_7)
}
```

**route usage**:
```kotlin
val input = call.receive<CreateCompany>(schema = "create_company.schema.json")
```

The `receive(schema)` extension validates raw JSON against the schema BEFORE deserialization. Invalid payloads throw `RequestBodyValidationException` → StatusPages maps to HTTP 400 with error list.

---

## Adding a New Schema — Checklist

Follow these steps when introducing a new endpoint with a request/response schema:

### 1. Create the JSON schema file

```
application/src/main/resources/schemas/<name>.schema.json
```

Use `snake_case` naming. Follow the patterns above for the schema category (create, update, response, polymorphic).

### 2. Register in the schema loader (if used for request validation)

Add a `.register()` line in `ApplicationCall.ext.kt`:

```kotlin
.register(readResourceFile("/schemas/<name>.schema.json"), SchemaType.DRAFT_7)
```

### 3. Add to `openapi.yaml` components/schemas

```yaml
components:
  schemas:
    YourSchemaName:
      $ref: "../schemas/<name>.schema.json"
```

### 4. Reference in the path operation

```yaml
requestBody:
  required: true
  content:
    application/json:
      schema:
        $ref: "#/components/schemas/YourSchemaName"
```

Or in responses:
```yaml
responses:
  "200":
    content:
      application/json:
        schema:
          $ref: "#/components/schemas/YourSchemaName"
```

### 5. Use in the route handler

```kotlin
val input = call.receive<YourKotlinType>(schema = "<name>.schema.json")
```

### 6. Validate and bundle

```bash
npm run validate    # Fix any errors
npm run bundle      # Regenerate documentation.yaml
```

---

## Validation Workflow (End of Implementation)

After finishing all code and OpenAPI changes:

```bash
# Step 1: Validate OpenAPI spec
npm run validate

# Step 2: Run Kotlin quality gates
./gradlew check --no-daemon

# Step 3: If both pass, generate the bundled documentation
npm run bundle

# Step 4: Verify the generated documentation.yaml looks correct
npm run preview
```

If `npm run validate` reports errors, fix the `openapi.yaml` or schema files and re-run. Common issues:
- Missing `operationId` on a path operation
- Schema `$ref` pointing to a non-existent file
- Using `nullable: true` instead of union types
- Missing `security` declaration on an operation
