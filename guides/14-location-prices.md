# Location Prices API

Base URL: `/locations/{locationId}/prices`

API pentru management prețuri la nivel de locație (clădiri și spații de închiriat).

**Concept:** Toate contoarele de același tip dintr-o locație împart același preț default, configurat la nivel de locație.

---

## Endpoints

### 1. Update Location Price
**PATCH** `/locations/{locationId}/prices`

Actualizează prețul default pentru un tip de contor la nivel de locație.

#### Path Parameters
- `locationId` (Long, **required**) - ID-ul locației (Building, Room, RentalSpace)

#### Request Body
```json
{
  "counterType": "WATER",           // Required: tip contor
  "unitPrice": 15.50,               // Required: preț per unitate (RON)
  "updateAllCounters": true,        // Optional: actualizează și contorii individuali
  "recalculateAll": true            // Optional: recalculează toate costurile istorice
}
```

**CounterType values:**
- `WATER`
- `GAS`
- `ELECTRICITY_220`
- `ELECTRICITY_380`

#### Response (200 OK)
```json
{
  "success": true,
  "message": "Price updated for WATER, all counter prices updated and costs recalculated",
  "data": {
    "id": 1,
    "name": "Hala C8",
    "counterTypePrices": [
      {
        "counterType": "WATER",
        "unitPrice": 15.50
      }
    ]
  }
}
```

#### Exemplu
```http
PATCH /locations/1/prices
Content-Type: application/json

{
  "counterType": "WATER",
  "unitPrice": 12.00,
  "updateAllCounters": true,
  "recalculateAll": true
}
```

---

### 2. Get Location Price
**GET** `/locations/{locationId}/prices?counterType={type}`

Obține prețul default pentru un tip de contor specific.

#### Path Parameters
- `locationId` (Long, **required**) - ID-ul locației

#### Query Parameters
- `counterType` (CounterType, **required**) - Tipul contorului

#### Response (200 OK)
```json
{
  "success": true,
  "data": 15.50
}
```

**Sau dacă nu există preț configurat:**
```json
{
  "success": true,
  "message": "No price configured for WATER at this location",
  "data": null
}
```

#### Exemplu
```http
GET /locations/1/prices?counterType=WATER
```

---

### 3. Get All Location Prices
**GET** `/locations/{locationId}/all-prices`

Obține toate prețurile configurate pentru o locație.

#### Path Parameters
- `locationId` (Long, **required**) - ID-ul locației

#### Response (200 OK)
```json
{
  "success": true,
  "data": {
    "id": 1,
    "name": "Hala C8",
    "counterTypePrices": [
      { "counterType": "WATER", "unitPrice": 10.50 },
      { "counterType": "GAS", "unitPrice": 5.00 },
      { "counterType": "ELECTRICITY_220", "unitPrice": 0.85 }
    ],
    "counters": [
      {
        "id": 123,
        "name": "Contor Apa A1",
        "counterType": "WATER",
        "defaultUnitPrice": 10.50
      }
    ]
  }
}
```

#### Exemplu
```http
GET /locations/1/all-prices
```

---

## Sistem Prețuri - 3 Niveluri

### Prioritate (de la cel mai specific la cel mai general):

```
1. IndexData.unitPrice              (Preț la nivel de citire - override)
   ↓
2. IndexCounter.defaultUnitPrice    (Preț la nivel de contor - override)
   ↓
3. Location.counterTypePrices       (Preț la nivel de locație - default)
```

**Exemplu:**
- Locația "Hala C8" are WATER: 10.00 RON
- Contorul "Contor A1" are defaultUnitPrice: null → folosește 10.00
- Contorul "Contor A2" are defaultUnitPrice: 12.00 → folosește 12.00
- Citirea din decembrie are unitPrice: 15.00 → folosește 15.00

---

## Modele de Date

### LocationPriceUpdateDto
```typescript
interface LocationPriceUpdateDto {
  counterType: CounterType;          // Required
  unitPrice: number;                 // Required
  updateAllCounters?: boolean;       // Optional, default: false
  recalculateAll?: boolean;          // Optional, default: false
}
```

### CounterTypePrice
```typescript
interface CounterTypePrice {
  counterType: CounterType;
  unitPrice: number;
}
```

---

## Exemple de Utilizare

### Frontend - Setup Prețuri Clădire

```typescript
async function setupBuildingPrices(locationId: number) {
  const prices = [
    { counterType: 'WATER', unitPrice: 10.00 },
    { counterType: 'GAS', unitPrice: 5.50 },
    { counterType: 'ELECTRICITY_220', unitPrice: 0.85 }
  ];
  
  for (const price of prices) {
    await fetch(`/locations/${locationId}/prices`, {
      method: 'PATCH',
      headers: {
        'Content-Type': 'application/json',
        'Authorization': `Bearer ${accessToken}`
      },
      body: JSON.stringify({
        counterType: price.counterType,
        unitPrice: price.unitPrice,
        updateAllCounters: true,
        recalculateAll: false  // Prima configurare, nu e nevoie de recalculare
      })
    });
  }
  
  console.log('Prețuri configurate pentru toate tipurile de contoare');
}
```

### Frontend - Actualizare Preț cu Recalculare

```typescript
async function updateWaterPrice(locationId: number, newPrice: number) {
  const response = await fetch(`/locations/${locationId}/prices`, {
    method: 'PATCH',
    headers: {
      'Content-Type': 'application/json',
      'Authorization': `Bearer ${accessToken}`
    },
    body: JSON.stringify({
      counterType: 'WATER',
      unitPrice: newPrice,
      updateAllCounters: true,   // Sincronizează toate contoarele
      recalculateAll: true        // Recalculează istoric
    })
  });
  
  const { message } = await response.json();
  console.log(message);
}
```

### Frontend - UI pentru Management Prețuri

```typescript
async function showPriceManager(locationId: number) {
  // Obține prețurile actuale
  const response = await fetch(
    `/locations/${locationId}/all-prices`,
    {
      headers: {
        'Authorization': `Bearer ${accessToken}`
      }
    }
  );
  
  const { data: location } = await response.json();
  
  // Afișează în UI
  console.log(`Prețuri pentru: ${location.name}`);
  
  location.counterTypePrices.forEach(price => {
    console.log(`${price.counterType}: ${price.unitPrice} RON`);
  });
  
  // User modifică un preț → update
  await updateLocationPrice(locationId, 'WATER', newPrice);
}
```

---

## Parametri Opționali Explicați

### updateAllCounters
**Când:** `false` (default)
- Doar locația primește noul preț
- Contorii cu `defaultUnitPrice` null vor folosi noul preț automat
- Contorii cu `defaultUnitPrice` setat rămân cu prețul lor

**Când:** `true`
- Locația primește noul preț
- **TOȚI** contorii de acel tip primesc `defaultUnitPrice = noul preț`
- Sincronizare completă

### recalculateAll
**Când:** `false` (default)
- Doar citirile VIITOARE vor folosi noul preț
- Istoricul costurilor rămâne neschimbat

**Când:** `true`
- Toate citirile FĂRĂ `unitPrice` local se recalculează cu noul preț
- Citirile cu `unitPrice` local rămân neschimbate
- **Atenție:** Modifică datele istorice!

---

## Use Cases

### 1. Setup Inițial Clădire
```http
PATCH /locations/1/prices
{ "counterType": "WATER", "unitPrice": 10.00 }

PATCH /locations/1/prices
{ "counterType": "GAS", "unitPrice": 5.50 }

PATCH /locations/1/prices
{ "counterType": "ELECTRICITY_220", "unitPrice": 0.85 }
```

### 2. Creștere Preț cu Efect în Viitor
```http
PATCH /locations/1/prices
{
  "counterType": "WATER",
  "unitPrice": 12.00,
  "updateAllCounters": false,    // Nu sincroniza contorii cu override
  "recalculateAll": false         // Nu modifica istoricul
}
```

### 3. Corecție Preț cu Recalculare Istoric
```http
PATCH /locations/1/prices
{
  "counterType": "WATER",
  "unitPrice": 10.50,
  "updateAllCounters": true,     // Sincronizează tot
  "recalculateAll": true          // Recalculează istoric
}
```

### 4. Verificare Prețuri Actuale
```http
GET /locations/1/all-prices
```

---

## Note Importante

1. **Prețurile sunt în RON** (nu EUR sau altă monedă)
2. **Un singur preț per tip** - nu poți avea multiple prețuri WATER active simultan
3. **Recalcularea este permanentă** - datele istorice se modifică
4. **updateAllCounters**: util când vrei sincronizare completă
5. **Citirile cu preț local** nu sunt afectate de nicio actualizare

---

## Erori Posibile

### 400 Bad Request
```json
{
  "success": false,
  "message": "counterType is required"
}
```
**Cauză:** Lipsește tipul contorului

### 400 Bad Request
```json
{
  "success": false,
  "message": "unitPrice is required"
}
```
**Cauză:** Lipsește prețul

### 404 Not Found
```json
{
  "success": false,
  "message": "Location not found with id: 1"
}
```
**Cauză:** ID locație invalid

### 500 Internal Server Error
```json
{
  "success": false,
  "message": "Failed to update price: ..."
}
```
**Cauză:** Eroare la actualizare

---

## Vezi și

- [Index Counters API](./06-index-counters.md) - Management contoare și citiri
- [Consumption Statistics](./13-consumption-statistics.md) - Statistici de consum
- [Counter Prices](./06-index-counters.md#update-counter-price) - Prețuri la nivel de contor

