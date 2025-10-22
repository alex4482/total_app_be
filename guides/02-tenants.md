# Tenants API

Base URL: `/tenants`

## Endpoints

### 1. List Tenants
**GET** `/tenants`

Returnează lista de chiriași filtrati după diverse criterii.

#### Query Parameters
- `active` (Boolean, optional) - Filtrează după status activ/inactiv
- `buildingLocation` (String, optional) - Filtrează după locație (ex: "LETCANI", "TOMESTI")
- `buildingId` (String, optional) - Filtrează după ID clădire
- `groundLevel` (Boolean, optional) - Filtrează după nivel (parter/etaj)

#### Response
**Success (200 OK)**
```json
[
  {
    "id": 1,
    "name": "Tenant Name SRL",
    "cui": "RO12345678",
    "regNumber": "J40/1234/2023",
    "pf": false,
    "active": true,
    "emails": ["email@example.com"],
    "phoneNumbers": ["0712345678"],
    "observations": [],
    "attachmentIds": [],
    "rentalData": []
  }
]
```

**No Content (204)** - Nu există chiriași care să corespundă filtrelor

---

### 2. Get Tenant by ID
**GET** `/tenants/{id}`

Returnează detaliile unui chiriași specific.

#### Path Parameters
- `id` (Long) - ID-ul chiriașului

#### Response
**Success (302 Found)**
```json
{
  "id": 1,
  "name": "Tenant Name SRL",
  "cui": "RO12345678",
  "regNumber": "J40/1234/2023",
  "pf": false,
  "active": true,
  "emails": ["email@example.com"],
  "phoneNumbers": ["0712345678"],
  "observations": [
    {
      "date": "2024-01-15T10:30:00",
      "text": "Observație importantă"
    }
  ],
  "attachmentIds": ["uuid-file-1", "uuid-file-2"],
  "rentalData": []
}
```

**Error**
- `400 Bad Request` - ID invalid
- `204 No Content` - Chiriașul nu există

---

### 3. Create Tenant
**POST** `/tenants`

Creează un chiriași nou.

#### Request Body
```json
{
  "name": "Tenant Name SRL",
  "cui": "RO12345678",
  "regNumber": "J40/1234/2023",
  "pf": false,
  "active": false,
  "emails": ["email@example.com", "alt@example.com"],
  "phoneNumbers": ["0712345678", "0787654321"],
  "observations": [
    {
      "date": "2024-01-15T10:30:00",
      "text": "Observație la creare"
    }
  ]
}
```

**Note:**
- `name` - Obligatoriu, numele chiriașului
- `cui` - CUI/CNP (opțional)
- `regNumber` - Număr registru comerț (opțional)
- `pf` - Boolean, persoană fizică sau juridică (opțional)
- `active` - Dacă nu este specificat, **default este `false`**

#### Response
**Success (201 Created)**
```json
{
  "id": 1,
  "name": "Tenant Name SRL",
  "cui": "RO12345678",
  "regNumber": "J40/1234/2023",
  "pf": false,
  "active": false,
  "emails": ["email@example.com"],
  "phoneNumbers": ["0712345678"],
  "observations": [],
  "attachmentIds": [],
  "rentalData": []
}
```

**Error**
- `400 Bad Request` - Date invalide (ValidationException)
- `500 Internal Server Error` - Eroare la salvare

---

### 4. Update Tenant
**PATCH** `/tenants/{id}`

Actualizează detaliile unui chiriași existent.

#### Path Parameters
- `id` (Long) - ID-ul chiriașului de actualizat

#### Request Body
```json
{
  "name": "Tenant Name SRL Updated",
  "cui": "RO12345678",
  "regNumber": "J40/1234/2023",
  "pf": false,
  "active": true,
  "emails": ["newemail@example.com"],
  "phoneNumbers": ["0712345678"],
  "observations": []
}
```

#### Response
**Success (200 OK)**
```json
{
  "id": 1,
  "name": "Tenant Name SRL Updated",
  "cui": "RO12345678",
  "regNumber": "J40/1234/2023",
  "pf": false,
  "active": true,
  "emails": ["newemail@example.com"],
  "phoneNumbers": ["0712345678"],
  "observations": [],
  "attachmentIds": [],
  "rentalData": []
}
```

**Error**
- `400 Bad Request` - Date invalide
- `500 Internal Server Error` - Eroare la actualizare

---

### 5. Delete Tenant
**DELETE** `/tenants/{id}`

Șterge un chiriași și toate fișierele asociate acestuia.

#### Path Parameters
- `id` (Long) - ID-ul chiriașului de șters

#### Response
**Success (204 No Content)** - Chiriașul a fost șters cu succes

**Error**
- `400 Bad Request` - ID invalid
- `404 Not Found` - Chiriașul nu există
- `409 Conflict` - Chiriașul nu poate fi șters (există referințe în alte tabele)
- `500 Internal Server Error` - Eroare neașteptată

**Note importante:**
- Când ștergi un chiriași, se șterg **automat toate fișierele** asociate acestuia (și din DB și din filesystem)
- Fișierele temporare NU sunt șterse automat

---

### 6. Bulk Delete Tenants
**DELETE** `/tenants/bulk-delete`

Șterge mai mulți chiriași simultan.

#### Request Body
```json
[1, 2, 3, 4, 5]
```

Array de ID-uri de chiriași de șters.

#### Response
**Success (200 OK)**
```json
{
  "totalRequested": 5,
  "deletedCount": 3,
  "failedCount": 2,
  "failedTenants": [
    {
      "id": 2,
      "name": "Tenant 2",
      "reason": "Tenant has active rental agreements"
    },
    {
      "id": 4,
      "name": "Tenant 4",
      "reason": "Not found"
    }
  ]
}
```

**Error**
- `400 Bad Request` - Lista goală sau null

---

### 7. Import Tenants from Excel
**POST** `/tenants/import`

Importă chiriași dintr-un fișier Excel. Poate crea chiriași noi sau actualiza existenți.

#### Request
**Content-Type:** `multipart/form-data`

**Form Parameters:**
- `file` (File) - Fișier Excel (.xlsx sau .xls)

#### Excel Format
Fișierul Excel trebuie să aibă următoarele coloane (prima linie = header):

| Cumparator | Nr. Reg | CodFiscal | email | telefoane |
|------------|---------|-----------|-------|-----------|
| Tenant SRL | J40/123 | RO123456  | test@example.com | 0712345678 |

**Coloane:**
- `Cumparator` - **Obligatoriu**, numele chiriașului
- `Nr. Reg` - Număr registru (opțional)
- `CodFiscal` - CUI (opțional)
- `email` - O singură adresă de email (opțional)
- `telefoane` - Un singur număr de telefon (opțional)

**Comportament:**
- Dacă un chiriași cu același **nume** există deja → se face **UPDATE** (se actualizează doar câmpurile prezente în Excel)
- Dacă nu există → se creează un chiriași nou
- Chiriașii importați sunt setați ca **active=false** by default

#### Response
**Success (200 OK)**
```json
{
  "totalRows": 10,
  "savedCount": 8,
  "skippedCount": 2,
  "skippedNames": ["Tenant duplicat 1", "Tenant duplicat 2"]
}
```

**Error**
- `400 Bad Request` - Fișier lipsă sau format invalid
- `500 Internal Server Error` - Eroare la procesare

---

### 8. New Rental Agreement
**POST** `/tenants/new-rental-agreement`

Creează un nou contract de închiriere pentru un chiriași.

#### Request Body
```json
{
  "tenantId": 1,
  "spaceId": 5,
  "startDate": "2024-01-01",
  "endDate": "2024-12-31",
  "monthlyRent": 1500.00,
  "deposit": 3000.00
}
```

#### Response
**Success (201 Created)**
```json
{
  "id": 1,
  "tenant": { ... },
  "rentalSpace": { ... },
  "startDate": "2024-01-01",
  "endDate": "2024-12-31",
  "monthlyRent": 1500.00,
  "deposit": 3000.00
}
```

**Error**
- `500 Internal Server Error` - Eroare la creare

---

## Model de Date

### Tenant
```typescript
interface Tenant {
  id: number;
  name: string;
  cui?: string;
  regNumber?: string;
  pf?: boolean;
  active?: boolean;
  emails?: string[];
  phoneNumbers?: string[];
  observations?: Observation[];
  attachmentIds?: string[];
  rentalData?: TenantRentalData[];
}

interface Observation {
  date: string; // ISO date format
  text: string;
}
```

---

## Note pentru Frontend
1. **Active by default:** Când creezi un chiriași nou, câmpul `active` este `false` dacă nu îl specifici explicit
2. **Duplicate check:** La import Excel, se verifică duplicatele după **numele** chiriașului
3. **File deletion:** Când ștergi un chiriași, **toate fișierele** sale sunt șterse automat
4. **Bulk operations:** Folosește bulk delete pentru a șterge mai mulți chiriași deodată - primești înapoi o listă cu cei care nu au putut fi șterși și motivul
5. **Import/Update logic:** La import Excel, dacă există deja un tenant cu același nume, se face UPDATE doar la câmpurile prezente în Excel

