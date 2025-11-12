# Quick Cheat Sheet - Sistem Contori

## ⚡ Lista Compactă Endpoint-uri

### 🏢 Management Prețuri

```http
# Setare preț la nivel de locație (afectează toți contorii)
PATCH /locations/{locationId}/prices
Body: { counterType, unitPrice, updateAllCounters?, recalculateAll? }

# Obținere preț locație
GET /locations/{locationId}/prices?counterType=WATER

# Toate prețurile unei locații
GET /locations/{locationId}/all-prices

# Setare preț la nivel de contor
PATCH /index-counters/{counterId}/default-price
Body: { defaultUnitPrice, recalculateAll? }

# Setare preț la nivel de citire
PATCH /index-counters/data/{readingId}/price
Body: { unitPrice }
```

### 📊 Statistici

```http
# Statistici complete
GET /index-counters/statistics?startDate=2025-01-01&endDate=2025-12-31&buildingLocation?

# Statistici per tip
GET /index-counters/statistics/by-type?startDate=2025-01-01&endDate=2025-12-31&counterType?
```

### 📄 Rapoarte Excel

```http
# Raport un an
GET /consumption-reports/rental/{rentalAgreementId}/year/{year}
→ Download Excel

# Raport multi-anual
GET /consumption-reports/rental/{rentalAgreementId}/years?start=2023&end=2025
→ Download Excel

# Lista contracte active
GET /consumption-reports/all-active/year/{year}
```

---

## 📊 Structura Prețuri (3 Niveluri)

```
Location (Clădire/Spațiu)
  ├─ WATER: 10.00 RON          ← Nivel 3: Default locație
  ├─ GAS: 5.00 RON
  └─ ELECTRICITY: 0.80 RON
      │
      └─ IndexCounter
           └─ defaultUnitPrice: 11.00  ← Nivel 2: Override contor
               │
               └─ IndexData (citire)
                    └─ unitPrice: 12.00  ← Nivel 1: Override citire (prioritate maximă)
```

**Calcul:**  
`effectiveUnitPrice` = `unitPrice` OR `counter.defaultUnitPrice` OR `location.price[type]`

---

## 📝 Modificări Endpoint-uri Existente

### POST `/index-counters` - Câmp NOU
```json
{
  "defaultUnitPrice": 15.50  // OPTIONAL
}
```

### POST `/index-counters/data` - Câmp NOU
```json
{
  "unitPrice": 16.00  // OPTIONAL
}
```

### Response IndexData - Câmpuri NOI
```json
{
  "unitPrice": 16.00,          // Preț local (null = folosește default)
  "effectiveUnitPrice": 16.00, // Prețul efectiv folosit
  "totalCost": 800.0           // Calculat automat
}
```

---

## 💡 Use Cases Comune

### 1. Setare preț global pentru o clădire
```bash
PATCH /locations/1/prices
{
  "counterType": "WATER",
  "unitPrice": 15.50,
  "updateAllCounters": true,
  "recalculateAll": true
}
```

### 2. Preț special pentru un spațiu
```bash
PATCH /index-counters/123/default-price
{ "defaultUnitPrice": 12.00 }
```

### 3. Preț exceptional pentru o lună
```bash
POST /index-counters/data
{
  "counterId": 123,
  "index": 1500,
  "readingDate": "2025-12-15",
  "unitPrice": 18.00  // Override doar pentru decembrie
}
```

### 4. Download raport Excel
```bash
GET /consumption-reports/rental/2/year/2025
→ Descarcă: "SC-Auto-Adria-SRL-A5-2025.xlsx"
```

---

## 🎯 Counter Types
- `WATER`
- `GAS`
- `ELECTRICITY_220`
- `ELECTRICITY_380`

## 🏢 Building Locations
- `LETCANI`
- `TOMESTI`

## 📅 Date Format
`yyyy-MM-dd` (ex: "2025-11-08")

---

## 🔑 Format Response Standard
```json
{
  "success": true,
  "message": "Mesaj descriptiv",
  "data": { ... }
}
```

