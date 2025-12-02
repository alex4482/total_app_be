# Reminder API Documentation

## Overview
API-ul pentru gestionarea reminder-urilor suportă două tipuri de reminder-uri:
- **STANDARD**: Calculează automat intervalele între email-uri bazate pe warning start date, expiration date și numărul de email-uri
- **CUSTOM**: Permite specificarea momentelor exacte când să se trimită email-urile

## Endpoints

### 1. CREATE Reminder
**POST** `/reminders`

Creează un reminder nou.

**Request Body:**
```json
{
  "emailTitle": "Titlu email",
  "emailMessage": "Mesaj email",
  "expirationDate": "2024-12-31T00:00:00Z",
  "warningStartDate": "2024-12-01T00:00:00Z",
  "warningEmailCount": 3,
  "recipientEmail": "email@example.com",
  "groupings": ["masini", "casa"],
  "reminderType": "STANDARD",  // sau "CUSTOM"
  "scheduledTimes": [  // OBLIGATORIU pentru CUSTOM, ignorat pentru STANDARD
    "2024-12-15T09:00:00Z",
    "2024-12-20T09:00:00Z",
    "2024-12-25T09:00:00Z"
  ]
}
```

**Response:**
```json
{
  "success": true,
  "message": "Reminder created successfully",
  "data": {
    "id": "uuid",
    "emailTitle": "Titlu email",
    "emailMessage": "Mesaj email",
    "expirationDate": "2024-12-31T00:00:00Z",
    "warningStartDate": "2024-12-01T00:00:00Z",
    "warningEmailCount": 3,
    "recipientEmail": "email@example.com",
    "groupings": ["masini", "casa"],
    "emailsSentCount": 0,
    "lastEmailSentAt": null,
    "expiredEmailSent": false,
    "active": true,
    "createdAt": "2024-11-01T10:00:00Z",
    "updatedAt": "2024-11-01T10:00:00Z",
    "reminderType": "STANDARD",
    "scheduledTimes": null  // sau array de timestamps pentru CUSTOM
  }
}
```

**Note:**
- Pentru `reminderType: "STANDARD"`: `scheduledTimes` este ignorat, se folosesc `warningStartDate`, `expirationDate`, `warningEmailCount`
- Pentru `reminderType: "CUSTOM"`: `scheduledTimes` este OBLIGATORIU, `warningStartDate`, `expirationDate`, `warningEmailCount` sunt opționale (dar recomandate pentru tracking)
- Dacă `reminderType` nu este specificat, default este `STANDARD`

---

### 2. GET All Reminders
**GET** `/reminders`

Returnează toate reminder-urile.

**Response:**
```json
{
  "success": true,
  "data": [
    {
      "id": "uuid",
      "emailTitle": "...",
      // ... toate câmpurile
    }
  ]
}
```

---

### 3. GET Reminder by ID
**GET** `/reminders/{id}`

Returnează un reminder specific.

**Path Parameters:**
- `id` (UUID): ID-ul reminder-ului

**Response:**
```json
{
  "success": true,
  "data": {
    "id": "uuid",
    // ... toate câmpurile
  }
}
```

---

### 4. GET Reminders by Grouping
**GET** `/reminders/grouping/{grouping}`

Returnează reminder-urile dintr-un anumit grouping.

**Path Parameters:**
- `grouping` (String): Numele grouping-ului (ex: "masini", "casa")

**Response:**
```json
{
  "success": true,
  "data": [
    {
      "id": "uuid",
      // ... toate câmpurile
    }
  ]
}
```

---

### 5. UPDATE Reminder
**PUT** `/reminders/{id}`

Actualizează detaliile unui reminder. Toate câmpurile sunt opționale - doar câmpurile trimise vor fi actualizate.

**Path Parameters:**
- `id` (UUID): ID-ul reminder-ului

**Request Body:**
```json
{
  "emailTitle": "Titlu nou",  // opțional
  "emailMessage": "Mesaj nou",  // opțional
  "expirationDate": "2024-12-31T00:00:00Z",  // opțional
  "warningStartDate": "2024-12-01T00:00:00Z",  // opțional
  "warningEmailCount": 5,  // opțional
  "recipientEmail": "nou@example.com",  // opțional
  "groupings": ["masini"],  // opțional
  "reminderType": "CUSTOM",  // opțional
  "scheduledTimes": [  // opțional, dar OBLIGATORIU dacă reminderType este CUSTOM
    "2024-12-15T09:00:00Z",
    "2024-12-20T09:00:00Z"
  ]
}
```

**Response:**
```json
{
  "success": true,
  "message": "Reminder updated successfully",
  "data": {
    "id": "uuid",
    // ... toate câmpurile actualizate
  }
}
```

**Note:**
- Dacă actualizezi `reminderType` la `CUSTOM`, trebuie să furnizezi `scheduledTimes`
- Dacă actualizezi `scheduledTimes` pentru un reminder CUSTOM, vechile schedule-uri vor fi șterse și înlocuite cu cele noi

---

### 6. DELETE Reminder
**DELETE** `/reminders/{id}`

Șterge un reminder.

**Path Parameters:**
- `id` (UUID): ID-ul reminder-ului

**Response:**
```json
{
  "success": true,
  "message": "Reminder deleted successfully",
  "data": null
}
```

---

### 7. SET Reminder Active/Inactive
**PUT** `/reminders/{id}/active?active=true`

Activează sau dezactivează un reminder.

**Path Parameters:**
- `id` (UUID): ID-ul reminder-ului

**Query Parameters:**
- `active` (boolean): `true` pentru activ, `false` pentru inactiv

**Response:**
```json
{
  "success": true,
  "message": "Reminder activated successfully",  // sau "Reminder stopped successfully"
  "data": {
    "id": "uuid",
    "active": true,
    // ... toate câmpurile
  }
}
```

---

## Tipuri de Reminder

### STANDARD Reminder
- Calculează automat intervalele între email-uri
- Distribuie email-urile uniform între `warningStartDate` și `expirationDate`
- După expirare, continuă să trimită email-uri la același interval până când este oprit manual
- **Câmpuri necesare:**
  - `warningStartDate`
  - `expirationDate`
  - `warningEmailCount`
  - `recipientEmail`
  - `emailTitle`
  - `emailMessage`

### CUSTOM Reminder
- Permite specificarea momentelor exacte când să se trimită email-urile
- Email-urile se trimit doar la momentele specificate în `scheduledTimes`
- Nu trimite email-uri după ce toate schedule-urile au fost procesate
- **Câmpuri necesare:**
  - `reminderType: "CUSTOM"`
  - `scheduledTimes` (array de timestamps ISO 8601)
  - `recipientEmail`
  - `emailTitle`
  - `emailMessage`

---

## Exemple de Utilizare

### Exemplu 1: Creare STANDARD Reminder
```json
POST /reminders
{
  "emailTitle": "Expirare asigurare",
  "emailMessage": "Asigurarea expiră în curând!",
  "expirationDate": "2024-12-31T00:00:00Z",
  "warningStartDate": "2024-12-01T00:00:00Z",
  "warningEmailCount": 3,
  "recipientEmail": "user@example.com",
  "groupings": ["masini"]
}
```

### Exemplu 2: Creare CUSTOM Reminder
```json
POST /reminders
{
  "emailTitle": "Plată factură",
  "emailMessage": "Nu uita să plătești factura!",
  "expirationDate": "2024-12-31T00:00:00Z",
  "warningStartDate": "2024-12-01T00:00:00Z",
  "warningEmailCount": 0,
  "recipientEmail": "user@example.com",
  "groupings": ["casa"],
  "reminderType": "CUSTOM",
  "scheduledTimes": [
    "2024-12-05T09:00:00Z",
    "2024-12-10T09:00:00Z",
    "2024-12-15T09:00:00Z",
    "2024-12-20T09:00:00Z"
  ]
}
```

### Exemplu 3: Update Parțial
```json
PUT /reminders/{id}
{
  "emailTitle": "Titlu actualizat",
  "recipientEmail": "nou@example.com"
}
```

### Exemplu 4: Schimbare tip de STANDARD la CUSTOM
```json
PUT /reminders/{id}
{
  "reminderType": "CUSTOM",
  "scheduledTimes": [
    "2024-12-10T09:00:00Z",
    "2024-12-20T09:00:00Z"
  ]
}
```

---

## Note Importante

1. **Format Timestamp**: Toate timestamp-urile trebuie să fie în format ISO 8601 (ex: `2024-12-31T00:00:00Z`)

2. **Job Scheduling**: Job-ul de procesare rulează zilnic la ora 9:00 AM și verifică:
   - Reminder-uri STANDARD care au nevoie de email-uri de avertizare
   - Reminder-uri STANDARD expirate care trebuie să continue să trimită email-uri
   - Reminder-uri CUSTOM cu schedule-uri care au ajuns la timp

3. **Email Prefix**: Toate email-urile trimise vor avea un prefix configurat (ex: `[DONIX]`) și un sufix de mesaj

4. **Groupings**: Reminder-urile pot aparține la mai multe grouping-uri pentru organizare (ex: "masini", "casa", "apartament")

5. **Active Status**: Doar reminder-urile active (`active: true`) vor fi procesate de job-ul de trimitere email-uri

---

## Status Codes

- `200 OK`: Succes
- `400 Bad Request`: Date invalide
- `404 Not Found`: Reminder nu a fost găsit
- `500 Internal Server Error`: Eroare server

---

## Schema Database

### Tabel: `reminder`
- `id` (UUID, PK)
- `email_title` (VARCHAR 500)
- `email_message` (TEXT)
- `expiration_date` (TIMESTAMP)
- `warning_start_date` (TIMESTAMP)
- `warning_email_count` (INTEGER)
- `recipient_email` (VARCHAR 255)
- `emails_sent_count` (INTEGER, default 0)
- `last_email_sent_at` (TIMESTAMP, nullable)
- `expired_email_sent` (BOOLEAN, default false)
- `active` (BOOLEAN, default true)
- `created_at` (TIMESTAMP)
- `updated_at` (TIMESTAMP)
- `reminder_type` (VARCHAR 20, default 'STANDARD')

### Tabel: `reminder_schedule` (pentru CUSTOM reminders)
- `id` (UUID, PK)
- `reminder_id` (UUID, FK -> reminder.id)
- `scheduled_time` (TIMESTAMP)
- `sent` (BOOLEAN, default false)
- `sent_at` (TIMESTAMP, nullable)
- `created_at` (TIMESTAMP)

### Tabel: `reminder_grouping` (many-to-many)
- `reminder_id` (UUID, FK -> reminder.id)
- `grouping_name` (VARCHAR 255)

