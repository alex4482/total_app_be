# Rental Agreement - Servicii și Contract

## 📋 Modificări Backend (Noiembrie 2025)

### ✅ Ce s-a adăugat:

1. **Câmpuri contract** în `TenantRentalData`:
   - `contractNumber` (String, opțional)
   - `contractDate` (Date, opțional)

2. **Sistem servicii cu istoric**:
   - `ServiceData` - clasă embedded pentru istoricul serviciilor
   - `serviceChanges` (List<ServiceData>) - istoricul schimbărilor de servicii
   - Servicii disponibile:
     - **Salubrizare** (Boolean + cost lunar)
     - **Alarma** (Boolean + cost lunar)

3. **Excel Report actualizat**:
   - Folosește `contractNumber` și `contractDate` (lasă gol dacă nu sunt)
   - Afișează serviciile (SERV. SALUBR. și COTA AB. ALARMA) cu costuri pe lună
   - Calcul automat: pentru fiecare lună se găsește cea mai recentă schimbare de servicii

---

## 🔧 Modificări necesare în Frontend

### 1. **DTO-uri actualizate**

#### **TenantRentalDto** (pentru creare contract nou)

**Câmpuri noi adăugate:**
```typescript
interface TenantRentalDto {
  tenantId: number;
  rentalSpaceId: number;
  startDate: string; // ISO date
  endDate?: string; // ISO date (optional)
  price: number;
  currency?: "RON" | "EURO";
  
  // NOU: Contract information (optional)
  contractNumber?: string;
  contractDate?: string; // ISO date
  
  // NOU: Services (optional - if provided, will create initial service change)
  salubrizare?: boolean;
  alarma?: boolean;
  salubrizareCost?: number;
  alarmaCost?: number;
}
```

**Exemplu request:**
```json
{
  "tenantId": 1,
  "rentalSpaceId": 5,
  "startDate": "2025-01-01",
  "endDate": "2025-12-31",
  "price": 1500.00,
  "currency": "RON",
  "contractNumber": "CTR-2025-001",
  "contractDate": "2025-01-01",
  "salubrizare": true,
  "salubrizareCost": 50.00,
  "alarma": true,
  "alarmaCost": 30.00
}
```

---

#### **UpdateTenantRentalDto** (pentru actualizare contract)

**Câmpuri noi adăugate:**
```typescript
interface UpdateTenantRentalDto {
  startDate?: string; // ISO date
  endDate?: string; // ISO date
  rent?: number;
  currency?: "RON" | "EURO";
  
  // NOU: Contract information (optional)
  contractNumber?: string;
  contractDate?: string; // ISO date
  
  // NOU: Services change (optional - if provided, will add a new service change entry)
  salubrizare?: boolean;
  alarma?: boolean;
  salubrizareCost?: number;
  alarmaCost?: number;
  serviceChangeDate?: string; // ISO date - Date from which the service change applies
}
```

**Exemplu request pentru schimbare servicii:**
```json
{
  "salubrizare": true,
  "salubrizareCost": 60.00,
  "serviceChangeDate": "2025-06-01"
}
```

**Notă importantă:** Dacă trimiteți servicii în `updateRentalAgreement`, se va adăuga o **nouă intrare în istoric** (nu se înlocuiește). Schimbarea se aplică din `serviceChangeDate` (sau din data curentă dacă nu este specificată).

---

### 2. **TenantRentalData - Response actualizat**

**Câmpuri noi în response:**
```typescript
interface TenantRentalData {
  id: number;
  tenant: Tenant;
  rentalSpace: RentalSpace;
  startDate: string;
  endDate?: string;
  rent: number;
  currency: "RON" | "EURO";
  priceChanges: PriceData[];
  
  // NOU: Contract information
  contractNumber?: string;
  contractDate?: string;
  
  // NOU: Services history
  serviceChanges: ServiceData[];
}

interface ServiceData {
  salubrizare?: boolean; // null = nu se schimbă, true/false = se activează/dezactivează
  alarma?: boolean; // null = nu se schimbă, true/false = se activează/dezactivează
  salubrizareCost?: number; // cost lunar pentru salubrizare
  alarmaCost?: number; // cost lunar pentru alarma
  changeTime: string; // ISO date - data de la care se aplică schimbarea
}
```

---

### 3. **UI/UX Modificări necesare**

#### **A. Formular creare contract nou**

Adăugați câmpuri noi:

1. **Secțiune Contract:**
   - Input: "Număr contract" (`contractNumber`)
   - Date picker: "Data contract" (`contractDate`)

2. **Secțiune Servicii:**
   - Checkbox: "Servicii Salubrizare" (`salubrizare`)
   - Input number: "Cost lunar Salubrizare" (`salubrizareCost`) - vizibil doar dacă checkbox-ul este bifat
   - Checkbox: "Servicii Alarma" (`alarma`)
   - Input number: "Cost lunar Alarma" (`alarmaCost`) - vizibil doar dacă checkbox-ul este bifat

**Validare:**
- Dacă checkbox-ul este bifat, costul trebuie să fie > 0
- Dacă checkbox-ul nu este bifat, costul poate fi null sau 0

---

#### **B. Formular actualizare contract**

Adăugați aceleași câmpuri ca mai sus, plus:

- **Date picker:** "Data de la care se aplică schimbarea serviciilor" (`serviceChangeDate`)
  - Opțional: dacă nu este specificată, se folosește data curentă
  - Trebuie să fie între `startDate` și `endDate` ale contractului

**Comportament:**
- Dacă trimiteți servicii, se adaugă o **nouă intrare în istoric**
- Schimbarea se aplică doar din luna specificată în `serviceChangeDate`
- În Excel, pentru fiecare lună se folosește cea mai recentă schimbare care se aplică

---

#### **C. Afișare servicii în lista contractelor**

Adăugați coloane noi în tabelul de contracte:

- "Contract nr." - afișează `contractNumber` (sau "-" dacă nu există)
- "Data contract" - afișează `contractDate` (sau "-" dacă nu există)
- "Servicii" - badge-uri pentru servicii active:
  - Badge "Salubrizare" (verde) dacă ultima schimbare are `salubrizare: true`
  - Badge "Alarma" (albastru) dacă ultima schimbare are `alarma: true`

---

#### **D. Detalii contract - Istoric servicii**

Adăugați o secțiune nouă pentru istoricul serviciilor:

**Tabel "Istoric Servicii":**

| Data | Salubrizare | Cost Salubrizare | Alarma | Cost Alarma |
|------|-------------|------------------|--------|-------------|
| 2025-01-01 | ✅ | 50.00 RON | ✅ | 30.00 RON |
| 2025-06-01 | ✅ | 60.00 RON | ✅ | 30.00 RON |
| 2025-09-01 | ❌ | - | ✅ | 35.00 RON |

**Logica:**
- Sortează `serviceChanges` după `changeTime` (DESC)
- Afișează pentru fiecare intrare:
  - `changeTime` (formatat)
  - Status salubrizare (✅ dacă `salubrizare: true`, ❌ dacă `salubrizare: false`, "-" dacă `null`)
  - Cost salubrizare (sau "-" dacă nu este activ)
  - Status alarma (similar)
  - Cost alarma (similar)

---

### 4. **Excel Report - Comportament**

Excel-ul generat va include automat:

1. **Header:**
   - "Contract nr." - afișează `contractNumber/contractDate` (sau lasă gol dacă nu sunt)

2. **Tabel consum:**
   - Rând "SERV. SALUBR." - costuri pe lună
   - Rând "COTA AB. ALARMA" - costuri pe lună

**Calcul automat:**
- Pentru fiecare lună, se găsește cea mai recentă schimbare de servicii care se aplică
- Dacă serviciul este activ (`true`), se afișează costul
- Dacă serviciul nu este activ (`false` sau `null`), se afișează 0

**Exemplu:**
- Contract începe 2025-01-01 cu salubrizare 50 RON
- La 2025-06-01 se schimbă la 60 RON
- Excel va afișa:
  - Ianuarie-Mai: 50 RON
  - Iunie-Decembrie: 60 RON

---

## 📝 Endpoints afectate

### **POST /tenants/{tenantId}/rental-agreements**
**Modificat:** Acceptă acum câmpurile noi (`contractNumber`, `contractDate`, servicii)

### **PUT /tenant-rental-data/{id}**
**Modificat:** Acceptă acum câmpurile noi pentru actualizare contract și servicii

### **GET /tenant-rental-data/{id}**
**Modificat:** Returnează acum `contractNumber`, `contractDate`, și `serviceChanges`

### **GET /consumption-reports/rental/{rentalAgreementId}/year/{year}**
**Modificat:** Excel-ul generat include acum serviciile cu costuri pe lună

---

## ⚠️ Note importante

1. **Servicii opționale:** Toate câmpurile de servicii sunt opționale. Dacă nu sunt trimise, nu se adaugă nicio intrare în istoric.

2. **Istoric servicii:** Fiecare schimbare de servicii se adaugă ca o **nouă intrare** în `serviceChanges`. Nu se înlocuiesc intrările existente.

3. **Validare date:** `serviceChangeDate` trebuie să fie între `startDate` și `endDate` ale contractului.

4. **Null vs false:** 
   - `null` = nu se schimbă serviciul (se păstrează starea anterioară)
   - `true` = se activează serviciul
   - `false` = se dezactivează serviciul

5. **Costuri:** Costurile trebuie să fie > 0 dacă serviciul este activ (`true`).

---

## 🧪 Exemple de utilizare

### **Exemplu 1: Creare contract cu servicii**

```typescript
const createContract = async () => {
  const response = await fetch(`/tenants/${tenantId}/rental-agreements`, {
    method: 'POST',
    headers: { 'Content-Type': 'application/json' },
    body: JSON.stringify({
      tenantId: 1,
      rentalSpaceId: 5,
      startDate: '2025-01-01',
      endDate: '2025-12-31',
      price: 1500.00,
      currency: 'RON',
      contractNumber: 'CTR-2025-001',
      contractDate: '2025-01-01',
      salubrizare: true,
      salubrizareCost: 50.00,
      alarma: true,
      alarmaCost: 30.00
    })
  });
};
```

### **Exemplu 2: Schimbare servicii în timpul contractului**

```typescript
const updateServices = async (rentalAgreementId: number) => {
  const response = await fetch(`/tenant-rental-data/${rentalAgreementId}`, {
    method: 'PUT',
    headers: { 'Content-Type': 'application/json' },
    body: JSON.stringify({
      salubrizare: true,
      salubrizareCost: 60.00, // Creștere de la 50 la 60
      serviceChangeDate: '2025-06-01' // Se aplică din iunie
    })
  });
};
```

### **Exemplu 3: Dezactivare serviciu**

```typescript
const disableService = async (rentalAgreementId: number) => {
  const response = await fetch(`/tenant-rental-data/${rentalAgreementId}`, {
    method: 'PUT',
    headers: { 'Content-Type': 'application/json' },
    body: JSON.stringify({
      salubrizare: false, // Dezactivează salubrizarea
      serviceChangeDate: '2025-09-01'
    })
  });
};
```

---

## ✅ Checklist Frontend

- [ ] Actualizare `TenantRentalDto` interface
- [ ] Actualizare `UpdateTenantRentalDto` interface
- [ ] Actualizare `TenantRentalData` interface (adaugare `contractNumber`, `contractDate`, `serviceChanges`)
- [ ] Adăugare câmpuri contract în formular creare contract
- [ ] Adăugare câmpuri servicii în formular creare contract
- [ ] Adăugare câmpuri servicii în formular actualizare contract
- [ ] Adăugare date picker pentru `serviceChangeDate`
- [ ] Validare: cost > 0 dacă serviciul este activ
- [ ] Validare: `serviceChangeDate` între `startDate` și `endDate`
- [ ] Afișare contract number și date în lista contractelor
- [ ] Afișare badge-uri servicii active în lista contractelor
- [ ] Secțiune istoric servicii în detalii contract
- [ ] Testare creare contract cu servicii
- [ ] Testare schimbare servicii în timpul contractului
- [ ] Testare dezactivare servicii
- [ ] Verificare Excel generat include serviciile corect

---

**Data actualizării:** Noiembrie 2025  
**Versiune:** 1.0

