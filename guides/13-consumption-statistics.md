# Consumption Statistics API

Base URL: `/index-counters/statistics`

API pentru obținerea statisticilor agregate de consum pentru contoare.

---

## Endpoints

### 1. Get Complete Statistics
**GET** `/index-counters/statistics`

Obține statistici complete de consum pentru o perioadă, cu agregări multiple.

#### Query Parameters
- `startDate` (String, **required**) - Data început în format `yyyy-MM-dd` (ex: "2025-01-01")
- `endDate` (String, **required**) - Data sfârșit în format `yyyy-MM-dd` (ex: "2025-12-31")
- `buildingLocation` (BuildingLocation, optional) - Filtrare pe locație: "LETCANI", "TOMESTI"

#### Response (200 OK)
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
    },
    "byBuilding": { ... }
  }
}
```

#### Exemplu
```http
GET /index-counters/statistics?startDate=2025-01-01&endDate=2025-12-31
GET /index-counters/statistics?startDate=2025-01-01&endDate=2025-12-31&buildingLocation=LETCANI
```

---

### 2. Get Statistics by Counter Type
**GET** `/index-counters/statistics/by-type`

Obține statistici detaliate per contor individual, opțional filtrate pe tip.

#### Query Parameters
- `startDate` (String, **required**) - Data început `yyyy-MM-dd`
- `endDate` (String, **required**) - Data sfârșit `yyyy-MM-dd`
- `counterType` (CounterType, optional) - Filtrare pe tip: "WATER", "GAS", "ELECTRICITY_220", "ELECTRICITY_380"

#### Response (200 OK)
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

#### Exemplu
```http
GET /index-counters/statistics/by-type?startDate=2025-01-01&endDate=2025-12-31
GET /index-counters/statistics/by-type?startDate=2025-01-01&endDate=2025-12-31&counterType=WATER
```

---

## Modele de Date

### ConsumptionStatistics
```typescript
interface ConsumptionStatistics {
  period: string;                    // "2025" sau "2025-01-2025-12"
  totalConsumption: number;          // Consum total toate tipurile
  totalCost: number;                 // Cost total toate tipurile
  byCounterType: {
    [type: string]: CounterTypeStats;
  };
  byLocation: {
    [location: string]: LocationStats;
  };
  byBuilding: {
    [building: string]: BuildingStats;
  };
}
```

### CounterTypeStats
```typescript
interface CounterTypeStats {
  counterType: string;               // "WATER", "GAS", etc.
  totalConsumption: number;          // Consum total pentru acest tip
  totalCost: number;                 // Cost total pentru acest tip
  readingsCount: number;             // Număr citiri în perioadă
}
```

### LocationStats
```typescript
interface LocationStats {
  locationId: string;
  locationName: string;
  totalConsumption: number;          // Consum total locație
  totalCost: number;                 // Cost total locație
  byCounterType: {                   // Breakdown per tip contor
    [type: string]: number;          // Consum per tip
  };
}
```

---

## Exemple de Utilizare

### Frontend - Dashboard Consum

```typescript
async function loadConsumptionDashboard(year: number) {
  const startDate = `${year}-01-01`;
  const endDate = `${year}-12-31`;
  
  const response = await fetch(
    `/index-counters/statistics?startDate=${startDate}&endDate=${endDate}`,
    {
      headers: {
        'Authorization': `Bearer ${accessToken}`
      }
    }
  );
  
  const { data: stats } = await response.json();
  
  // Afișează total general
  console.log(`Total consum ${year}: ${stats.totalConsumption}`);
  console.log(`Total cost ${year}: ${stats.totalCost} RON`);
  
  // Chart per tip contor
  const chartData = Object.entries(stats.byCounterType).map(([type, data]) => ({
    name: type,
    consumption: data.totalConsumption,
    cost: data.totalCost
  }));
  
  renderChart(chartData);
}
```

### Frontend - Comparație Perioade

```typescript
async function compareConsumption(
  period1Start: string,
  period1End: string,
  period2Start: string,
  period2End: string
) {
  // Obține statistici pentru ambele perioade
  const [stats1, stats2] = await Promise.all([
    fetch(`/index-counters/statistics?startDate=${period1Start}&endDate=${period1End}`)
      .then(r => r.json()),
    fetch(`/index-counters/statistics?startDate=${period2Start}&endDate=${period2End}`)
      .then(r => r.json())
  ]);
  
  // Calculează diferențe
  const consumptionDiff = stats2.data.totalConsumption - stats1.data.totalConsumption;
  const costDiff = stats2.data.totalCost - stats1.data.totalCost;
  
  console.log(`Diferență consum: ${consumptionDiff} (${(consumptionDiff / stats1.data.totalConsumption * 100).toFixed(2)}%)`);
  console.log(`Diferență cost: ${costDiff} RON`);
}
```

### Frontend - Statistici per Locație

```typescript
async function showLocationConsumption(startDate: string, endDate: string) {
  const response = await fetch(
    `/index-counters/statistics?startDate=${startDate}&endDate=${endDate}`,
    {
      headers: {
        'Authorization': `Bearer ${accessToken}`
      }
    }
  );
  
  const { data: stats } = await response.json();
  
  // Sortează locațiile după consum
  const sortedLocations = Object.entries(stats.byLocation)
    .sort(([, a], [, b]) => b.totalConsumption - a.totalConsumption);
  
  sortedLocations.forEach(([locationName, locationData]) => {
    console.log(`${locationName}: ${locationData.totalConsumption} → ${locationData.totalCost} RON`);
    
    // Breakdown per tip
    Object.entries(locationData.byCounterType).forEach(([type, consumption]) => {
      console.log(`  - ${type}: ${consumption}`);
    });
  });
}
```

---

## Use Cases

### 1. Dashboard General
```http
GET /index-counters/statistics?startDate=2025-01-01&endDate=2025-12-31
```
→ Overview complet consum anual

### 2. Raportare Lunară
```http
GET /index-counters/statistics?startDate=2025-11-01&endDate=2025-11-30
```
→ Statistici pentru noiembrie 2025

### 3. Analiză per Clădire
```http
GET /index-counters/statistics?startDate=2025-01-01&endDate=2025-12-31&buildingLocation=LETCANI
```
→ Doar consumul pentru clădirea de la Letcani

### 4. Monitorizare Apă
```http
GET /index-counters/statistics/by-type?startDate=2025-01-01&endDate=2025-12-31&counterType=WATER
```
→ Detalii per contor de apă

---

## Note Importante

1. **Datele sunt calculate în timp real** - nu sunt pre-agregate
2. **Consumul** = suma tuturor citirilor din perioadă
3. **Costul** = suma tuturor `totalCost` din citiri
4. **Period format** = determinat automat bazat pe `startDate` și `endDate`
5. **Filtrele sunt opționale** - poți obține statistici pentru toate locațiile

---

## Erori Posibile

### 400 Bad Request
```json
{
  "success": false,
  "message": "startDate and endDate are required"
}
```
**Cauză:** Parametri lipsă

### 400 Bad Request
```json
{
  "success": false,
  "message": "Invalid date format. Use yyyy-MM-dd"
}
```
**Cauză:** Format dată invalid

### 500 Internal Server Error
```json
{
  "success": false,
  "message": "Failed to get statistics: ..."
}
```
**Cauză:** Eroare la calculare statistici

---

## Vezi și

- [Index Counters API](./06-index-counters.md) - Management contoare și citiri
- [Consumption Reports](./12-consumption-reports.md) - Rapoarte Excel
- [Location Prices](./14-location-prices.md) - Management prețuri

