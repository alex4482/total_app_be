# Exemple Practice de Utilizare

## 📋 8 Scenarii Complete Step-by-Step

---

## Scenario 1: Setup Inițial - Clădire Nouă

### Step 1: Creare Contor cu Preț Default
```http
POST /index-counters
Content-Type: application/json

{
  "name": "Contor Apa Hala C8 - A5/3",
  "locationId": 15,
  "counterType": "WATER",
  "locationType": "RENTAL_SPACE",
  "buildingLocation": "LETCANI",
  "defaultUnitPrice": 10.50
}
```

**Response 201:**
```json
{
  "success": true,
  "message": "Counter created successfully",
  "data": {
    "id": 123,
    "name": "Contor Apa Hala C8 - A5/3",
    "counterType": "WATER",
    "defaultUnitPrice": 10.50,
    "location": {
      "id": 15,
      "name": "A5/3"
    }
  }
}
```

### Step 2: Prima Citire
```http
POST /index-counters/data
Content-Type: application/json

{
  "counterId": 123,
  "index": 1000.0,
  "readingDate": "2025-01-01"
}
```

**Response 201:**
```json
{
  "success": true,
  "message": "Index data added successfully",
  "data": {
    "id": 1001,
    "index": 1000.0,
    "consumption": 0.0,
    "readingDate": "2025-01-01",
    "unitPrice": null,
    "effectiveUnitPrice": 10.50,
    "totalCost": 0.0
  }
}
```

### Step 3: A Doua Citire (după o lună)
```http
POST /index-counters/data
Content-Type: application/json

{
  "counterId": 123,
  "index": 1050.5,
  "readingDate": "2025-02-01"
}
```

**Response 201:**
```json
{
  "success": true,
  "message": "Index data added successfully",
  "data": {
    "id": 1002,
    "index": 1050.5,
    "consumption": 50.5,
    "readingDate": "2025-02-01",
    "unitPrice": null,
    "effectiveUnitPrice": 10.50,
    "totalCost": 530.25
  }
}
```

---

## Scenario 2: Schimbare Preț Global

### Actualizare Preț la Nivel de Locație
```http
PATCH /locations/15/prices
Content-Type: application/json

{
  "counterType": "WATER",
  "unitPrice": 12.00,
  "updateAllCounters": true,
  "recalculateAll": true
}
```

**Response 200:**
```json
{
  "success": true,
  "message": "Price updated for WATER, all counter prices updated and costs recalculated",
  "data": {
    "id": 15,
    "name": "A5/3",
    "counterTypePrices": [
      {
        "counterType": "WATER",
        "unitPrice": 12.00
      }
    ],
    "counters": [
      {
        "id": 123,
        "defaultUnitPrice": 12.00
      }
    ]
  }
}
```

**Efect:** Toate citirile anterioare (fără unitPrice local) vor avea acum:
- `effectiveUnitPrice`: 12.00
- `totalCost`: recalculat cu 12.00

---

## Scenario 3: Preț Special Decembrie

### Citire cu Preț Override
```http
POST /index-counters/data
Content-Type: application/json

{
  "counterId": 123,
  "index": 1150.0,
  "readingDate": "2025-12-01",
  "unitPrice": 15.00
}
```

**Response 201:**
```json
{
  "success": true,
  "message": "Index data added successfully",
  "data": {
    "id": 1012,
    "index": 1150.0,
    "consumption": 45.0,
    "readingDate": "2025-12-01",
    "unitPrice": 15.00,
    "effectiveUnitPrice": 15.00,
    "totalCost": 675.00
  }
}
```

---

## Scenario 4: Vizualizare Statistici

### Statistici An 2025
```http
GET /index-counters/statistics?startDate=2025-01-01&endDate=2025-12-31
```

**Response 200:**
```json
{
  "success": true,
  "data": {
    "period": "2025",
    "totalConsumption": 12450.5,
    "totalCost": 156380.25,
    "byCounterType": {
      "WATER": {
        "counterType": "WATER",
        "totalConsumption": 3500.0,
        "totalCost": 42000.0,
        "readingsCount": 120
      },
      "ELECTRICITY_220": {
        "counterType": "ELECTRICITY_220",
        "totalConsumption": 8950.5,
        "totalCost": 114380.25,
        "readingsCount": 120
      }
    },
    "byLocation": {
      "A5/3": {
        "locationId": "A5/3",
        "locationName": "A5/3",
        "totalConsumption": 1250.0,
        "totalCost": 15000.0,
        "byCounterType": {
          "WATER": 450.0,
          "ELECTRICITY_220": 800.0
        }
      }
    }
  }
}
```

---

## Scenario 5: Download Raport Excel

### Download Raport pentru Contract 2, Anul 2025
```http
GET /consumption-reports/rental/2/year/2025
Accept: application/octet-stream
```

**Response 200:**
```
Content-Type: application/octet-stream
Content-Disposition: attachment; filename="SC-Auto-Adria-SRL-A5_3-2025.xlsx"
Content-Length: 45678

[Binary Excel Data]
```

**Rezultat:** Se descarcă fișierul Excel cu:
- Header: Tenant info, contract, spațiu
- Tabel consumuri lunare per tip contor
- Calcule automate: TOTAL, Cota întreținere 3%, TOTAL FINAL

---

## Scenario 6: Corecție Preț

### Ștergere Override la o Citire
```http
PATCH /index-counters/data/1012/price
Content-Type: application/json

{
  "unitPrice": null
}
```

**Response 200:**
```json
{
  "success": true,
  "message": "Local price cleared - now using default price",
  "data": {
    "id": 1012,
    "index": 1150.0,
    "consumption": 45.0,
    "readingDate": "2025-12-01",
    "unitPrice": null,
    "effectiveUnitPrice": 12.00,
    "totalCost": 540.00
  }
}
```

**Efect:** Citirea folosește acum prețul de la contor/locație (12.00) în loc de 15.00

---

## Scenario 7: Setup Multi-Locație

### Setare Prețuri pentru Toată Clădirea
```http
# Apă
PATCH /locations/1/prices
{ "counterType": "WATER", "unitPrice": 10.00 }

# Gaz
PATCH /locations/1/prices
{ "counterType": "GAS", "unitPrice": 5.50 }

# Electricitate
PATCH /locations/1/prices
{ "counterType": "ELECTRICITY_220", "unitPrice": 0.85 }
```

### Verificare Prețuri
```http
GET /locations/1/all-prices
```

**Response 200:**
```json
{
  "success": true,
  "data": {
    "id": 1,
    "name": "Hala C8",
    "counterTypePrices": [
      { "counterType": "WATER", "unitPrice": 10.00 },
      { "counterType": "GAS", "unitPrice": 5.50 },
      { "counterType": "ELECTRICITY_220", "unitPrice": 0.85 }
    ]
  }
}
```

---

## Scenario 8: Raport Multi-Anual

### Download Raport 2023-2025
```http
GET /consumption-reports/rental/2/years?start=2023&end=2025
```

**Response 200:**
```
Content-Type: application/octet-stream
Content-Disposition: attachment; filename="SC-Auto-Adria-SRL-A5_3-2023-2025.xlsx"

[Excel cu 3 sheets: ANUL 2023, ANUL 2024, ANUL 2025]
```

---

## 🧪 Testing Checklist

- [ ] Creare contor cu și fără `defaultUnitPrice`
- [ ] Adăugare citire cu și fără `unitPrice`
- [ ] Verificare calcul automat `totalCost`
- [ ] Actualizare preț locație cu `recalculateAll: true`
- [ ] Actualizare preț locație cu `recalculateAll: false`
- [ ] Actualizare preț contor individual
- [ ] Ștergere preț local (`unitPrice: null`)
- [ ] Download Excel un an
- [ ] Download Excel multi-anual
- [ ] Statistici cu filtre
- [ ] Statistici fără filtre

---

## 🔍 Debugging Tips

### Verifică prețul efectiv folosit:
```http
GET /index-counters/{counterId}
```
→ Verifică `indexData[].effectiveUnitPrice` vs `unitPrice` vs `defaultUnitPrice`

### Verifică prețurile locației:
```http
GET /locations/{locationId}/all-prices
```

### Test recalculare:
1. Schimbă preț cu `recalculateAll: false`
2. Verifică că `totalCost` nu s-a schimbat
3. Schimbă preț cu `recalculateAll: true`
4. Verifică că `totalCost` s-a actualizat

