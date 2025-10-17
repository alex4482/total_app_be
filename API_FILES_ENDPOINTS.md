# API Endpoints - File Upload & Management

## Rezumat rapid pentru integrare Frontend

Aplicația folosește un sistem de **upload în 2 pași**:
1. **Upload temporar** - fișierele sunt încărcate într-o zonă temporară
2. **Commit** - fișierele sunt asociate definitiv cu o entitate (tenant, building, etc.)

---

## 📤 1. UPLOAD TEMPORAR

### POST `/files/temp`

Încarcă fișiere în zona temporară (staging).

**Content-Type:** `multipart/form-data`

**Parametri:**
- `files` (required) - unul sau mai multe fișiere
- `batchId` (optional) - UUID pentru gruparea fișierelor din același upload
  - Dacă nu este furnizat, se generează automat unul nou

**Request Example (JavaScript/Fetch):**
```javascript
const formData = new FormData();
formData.append('files', file1);
formData.append('files', file2);
// batchId optional - se generează automat dacă lipsește

const response = await fetch('/uploads/temp', {
  method: 'POST',
  body: formData
});

const result = await response.json();
```

**Response:** `List<TempUploadDto>`

```json
[
  {
    "tempId": "550e8400-e29b-41d4-a716-446655440000",
    "batchId": "660e8400-e29b-41d4-a716-446655440001",
    "filename": "contract.pdf",
    "contentType": "application/pdf",
    "sizeBytes": 245680
  },
  {
    "tempId": "770e8400-e29b-41d4-a716-446655440002",
    "batchId": "660e8400-e29b-41d4-a716-446655440001",
    "filename": "factura.xlsx",
    "contentType": "application/vnd.openxmlformats-officedocument.spreadsheetml.sheet",
    "sizeBytes": 15234
  }
]
```

**Note:**
- Fișierele rămân în zona temporară până la commit
- Salvează `tempId`-urile pentru pasul următor
- Multiple fișiere pot fi grupate cu același `batchId`

---

## ✅ 2. COMMIT FILES (asociere cu entitate)

### POST `/files/commit`

Asociază fișierele temporare cu o entitate specifică (TENANT, BUILDING, etc.).

**Parametri:**
- `ownerType` (required) - tipul entității (TENANT, BUILDING, ROOM, RENTAL_SPACE, etc.)
- `ownerId` (required) - ID-ul entității
- `tempIds` (required) - listă de UUID-uri din răspunsul upload-ului temporar
- `overwrite` (optional, default=false) - permite suprascrierea fișierelor cu același nume

**Request Example:**
```javascript
// Variant 1: Query parameters
const params = new URLSearchParams();
params.append('ownerType', 'TENANT');
params.append('ownerId', '123');
params.append('tempIds', '550e8400-e29b-41d4-a716-446655440000');
params.append('tempIds', '770e8400-e29b-41d4-a716-446655440002');
params.append('overwrite', 'false');

const response = await fetch('/files/commit?' + params.toString(), {
  method: 'POST'
});

const committedFiles = await response.json();
```

**Response:** `List<FileDto>`

```json
[
  {
    "id": "880e8400-e29b-41d4-a716-446655440003",
    "ownerType": "TENANT",
    "ownerId": 123,
    "filename": "contract.pdf",
    "contentType": "application/pdf",
    "sizeBytes": 245680,
    "checksum": "sha256:abcdef123456...",
    "downloadUrl": "/files/880e8400-e29b-41d4-a716-446655440003"
  }
]
```

**Funcționalități commit:**
- ✅ Verifică duplicare după checksum (evită upload-uri duplicate)
- ✅ Verifică unicitate nume fișier per owner (dacă `overwrite=false`)
- ✅ Mută fișierul din zona temporară în storage permanent
- ✅ Șterge fișierele temporare automat
- ✅ Tranzacțional - rollback automat în caz de eroare

**Status Codes:**
- `200 OK` - Fișiere commit-uite cu succes
- `400 BAD REQUEST` - Parametri lipsă sau invalizi (tempId inexistent)
- `409 CONFLICT` - Fișier cu același nume există deja (când overwrite=false)
- `500 INTERNAL SERVER ERROR` - Eroare la salvarea fișierelor

**OwnerType-uri disponibile:**
```
TENANT, BUILDING, ROOM, RENTAL_SPACE, EMAIL_DATA, 
BUILDING_LOCATION, FIRM, CAR, OTHER
```

---

## 📋 3. LISTARE FIȘIERE

### GET `/files`

Listează toate fișierele asociate cu o entitate.

**Parametri:**
- `ownerType` (required) - tipul entității (TENANT, BUILDING, etc.)
- `ownerId` (required) - ID-ul entității

**Request Example:**
```javascript
const response = await fetch('/files?ownerType=TENANT&ownerId=123');
const files = await response.json();
```

**Response:** `List<FileDto>`

```json
[
  {
    "id": "880e8400-e29b-41d4-a716-446655440003",
    "ownerType": "TENANT",
    "ownerId": 123,
    "filename": "contract.pdf",
    "contentType": "application/pdf",
    "sizeBytes": 245680,
    "checksum": "sha256:abcdef123456...",
    "downloadUrl": "/files/download/880e8400-e29b-41d4-a716-446655440003"
  }
]
```

---

## ⬇️ 4. DOWNLOAD FIȘIER

### GET `/files/download/{id}`

Descarcă un fișier după UUID.

**Parametri:**
- `id` (path) - UUID-ul fișierului

**Request Example:**
```javascript
// Direct link
window.open('/files/download/880e8400-e29b-41d4-a716-446655440003');

// Sau cu fetch pentru download
const response = await fetch('/files/download/880e8400-e29b-41d4-a716-446655440003');
const blob = await response.blob();
const url = window.URL.createObjectURL(blob);
const a = document.createElement('a');
a.href = url;
a.download = 'contract.pdf';
a.click();
```

**Headers returnate:**
- `Content-Type` - tipul fișierului
- `Content-Disposition: attachment; filename*=UTF-8''[filename]`
- `Content-Length` - dimensiunea în bytes

**Note:**
- Browserul va descărca fișierul automat cu numele original
- Fișierul este încărcat din BLOB (database) sau filesystem (fallback)

---

## 🔄 FLOW COMPLET DE UPLOAD

```
┌─────────────────────────────────────────────────────────┐
│  1. CLIENT: Upload fișiere la /uploads/temp            │
│     ↓ primește List<TempUploadDto> cu tempId-uri       │
├─────────────────────────────────────────────────────────┤
│  2. CLIENT: Salvează tempId-urile temporar             │
│     (ex: în state/form până la submit final)            │
├─────────────────────────────────────────────────────────┤
│  3. CLIENT: La submit formular principal               │
│     ↓ trimite tempId-uri + ownerType + ownerId         │
│     ↓ la endpoint de commit                             │
├─────────────────────────────────────────────────────────┤
│  4. SERVER: Commit asociază fișierele cu entitatea     │
│     ↓ returnează List<FileDto> cu id-uri permanente    │
├─────────────────────────────────────────────────────────┤
│  5. CLIENT: Poate lista/descărca fișierele             │
│     ↓ GET /files?ownerType=X&ownerId=Y                 │
│     ↓ GET /files/download/{id}                          │
└─────────────────────────────────────────────────────────┘
```

---

## 📝 EXEMPLE PRACTICE

### Exemplu 1: Upload contract pentru tenant

```javascript
// Pas 1: Upload temporar
async function uploadTempFiles(files) {
  const formData = new FormData();
  files.forEach(file => formData.append('files', file));
  
  const res = await fetch('/uploads/temp', {
    method: 'POST',
    body: formData
  });
  
  return await res.json(); // Array de TempUploadDto
}

// Pas 2: Salvează în formular
const tempUploads = await uploadTempFiles([contractFile, facturaFile]);
const tempIds = tempUploads.map(u => u.tempId);

// Pas 3: La submit, commit fișierele
async function submitTenantWithFiles(tenantData, tempIds) {
  // Mai întâi salvează tenant-ul (sau folosește ID existent)
  const tenant = await saveTenant(tenantData);
  
  // Apoi commit fișierele
  const commitRes = await fetch('/files/commit', {
    method: 'POST',
    headers: { 'Content-Type': 'application/json' },
    body: JSON.stringify({
      ownerType: 'TENANT',
      ownerId: tenant.id,
      tempIds: tempIds,
      overwrite: false
    })
  });
  
  return await commitRes.json(); // Array de FileDto
}
```

### Exemplu 2: Listare și afișare fișiere existente

```javascript
async function loadTenantFiles(tenantId) {
  const res = await fetch(`/files?ownerType=TENANT&ownerId=${tenantId}`);
  const files = await res.json();
  
  // Afișare în UI
  files.forEach(file => {
    console.log(`${file.filename} (${file.sizeBytes} bytes)`);
    console.log(`Download: ${file.downloadUrl}`);
  });
  
  return files;
}
```

### Exemplu 3: Component React de upload

```jsx
function FileUploader({ ownerType, ownerId }) {
  const [tempFiles, setTempFiles] = useState([]);
  
  const handleUpload = async (e) => {
    const files = Array.from(e.target.files);
    const formData = new FormData();
    files.forEach(f => formData.append('files', f));
    
    const res = await fetch('/uploads/temp', {
      method: 'POST',
      body: formData
    });
    
    const uploaded = await res.json();
    setTempFiles(prev => [...prev, ...uploaded]);
  };
  
  const handleCommit = async () => {
    const tempIds = tempFiles.map(f => f.tempId);
    
    const res = await fetch('/files/commit', {
      method: 'POST',
      headers: { 'Content-Type': 'application/json' },
      body: JSON.stringify({
        ownerType,
        ownerId,
        tempIds,
        overwrite: false
      })
    });
    
    const committed = await res.json();
    console.log('Committed files:', committed);
    setTempFiles([]);
  };
  
  return (
    <div>
      <input type="file" multiple onChange={handleUpload} />
      <ul>
        {tempFiles.map(f => (
          <li key={f.tempId}>{f.filename} ({f.sizeBytes} bytes)</li>
        ))}
      </ul>
      {tempFiles.length > 0 && (
        <button onClick={handleCommit}>Commit Files</button>
      )}
    </div>
  );
}
```

---

## 🔒 SECURITATE & VALIDĂRI

- ✓ Validare dimensiune fișier (configurabil în `application.properties`)
- ✓ Checksum SHA-256 pentru deduplicare
- ✓ Previne suprascrieri accidentale (prin parametrul `overwrite`)
- ✓ Validare OwnerType (enum strict)
- ⚠️ **Verifică** dacă ai autentificare/autorizare pe aceste endpoint-uri!

---

## 📦 STORAGE

Fișierele sunt stocate în **două locații**:
1. **Database BLOB** - conținutul fișierului în coloana `data` (FileAsset)
2. **Filesystem** - copie pe disk în directoare organizate după owner

Dacă vrei să dezactivezi storage-ul în database (numai filesystem), setează `fa.setData(null)` în `FileCommitService`.

---

## 🐛 DEBUGGING

**Log-uri utile:**
- `logs/app.log` - verifică erorile de upload/commit

**Verificări database:**
```sql
-- Fișiere temporare
SELECT * FROM temp_upload;

-- Fișiere permanente
SELECT * FROM file_asset WHERE owner_type = 'TENANT' AND owner_id = 123;
```

**Verificări filesystem:**
- Temp: directorul temporar configurat
- Permanent: directoare organizate după `ownerType/ownerId/fileId_filename`

---

**Autor:** Generat automat  
**Data:** 2025-10-17  
**Versiune API:** 1.0
