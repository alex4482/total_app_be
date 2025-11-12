# Counter Replacement Guide

## Overview

This guide covers the counter replacement functionality, which allows you to seamlessly replace an old counter with a new one while maintaining data continuity.

## When to Use Counter Replacement

Counter replacement is needed when:
- A physical counter is broken or malfunctioning
- You want to upgrade to a new counter model
- A counter has reached the end of its lifecycle
- A counter needs to be recalibrated or reset

## How It Works

The replacement process:

1. **Records the final reading** of the old counter
2. **Creates a new counter** with the same characteristics (location, type, etc.)
3. **Links the readings** so consumption can be calculated across the replacement boundary
4. **Maintains data continuity** using `ReplacedCounterIndexData`

## Endpoint

### Replace Counter

**POST** `/index-counters/replace`

Replaces an old counter with a new one.

#### Request Body

```json
{
  "oldCounterId": 123,
  "oldCounterFinalIndex": 1500.5,
  "newCounterName": "Contor Nou Apa",
  "newCounterInitialIndex": 0.0,
  "replacementDate": "2025-11-09",
  "counterType": "WATER",
  "locationType": "ROOM",
  "buildingLocation": "LETCANI",
  "defaultUnitPrice": 5.0
}
```

#### Required Fields

| Field | Type | Description |
|-------|------|-------------|
| `oldCounterId` | Long | ID of the counter being replaced |
| `oldCounterFinalIndex` | Double | Final reading from the old counter |
| `newCounterName` | String | Name for the new counter |
| `newCounterInitialIndex` | Double | Initial reading of the new counter |
| `replacementDate` | Date | Date when the replacement occurred (format: `yyyy-MM-dd`) |

#### Optional Fields

| Field | Type | Description |
|-------|------|-------------|
| `counterType` | CounterType | Type of counter (defaults to old counter's type) |
| `locationType` | LocationType | Location type (defaults to old counter's location type) |
| `buildingLocation` | BuildingLocation | Building location (defaults to old counter's building location) |
| `defaultUnitPrice` | Double | Default unit price for the new counter (defaults to old counter's price) |

#### Response

**Success (201 Created):**

```json
{
  "success": true,
  "message": "Counter replaced successfully",
  "data": {
    "id": 456,
    "name": "Contor Nou Apa",
    "counterType": "WATER",
    "locationType": "ROOM",
    "buildingLocation": "LETCANI",
    "defaultUnitPrice": 5.0,
    "location": {
      "id": "ROOM_001",
      "name": "Spatiu 1"
    },
    "indexData": [
      {
        "id": 789,
        "index": 0.0,
        "readingDate": "2025-11-09",
        "consumption": null,
        "unitPrice": null,
        "totalCost": null,
        "effectiveUnitPrice": 5.0,
        "replacementData": {
          "oldIndexData": {
            "id": 788,
            "index": 1500.5,
            "readingDate": "2025-11-09"
          },
          "newCounterInitialIndex": 0.0
        }
      }
    ]
  }
}
```

**Error (400 Bad Request):**

```json
{
  "success": false,
  "message": "oldCounterId is required",
  "data": null
}
```

**Error (404 Not Found):**

```json
{
  "success": false,
  "message": "Old counter not found with id: 123",
  "data": null
}
```

**Error (500 Internal Server Error):**

```json
{
  "success": false,
  "message": "Failed to replace counter: <error details>",
  "data": null
}
```

## Usage Examples

### Example 1: Basic Replacement

Replace a water counter that has reached its maximum reading:

```bash
curl -X POST http://localhost:8080/index-counters/replace \
  -H "Content-Type: application/json" \
  -d '{
    "oldCounterId": 45,
    "oldCounterFinalIndex": 9999.9,
    "newCounterName": "Water Counter - Building A - Room 5 (Replacement)",
    "newCounterInitialIndex": 0.0,
    "replacementDate": "2025-11-09"
  }'
```

This will:
- Use the old counter's type, location, and price settings
- Create a new counter starting at 0.0
- Link the readings for consumption tracking

### Example 2: Replacement with New Settings

Replace an electricity counter and change its default price:

```bash
curl -X POST http://localhost:8080/index-counters/replace \
  -H "Content-Type: application/json" \
  -d '{
    "oldCounterId": 67,
    "oldCounterFinalIndex": 5432.1,
    "newCounterName": "Electricity Counter - New Model",
    "newCounterInitialIndex": 0.0,
    "replacementDate": "2025-11-09",
    "counterType": "ELECTRICITY_220",
    "defaultUnitPrice": 0.85
  }'
```

### Example 3: Replacement with Non-Zero Initial Index

Replace a gas counter where the new counter doesn't start at zero:

```bash
curl -X POST http://localhost:8080/index-counters/replace \
  -H "Content-Type: application/json" \
  -d '{
    "oldCounterId": 89,
    "oldCounterFinalIndex": 3456.7,
    "newCounterName": "Gas Counter - Recalibrated",
    "newCounterInitialIndex": 100.0,
    "replacementDate": "2025-11-09"
  }'
```

## Data Model

### ReplacedCounterIndexData

The replacement creates a special `ReplacedCounterIndexData` entry that:

- Extends `IndexData` with replacement-specific fields
- Links to the old counter's final reading via `oldIndexData`
- Stores the new counter's initial index
- Records the replacement date

This ensures:
- **Data continuity**: Consumption can be calculated across the replacement
- **Audit trail**: You can see when and why counters were replaced
- **Historical accuracy**: Old readings remain unchanged and accessible

## Consumption Calculation

When calculating consumption across a counter replacement:

1. **Before replacement**: Normal consumption calculation using consecutive readings
2. **At replacement**: Consumption = (old final index - old previous index) + (new second reading - new initial index)
3. **After replacement**: Normal consumption calculation on new counter

The `ConsumptionService` handles this automatically when it encounters a `ReplacedCounterIndexData` entry.

## Best Practices

### 1. Timing of Replacement

- Record the replacement on the same date you physically replace the counter
- Take final and initial readings as close together as possible
- If readings can't be simultaneous, use the timestamp feature to record exact times

### 2. Naming Convention

Use clear, descriptive names for new counters:

```
<Type> Counter - <Location> - <Area> (Replacement <Date>)
```

Example: `"Water Counter - Building A - Room 5 (Replacement Nov 2025)"`

### 3. Price Updates

- If the new counter measures differently, update `defaultUnitPrice`
- If tariffs have changed, this is a good time to update prices
- Consider whether to recalculate old readings (usually not recommended)

### 4. Verification

After replacement:

1. Verify the old counter shows the final reading
2. Verify the new counter is created with correct settings
3. Check that consumption calculations work across the boundary
4. Add a note/observation to document the replacement reason

### 5. Migration from Manual Tracking

If you're replacing a counter that wasn't tracked in the system:

```json
{
  "oldCounterId": 123,
  "oldCounterFinalIndex": 5432.1,
  "newCounterName": "Newly Tracked Water Counter",
  "newCounterInitialIndex": 0.0,
  "replacementDate": "2025-11-09"
}
```

## Common Issues

### Issue: "Counter not found"

**Problem**: The old counter ID doesn't exist.

**Solution**: 
- Verify the counter ID using `GET /index-counters`
- Check if the counter was already replaced or deleted

### Issue: "Invalid replacement date"

**Problem**: The replacement date is malformed.

**Solution**: Use the format `yyyy-MM-dd`, e.g., `"2025-11-09"`

### Issue: Consumption not calculating correctly

**Problem**: Consumption shows unexpected values after replacement.

**Solution**:
- Verify the old final index matches the actual reading
- Verify the new initial index is correct
- Check that both readings are on the same date
- Ensure the counter type hasn't changed (unless intentional)

## Integration with Other Features

### Reports

Excel reports automatically handle counter replacements:
- Show old counter readings up to replacement date
- Show new counter readings from replacement date onward
- Calculate total consumption correctly across the boundary

### Statistics

Statistics endpoints aggregate consumption across replacements:
- `GET /index-counters/statistics` includes replaced counters
- Consumption is calculated correctly using linked readings
- Old and new counters are treated as a continuous timeline

### Pricing

The three-tier pricing system applies to both old and new counters:
- Old counter's final reading uses its effective price at the time
- New counter can inherit or override the price settings
- Location-level price changes affect both counters

## Migration Guide

If you need to bulk-replace multiple counters:

1. **Prepare replacement data** in a spreadsheet
2. **Create a script** that calls the endpoint for each replacement
3. **Run replacements** in order (oldest to newest)
4. **Verify** each replacement before proceeding to the next

Example script structure:

```javascript
const replacements = [
  { oldCounterId: 1, oldCounterFinalIndex: 1234.5, ... },
  { oldCounterId: 2, oldCounterFinalIndex: 5678.9, ... },
  // ...
];

for (const replacement of replacements) {
  const response = await fetch('/index-counters/replace', {
    method: 'POST',
    headers: { 'Content-Type': 'application/json' },
    body: JSON.stringify(replacement)
  });
  
  if (!response.ok) {
    console.error(`Failed to replace counter ${replacement.oldCounterId}`);
    break; // Stop on error
  }
}
```

## See Also

- [Counter Management Guide](./08-reading-and-index.md)
- [Consumption Calculation Guide](./15-consumption-full-api.md)
- [Price Management Guide](./14-location-prices.md)
- [Excel Reports Guide](./12-consumption-reports.md)

