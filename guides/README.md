# Backend API - Ghid Complet de Integrare Frontend

Acest document oferă o vedere de ansamblu asupra tuturor API-urilor disponibile în aplicația backend și cum să le folosești din frontend.

## 📋 Cuprins

### 🌐 API Documentation
1. [Authentication API](./01-authentication.md) - Autentificare și gestionare tokene JWT
2. [Tenants API](./02-tenants.md) - Gestionare chiriași (CRUD, import Excel, bulk operations)
3. [Buildings API](./03-buildings.md) - Gestionare clădiri și spații de închiriat
4. [Files API](./04-files.md) - Upload, download, și gestionare fișiere
5. [Email Presets API](./05-email-presets.md) - Șabloane email și trimitere email-uri
6. [Index Counters API](./06-index-counters.md) - Gestionare contoare și citiri

### 🛠️ Development & Deployment
7. [Database Migrations](./07-database-migrations.md) - Gestionare schema bază de date cu Flyway

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

### Workflow autentificare:

```typescript
// 1. Login
const loginResponse = await fetch('/auth/login', {
  method: 'POST',
  headers: { 'Content-Type': 'application/json' },
  body: JSON.stringify({ username: 'user', password: 'pass' })
});
const { tokens } = await loginResponse.json();

// 2. Salvează token-urile
localStorage.setItem('accessToken', tokens.accessToken);
localStorage.setItem('refreshToken', tokens.refreshToken);

// 3. Folosește access token în toate request-urile
const response = await fetch('/tenants', {
  headers: { 
    'Authorization': `Bearer ${localStorage.getItem('accessToken')}`
  }
});

// 4. Dacă primești 401, refresh token-ul
if (response.status === 401) {
  const refreshResponse = await fetch('/auth/refresh-token', {
    method: 'POST',
    headers: { 'Content-Type': 'application/json' },
    body: JSON.stringify({ 
      refreshToken: localStorage.getItem('refreshToken') 
    })
  });
  const { tokens: newTokens } = await refreshResponse.json();
  localStorage.setItem('accessToken', newTokens.accessToken);
  localStorage.setItem('refreshToken', newTokens.refreshToken);
  // Retry request-ul original
}
```

---

## 📁 Workflow-uri Principale

### 1. Gestionare Chiriași

#### Creare chiriași simplu:
```typescript
const tenant = await fetch('/tenants', {
  method: 'POST',
  headers: {
    'Content-Type': 'application/json',
    'Authorization': `Bearer ${token}`
  },
  body: JSON.stringify({
    name: "Tenant SRL",
    cui: "RO12345678",
    regNumber: "J40/1234/2023",
    active: true,
    emails: ["contact@tenant.com"],
    phoneNumbers: ["0712345678"]
  })
});
```

#### Import chiriași din Excel:
```typescript
const formData = new FormData();
formData.append('file', excelFile);

const result = await fetch('/tenants/import', {
  method: 'POST',
  headers: { 'Authorization': `Bearer ${token}` },
  body: formData
});

const { savedCount, skippedCount, skippedNames } = await result.json();
console.log(`Salvați: ${savedCount}, Omisi: ${skippedCount}`);
```

#### Ștergere multiplă:
```typescript
const result = await fetch('/tenants/bulk-delete', {
  method: 'DELETE',
  headers: {
    'Content-Type': 'application/json',
    'Authorization': `Bearer ${token}`
  },
  body: JSON.stringify([1, 2, 3, 4, 5]) // IDs
});

const { deletedCount, failedTenants } = await result.json();
```

---

### 2. Gestionare Fișiere

#### Upload fișiere (2 pași):

```typescript
// PASUL 1: Upload temporar
const formData = new FormData();
files.forEach(file => formData.append('files', file));

const tempResponse = await fetch('/files/temp', {
  method: 'POST',
  headers: { 'Authorization': `Bearer ${token}` },
  body: formData
});
const tempFiles = await tempResponse.json();
// tempFiles = [{ tempId: "uuid-1", filename: "doc.pdf", ... }]

// PASUL 2: Commit la owner (ex: tenant cu id=5)
const tempIds = tempFiles.map(f => f.tempId).join('&tempIds=');
const commitResponse = await fetch(
  `/files/commit?ownerType=TENANT&ownerId=5&tempIds=${tempIds}`,
  {
    method: 'POST',
    headers: { 'Authorization': `Bearer ${token}` }
  }
);
const permanentFiles = await commitResponse.json();
```

#### Download fișier:
```typescript
window.open(`/files/${fileId}`, '_blank');
```

#### Download multiple fișiere ca ZIP:
```typescript
const fileIds = ['uuid1', 'uuid2', 'uuid3'];
const url = `/files/download-zip?${fileIds.map(id => `fileIds=${id}`).join('&')}`;
window.open(url, '_blank');
```

#### Ștergere fișier:
```typescript
await fetch(`/files/${fileId}`, {
  method: 'DELETE',
  headers: { 'Authorization': `Bearer ${token}` }
});
```

---

### 3. Trimitere Email-uri

#### Cu atașamente din temp upload:

```typescript
// 1. Upload fișiere în temp
const formData = new FormData();
attachments.forEach(file => formData.append('files', file));

const tempResponse = await fetch('/files/temp', {
  method: 'POST',
  headers: { 'Authorization': `Bearer ${token}` },
  body: formData
});
const tempFiles = await tempResponse.json();

// 2. Trimite emailul
const emailData = {
  data: [{
    subject: "Factura Luna Ianuarie",
    message: "Bună ziua,\n\nVă trimitem factura...",
    attachedFilesIds: tempFiles.map(f => f.tempId),
    recipients: ["client@example.com", "alt@example.com"]
  }]
};

const sendResponse = await fetch('/email-presets/send-emails', {
  method: 'POST',
  headers: {
    'Content-Type': 'application/json',
    'Authorization': `Bearer ${token}`
  },
  body: JSON.stringify(emailData)
});

const failedEmails = await sendResponse.json();
if (failedEmails.length === 0) {
  console.log('Toate email-urile au fost trimise cu succes!');
} else {
  failedEmails.forEach(email => {
    console.error(`Failed: ${email.errorMessage}`);
  });
}
```

---

## 🔧 Tipuri și Enumerări Comune

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

### BuildingLocation
```typescript
type BuildingLocation = 
  | "LETCANI"
  | "TOMESTI";
```

### CounterType
```typescript
type CounterType = 
  | "WATER"
  | "GAS"
  | "ELECTRICITY";
```

---

## ⚠️ Lucruri Importante de Știut

### 1. Fișiere
- **Temp upload → Commit workflow:** Fișierele TREBUIE mai întâi uploadate în `/files/temp`, apoi committed la un owner
- **Auto-delete:** Când ștergi un tenant/building, fișierele sale sunt șterse AUTOMAT
- **Temp files:** Fișierele temporare NU sunt șterse automat - gestionează-le manual
- **ZIP download:** Este GET (nu POST), poți deschide direct în browser cu `window.open()`

### 2. Tenants
- **Active default:** Când creezi un tenant nou fără să specifici `active`, va fi `false` by default
- **Import Excel:** Dacă există deja un tenant cu același nume → UPDATE, altfel CREATE
- **Bulk delete:** Primești înapoi lista tenants care NU au putut fi șterși (cu motivul)

### 3. Email
- **Atașamente:** Folosește ID-urile TEMPORARE (din `/files/temp`), nu cele permanente
- **Validare:** Backend-ul validează format email, subject obligatoriu, message obligatoriu
- **Response:** Primești înapoi DOAR email-urile care au EȘUAT

### 4. Email Presets
- **Update by name:** Când salvezi un preset cu un `name` care există deja → face UPDATE automat
- **Bulk save:** `POST /email-presets/bulk` ȘTERGE toate preset-urile existente!

---

## 🚨 Gestionare Erori

### Status Codes Comune:
- `200 OK` - Success
- `201 Created` - Resursa a fost creată
- `204 No Content` - Success, fără conținut de returnat
- `400 Bad Request` - Date invalide în request
- `401 Unauthorized` - Token lipsă sau invalid (refresh token!)
- `403 Forbidden` - Acces interzis
- `404 Not Found` - Resursa nu există
- `409 Conflict` - Conflict (ex: fișier duplicat, tenant cu dependințe)
- `500 Internal Server Error` - Eroare server

### Pattern de Gestionare:

```typescript
async function apiCall(url: string, options: RequestInit) {
  const response = await fetch(url, {
    ...options,
    headers: {
      ...options.headers,
      'Authorization': `Bearer ${getAccessToken()}`
    }
  });

  if (response.status === 401) {
    // Token expirat, încearcă refresh
    await refreshAccessToken();
    // Retry request
    return apiCall(url, options);
  }

  if (!response.ok) {
    const errorText = await response.text();
    throw new Error(`API Error ${response.status}: ${errorText}`);
  }

  // 204 No Content nu are body
  if (response.status === 204) {
    return null;
  }

  return response.json();
}
```

---

## 📝 Exemple Complete

### Exemplu: Creare tenant cu fișiere

```typescript
async function createTenantWithFiles(
  tenantData: CreateTenantDto,
  files: File[]
) {
  // 1. Creează tenant
  const tenant = await fetch('/tenants', {
    method: 'POST',
    headers: {
      'Content-Type': 'application/json',
      'Authorization': `Bearer ${token}`
    },
    body: JSON.stringify(tenantData)
  }).then(r => r.json());

  // 2. Upload fișiere temporar
  const formData = new FormData();
  files.forEach(file => formData.append('files', file));
  
  const tempFiles = await fetch('/files/temp', {
    method: 'POST',
    headers: { 'Authorization': `Bearer ${token}` },
    body: formData
  }).then(r => r.json());

  // 3. Commit fișiere la tenant
  const tempIds = tempFiles.map(f => f.tempId).join('&tempIds=');
  const permanentFiles = await fetch(
    `/files/commit?ownerType=TENANT&ownerId=${tenant.id}&tempIds=${tempIds}`,
    {
      method: 'POST',
      headers: { 'Authorization': `Bearer ${token}` }
    }
  ).then(r => r.json());

  return { tenant, files: permanentFiles };
}
```

### Exemplu: Trimitere email cu preset

```typescript
async function sendEmailFromPreset(
  presetId: number,
  recipients: string[],
  attachments: File[]
) {
  // 1. Obține preset
  const { presets } = await fetch('/email-presets', {
    headers: { 'Authorization': `Bearer ${token}` }
  }).then(r => r.json());
  
  const preset = presets.find(p => p.id === presetId);

  // 2. Upload atașamente în temp
  const formData = new FormData();
  attachments.forEach(file => formData.append('files', file));
  
  const tempFiles = await fetch('/files/temp', {
    method: 'POST',
    headers: { 'Authorization': `Bearer ${token}` },
    body: formData
  }).then(r => r.json());

  // 3. Trimite email
  const failedEmails = await fetch('/email-presets/send-emails', {
    method: 'POST',
    headers: {
      'Content-Type': 'application/json',
      'Authorization': `Bearer ${token}`
    },
    body: JSON.stringify({
      data: [{
        subject: preset.subject,
        message: preset.message,
        attachedFilesIds: tempFiles.map(f => f.tempId),
        recipients: recipients
      }]
    })
  }).then(r => r.json());

  return {
    success: failedEmails.length === 0,
    failed: failedEmails
  };
}
```

---

## 📚 Resurse Adiționale

Pentru detalii complete despre fiecare endpoint, consultă fișierele individuale:

### API Documentation:
- **[01-authentication.md](./01-authentication.md)** - Login, refresh token
- **[02-tenants.md](./02-tenants.md)** - CRUD tenants, import Excel, bulk delete
- **[03-buildings.md](./03-buildings.md)** - Buildings, rental spaces
- **[04-files.md](./04-files.md)** - Upload, download, ZIP, delete files
- **[05-email-presets.md](./05-email-presets.md)** - Email templates și trimitere
- **[06-index-counters.md](./06-index-counters.md)** - Contoare și citiri

### Development & Deployment:
- **[07-database-migrations.md](./07-database-migrations.md)** - Gestionare schema DB, Flyway workflow, securitate

---

## 🔄 Versioning

**Current API Version:** v1  
**Last Updated:** Octombrie 2024

Pentru probleme sau întrebări despre API, contactează echipa de backend.

