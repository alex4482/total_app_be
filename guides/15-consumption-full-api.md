# API Endpoints - Sistem Contori și Consum

## 📊 Documentație Completă - Toate Endpoint-urile

---

## 1. Management Prețuri la Nivel de Locație

### **PATCH** `/locations/{locationId}/prices`
Actualizează prețul default pentru un tip de contor la nivel de locație.
Toate contoarele de acel tip din locație vor folosi acest preț.

**Request Body:**
```json
{
  "counterType": "WATER",           // Required: "WATER", "GAS", "ELECTRICITY_220", "ELECTRICITY_380"
  "unitPrice": 15.50,               // Required: preț per unitate (RON)
  "updateAllCounters": true,        // Optional: actualizează și contorii individuali
  "recalculateAll": true            // Optional: recalculează toate costurile istorice
}
```

**Response 200:**
```json
{
  "success": true,
  "message": "Price updated for WATER, all counter prices updated and costs recalculated",
  "data": {
    "id": 1,
    "name": "Hala C8",
    "counterTypePrices": [
      { "counterType": "WATER", "unitPrice": 15.50 }
    ]
  }
}
```

---

### **GET** `/locations/{locationId}/prices?counterType=WATER`
Obține prețul default pentru un tip de contor specific la o locație.

**Query Parameters:**
- `counterType` (required): "WATER", "GAS", "ELECTRICITY_220", "ELECTRICITY_380"

**Response 200:**
```json
{
  "success": true,
  "data": 15.50
}
```

---

### **GET** `/locations/{locationId}/all-prices`
Obține toate prețurile configurate pentru o locație.

**Response 200:**
```json
{
  "success": true,
  "data": {
    "id": 1,
    "name": "Hala C8",
    "counterTypePrices": [
      { "counterType": "WATER", "unitPrice": 10.50 },
      { "counterType": "ELECTRICITY_220", "unitPrice": 0.85 },
      { "counterType": "GAS", "unitPrice": 5.00 }
    ],
    "counters": [...]
  }
}
```

---

## 2. Management Prețuri la Nivel de Contor

### **PATCH** `/index-counters/{counterId}/default-price`
Actualizează prețul default pentru un contor specific (override față de locație).

**Request Body:**
```json
{
  "defaultUnitPrice": 16.00,    // Required: preț per unitate
  "recalculateAll": true        // Optional: recalculează costurile citirilor
}
```

**Response 200:**
```json
{
  "success": true,
  "message": "Default price updated and all costs recalculated",
  "data": {
    "id": 123,
    "name": "Contor apa A1",
    "defaultUnitPrice": 16.00,
    "counterType": "WATER"
  }
}
```

---

### **PATCH** `/index-counters/data/{readingId}/price`
Actualizează prețul pentru o citire specifică (override față de contor/locație).

**Request Body:**
```json
{
  "unitPrice": 17.00    // Poate fi null pentru a șterge override-ul
}
```

**Response 200:**
```json
{
  "success": true,
  "message": "Local price updated successfully",
  "data": {
    "id": 456,
    "index": 1500.5,
    "consumption": 50.0,
    "unitPrice": 17.00,
    "effectiveUnitPrice": 17.00,
    "totalCost": 850.00
  }
}
```

---

## 3. Statistici Consum

### **GET** `/index-counters/statistics`
Obține statistici complete de consum pentru o perioadă.

**Query Parameters:**
- `startDate` (required): "yyyy-MM-dd" (ex: "2025-01-01")
- `endDate` (required): "yyyy-MM-dd" (ex: "2025-12-31")
- `buildingLocation` (optional): "LETCANI", "TOMESTI"

**Response 200:**
```json
{
  "success": true,
  "data": {
    "period": "2025",
    "totalConsumption": 15000.0,
    "totalCost": 225000.0,
    "byCounterType": {
      "WATER": {
        "counterType": "WATER",
        "totalConsumption": 5000.0,
        "totalCost": 75000.0,
        "readingsCount": 120
      },
      "ELECTRICITY_220": {
        "counterType": "ELECTRICITY_220",
        "totalConsumption": 10000.0,
        "totalCost": 150000.0,
        "readingsCount": 120
      }
    },
    "byLocation": { ... },
    "byBuilding": { ... }
  }
}
```

---

### **GET** `/index-counters/statistics/by-type`
Obține statistici detaliate per tip de contor.

**Query Parameters:**
- `startDate` (required): "yyyy-MM-dd"
- `endDate` (required): "yyyy-MM-dd"
- `counterType` (optional): filtrare pe tip specific

**Response 200:**
```json
{
  "success": true,
  "data": {
    "Contor Apa A1": {
      "counterType": "WATER",
      "totalConsumption": 500.0,
      "totalCost": 7500.0,
      "readingsCount": 12
    },
    "Contor Apa A2": {
      "counterType": "WATER",
      "totalConsumption": 450.0,
      "totalCost": 6750.0,
      "readingsCount": 12
    }
  }
}
```

---

## 4. Rapoarte Excel

### **GET** `/consumption-reports/rental/{rentalAgreementId}/year/{year}`
Generează și descarcă raport Excel pentru un contract și an specific.

**Path Parameters:**
- `rentalAgreementId`: ID-ul contractului de închiriere
- `year`: Anul pentru care se generează raportul (ex: 2025)

**Response 200:**
- Content-Type: `application/octet-stream`
- Content-Disposition: `attachment; filename="<tenant>-<spatiu>-<an>.xlsx"`
- Body: Binary Excel file

**Exemplu:**
```
GET /consumption-reports/rental/2/year/2025
→ Descarcă: "SC-Auto-Adria-SRL-A5_3-2025.xlsx"
```

---

### **GET** `/consumption-reports/rental/{rentalAgreementId}/years?start=2023&end=2025`
Generează raport Excel multi-anual (o fișă per an).

**Path Parameters:**
- `rentalAgreementId`: ID-ul contractului

**Query Parameters:**
- `start` (optional): An început (default: anul curent)
- `end` (optional): An final (default: anul curent)

**Response 200:**
- Content-Type: `application/octet-stream`
- Excel cu mai multe sheets (câte unul per an)

---

### **GET** `/consumption-reports/all-active/year/{year}`
Listează toate contractele active pentru care se pot genera rapoarte.

**Response 200:**
```json
{
  "success": true,
  "message": "Found 5 active rental agreements",
  "data": [
    {
      "rentalAgreementId": 2,
      "tenantName": "SC Auto Adria SRL",
      "spaceName": "A5/3"
    },
    {
      "rentalAgreementId": 3,
      "tenantName": "AIRSOFT IASI S.R.L.",
      "spaceName": "A1"
    }
  ]
}
```

---

## 5. Endpoint-uri Existente Modificate

### **POST** `/index-counters`
Creare contor - ADĂUGAT câmp `defaultUnitPrice`.

**Request Body:**
```json
{
  "name": "Contor Apa A1",
  "locationId": 1,
  "counterType": "WATER",
  "locationType": "RENTAL_SPACE",
  "buildingLocation": "LETCANI",
  "defaultUnitPrice": 15.50    // OPTIONAL: preț default pentru acest contor
}
```

---

### **POST** `/index-counters/data`
Adăugare citire - ADĂUGAT câmp `unitPrice`.

**Request Body:**
```json
{
  "counterId": 123,
  "index": 1500.5,
  "readingDate": "2025-11-08",
  "unitPrice": 16.00    // OPTIONAL: preț specific pentru această citire
}
```

**Response - câmpuri noi în IndexData:**
```json
{
  "id": 456,
  "index": 1500.5,
  "consumption": 50.0,
  "readingDate": "2025-11-08",
  "unitPrice": 16.00,              // NEW: preț local (poate fi null)
  "effectiveUnitPrice": 16.00,     // NEW: prețul efectiv folosit
  "totalCost": 800.0               // NEW: cost total calculat automat
}
```

---

## 📋 Prioritate Prețuri (pentru Frontend)

Sistem cu 3 niveluri de prețuri:

```
1. IndexData.unitPrice           (cel mai specific - override la citire)
   ↓
2. IndexCounter.defaultUnitPrice  (override la contor)
   ↓
3. Location.counterTypePrices     (default la locație)
```

**Afișare în UI:**
- Dacă `unitPrice` != null → afișează "Preț specific: 17.00 RON"
- Dacă `effectiveUnitPrice` != `unitPrice` → afișează "Folosește preț: 15.50 RON (default locație)"

---

## 🎨 Exemple de Integrare Frontend

### Actualizare Preț la Nivel de Clădire
```typescript
async function updateBuildingPrice(locationId: number, counterType: string, price: number) {
  const response = await fetch(`/locations/${locationId}/prices`, {
    method: 'PATCH',
    headers: { 'Content-Type': 'application/json' },
    body: JSON.stringify({
      counterType: counterType,
      unitPrice: price,
      updateAllCounters: true,
      recalculateAll: true
    })
  });
  return response.json();
}
```

### Download Raport Excel
```typescript
async function downloadConsumptionReport(rentalAgreementId: number, year: number) {
  const response = await fetch(
    `/consumption-reports/rental/${rentalAgreementId}/year/${year}`
  );
  const blob = await response.blob();
  const url = window.URL.createObjectURL(blob);
  const a = document.createElement('a');
  a.href = url;
  a.download = `raport-${year}.xlsx`;
  a.click();
}
```

### Obținere Statistici
```typescript
async function getConsumptionStats(startDate: string, endDate: string) {
  const response = await fetch(
    `/index-counters/statistics?startDate=${startDate}&endDate=${endDate}`
  );
  return response.json();
}
```

---

## ⚠️ Note Importante

1. **Toate endpoint-urile returnează format consistent:**
   ```json
   { "success": true/false, "message": "...", "data": {...} }
   ```

2. **Datele sunt în format:** `yyyy-MM-dd` (ex: "2025-11-08")

3. **Calcul automat:** `totalCost = consumption × effectiveUnitPrice`

4. **Recalculare:** Când actualizezi un preț cu `recalculateAll: true`, doar citirile fără `unitPrice` local se recalculează

5. **Counter Types disponibile:**
   - `ELECTRICITY_220`
   - `ELECTRICITY_380`
   - `GAS`
   - `WATER`

6. **Building Locations disponibile:**
   - `LETCANI`
   - `TOMESTI`

