# Buildings API

Base URL: `/buildings`

## Endpoints

### 1. List Buildings
**GET** `/buildings`

Returnează lista de clădiri filtrate după locație.

#### Query Parameters
- `buildingLocation` (String, **required**) - Locația clădirilor (ex: "PANTELIMON", "BERCENI")

#### Response
**Success (200 OK)**
```json
[
  {
    "id": 1,
    "name": "Building A",
    "buildingLocation": "PANTELIMON",
    "rooms": []
  }
]
```

---

### 2. Get Building by ID
**GET** `/buildings/{bid}`

Returnează detaliile unei clădiri specifice.

#### Path Parameters
- `bid` (Long) - ID-ul clădirii

#### Response
**Success (200 OK)**
```json
{
  "id": 1,
  "name": "Building A",
  "address": "Strada Exemplu nr. 1",
  "buildingLocation": "LETCANI",
  "rooms": [
    {
      "id": 10,
      "name": "Room 1",
      "area": 50.5
    }
  ]
}
```

---

### 3. Create Building
**POST** `/buildings`

Creează o clădire nouă.

#### Request Body
```json
{
  "name": "Building B",
  "address": "Strada Nouă nr. 10",
  "buildingLocation": "TOMESTI"
}
```

#### Response
**Success (200 OK)**
```json
{
  "id": 2,
  "name": "Building B",
  "address": "Strada Nouă nr. 10",
  "buildingLocation": "TOMESTI",
  "rooms": []
}
```

---

### 4. List Spaces from Building
**GET** `/buildings/{bid}/spaces`

Returnează lista spațiilor dintr-o clădire.

#### Path Parameters
- `bid` (String) - ID-ul clădirii

#### Query Parameters
- `groundLevel` (Boolean, **required**) - Filtrare după nivel (parter/etaj)
- `empty` (Boolean, **required**) - Filtrare după disponibilitate (liber/ocupat)

#### Response
**Success (200 OK)**
```json
[
  {
    "id": 1,
    "name": "Space 101",
    "floor": 0,
    "area": 45.5,
    "groundLevel": true,
    "isEmpty": true,
    "building": { ... }
  }
]
```

---

### 5. List All Spaces
**GET** `/buildings/spaces`

Returnează lista tuturor spațiilor filtrate.

#### Query Parameters
- `buildingLocation` (String, **required**)
- `groundLevel` (Boolean, **required**)
- `empty` (Boolean, **required**)

#### Response
Similar cu endpoint-ul anterior.

---

### 6. List Spaces Rented by Tenant (in specific building)
**GET** `/buildings/{bid}/spaces/{tid}`

Returnează spațiile închiriate de un anumit chiriași într-o clădire specifică.

#### Path Parameters
- `buildingId` (String) - ID clădire
- `tenantId` (String) - ID chiriași

#### Query Parameters
- `buildingLocation` (String, **required**)
- `groundLevel` (Boolean, **required**)

---

### 7. List Spaces Rented by Tenant (all buildings)
**GET** `/buildings/spaces/{tid}`

Returnează toate spațiile închiriate de un anumit chiriași.

#### Path Parameters
- `tenantId` (String) - ID chiriași

#### Query Parameters
- `buildingLocation` (String, **required**)
- `groundLevel` (Boolean, **required**)

---

### 8. Get Space by ID
**GET** `/buildings/spaces/{sid}` sau **GET** `/buildings/{bid}/spaces/{sid}`

Returnează detaliile unui spațiu specific.

#### Path Parameters
- `bid` (Long, optional) - ID clădire
- `sid` (Long, **required**) - ID spațiu

#### Response
**Success (200 OK)**
```json
{
  "id": 1,
  "name": "Space 101",
  "floor": 0,
  "area": 45.5,
  "groundLevel": true,
  "isEmpty": false,
  "building": {
    "id": 1,
    "name": "Building A"
  }
}
```

---

### 9. Add Space to Building
**POST** `/buildings/spaces` sau **POST** `/buildings/{bid}/spaces`

Adaugă un spațiu nou la o clădire.

#### Path Parameters
- `bid` (Long) - ID clădire

#### Request Body
```json
{
  "name": "Space 102",
  "floor": 1,
  "area": 60.0,
  "groundLevel": false
}
```

#### Response
**Success (200 OK)**
```json
{
  "id": 2,
  "name": "Space 102",
  "floor": 1,
  "area": 60.0,
  "groundLevel": false,
  "isEmpty": true,
  "building": { ... }
}
```

---

### 10. List Building Locations
**GET** `/buildings/locations`

Returnează lista de locații disponibile pentru clădiri (enum BuildingLocation).

#### Response
**Success (200 OK)**
```json
[
  "LETCANI",
  "TOMESTI"
]
```

---

### 11. List Location Types
**GET** `/buildings/location-types`

Returnează lista de tipuri de locații disponibile (enum LocationType).

#### Response
**Success (200 OK)**
```json
[
  "OFFICE",
  "WAREHOUSE",
  "STORAGE",
  "RETAIL"
]
```

---

### 12. Import Locations from Excel
**POST** `/buildings/import`

Importă locații (Building, Room, RentalSpace) dintr-un fișier Excel.

#### Request
- **Content-Type**: `multipart/form-data`
- **Parameter**: `file` (MultipartFile) - fișier Excel (.xlsx)

#### Excel Format
Fișierul Excel trebuie să conțină următoarele coloane (header row obligatoriu):

**Notă:** Numele coloanelor sunt configurabile în `application.properties` sub `app.excel.location.columns.*`

Configurare implicită:

| Column | Name | Description | Required | Applies To |
|--------|------|-------------|----------|------------|
| 0 | LocationType | Tipul locației: "Building", "Room", "RentalSpace" | ✓ | All |
| 1 | Name | Numele locației (UNIC în baza de date) | ✓ | All |
| 2 | OfficialName | Numele oficial (din registre) | ✗ | All |
| 3 | BuildingLocation | Locația: "LETCANI" sau "TOMESTI" | ✗ | All |
| 4 | Mp | Suprafața în metri pătrați | ✗ | All |
| 5 | GroundLevel | true/false pentru parter | ✗ | Room, RentalSpace |
| 6 | BuildingName | Numele clădirii părinte | ✗ | Room, RentalSpace |

**Customizare nume coloane în `application.properties`:**
```properties
app.excel.location.columns.locationType=LocationType
app.excel.location.columns.name=Name
app.excel.location.columns.officialName=OfficialName
app.excel.location.columns.buildingLocation=BuildingLocation
app.excel.location.columns.mp=Mp
app.excel.location.columns.groundLevel=GroundLevel
app.excel.location.columns.buildingName=BuildingName
```

**Valori valide:**
- **LocationType**: BUILDING, ROOM, RENTAL_SPACE
- **BuildingLocation**: LETCANI, TOMESTI
- **GroundLevel**: true, false, da, yes, 1, x

**Comportament:**
- Numele locației este **UNIC** - dacă există deja o locație cu același nume, se va face **UPDATE**
- Dacă nu există, se va face **CREATE**
- Doar câmpurile prezente în Excel vor fi actualizate
- Pentru Room/RentalSpace, BuildingName trebuie să fie numele exact al unei clădiri existente

#### Request Example (FormData)
```typescript
const formData = new FormData();
formData.append('file', excelFile);

fetch('/buildings/import', {
  method: 'POST',
  body: formData
});
```

#### Response
**Success (200 OK)**
```json
{
  "totalRows": 15,
  "created": 8,
  "updated": 5,
  "skipped": 2,
  "errors": [
    "Row 3: Invalid BuildingLocation 'INVALID'. Valid values: LETCANI, TOMESTI",
    "Row 7: Building with name 'Clădire Inexistentă' not found",
    "Row 9: Invalid LocationType 'OFFICE'. Valid values: BUILDING, ROOM, RENTALSPACE"
  ]
}
```

---

### 13. Export Locations to Excel
**GET** `/buildings/export`

Exportă toate locațiile (Building, Room, RentalSpace) într-un fișier Excel.

#### Response
**Success (200 OK)**
- **Content-Type**: `application/octet-stream`
- **Content-Disposition**: `attachment; filename="locations-export.xlsx"`
- **Body**: fișier Excel binar

#### Usage Example
```typescript
// Download Excel file
const response = await fetch('/buildings/export', {
  method: 'GET',
  credentials: 'include'
});

const blob = await response.blob();
const url = window.URL.createObjectURL(blob);
const a = document.createElement('a');
a.href = url;
a.download = 'locations-export.xlsx';
document.body.appendChild(a);
a.click();
a.remove();
window.URL.revokeObjectURL(url);
```

**Excel Structure:**
Același format ca la import, cu toate locațiile din baza de date.

---

## Model de Date

### Building
```typescript
interface Building {
  id: number;
  name: string;
  officialName?: string;
  location: BuildingLocation; // enum
  mp?: number;
  type?: LocationType; // enum
  rooms: Room[];
}
```

### Room
```typescript
interface Room {
  id: number;
  name: string;
  officialName?: string;
  location: BuildingLocation;
  mp?: number;
  type?: LocationType;
  groundLevel?: boolean;
  building?: Building;
}
```

### RentalSpace (extends Room)
```typescript
interface RentalSpace extends Room {
  rentalAgreement?: TenantRentalData;
}
```

### LocationImportResultDto
```typescript
interface LocationImportResultDto {
  totalRows: number;
  created: number;
  updated: number;
  skipped: number;
  errors: string[];
}
```

---

## Note pentru Frontend
1. **buildingLocation** este un parametru obligatoriu pentru majoritatea endpoint-urilor
2. Valorile posibile pentru `buildingLocation`: "LETCANI", "TOMESTI"
3. `groundLevel` = `true` înseamnă parter, `false` înseamnă etaj
4. `isEmpty` indică dacă spațiul este disponibil sau ocupat

