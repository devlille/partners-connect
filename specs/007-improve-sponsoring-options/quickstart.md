# Quickstart: Enhanced Sponsoring Options

## Feature Overview
This quickstart demonstrates the four types of sponsoring options and how partners interact with them during partnership creation.

## Prerequisites
- Partners Connect server running locally
- Database with test event and sponsoring packs
- Test company and organizer accounts

## Test Scenarios

### Scenario 1: Create Text Option (Existing Behavior)
**Purpose**: Verify backward compatibility with existing text-only options

```bash
# Create text option (current behavior)
curl -X POST http://localhost:8080/orgs/devlille/events/devlille-2025/options \
  -H "Content-Type: application/json" \
  -H "Authorization: Bearer $ORG_TOKEN" \
  -d '{
    "translations": [
      {
        "language": "en",
        "name": "Social Media Mention",
        "description": "We will mention your company on our social media channels"
      },
      {
        "language": "fr", 
        "name": "Mention sur les réseaux sociaux",
        "description": "Nous mentionnerons votre entreprise sur nos réseaux sociaux"
      }
    ],
    "price": 500
  }'

# Expected: HTTP 201 with option ID
# Verify: Option created with type=TEXT, no additional metadata
```

### Scenario 2: Create Typed Quantitative Option
**Purpose**: Test quantitative options where partners select quantities

```bash
# Create typed quantitative option
curl -X POST http://localhost:8080/orgs/devlille/events/devlille-2025/options \
  -H "Content-Type: application/json" \
  -H "Authorization: Bearer $ORG_TOKEN" \
  -d '{
    "translations": [
      {
        "language": "en",
        "name": "Job Offers",
        "description": "Post job offers on our job board"
      },
      {
        "language": "fr",
        "name": "Offres d'\''emploi", 
        "description": "Publier des offres d'\''emploi sur notre site"
      }
    ],
    "price": 100,
    "type": "typed_quantitative",
    "type_descriptor": "job_offer"
  }'

# Expected: HTTP 201 with option ID
# Verify: Option created with type=TYPED_QUANTITATIVE, typeDescriptor set
```

### Scenario 3: Create Typed Number Option
**Purpose**: Test fixed quantity options set by organizers

```bash
# Create typed number option
curl -X POST http://localhost:8080/orgs/devlille/events/devlille-2025/options \
  -H "Content-Type: application/json" \
  -H "Authorization: Bearer $ORG_TOKEN" \
  -d '{
    "translations": [
      {
        "language": "en",
        "name": "Conference Tickets",
        "description": "Free tickets for your team members"
      },
      {
        "language": "fr",
        "name": "Billets de conférence",
        "description": "Billets gratuits pour vos équipes"
      }
    ],
    "price": null,
    "type": "typed_number",
    "type_descriptor": "nb_ticket",
    "fixed_quantity": 5
  }'

# Expected: HTTP 201 with option ID
# Verify: Option created with fixedQuantity=5, price=null (included in pack)
```

### Scenario 4: Create Typed Selectable Option
**Purpose**: Test selectable value options with predefined choices

```bash
# Create typed selectable option
curl -X POST http://localhost:8080/orgs/devlille/events/devlille-2025/options \
  -H "Content-Type: application/json" \
  -H "Authorization: Bearer $ORG_TOKEN" \
  -d '{
    "translations": [
      {
        "language": "en",
        "name": "Exhibition Booth",
        "description": "Physical booth space at the venue"
      },
      {
        "language": "fr",
        "name": "Stand d'\''exposition",
        "description": "Espace de stand physique sur le lieu"
      }
    ],
    "price": 1000,
    "type": "typed_selectable",
    "type_descriptor": "booth",
    "selectable_values": ["3x3m", "3x6m", "6x6m"]
  }'

# Expected: HTTP 201 with option ID
# Verify: Option created with 3 selectable values
```

### Scenario 5: Create Partnership with Mixed Selections
**Purpose**: Test partnership creation with different option types and selections

```bash
# First, attach options to a pack (assuming pack exists)
curl -X POST http://localhost:8080/orgs/devlille/events/devlille-2025/packs/$PACK_ID/options \
  -H "Content-Type: application/json" \
  -H "Authorization: Bearer $ORG_TOKEN" \
  -d '{
    "required": [],
    "optional": ["'$TEXT_OPTION_ID'", "'$QUANTITATIVE_OPTION_ID'", "'$SELECTABLE_OPTION_ID'"]
  }'

# Create partnership with selections
curl -X POST http://localhost:8080/events/devlille-2025/partnerships \
  -H "Content-Type: application/json" \
  -d '{
    "company_id": "'$COMPANY_ID'",
    "pack_id": "'$PACK_ID'",
    "option_ids": ["'$TEXT_OPTION_ID'", "'$QUANTITATIVE_OPTION_ID'", "'$SELECTABLE_OPTION_ID'"],
    "option_selections": {
      "'$QUANTITATIVE_OPTION_ID'": {
        "quantity": 3
      },
      "'$SELECTABLE_OPTION_ID'": {
        "value": "3x6m"
      }
    },
    "contact_name": "John Doe",
    "contact_role": "Marketing Manager", 
    "language": "en"
  }'

# Expected: HTTP 201 with partnership ID
# Verify: Partnership created with correct selections and pricing
```

### Scenario 6: Validate Pricing Calculations
**Purpose**: Verify linear scaling pricing for quantitative options

```bash
# Get partnership details to verify pricing
curl -X GET http://localhost:8080/events/devlille-2025/partnerships/$PARTNERSHIP_ID \
  -H "Accept-Language: en"

# Expected response should show:
# - Text option: price = 500 (base price)
# - Quantitative option: calculatedPrice = 300 (100 * 3 quantity)
# - Selectable option: price = 1000 (base price, no quantity multiplier)
# - totalPrice = 1800 (sum of all calculated prices)
```

### Scenario 7: Test Validation Rules
**Purpose**: Verify validation for different option types

```bash
# Test: Create quantitative option without type descriptor (should fail)
curl -X POST http://localhost:8080/orgs/devlille/events/devlille-2025/options \
  -H "Content-Type: application/json" \
  -H "Authorization: Bearer $ORG_TOKEN" \
  -d '{
    "translations": [{"language": "en", "name": "Invalid Option"}],
    "type": "typed_quantitative"
  }'
# Expected: HTTP 400 - type_descriptor required

# Test: Create selectable option without values (should fail)
curl -X POST http://localhost:8080/orgs/devlille/events/devlille-2025/options \
  -H "Content-Type: application/json" \
  -H "Authorization: Bearer $ORG_TOKEN" \
  -d '{
    "translations": [{"language": "en", "name": "Invalid Option"}],
    "type": "typed_selectable",
    "type_descriptor": "booth",
    "selectable_values": []
  }'
# Expected: HTTP 400 - at least one selectable value required

# Test: Partnership with zero quantity (should exclude option)
curl -X POST http://localhost:8080/events/devlille-2025/partnerships \
  -H "Content-Type: application/json" \
  -d '{
    "company_id": "'$COMPANY_ID'",
    "pack_id": "'$PACK_ID'", 
    "option_ids": ["'$QUANTITATIVE_OPTION_ID'"],
    "option_selections": {
      "'$QUANTITATIVE_OPTION_ID'": {
        "quantity": 0
      }
    },
    "contact_name": "John Doe",
    "contact_role": "Marketing Manager",
    "language": "en"
  }'
# Expected: HTTP 201 - partnership created but quantitative option excluded
```

### Scenario 8: Test Deletion Prevention
**Purpose**: Verify protection against deleting used options/values

```bash
# Try to delete option that's used in partnership (should fail)
curl -X DELETE http://localhost:8080/orgs/devlille/events/devlille-2025/options/$USED_OPTION_ID \
  -H "Authorization: Bearer $ORG_TOKEN"
# Expected: HTTP 409 - option in use by partnerships

# Try to update selectable option by removing a used value (should fail)
curl -X PUT http://localhost:8080/orgs/devlille/events/devlille-2025/options/$SELECTABLE_OPTION_ID \
  -H "Content-Type: application/json" \
  -H "Authorization: Bearer $ORG_TOKEN" \
  -d '{
    "translations": [{"language": "en", "name": "Exhibition Booth"}],
    "type": "typed_selectable",
    "type_descriptor": "booth",
    "selectable_values": ["3x3m", "6x6m"]
  }'
# Expected: HTTP 409 - cannot remove "3x6m" value that's used in partnerships
```

## Success Criteria

### Functional Tests Pass
- [x] Text options work identically to current system
- [x] Quantitative options accept partner quantity selections
- [x] Number options apply fixed quantities automatically  
- [x] Selectable options accept valid value selections
- [x] Pricing calculations are correct (linear scaling)
- [x] Validation prevents invalid configurations
- [x] Deletion protection works for used options/values

### Performance Tests Pass
- [x] All API responses complete within 2 seconds
- [x] Database queries remain efficient with new columns
- [x] No N+1 query issues with selectable values

### Compatibility Tests Pass
- [x] Existing text options continue to work unchanged
- [x] Existing partnerships display correctly
- [x] API responses maintain backward compatibility
- [x] Database migrations apply without data loss

## Troubleshooting

### Common Issues

**Invalid Type Descriptor Error**
```
HTTP 400: type_descriptor required for typed_quantitative options
```
Solution: Add type_descriptor field to request body

**Missing Selectable Values**
```
HTTP 400: at least one selectable value required for typed_selectable
```
Solution: Add selectable_values array with minimum 1 item

**Invalid Partner Selection**
```
HTTP 400: selected_value must be from available selectable_values
```
Solution: Use exact string from option's selectable_values list

**Partnership Pricing Incorrect**
- Check quantitative option: calculatedPrice = price × selectedQuantity
- Check other options: calculatedPrice = price (no quantity multiplier)
- Check totalPrice: sum of all calculatedPrice values

### Debug Commands

```bash
# Check option details including type and metadata
curl -X GET http://localhost:8080/orgs/devlille/events/devlille-2025/options/$OPTION_ID \
  -H "Accept-Language: en"

# Check partnership selections and pricing
curl -X GET http://localhost:8080/events/devlille-2025/partnerships/$PARTNERSHIP_ID \
  -H "Accept-Language: en"

# Check pack options with type information
curl -X GET http://localhost:8080/events/devlille-2025/packs \
  -H "Accept-Language: en"
```