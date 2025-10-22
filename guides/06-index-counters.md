# Index Counters API

Base URL: `/index-counters`

Această API gestionează contoarele de index (apă, gaz, electricitate, etc.) și citirile acestora.

## Endpoints

### 1. List Counters
**GET** `/index-counters`

Returnează lista contoarele filtrate după tip, locație și owner.

#### Query Parameters (toate **required**)
- `type` (CounterType) - Tipul contorului: "WATER", "GAS", "ELECTRICITY", etc.
- `locationId` (String) - ID-ul locației
- `locationType` (LocationType) - Tipul locației
- `buildingLocation` (BuildingLocation) - Locația clădirii: "LETCANI", "TOMESTI"

#### Response (200 OK)
```json
[
  {
    "id": 1,
    "type": "WATER",
    "serialNumber": "WM123456",
    "locationId": "5",
    "locationType": "ROOM",
    "buildingLocation": "LETCANI",
    "readings": []
  }
]
```

---

### 2. Get Counter by ID
**GET** `/index-counters/{id}`

Returnează detaliile unui contor specific.

#### Path Parameters
- `id` (Long, **required**) - ID-ul contorului

#### Response (200 OK)
```json
{
  "id": 1,
  "type": "WATER",
  "serialNumber": "WM123456",
  "locationId": "5",
  "locationType": "ROOM",
  "buildingLocation": "LETCANI",
  "readings": [
    {
      "id": 10,
      "value": 1234.5,
      "date": "2024-01-15T10:00:00",
      "notes": "Citire lunară"
    }
  ]
}
```

---

### 3. Add Counter
**POST** `/index-counters`

Creează un contor nou.

#### Request Body
```json
{
  "type": "WATER",
  "serialNumber": "WM123456",
  "locationId": "5",
  "locationType": "ROOM",
  "buildingLocation": "LETCANI"
}
```

#### Response (200 OK)
```json
{
  "id": 1,
  "type": "WATER",
  "serialNumber": "WM123456",
  "locationId": "5",
  "locationType": "ROOM",
  "buildingLocation": "LETCANI",
  "readings": []
}
```

---

### 4. Add Counter Reading
**POST** `/index-counters/data`

Adaugă o citire nouă pentru un contor.

#### Request Body
```json
{
  "counterId": 1,
  "value": 1234.5,
  "date": "2024-01-15T10:00:00",
  "notes": "Citire lunară ianuarie"
}
```

#### Response (200 OK)
```json
{
  "id": 10,
  "value": 1234.5,
  "date": "2024-01-15T10:00:00",
  "notes": "Citire lunară ianuarie",
  "counter": {
    "id": 1,
    "type": "WATER",
    "serialNumber": "WM123456"
  }
}
```

---

## Model de Date

### IndexCounter
```typescript
interface IndexCounter {
  id: number;
  type: CounterType;           // "WATER", "GAS", "ELECTRICITY"
  serialNumber: string;        // Număr de serie contor
  locationId: string;
  locationType: LocationType;  // "ROOM", "BUILDING", etc.
  buildingLocation: string;    // "LETCANI", "TOMESTI"
  readings: IndexData[];       // Lista citirilor
}
```

### IndexData
```typescript
interface IndexData {
  id: number;
  value: number;               // Valoarea citită
  date: string;                // Data citiri (ISO format)
  notes?: string;              // Observații
  counter?: IndexCounter;      // Referință la contor
}
```

### CounterType (enum)
- `WATER` - Apă
- `GAS` - Gaz
- `ELECTRICITY` - Electricitate

### LocationType (enum)
- `ROOM` - Cameră
- `BUILDING` - Clădire
- (alte tipuri posibile)

---

## Note pentru Frontend
1. Toate query parameters pentru listare sunt **obligatorii**
2. Citirile sunt ordonate cronologic (cele mai recente primul)
3. Un contor poate avea multiple citiri de-a lungul timpului
4. `serialNumber` trebuie să fie unic pentru fiecare contor

