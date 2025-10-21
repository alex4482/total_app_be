await fetch(`/files/${fileId}`, { method: 'DELETE' });
```

### 4. Important:
- **Fișierele temporare** nu sunt șterse automat - asigură-te că faci commit după upload
- **modifiedAt** = data ultimei modificări a fișierului (File.lastModified)
- **uploadedAt** = data la care fișierul a fost încărcat pe server
- Când ștergi un tenant/building, fișierele asociate sunt șterse **automat**
- ZIP download-ul este **GET**, nu POST (pentru ușurință în browser)
# Files API

Base URL: `/files`

Această API gestionează upload-ul, listarea, descărcarea și ștergerea fișierelor asociate cu diverse entități (tenants, buildings, etc.).

## Workflow de Upload Fișiere

### Pasul 1: Upload Temporar
Fișierele sunt mai întâi uploadate într-o zonă temporară.

**POST** `/files/temp`

**Content-Type:** `multipart/form-data`

#### Request
```typescript
// Form data
files: File[]  // unul sau mai multe fișiere
batchId?: string  // UUID optional pentru grupare
```

#### Response (200 OK)
```json
[
  {
    "tempId": "uuid-temp-1",
    "filename": "document.pdf",
    "size": 102400,
    "contentType": "application/pdf",
    "batchId": "uuid-batch-1"
  }
]
```

### Pasul 2: Commit la Owner
După upload temporar, fișierele trebuie "committed" la un owner (tenant, building, etc.).

**POST** `/files/commit`

#### Query Parameters
- `ownerType` (String, **required**) - Tipul owner-ului: "TENANT", "BUILDING", "ROOM", etc.
- `ownerId` (Long, **required**) - ID-ul owner-ului
- `tempIds` (UUID[], **required**) - Lista de ID-uri temporare
- `overwrite` (Boolean, optional, default=false) - Permite suprascrierea fișierelor existente

#### Request Example
```
POST /files/commit?ownerType=TENANT&ownerId=5&tempIds=uuid1&tempIds=uuid2&overwrite=false
```

#### Response (200 OK)
```json
[
  {
    "id": "uuid-permanent-1",
    "ownerType": "TENANT",
    "ownerId": 5,
    "filename": "document.pdf",
    "contentType": "application/pdf",
    "size": 102400,
    "checksum": "abc123...",
    "downloadUrl": "/files/uuid-permanent-1",
    "modifiedAt": "2024-01-15T10:30:00Z",
    "uploadedAt": "2024-01-15T10:30:00Z"
  }
]
```

**Errors:**
- `400 Bad Request` - Parametri lipsă sau ownerType invalid
- `409 Conflict` - Fișier cu același nume există deja (și overwrite=false)
- `500 Internal Server Error` - Eroare la procesare

---

## Endpoints

### 1. List Files by Owner
**GET** `/files`

Returnează lista fișierelor pentru un owner specific.

#### Query Parameters
- `ownerType` (String, **required**) - "TENANT", "BUILDING", "ROOM", etc.
- `ownerId` (Long, **required**) - ID-ul owner-ului

#### Example
```
GET /files?ownerType=TENANT&ownerId=5
```

#### Response (200 OK)
```json
[
  {
    "id": "uuid-1",
    "ownerType": "TENANT",
    "ownerId": 5,
    "filename": "contract.pdf",
    "contentType": "application/pdf",
    "size": 204800,
    "checksum": "abc123...",
    "downloadUrl": "/files/uuid-1",
    "modifiedAt": "2024-01-15T10:30:00Z",
    "uploadedAt": "2024-01-15T10:30:00Z"
  }
]
```

**Errors:**
- `400 Bad Request` - ownerType invalid

---

### 2. Download File
**GET** `/files/{id}`

Descarcă un fișier după ID-ul său.

#### Path Parameters
- `id` (UUID, **required**) - ID-ul fișierului

#### Response (200 OK)
- **Headers:**
  - `Content-Type`: tipul fișierului (ex: "application/pdf")
  - `Content-Disposition`: `attachment; filename*=UTF-8''encoded-filename.pdf`
  - `Content-Length`: dimensiunea în bytes
- **Body:** bytes-urile fișierului

**Errors:**
- `404 Not Found` - Fișierul nu există

---

### 3. Download Multiple Files as ZIP
**GET** `/files/download-zip`

Descarcă mai multe fișiere într-o arhivă ZIP.

#### Query Parameters
- `fileIds` (UUID[], **required**) - Lista de ID-uri fișiere

#### Example
```
GET /files/download-zip?fileIds=uuid1&fileIds=uuid2&fileIds=uuid3
```

#### Response (200 OK)
- **Headers:**
  - `Content-Type`: `application/octet-stream`
  - `Content-Disposition`: `attachment; filename*=UTF-8''files_20240115_103000.zip`
  - `Content-Length`: dimensiunea ZIP-ului
- **Body:** bytes-urile arhivei ZIP

**Behavior:**
- Fișierele lipsă sunt **omise** (nu dau eroare)
- Dacă NICIUN fișier nu este găsit → `404 Not Found`
- Numele ZIP-ului conține timestamp-ul descărcării

**Errors:**
- `400 Bad Request` - Lista fileIds lipsă sau goală
- `404 Not Found` - Niciun fișier valid găsit
- `500 Internal Server Error` - Eroare la crearea ZIP-ului

---

### 4. Delete File
**DELETE** `/files/{id}`

Șterge un fișier (din baza de date și filesystem).

#### Path Parameters
- `id` (UUID, **required**) - ID-ul fișierului

#### Response
**Success (204 No Content)** - Fișierul a fost șters

**Errors:**
- `404 Not Found` - Fișierul nu există

---

## OwnerType Values

Valorile valide pentru `ownerType`:
- `TENANT` - Chiriași
- `BUILDING` - Clădire
- `ROOM` - Cameră/Spațiu
- `RENTAL_SPACE` - Spațiu de închiriat
- `EMAIL_DATA` - Date email
- `BUILDING_LOCATION` - Locație clădire
- `FIRM` - Firmă
- `CAR` - Mașină
- `OTHER` - Altele

---

## Model de Date

### FileDto
```typescript
interface FileDto {
  id: string;              // UUID
  ownerType: string;       // TENANT, BUILDING, etc.
  ownerId: number;
  filename: string;        // Numele original al fișierului
  contentType: string;     // MIME type (ex: "application/pdf")
  size: number;            // Dimensiune în bytes
  checksum: string;        // Hash pentru verificare integritate
  downloadUrl: string;     // URL relativ pentru download (ex: "/files/uuid")
  modifiedAt?: string;     // Data ultimei modificări (ISO format)
  uploadedAt?: string;     // Data upload-ului pe server (ISO format)
}
```

### TempUploadDto
```typescript
interface TempUploadDto {
  tempId: string;          // UUID temporar
  filename: string;
  size: number;
  contentType: string;
  batchId: string;         // UUID pentru grupare
}
```

---

## Note pentru Frontend

### 1. Workflow complet de upload:
```typescript
// Pasul 1: Upload temporar
const formData = new FormData();
files.forEach(file => formData.append('files', file));

const tempResponse = await fetch('/files/temp', {
  method: 'POST',
  body: formData
});
const tempFiles = await tempResponse.json();

// Pasul 2: Commit la owner
const tempIds = tempFiles.map(f => f.tempId);
const commitResponse = await fetch(
  `/files/commit?ownerType=TENANT&ownerId=5&tempIds=${tempIds.join('&tempIds=')}`,
  { method: 'POST' }
);
const permanentFiles = await commitResponse.json();
```

### 2. Download ZIP:
```typescript
const fileIds = ['uuid1', 'uuid2', 'uuid3'];
const url = `/files/download-zip?${fileIds.map(id => `fileIds=${id}`).join('&')}`;
window.open(url, '_blank');
```

### 3. Ștergere fișier:
```typescript

