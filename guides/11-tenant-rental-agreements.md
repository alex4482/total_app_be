# 🏠 Tenant Rental Agreements API

Sistem pentru gestionarea contractelor de închiriere între tenants și rental spaces.

## 📋 Overview

Sistemul permite:
- Crearea de contracte de închiriere între un tenant și un rental space
- Actualizarea detaliilor contractului (startDate, endDate, rent)
- **Schimbarea prețului chiriei cu data efectivă** (programare modificări preț)
- Terminarea contractului (setare endDate)
- Ștergerea completă a contractului

**Note importante:**
- Un rental space poate avea doar UN contract activ (OneToOne relație)
- Istoricul modificărilor de preț este păstrat în `priceChanges`
- Prețul curent este stocat în câmpul `rent`

---

## 🔌 Endpoints

### Base Path
```
/tenants
```

---

### 1. Create Rental Agreement

**POST** `/tenants/new-rental-agreement`

Creează un nou contract de închiriere între un tenant și un rental space.

#### Request Body
```json
{
  "tenantId": 1,
  "rentalSpaceId": 5,
  "startDate": "2025-01-01",
  "price": 1500.00
}
```

#### Fields
- `tenantId` (Long, **required**) - ID-ul tenantului
- `rentalSpaceId` (Long, **required**) - ID-ul rental space-ului
- `startDate` (Date, **required**) - Data de început a contractului (format: "yyyy-MM-dd")
- `price` (Double, **required**) - Prețul inițial al chiriei

#### Response (201 Created)
```json
{
  "success": true,
  "message": "Rental agreement created successfully",
  "data": {
    "id": 1,
    "tenant": {
      "id": 1,
      "name": "Ion Popescu",
      ...
    },
    "rentalSpace": {
      "id": 5,
      "name": "Apartament 3A",
      ...
    },
    "startDate": "2025-01-01",
    "endDate": null,
    "rent": 1500.00,
    "priceChanges": [
      {
        "newPrice": 1500.00,
        "changeTime": "2025-01-01"
      }
    ]
  }
}
```

#### Errors
- `400 Bad Request` - Date lipsă sau invalide
- `404 Not Found` - Tenant sau rental space nu există
- `400 Bad Request` - Rental space-ul este deja ocupat (are deja un contract activ)

---

### 2. Update Rental Agreement

**PUT** `/tenants/rental-agreement/{id}`

Actualizează detaliile unui contract de închiriere existent.

#### Path Parameters
- `id` (Long, **required**) - ID-ul contractului de închiriere

#### Request Body
```json
{
  "startDate": "2025-01-15",
  "endDate": "2025-12-31",
  "rent": 1600.00
}
```

#### Fields (toate opționale)
- `startDate` (Date, optional) - Noua dată de început
- `endDate` (Date, optional) - Noua dată de sfârșit
- `rent` (Double, optional) - Noul preț (dar nu se adaugă în istoric - folosește change-price pentru asta)

#### Response (200 OK)
```json
{
  "success": true,
  "message": "Rental agreement updated successfully",
  "data": {
    "id": 1,
    "startDate": "2025-01-15",
    "endDate": "2025-12-31",
    "rent": 1600.00,
    ...
  }
}
```

#### Errors
- `404 Not Found` - Contractul nu există
- `400 Bad Request` - Date invalide

---

### 3. Change Price (cu Data Efectivă)

**POST** `/tenants/rental-agreement/{id}/change-price`

Schimbă prețul chiriei cu posibilitatea de a seta data la care începe schimbarea prețului. **Aceasta este metoda recomandată pentru schimbarea prețului** - adaugă automat în istoric.

#### Path Parameters
- `id` (Long, **required**) - ID-ul contractului de închiriere

#### Request Body
```json
{
  "newPrice": 1800.00,
  "effectiveDate": "2025-03-01"
}
```

#### Fields
- `newPrice` (Double, **required**) - Noul preț (trebuie să fie > 0)
- `effectiveDate` (Date, **required**) - Data de la care începe noul preț (format: "yyyy-MM-dd")

#### Response (200 OK)
```json
{
  "success": true,
  "message": "Price changed successfully",
  "data": {
    "id": 1,
    "rent": 1800.00,
    "priceChanges": [
      {
        "newPrice": 1500.00,
        "changeTime": "2025-01-01"
      },
      {
        "newPrice": 1800.00,
        "changeTime": "2025-03-01"
      }
    ],
    ...
  }
}
```

#### Validări
- `effectiveDate` trebuie să fie între `startDate` și `endDate` (dacă există)
- `effectiveDate` nu poate fi înainte de `startDate`
- `effectiveDate` nu poate fi după `endDate` (dacă există)

#### Errors
- `404 Not Found` - Contractul nu există
- `400 Bad Request` - Date invalide sau validări eșuate

---

### 4. Terminate Rental Agreement

**POST** `/tenants/rental-agreement/{id}/terminate`

Termină un contract de închiriere prin setarea datei de sfârșit.

#### Path Parameters
- `id` (Long, **required**) - ID-ul contractului de închiriere

#### Request Body
```json
{
  "endDate": "2025-12-31"
}
```

#### Fields
- `endDate` (Date, **required**) - Data de sfârșit a contractului (format: "yyyy-MM-dd")

#### Response (200 OK)
```json
{
  "success": true,
  "message": "Rental agreement terminated successfully",
  "data": {
    "id": 1,
    "endDate": "2025-12-31",
    ...
  }
}
```

#### Validări
- `endDate` nu poate fi înainte de `startDate`

#### Errors
- `404 Not Found` - Contractul nu există
- `400 Bad Request` - Dată invalide

---

### 5. Delete Rental Agreement

**DELETE** `/tenants/rental-agreement/{id}`

Șterge complet contractul de închiriere (elimină legătura între tenant și rental space).

#### Path Parameters
- `id` (Long, **required**) - ID-ul contractului de închiriere

#### Response (200 OK)
```json
{
  "success": true,
  "message": "Rental agreement deleted successfully",
  "data": null
}
```

#### Comportament
- Elimină legătura din `Tenant.rentalData`
- Elimină legătura din `RentalSpace.rentalAgreement` (setează la null)
- Șterge entitatea `TenantRentalData` din baza de date

#### Errors
- `404 Not Found` - Contractul nu există

---

## 📊 Model de Date

### TenantRentalDto (pentru creare)
```typescript
interface TenantRentalDto {
  tenantId: number;           // Long (required)
  rentalSpaceId: number;      // Long (required)
  startDate: string;          // Date format: "yyyy-MM-dd" (required)
  price: number;              // Double (required)
}
```

### UpdateTenantRentalDto (pentru actualizare)
```typescript
interface UpdateTenantRentalDto {
  startDate?: string;         // Date format: "yyyy-MM-dd" (optional)
  endDate?: string;           // Date format: "yyyy-MM-dd" (optional)
  rent?: number;              // Double (optional)
}
```

### ChangePriceDto (pentru schimbare preț)
```typescript
interface ChangePriceDto {
  newPrice: number;           // Double (required, > 0)
  effectiveDate: string;      // Date format: "yyyy-MM-dd" (required)
}
```

### TerminateRentalDto (pentru terminare)
```typescript
interface TerminateRentalDto {
  endDate: string;            // Date format: "yyyy-MM-dd" (required)
}
```

### TenantRentalData (Response)
```typescript
interface TenantRentalData {
  id: number;                 // Long
  tenant: Tenant;             // Obiect tenant complet
  rentalSpace: RentalSpace;  // Obiect rental space complet
  startDate: string;          // Date format: "yyyy-MM-dd"
  endDate: string | null;     // Date format: "yyyy-MM-dd" sau null
  rent: number;               // Double - prețul curent
  priceChanges: PriceData[];  // Istoric modificări preț
}

interface PriceData {
  newPrice: number;           // Double
  changeTime: string;          // Date format: "yyyy-MM-dd"
}

interface Tenant {
  id: number;
  name: string;
  cui?: string;
  regNumber?: string;
  pf?: boolean;
  active?: boolean;
  emails?: string[];
  phoneNumbers?: string[];
  // ... alte câmpuri
}

interface RentalSpace {
  id: number;
  name: string;
  officialName?: string;
  location?: string;
  mp?: number;
  groundLevel?: boolean;
  // ... alte câmpuri
}
```

### ApiResponse<T>
```typescript
interface ApiResponse<T> {
  success: boolean;
  message: string | null;
  data: T | null;
}
```

---

## 🔄 Fluxuri de Utilizare

### 1. Creare Contract Nou
```
1. POST /tenants/new-rental-agreement
   - Body: { tenantId, rentalSpaceId, startDate, price }
   - Result: Contract creat, rental space devine ocupat
```

### 2. Schimbare Preț (Programare Viitoare)
```
1. POST /tenants/rental-agreement/{id}/change-price
   - Body: { newPrice: 1800, effectiveDate: "2025-03-01" }
   - Result: Preț actualizat, adăugat în istoric
```

### 3. Actualizare Contract
```
1. PUT /tenants/rental-agreement/{id}
   - Body: { startDate, endDate, rent } (opționale)
   - Result: Detalii contract actualizate
```

### 4. Terminare Contract
```
1. POST /tenants/rental-agreement/{id}/terminate
   - Body: { endDate: "2025-12-31" }
   - Result: Contract terminat, dar legătura rămâne
```

### 5. Ștergere Completă Contract
```
1. DELETE /tenants/rental-agreement/{id}
   - Result: Contract șters, rental space devine liber
```

---

## 🎯 Exemple de Utilizare

### Creare Contract
```typescript
const createRentalAgreement = async () => {
  const agreement = {
    tenantId: 1,
    rentalSpaceId: 5,
    startDate: "2025-01-01",
    price: 1500.00
  };
  
  const response = await fetch('/tenants/new-rental-agreement', {
    method: 'POST',
    headers: { 'Content-Type': 'application/json' },
    body: JSON.stringify(agreement)
  });
  
  const { data } = await response.json();
  return data;
};
```

### Schimbare Preț cu Data Efectivă
```typescript
const changePrice = async (agreementId: number, newPrice: number, effectiveDate: string) => {
  const response = await fetch(`/tenants/rental-agreement/${agreementId}/change-price`, {
    method: 'POST',
    headers: { 'Content-Type': 'application/json' },
    body: JSON.stringify({
      newPrice,
      effectiveDate
    })
  });
  
  const { data } = await response.json();
  return data;
};

// Exemplu: Schimbă prețul la 1800 RON începând cu 1 martie 2025
await changePrice(1, 1800, "2025-03-01");
```

### Terminare Contract
```typescript
const terminateAgreement = async (agreementId: number, endDate: string) => {
  const response = await fetch(`/tenants/rental-agreement/${agreementId}/terminate`, {
    method: 'POST',
    headers: { 'Content-Type': 'application/json' },
    body: JSON.stringify({ endDate })
  });
  
  return await response.json();
};
```

---

## ⚠️ Validări și Constricții

### Creare Contract
1. Rental space-ul nu trebuie să fie deja ocupat
2. Tenant-ul și rental space-ul trebuie să existe
3. `price` trebuie să fie > 0

### Schimbare Preț
1. `newPrice` trebuie să fie > 0
2. `effectiveDate` trebuie să fie între `startDate` și `endDate` (dacă există)
3. `effectiveDate` nu poate fi înainte de `startDate`

### Terminare Contract
1. `endDate` nu poate fi înainte de `startDate`

### Ștergere Contract
1. Contractul trebuie să existe
2. După ștergere, rental space-ul devine liber (poate fi folosit pentru un nou contract)

---

## 📝 Note pentru Frontend

### 1. **Prețul Curent vs Istoric**
- Câmpul `rent` reprezintă prețul curent
- `priceChanges` reprezintă istoricul complet al modificărilor
- Folosește **change-price** endpoint pentru a adăuga în istoric automat

### 2. **Date Format**
- Toate datele sunt în format `"yyyy-MM-dd"` (ex: "2025-03-01")
- Backend-ul acceptă și procesează Date objects din Java

### 3. **Status Contract**
- **Activ**: `endDate` este `null`
- **Terminat**: `endDate` este setat
- **Rental Space Liber**: `rentalAgreement` este `null`

### 4. **Validare Date**
- Frontend-ul ar trebui să valideze că `effectiveDate` este între `startDate` și `endDate`
- Frontend-ul ar trebui să valideze că `endDate` nu este înainte de `startDate`

### 5. **UI Recomandări**
- **Creare Contract**: Formular cu selecție tenant și rental space (doar spații libere)
- **Schimbare Preț**: Formular cu câmp pentru preț nou și date picker pentru data efectivă
- **Istoric Preț**: Afișează `priceChanges` într-un tabel sau timeline
- **Terminare Contract**: Buton simplu care setează `endDate` la data curentă sau permite selecție

### 6. **Filtrare Rental Spaces**
- Când creezi un contract nou, filtrează rental spaces după `rentalAgreement == null` (spații libere)
- Poți folosi endpoint-ul de filtrare rental spaces cu `empty=true`

---

## 🔗 Legături cu Alte Endpoints

### Listare Rental Spaces Libere
```
GET /buildings/rental-spaces?empty=true
```
Returnează toate rental spaces care nu au contract activ.

### Listare Contracte ale unui Tenant
```
GET /tenants/{tenantId}
```
Returnează tenant-ul cu toate contractele sale în `rentalData[]`.

### Listare Contract al unui Rental Space
```
GET /buildings/rental-spaces/{id}
```
Returnează rental space-ul cu contractul său în `rentalAgreement`.

