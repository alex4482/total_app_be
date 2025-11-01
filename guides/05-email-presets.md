# Email Presets API

Base URL: `/email-presets`

Această API gestionează preset-urile de email (șabloane) și trimiterea de email-uri.

## Endpoints

### 1. List Email Presets
**GET** `/email-presets`

Returnează lista tuturor preset-urilor de email salvate.

#### Response (200 OK)
```json
{
  "presets": [
    {
      "id": 1,
      "name": "Facturi Lunare",
      "subject": "Factura pentru luna {luna}",
      "message": "Bună ziua,\n\nVă trimitem factura pentru luna curentă.",
      "recipients": ["email1@example.com", "email2@example.com"],
      "keywords": ["factura", "plata", "scadenta"]
    }
  ]
}
```

---

### 2. Create/Update Single Preset
**POST** `/email-presets`

Salvează un preset nou sau actualizează unul existent (dacă există deja un preset cu același `name`).

#### Request Body
```json
{
  "name": "Facturi Lunare",
  "subject": "Factura pentru luna {luna}",
  "message": "Bună ziua,\n\nVă trimitem factura pentru luna curentă.",
  "recipients": ["email1@example.com", "email2@example.com"],
  "keywords": ["factura", "plata", "scadenta"]
}
```

**Comportament:**
- Dacă există deja un preset cu același `name` → face **UPDATE**
- Dacă nu există → creează **nou preset**

#### Response (200 OK)
```json
{
  "id": 1,
  "name": "Facturi Lunare",
  "subject": "Factura pentru luna {luna}",
  "message": "Bună ziua,\n\nVă trimitem factura pentru luna curentă.",
  "recipients": ["email1@example.com", "email2@example.com"],
  "keywords": ["factura", "plata", "scadenta"]
}
```

---

### 3. Bulk Save Presets
**POST** `/email-presets/bulk`

Salvează mai multe preset-uri simultan. Preset-urile existente sunt păstrate și actualizate automat.

**Comportament:**
- Dacă presetul trimis are `id` → face **UPDATE** pe preset-ul existent
- Dacă presetul trimis **NU** are `id` dar există unul cu același `name` → face **UPDATE**
- Dacă presetul **NU** există deloc → face **INSERT**

#### Request Body
```json
{
  "presets": [
    {
      "name": "Preset 1",
      "subject": "Subject 1",
      "message": "Message 1",
      "recipients": ["email@example.com"],
      "keywords": ["keyword1"]
    },
    {
      "name": "Preset 2",
      "subject": "Subject 2",
      "message": "Message 2",
      "recipients": ["email@example.com"],
      "keywords": ["keyword2"]
    }
  ]
}
```

#### Response (200 OK)
```json
{
  "presets": [
    {
      "id": 1,
      "name": "Preset 1",
      ...
    },
    {
      "id": 2,
      "name": "Preset 2",
      ...
    }
  ]
}
```

---

### 4. Update Preset
**PUT** `/email-presets/{id}`

Actualizează un preset existent pe baza ID-ului.

#### Path Parameters
- `id` (Integer, **required**) - ID-ul preset-ului de actualizat

#### Request Body
```json
{
  "name": "Facturi Lunare Actualizat",
  "subject": "Factura pentru luna {luna}",
  "message": "Bună ziua,\n\nVă trimitem factura pentru luna curentă.",
  "recipients": ["email1@example.com", "email2@example.com"],
  "keywords": ["factura", "plata", "scadenta"]
}
```

**Notă:** Nu este necesar să incluzi câmpul `id` în body - se folosește cel din URL.

#### Response (200 OK)
```json
{
  "id": 1,
  "name": "Facturi Lunare Actualizat",
  "subject": "Factura pentru luna {luna}",
  "message": "Bună ziua,\n\nVă trimitem factura pentru luna curentă.",
  "recipients": ["email1@example.com", "email2@example.com"],
  "keywords": ["factura", "plata", "scadenta"]
}
```

**Errors:**
- `500 Internal Server Error` - Preset-ul nu există (mesaj: "Preset not found with id: {id}")

---

### 5. Delete Preset
**DELETE** `/email-presets/{id}`

Șterge un preset de email.

#### Path Parameters
- `id` (Integer, **required**) - ID-ul preset-ului

#### Response
**Success (200 OK)** - Preset șters cu succes

**Errors:**
- `404 Not Found` - Preset-ul nu există

---

### 6. Send Emails
**POST** `/email-presets/send-emails`

Trimite email-uri către destinatari. Fișierele atașate trebuie să fie din **temp upload** (ID-uri temporare).

#### Request Body
```json
{
  "data": [
    {
      "subject": "Factura luna Ianuarie",
      "message": "Bună ziua,\n\nVă trimitem factura...",
      "attachedFilesIds": ["uuid-temp-1", "uuid-temp-2"],
      "recipients": ["client1@example.com", "client2@example.com"]
    },
    {
      "subject": "Reminder plată",
      "message": "Vă rugăm să achitați factura...",
      "attachedFilesIds": ["uuid-temp-3"],
      "recipients": ["client3@example.com"]
    }
  ]
}
```

#### Response (200 OK)
Returnează lista email-urilor care **NU** au putut fi trimise (cu motivul erorii).

```json
[
  {
    "subject": "Reminder plată",
    "message": "Vă rugăm să achitați factura...",
    "attachedFilesIds": ["uuid-temp-3"],
    "recipients": ["invalid-email"],
    "errorMessage": "Email invalid: invalid-email"
  }
]
```

**Validări:**
- Recipients trebuie să existe și să nu fie gol
- Toate adresele de email trebuie să fie valide (format)
- Subject-ul este obligatoriu
- Message-ul este obligatoriu

**Erori posibile în response:**
- `"Nu există destinatari specificați"`
- `"Email invalid: {email}"`
- `"Subject-ul este obligatoriu"`
- `"Mesajul este obligatoriu"`
- `"Eroare la trimiterea emailului"`

---

## Model de Date

### EmailPreset
```typescript
interface EmailPreset {
  id?: number;
  name: string;              // Nume unic - folosit pentru update
  subject: string;           // Subiectul emailului
  message: string;           // Conținutul emailului
  recipients: string[];      // Lista de adrese email
  keywords: string[];        // Cuvinte cheie pentru căutare/filtrare
}
```

### EmailData
```typescript
interface EmailData {
  subject: string;
  message: string;
  attachedFilesIds: string[];  // UUID-uri de fișiere din temp upload
  recipients: string[];        // Adrese email
  errorMessage?: string;       // Completat dacă emailul nu a putut fi trimis
}
```

### SendEmailsDto
```typescript
interface SendEmailsDto {
  data: EmailData[];
}
```

---

## Note pentru Frontend

### 1. Workflow pentru trimitere email cu atașamente:

```typescript
// Pasul 1: Upload fișiere în temp
const formData = new FormData();
files.forEach(file => formData.append('files', file));
const tempResponse = await fetch('/files/temp', {
  method: 'POST',
  body: formData
});
const tempFiles = await tempResponse.json();

// Pasul 2: Trimite email cu ID-urile temporare
const emailData = {
  data: [{
    subject: "Factura",
    message: "Vă trimitem factura...",
    attachedFilesIds: tempFiles.map(f => f.tempId),
    recipients: ["client@example.com"]
  }]
};

const sendResponse = await fetch('/email-presets/send-emails', {
  method: 'POST',
  headers: { 'Content-Type': 'application/json' },
  body: JSON.stringify(emailData)
});

const failed = await sendResponse.json(); // Lista email-uri nereușite
```

### 2. Update vs Create Preset:
- Folosește **POST** `/email-presets` cu un `name` specific
- Dacă preset-ul cu acel `name` există → UPDATE automat
- Dacă nu există → CREATE

### 3. Important:
- Fișierele atașate la emailuri trebuie să fie din **temp upload** (nu permanent)
- După trimiterea cu succes, fișierele temporare pot fi șterse
- Response-ul la `send-emails` conține **DOAR** email-urile care au eșuat
- Dacă array-ul returnat este gol → toate email-urile au fost trimise cu succes

### 4. Validare email:
Backend-ul validează formatul adreselor de email. Asigură-te că afișezi mesajele de eroare din `errorMessage`.

