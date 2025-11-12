# Backend API - Ghid Complet de Integrare Frontend

Acest document oferă o vedere de ansamblu asupra tuturor API-urilor disponibile în aplicația backend și cum să le folosești din frontend.

## 📋 Cuprins

### 🌐 Core API Documentation
1. [Authentication API](./01-authentication.md) - Autentificare și gestionare tokene JWT
2. [Tenants API](./02-tenants.md) - Gestionare chiriași (CRUD, import Excel, bulk operations)
3. [Buildings API](./03-buildings.md) - Gestionare clădiri și spații de închiriat
4. [Files API](./04-files.md) - Upload, download, și gestionare fișiere
5. [Email Presets API](./05-email-presets.md) - Șabloane email și trimitere email-uri
6. [Index Counters API](./06-index-counters.md) - Gestionare contoare și citiri (BASIC)
7. [Database Migrations](./07-database-migrations.md) - Gestionare schema bază de date cu Flyway
8. [Backup & Restore](./08-backup-restore.md) - Backup și restore date
9. [File Manager API](./09-file-manager-api.md) - API dedicat pentru file manager
10. [Reminders API](./10-reminders.md) - Sistem de reminder-uri cu email
11. [Tenant Rental Agreements](./11-tenant-rental-agreements.md) - Management contracte de închiriere

### 📊 Consumption & Reports (NEW - Nov 2025)
12. **[Consumption Reports](./12-consumption-reports.md)** ⭐ - Rapoarte Excel de consum
13. **[Consumption Statistics](./13-consumption-statistics.md)** ⭐ - Statistici agregate de consum
14. **[Location Prices](./14-location-prices.md)** ⭐ - Management prețuri la nivel de locație
15. **[Consumption Full API](./15-consumption-full-api.md)** 📖 - Documentație completă (toate endpoint-urile)
16. **[Consumption Quick Reference](./16-consumption-quick-reference.md)** ⚡ - Cheat sheet pentru lookup rapid
17. **[Consumption Examples](./17-consumption-examples.md)** 🧪 - 8 scenarii practice step-by-step
18. **[Counter Replacement](./18-counter-replacement.md)** 🔄 - Înlocuire contoare cu continuitate date
19. **[Rental Agreement Services](./19-rental-agreement-services.md)** 🧹 - Servicii salubrizare și alarma cu istoric (DEPRECATED)
20. **[Services Management](./20-services-management.md)** ⚙️ - Servicii configurabile cu formule de calcul

### 📚 General Documentation
- [API Response Format](./API_RESPONSE_FORMAT.md) - Format standard pentru răspunsuri API
- [FE API Messages](./FE_API_MESSAGES.md) - Mesaje pentru frontend

---

## 🆕 Ce e NOU în Noiembrie 2025?

### ✅ Sistem Prețuri cu 3 Niveluri
- **Nivel 1 (Location):** Preț default pentru toate contoarele de un tip dintr-o locație
- **Nivel 2 (Counter):** Override opțional la nivel de contor individual
- **Nivel 3 (Reading):** Override opțional la nivel de citire specifică

### ✅ Rapoarte Excel Automate
- Generare rapoarte de consum în format Excel
- Download direct ca fișier `.xlsx`
- Format custom cu calcule automate (TOTAL, Cota întreținere 3%)
- Suport pentru rapoarte anuale și multi-anuale

### ✅ Statistici Avansate
- Agregare per tip contor (WATER, GAS, ELECTRICITY)
- Agregare per locație/spațiu
- Agregare per clădire
- Filtrare flexibilă pe perioade

### ✅ Calcule Automate
- Consum calculat automat între citiri consecutive
- Cost total calculat automat: `consumption × effectiveUnitPrice`
- Recalculare automată la schimbare prețuri

### ✅ Înlocuire Contoare
- Sistem de înlocuire contoare cu continuitate date
- Link între ultimul index vechi și primul index nou
- Calcul automat de consum peste granița de înlocuire

---

## 🚀 Quick Start - Consumption Features

### 1. Setup Prețuri Clădire
```typescript
// Setează prețuri default pentru o locație
await fetch('/locations/1/prices', {
  method: 'PATCH',
  headers: {
    'Content-Type': 'application/json',
    'Authorization': `Bearer ${token}`
  },
  body: JSON.stringify({
    counterType: 'WATER',
    unitPrice: 10.50,
    updateAllCounters: true
  })
});
```

### 2. Adaugă Citire cu Calcul Automat
```typescript
// Adaugă citire - consumul și costul se calculează automat
await fetch('/index-counters/data', {
  method: 'POST',
  headers: {
    'Content-Type': 'application/json',
    'Authorization': `Bearer ${token}`
  },
  body: JSON.stringify({
    counterId: 123,
    index: 1500.5,
    readingDate: '2025-11-08',
    unitPrice: null  // Folosește prețul default de la locație
  })
});
```

### 3. Vezi Statistici
```typescript
// Obține statistici de consum pentru un an
const stats = await fetch(
  '/index-counters/statistics?startDate=2025-01-01&endDate=2025-12-31',
  {
    headers: { 'Authorization': `Bearer ${token}` }
  }
).then(r => r.json());

console.log(`Total consum: ${stats.data.totalConsumption}`);
console.log(`Total cost: ${stats.data.totalCost} RON`);
```

### 4. Download Raport Excel
```typescript
// Generează și descarcă raport pentru un contract
const response = await fetch(
  '/consumption-reports/rental/2/year/2025',
  {
    headers: { 'Authorization': `Bearer ${token}` }
  }
);

const blob = await response.blob();
const url = window.URL.createObjectURL(blob);
const a = document.createElement('a');
a.href = url;
a.download = 'raport-2025.xlsx';
a.click();
```

---

## 🔗 Base URL

**Development:** `http://localhost:8080`  
**Production:** `https://api.donix.ro`

---

## 🔐 Autentificare

Toate request-urile (cu excepția `/auth/login` și `/auth/refresh-token`) necesită un token JWT valid în header:

```http
Authorization: Bearer {accessToken}
```

Vezi [Authentication API](./01-authentication.md) pentru detalii complete.

---

## 📖 Cum să Folosești Această Documentație

### Pentru Implementare Feature Nou:

**Opțiunea 1 - Overview Rapid:**
1. Citește **Quick Reference** (16) pentru sintaxa endpoint-urilor
2. Citește **Examples** (17) pentru use cases practice
3. Implementează

**Opțiunea 2 - Detaliat:**
1. Citește ghidul specific pentru feature (12-14)
2. Consultă **Full API** (15) pentru detalii complete
3. Testează cu **Examples** (17)

### Pentru Debugging/Testing:
1. Verifică **Examples** (17) pentru scenarii similare
2. Consultă **Full API** (15) pentru validări și erori
3. Verifică **Quick Reference** (16) pentru sintaxa corectă

---

## 🔧 Tipuri și Enumerări Comune

### CounterType
```typescript
type CounterType = 
  | "WATER"
  | "GAS"
  | "ELECTRICITY_220"
  | "ELECTRICITY_380";
```

### BuildingLocation
```typescript
type BuildingLocation = 
  | "LETCANI"
  | "TOMESTI";
```

### OwnerType (pentru fișiere)
```typescript
type OwnerType = 
  | "TENANT"
  | "BUILDING"
  | "ROOM"
  | "RENTAL_SPACE"
  | "EMAIL_DATA"
  | "BUILDING_LOCATION"
  | "FIRM"
  | "CAR"
  | "OTHER";
```

---

## ⚠️ Lucruri Importante de Știut

### Consumption & Reports
1. **Prețurile sunt în RON**
2. **Consumul se calculează automat** între citiri consecutive
3. **totalCost = consumption × effectiveUnitPrice** (calculat automat)
4. **Datele sunt în format ISO:** `yyyy-MM-dd`
5. **Recalcularea este opțională:** folosește `recalculateAll: true` doar când vrei să actualizezi istoric
6. **Rapoartele se generează on-demand** - nu sunt pre-generate

### General
- **Fișiere:** TREBUIE temp upload → commit workflow
- **Auto-delete:** Când ștergi un tenant/building, fișierele sale sunt șterse automat
- **Email atașamente:** Folosește ID-uri TEMPORARE din `/files/temp`
- **Tenants active:** Default `false` la creare

---

## 🚨 Gestionare Erori

### Status Codes Comune:
- `200 OK` - Success
- `201 Created` - Resursa a fost creată
- `204 No Content` - Success, fără conținut
- `400 Bad Request` - Date invalide
- `401 Unauthorized` - Token lipsă sau invalid (refresh!)
- `403 Forbidden` - Acces interzis
- `404 Not Found` - Resursa nu există
- `409 Conflict` - Conflict (duplicat, dependințe)
- `500 Internal Server Error` - Eroare server

### Pattern de Gestionare:
Vezi [Authentication API](./01-authentication.md) pentru pattern complet de auto-refresh token.

---

## 📝 Resurse Utile

### Pentru Development:
- **[API Response Format](./API_RESPONSE_FORMAT.md)** - Format standard răspunsuri
- **[FE API Messages](./FE_API_MESSAGES.md)** - Mesaje pentru UI
- **[Database Migrations](./07-database-migrations.md)** - Schema DB și migrații

### Pentru Testing:
- **[Consumption Examples](./17-consumption-examples.md)** - 8 scenarii practice
- **api-tests.http** (în `/docs`) - Colecție teste API
- **file-manager-api.http** (în `/docs`) - Teste file manager

---

## 🔄 Versioning

**Current API Version:** v1  
**Last Major Update:** Noiembrie 2025 (Consumption & Reports)  
**Last Updated:** Noiembrie 2025

Pentru probleme sau întrebări despre API, contactează echipa de backend.

---

## 📊 Statistici Documentație

- **18 ghiduri** API complete
- **14 endpoint-uri noi** pentru consumption & reports
- **100+ exemple** de request/response
- **3 nivele** de prețuri implementate
- **8 scenarii practice** documentate
- **Sistem complet** de înlocuire contoare
