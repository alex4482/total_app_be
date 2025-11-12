# Services Management - Servicii Configurabile

## 📋 Modificări Backend (Noiembrie 2025)

### ✅ Ce s-a adăugat:

1. **Sistem servicii general** (nu legat de rental agreements):
   - `Service` - definiție generală de serviciu (nume, cost default, formulă)
   - `ServiceFormula` - formule de calcul pentru servicii
   - Endpoint-uri pentru CRUD servicii

2. **Servicii active în rental agreements**:
   - `ActiveService` - link între un `Service` și un `TenantRentalData`
   - Cost custom per rental agreement
   - Date de activare/dezactivare

3. **Excel Report actualizat**:
   - Afișează serviciile active pentru fiecare lună
   - Calculează automat valorile (cost sau formulă)
   - Respectă datele de activare/dezactivare

---

## 🔧 API Endpoints

### **1. Services Management**

#### **GET /services**
Returnează toate serviciile active.

**Response:**
```json
{
  "success": true,
  "data": [
    {
      "id": 1,
      "name": "Salubrizare",
      "description": "Servicii de curățenie",
      "defaultMonthlyCost": 50.00,
      "formula": null,
      "active": true,
      "createdAt": "2025-01-01T00:00:00",
      "updatedAt": "2025-01-01T00:00:00"
    },
    {
      "id": 2,
      "name": "Cota întreținere",
      "description": "3% din chirie",
      "defaultMonthlyCost": null,
      "formula": {
        "id": 1,
        "expression": "rent * 0.03",
        "description": "3% din chirie"
      },
      "active": true
    }
  ]
}
```

---

#### **GET /services/all**
Returnează toate serviciile (inclusiv inactive).

---

#### **GET /services/{id}**
Returnează un serviciu specific.

---

#### **POST /services**
Creează un serviciu nou.

**Request Body:**
```json
{
  "name": "Salubrizare",
  "description": "Servicii de curățenie lunară",
  "unitOfMeasure": "lei",
  "defaultMonthlyCost": 50.00,
  "defaultIncludeInReport": true,
  "formula": null,
  "active": true
}
```

**Sau cu formulă:**
```json
{
  "name": "Cota întreținere",
  "description": "3% din chirie + 10% din apă și gaz",
  "unitOfMeasure": "lei",
  "defaultMonthlyCost": null,
  "defaultIncludeInReport": true,
  "formula": {
    "expression": "rent * 0.03 + (waterConsumption + gasConsumption) * 0.1",
    "description": "3% din chirie + 10% din consum apă și gaz"
  },
  "active": true
}
```

**Response:**
```json
{
  "success": true,
  "message": "Service created successfully",
  "data": {
    "id": 1,
    "name": "Salubrizare",
    ...
  }
}
```

---

#### **PUT /services/{id}**
Actualizează un serviciu existent.

**Request Body:** (toate câmpurile sunt opționale)
```json
{
  "name": "Salubrizare Premium",
  "unitOfMeasure": "lei",
  "defaultMonthlyCost": 60.00,
  "defaultIncludeInReport": true,
  "formula": null
}
```

---

#### **DELETE /services/{id}**
Șterge un serviciu (soft delete - setează `active: false`).

---

### **2. Rental Agreement - Active Services**

#### **POST /tenants/{tenantId}/rental-agreements**
Creează un contract nou cu servicii active.

**Request Body:**
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
  "activeServices": [
    {
      "serviceId": 1,
      "customMonthlyCost": 55.00,
      "includeInReport": true,
      "activeFrom": "2025-01-01",
      "activeUntil": null,
      "notes": "Cost negociat special"
    },
    {
      "serviceId": 2,
      "customMonthlyCost": null,
      "includeInReport": null,
      "activeFrom": "2025-01-01",
      "activeUntil": "2025-06-30",
      "notes": "Folosește valoarea implicită (IMPLICIT)"
    }
  ]
}
```

**Notă:** 
- `customMonthlyCost` - dacă este setat, se folosește în loc de `defaultMonthlyCost` sau formulă
- `includeInReport` - trei stări disponibile:
  - `null` (IMPLICIT) - folosește `service.defaultIncludeInReport` (implicit pentru servicii noi)
  - `true` (ON MANUAL) - include explicit în raport
  - `false` (OFF MANUAL) - exclude explicit din raport
- `activeFrom` - data de la care serviciul este activ (default: `startDate`)
- `activeUntil` - data până la care serviciul este activ (null = activ indefinit)

**Pentru revenirea la IMPLICIT după ce a fost setat manual, folosiți endpoint-ul `PUT /tenant-rental-data/{rentalAgreementId}/services` cu `useDefaultIncludeInReport: true`.**

---

#### **PUT /tenant-rental-data/{id}**
Actualizează contractul și serviciile active.

**Request Body:**
```json
{
  "activeServices": [
    {
      "serviceId": 1,
      "customMonthlyCost": 60.00,
      "includeInReport": true,
      "activeFrom": "2025-01-01",
      "activeUntil": null,
      "notes": "Cost actualizat"
    },
    {
      "serviceId": 2,
      "customMonthlyCost": null,
      "includeInReport": null,
      "activeFrom": "2025-01-01",
      "activeUntil": null,
      "notes": "Folosește valoarea implicită (IMPLICIT)"
    }
  ]
}
```

**Notă:** 
- Dacă trimiteți `activeServices`, se **înlocuiesc toate** serviciile existente.
- `includeInReport` - trei stări disponibile:
  - `null` (IMPLICIT) - folosește `service.defaultIncludeInReport`
  - `true` (ON MANUAL) - include explicit în raport
  - `false` (OFF MANUAL) - exclude explicit din raport
- **Pentru revenirea la IMPLICIT după ce a fost setat manual, folosiți endpoint-ul `PUT /tenant-rental-data/{rentalAgreementId}/services` cu `useDefaultIncludeInReport: true`.**

---

#### **PUT /tenant-rental-data/{rentalAgreementId}/services**
Actualizează serviciile active pentru un contract. Înlocuiește toate serviciile existente cu cele din request.

**Request Body:**
```json
{
  "services": [
    {
      "serviceId": 1,
      "active": true,
      "customMonthlyCost": 55.00,
      "includeInReport": true,
      "useDefaultIncludeInReport": false,
      "activeFrom": "2025-01-01",
      "activeUntil": null,
      "notes": "Cost negociat special"
    },
    {
      "serviceId": 2,
      "active": true,
      "customMonthlyCost": null,
      "includeInReport": null,
      "useDefaultIncludeInReport": true,
      "activeFrom": "2025-01-01",
      "activeUntil": null,
      "notes": "Folosește valoarea implicită"
    },
    {
      "serviceId": 3,
      "active": false
    }
  ]
}
```

**Stări pentru `includeInReport`:**

1. **IMPLICIT** (folosește valoarea din `Service.defaultIncludeInReport`):
   - Setează `useDefaultIncludeInReport: true`
   - Sau lasă `includeInReport: null` și `useDefaultIncludeInReport` nesetat pentru servicii noi

2. **ON MANUAL** (include explicit în raport):
   - Setează `includeInReport: true`
   - Setează `useDefaultIncludeInReport: false` sau lasă nesetat

3. **OFF MANUAL** (exclude explicit din raport):
   - Setează `includeInReport: false`
   - Setează `useDefaultIncludeInReport: false` sau lasă nesetat

**Response:**
```json
{
  "success": true,
  "message": "Serviciile au fost actualizate cu succes",
  "data": {
    "rentalAgreementId": 1,
    "services": [
      {
        "serviceId": 1,
        "serviceName": "Salubrizare",
        "serviceDescription": "Servicii de curățenie",
        "unitOfMeasure": "lei",
        "customMonthlyCost": 55.00,
        "includeInReport": true,
        "includeInReportMode": "MANUAL_ON",
        "activeFrom": "2025-01-01",
        "activeUntil": null,
        "notes": "Cost negociat special"
      },
      {
        "serviceId": 2,
        "serviceName": "Cota întreținere",
        "serviceDescription": "3% din chirie",
        "unitOfMeasure": "lei",
        "customMonthlyCost": null,
        "includeInReport": true,
        "includeInReportMode": "IMPLICIT",
        "activeFrom": "2025-01-01",
        "activeUntil": null,
        "notes": "Folosește valoarea implicită"
      }
    ]
  }
}
```

**Notă:** 
- `includeInReportMode` poate fi: `"IMPLICIT"`, `"MANUAL_ON"`, `"MANUAL_OFF"`
- `includeInReport` este valoarea rezolvată (folosește `service.defaultIncludeInReport` dacă modul este `"IMPLICIT"`)

---

#### **GET /tenant-rental-data/{rentalAgreementId}/services**
Returnează serviciile active pentru un contract, cu `includeInReport` rezolvat.

**Response:**
```json
{
  "success": true,
  "data": {
    "rentalAgreementId": 1,
    "services": [
      {
        "serviceId": 1,
        "serviceName": "Salubrizare",
        "serviceDescription": "Servicii de curățenie",
        "unitOfMeasure": "lei",
        "customMonthlyCost": 55.00,
        "includeInReport": true,
        "includeInReportMode": "MANUAL_ON",
        "activeFrom": "2025-01-01",
        "activeUntil": null,
        "notes": "Cost negociat special"
      },
      {
        "serviceId": 2,
        "serviceName": "Cota întreținere",
        "serviceDescription": "3% din chirie",
        "unitOfMeasure": "lei",
        "customMonthlyCost": null,
        "includeInReport": true,
        "includeInReportMode": "IMPLICIT",
        "activeFrom": "2025-01-01",
        "activeUntil": null,
        "notes": null
      }
    ]
  }
}
```

---

## 📊 Formule de Calcul

### **Variabile disponibile:**

- `rent` - chiria lunară
- `waterConsumption` - consum apă
- `gasConsumption` - consum gaz
- `electricityConsumption220V` - consum electricitate 220V
- `electricityConsumption380V` - consum electricitate 380V

### **Operatori suportați:**

- `+` - adunare
- `-` - scădere
- `*` - înmulțire
- `/` - împărțire
- `()` - paranteze pentru prioritizare

### **Exemple de formule:**

```javascript
// 3% din chirie
"rent * 0.03"

// 50% din consum apă + 30% din consum gaz
"waterConsumption * 0.5 + gasConsumption * 0.3"

// 10% din apă + gaz
"(waterConsumption + gasConsumption) * 0.1"

// Diferite tarife pentru 220V și 380V
"electricityConsumption220V * 0.05 + electricityConsumption380V * 0.03"

// 100 RON fix + 5% din consum apă
"100 + waterConsumption * 0.05"

// Suma tuturor utilităților
"waterConsumption + gasConsumption + electricityConsumption220V + electricityConsumption380V"
```

---

## 🔧 Modificări necesare în Frontend

### **1. Interfețe TypeScript**

```typescript
interface Service {
  id: number;
  name: string;
  description?: string;
  unitOfMeasure?: string;
  defaultMonthlyCost?: number;
  defaultIncludeInReport?: boolean;
  formula?: ServiceFormula;
  active: boolean;
  createdAt: string;
  updatedAt: string;
}

interface ServiceFormula {
  id: number;
  expression: string;
  description?: string;
}

interface ServiceDto {
  name: string;
  description?: string;
  unitOfMeasure?: string;
  defaultMonthlyCost?: number;
  defaultIncludeInReport?: boolean;
  formula?: ServiceFormulaDto;
  active?: boolean;
}

interface ServiceFormulaDto {
  expression: string;
  description?: string;
}

interface ActiveService {
  serviceId: number;
  customMonthlyCost?: number;
  includeInReport?: boolean | null; // null = IMPLICIT (use service.defaultIncludeInReport), true = ON MANUAL, false = OFF MANUAL
  activeFrom: string; // ISO date
  activeUntil?: string; // ISO date (optional)
  notes?: string;
}

interface ActiveServiceDto {
  serviceId: number;
  customMonthlyCost?: number;
  includeInReport?: boolean | null; // null = IMPLICIT (use service.defaultIncludeInReport), true = ON MANUAL, false = OFF MANUAL
  activeFrom?: string; // ISO date (optional - defaults to rental startDate)
  activeUntil?: string; // ISO date (optional)
  notes?: string;
}

// Notă: ActiveServiceDto este folosit pentru POST /tenants/{tenantId}/rental-agreements și PUT /tenant-rental-data/{id}
// Pentru revenirea la IMPLICIT după ce a fost setat manual, folosiți ServiceUpdateDto cu useDefaultIncludeInReport: true

interface ServiceUpdateDto {
  serviceId: number;
  active: boolean;
  customMonthlyCost?: number;
  includeInReport?: boolean | null; // true = ON MANUAL, false = OFF MANUAL, null = see useDefaultIncludeInReport
  useDefaultIncludeInReport?: boolean; // true = IMPLICIT (use service.defaultIncludeInReport) - ignoră includeInReport
  activeFrom?: string; // ISO date (optional - defaults to rental startDate)
  activeUntil?: string; // ISO date (optional)
  notes?: string;
}

// Notă: ServiceUpdateDto este folosit pentru PUT /tenant-rental-data/{rentalAgreementId}/services
// Pentru revenirea la IMPLICIT, setați useDefaultIncludeInReport: true (va seta includeInReport = null)

interface ServiceWithResolvedIncludeInReport {
  serviceId: number;
  serviceName: string;
  serviceDescription?: string;
  unitOfMeasure?: string;
  customMonthlyCost?: number;
  includeInReport: boolean; // Resolved value (uses service.defaultIncludeInReport if mode is IMPLICIT)
  includeInReportMode: "IMPLICIT" | "MANUAL_ON" | "MANUAL_OFF";
  activeFrom: string; // ISO date
  activeUntil?: string; // ISO date (optional)
  notes?: string;
}
```

---

### **2. UI/UX - Management Servicii**

#### **A. Lista Servicii**

**Endpoint:** `GET /services`

**Tabel:**
| Nume | Descriere | Cost/Formulă | Status | Acțiuni |
|------|-----------|--------------|--------|---------|
| Salubrizare | Servicii de curățenie | 50.00 RON | ✅ Activ | Edit | Delete |
| Cota întreținere | 3% din chirie | `rent * 0.03` | ✅ Activ | Edit | Delete |

**Acțiuni:**
- **Edit** - deschide formular de editare
- **Delete** - dezactivează serviciul (soft delete)

---

#### **B. Formular Creare/Editare Serviciu**

**Câmpuri:**
1. **Nume serviciu** (required) - input text
2. **Descriere** (optional) - textarea
3. **Unitate de măsură** (optional) - input text (ex: "lei", "mc", "kw")
4. **Tip calcul:**
   - Radio: "Cost fix lunar" / "Formulă de calcul"
5. **Dacă "Cost fix lunar":**
   - Input number: "Cost lunar (RON)"
6. **Dacă "Formulă de calcul":**
   - Textarea: "Expresie formulă"
   - Help text cu exemple și variabile disponibile
   - Preview: "Rezultat estimat: X RON" (dacă e posibil)
7. **Include în raport implicit** (optional) - checkbox (default: false)
   - Dacă bifat, serviciul va fi inclus automat în fisele de consum
   - Poate fi suprascris per contract

**Validare:**
- Nume: required, unique
- Cost sau formulă: cel puțin unul trebuie completat

---

#### **C. Formular Creare/Editare Contract - Secțiune Servicii**

**UI:**
- Checkbox list cu toate serviciile active
- Pentru fiecare serviciu selectat:
  - Input number: "Cost custom (RON)" - opțional, lasă gol pentru a folosi cost default sau formulă
  - **Stare "Include în raport":**
    - Radio buttons sau dropdown cu trei opțiuni:
      1. **IMPLICIT** - folosește valoarea din serviciu (`service.defaultIncludeInReport`)
      2. **ON MANUAL** - include explicit în raport
      3. **OFF MANUAL** - exclude explicit din raport
    - Indică starea actuală cu o etichetă (ex: "IMPLICIT (true)", "MANUAL_ON", "MANUAL_OFF")
  - Date picker: "Activ din" - default: `startDate` al contractului
  - Date picker: "Activ până" - opțional, lasă gol pentru activ indefinit
  - Textarea: "Note" - opțional

**Exemplu UI pentru "Include în raport":**
```
☐ IMPLICIT (folosește valoarea din serviciu)
● ON MANUAL (include explicit)
☐ OFF MANUAL (exclude explicit)

Stare actuală: IMPLICIT (true)
Valoare implicită din serviciu: true
```

**Validare:**
- `activeFrom` trebuie să fie între `startDate` și `endDate` ale contractului
- `activeUntil` (dacă setat) trebuie să fie între `activeFrom` și `endDate`

---

### **3. Excel Report - Comportament**

Excel-ul generat va include automat:

1. **Rânduri pentru fiecare serviciu activ cu `includeInReport = true`:**
   - Nume serviciu în prima coloană
   - Unitate de măsură din serviciu în a doua coloană
   - Costuri/valori calculate pe lună în coloanele lunilor

2. **Calcul automat:**
   - Pentru fiecare lună, verifică dacă serviciul este activ (`activeFrom` ≤ lună ≤ `activeUntil`)
   - Dacă este activ:
     - Folosește `customMonthlyCost` dacă este setat
     - Altfel, folosește `defaultMonthlyCost` dacă este setat
     - Altfel, calculează din formulă

3. **Filtrare servicii:**
   - Doar serviciile cu `includeInReport = true` (rezolvat) vor apărea în raport
   - Dacă modul este `IMPLICIT`, se folosește `service.defaultIncludeInReport`
   - Dacă modul este `MANUAL_ON`, se include în raport
   - Dacă modul este `MANUAL_OFF`, se exclude din raport
   - Serviciile hardcodate au fost eliminate - toate serviciile sunt definite prin endpoint

4. **Exemplu:**
   - Serviciu "Salubrizare" activ din 2025-01-01
   - Cost custom: 55 RON
   - Unitate de măsură: "lei"
   - `includeInReportMode: "MANUAL_ON"` → `includeInReport: true`
   - Excel va afișa: 55 RON pentru toate lunile din 2025
   
5. **Exemplu cu IMPLICIT:**
   - Serviciu "Cota întreținere" cu `defaultIncludeInReport: true`
   - Mod: `IMPLICIT` → `includeInReport: true` (folosește valoarea implicită)
   - Excel va afișa serviciul în raport

---

## 📝 Exemple de utilizare

### **Exemplu 1: Creare serviciu cu cost fix**

```typescript
const createService = async () => {
  const response = await fetch('/services', {
    method: 'POST',
    headers: { 'Content-Type': 'application/json' },
    body: JSON.stringify({
      name: 'Salubrizare',
      description: 'Servicii de curățenie lunară',
      unitOfMeasure: 'lei',
      defaultMonthlyCost: 50.00,
      defaultIncludeInReport: true,
      active: true
    })
  });
};
```

---

### **Exemplu 2: Creare serviciu cu formulă**

```typescript
const createServiceWithFormula = async () => {
  const response = await fetch('/services', {
    method: 'POST',
    headers: { 'Content-Type': 'application/json' },
    body: JSON.stringify({
      name: 'Cota întreținere',
      description: '3% din chirie + 10% din apă și gaz',
      unitOfMeasure: 'lei',
      defaultMonthlyCost: null,
      defaultIncludeInReport: true,
      formula: {
        expression: 'rent * 0.03 + (waterConsumption + gasConsumption) * 0.1',
        description: '3% din chirie + 10% din consum apă și gaz'
      },
      active: true
    })
  });
};
```

---

### **Exemplu 3: Actualizare servicii cu trei stări**

```typescript
const updateServices = async (rentalAgreementId: number) => {
  const response = await fetch(`/tenant-rental-data/${rentalAgreementId}/services`, {
    method: 'PUT',
    headers: { 'Content-Type': 'application/json' },
    body: JSON.stringify({
      services: [
        {
          serviceId: 1,
          active: true,
          customMonthlyCost: 55.00,
          includeInReport: true,
          useDefaultIncludeInReport: false, // ON MANUAL
          activeFrom: "2025-01-01",
          notes: "Cost negociat special"
        },
        {
          serviceId: 2,
          active: true,
          customMonthlyCost: null,
          includeInReport: null,
          useDefaultIncludeInReport: true, // IMPLICIT - folosește service.defaultIncludeInReport
          activeFrom: "2025-01-01",
          notes: "Folosește valoarea implicită"
        },
        {
          serviceId: 3,
          active: true,
          customMonthlyCost: null,
          includeInReport: false,
          useDefaultIncludeInReport: false, // OFF MANUAL - exclude explicit
          activeFrom: "2025-01-01",
          notes: "Nu include în raport"
        }
      ]
    })
  });
  
  const result = await response.json();
  // result.data.services[0].includeInReportMode = "MANUAL_ON"
  // result.data.services[1].includeInReportMode = "IMPLICIT"
  // result.data.services[2].includeInReportMode = "MANUAL_OFF"
};
```

---

### **Exemplu 4: Obținere servicii cu stări rezolvate**

```typescript
const getServices = async (rentalAgreementId: number) => {
  const response = await fetch(`/tenant-rental-data/${rentalAgreementId}/services`);
  const result = await response.json();
  
  result.data.services.forEach((service: ServiceWithResolvedIncludeInReport) => {
    console.log(`Service: ${service.serviceName}`);
    console.log(`Mode: ${service.includeInReportMode}`); // "IMPLICIT", "MANUAL_ON", "MANUAL_OFF"
    console.log(`Include in report: ${service.includeInReport}`); // Resolved value (true/false)
  });
};
```

---

### **Exemplu 5: Creare contract cu servicii**

```typescript
const createContractWithServices = async () => {
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
      activeServices: [
        {
          serviceId: 1, // Salubrizare
          customMonthlyCost: 55.00, // Override default cost
          includeInReport: true, // ON MANUAL - include explicit în raport
          activeFrom: '2025-01-01',
          activeUntil: null // Active indefinitely
        },
        {
          serviceId: 2, // Cota întreținere (formulă)
          customMonthlyCost: null, // Use formula
          includeInReport: null, // IMPLICIT - folosește service.defaultIncludeInReport
          activeFrom: '2025-01-01',
          activeUntil: '2025-06-30' // Only first 6 months
        },
        {
          serviceId: 3, // Serviciu auxiliar
          customMonthlyCost: 20.00,
          includeInReport: false, // OFF MANUAL - exclude explicit din raport
          activeFrom: '2025-01-01',
          activeUntil: null
        }
      ]
    })
  });
};
```

---

### **Exemplu 4: Actualizare servicii în contract**

```typescript
const updateServices = async (rentalAgreementId: number) => {
  const response = await fetch(`/tenant-rental-data/${rentalAgreementId}`, {
    method: 'PUT',
    headers: { 'Content-Type': 'application/json' },
    body: JSON.stringify({
      activeServices: [
        {
          serviceId: 1,
          customMonthlyCost: 60.00, // Increased cost
          includeInReport: true, // ON MANUAL - include explicit în raport
          activeFrom: '2025-01-01',
          activeUntil: null
        }
        // Note: This replaces ALL existing services
      ]
    })
  });
};
```

---

## ⚠️ Note importante

1. **Servicii generale:** Serviciile sunt create o singură dată și pot fi folosite în multiple contracte.

2. **Unitate de măsură:** Fiecare serviciu poate avea o unitate de măsură (ex: "lei", "mc", "kw") care va apărea în Excel.

3. **Cost custom:** Dacă setați `customMonthlyCost` într-un contract, acesta are prioritate peste `defaultMonthlyCost` sau formulă.

4. **Include în raport - trei stări disponibile:** 
   - **IMPLICIT** (`includeInReport: null`): folosește `service.defaultIncludeInReport` (implicit pentru servicii noi)
   - **ON MANUAL** (`includeInReport: true`): include explicit în raport
   - **OFF MANUAL** (`includeInReport: false`): exclude explicit din raport
   - Doar serviciile cu `includeInReport = true` (rezolvat) vor apărea în fisele de consum
   - Pentru revenirea la IMPLICIT după ce a fost setat manual, folosiți `PUT /tenant-rental-data/{rentalAgreementId}/services` cu `useDefaultIncludeInReport: true`

5. **Formule:** Formulele sunt evaluate pentru fiecare lună folosind valorile reale ale chiriei și consumului din acea lună.

6. **Date activare:** Serviciile pot fi activate/dezactivate pentru perioade specifice în cadrul unui contract.

7. **Înlocuire servicii:** Când actualizați `activeServices` într-un contract, se **înlocuiesc toate** serviciile existente. Nu se adaugă la lista existentă.

8. **Excel:** 
   - Serviciile apar în Excel doar pentru lunile în care sunt active (`activeFrom` ≤ lună ≤ `activeUntil`)
   - Doar serviciile cu `includeInReport = true` (rezolvat) vor apărea în raport
   - Dacă modul este `IMPLICIT`, se folosește `service.defaultIncludeInReport`
   - Dacă modul este `MANUAL_ON`, se include în raport
   - Dacă modul este `MANUAL_OFF`, se exclude din raport
   - Nu mai există servicii hardcodate - toate serviciile sunt definite prin endpoint

---

## ✅ Checklist Frontend

- [ ] Actualizare interfețe TypeScript (`Service`, `ServiceFormula`, `ActiveService`, etc.)
- [ ] Pagină management servicii (listă + CRUD)
- [ ] Formular creare/editare serviciu (cu suport pentru formule)
- [ ] Validare formule (syntax check)
- [ ] Secțiune servicii în formular creare contract
- [ ] Secțiune servicii în formular editare contract
- [ ] Validare date activare servicii
- [ ] Afișare servicii active în detalii contract
- [ ] Testare creare servicii cu cost fix
- [ ] Testare creare servicii cu formule
- [ ] Testare activare servicii în contracte
- [ ] Testare Excel cu servicii active
- [ ] Testare formule cu diferite variabile

---

**Data actualizării:** Noiembrie 2025  
**Versiune:** 1.0

