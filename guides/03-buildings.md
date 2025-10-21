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
  "buildingLocation": "PANTELIMON",
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
  "buildingLocation": "BERCENI"
}
```

#### Response
**Success (200 OK)**
```json
{
  "id": 2,
  "name": "Building B",
  "address": "Strada Nouă nr. 10",
  "buildingLocation": "BERCENI",
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

## Model de Date

### Building
```typescript
interface Building {
  id: number;
  name: string;
  address: string;
  buildingLocation: string;
  rooms: Room[];
}
```

### RentalSpace (extends Room)
```typescript
interface RentalSpace {
  id: number;
  name: string;
  floor: number;
  area: number;
  groundLevel: boolean;
  isEmpty: boolean;
  building: Building;
}
```

---

## Note pentru Frontend
1. **buildingLocation** este un parametru obligatoriu pentru majoritatea endpoint-urilor
2. Valorile posibile pentru `buildingLocation`: "PANTELIMON", "BERCENI", etc.
3. `groundLevel` = `true` înseamnă parter, `false` înseamnă etaj
4. `isEmpty` indică dacă spațiul este disponibil sau ocupat

