# 🔔 Reminder System API

Sistem pentru gestionarea reminderelor care trimit emailuri automate la intervale configurate.

## 📋 Overview

Sistemul de remindere permite:
- Crearea de remindere cu titlu, mesaj, date de expirare și configurare pentru emailuri de avertizare
- Trimiterea automată de emailuri de avertizare înainte de expirare
- Continuarea trimiterii de emailuri după expirare (la același interval) până la oprirea manuală
- Gruparea reminderele după categorii (ex: "masini", "casa", "apartament", "muncaX", "muncaY")
- Control manual pentru oprirea/activarea reminderele expirate

**Note importante:**
- Prefixul emailului și sufixul mesajului sunt configurabile global în `application.properties` (nu per reminder)
- Emailurile se trimit automat o dată pe zi (la 9:00 AM)
- După expirare, reminderele continuă să trimită emailuri la același interval până când sunt oprite manual

---

## 🔌 Endpoints

### Base Path
```
/reminders
```

---

### 1. Create Reminder

**POST** `/reminders`

Creează un reminder nou.

#### Request Body
```json
{
  "emailTitle": "Reînnoire asigurare mașină",
  "emailMessage": "Nu uita să reînnoiești asigurarea pentru mașina X.",
  "expirationDate": "2025-06-15T00:00:00Z",
  "warningStartDate": "2025-05-15T00:00:00Z",
  "warningEmailCount": 5,
  "recipientEmail": "user@example.com",
  "groupings": ["masini", "asigurari"]
}
```

#### Fields
- `emailTitle` (string, **required**) - Titlul emailului (va avea prefix adăugat automat)
- `emailMessage` (string, **required**) - Mesajul emailului (va avea sufix adăugat automat)
- `expirationDate` (ISO 8601 datetime, **required**) - Data când reminderul expiră
- `warningStartDate` (ISO 8601 datetime, **required**) - Data când începe trimiterea emailurilor de avertizare
- `warningEmailCount` (integer, **required**) - Numărul de emailuri de avertizare de trimis în perioada de avertizare
- `recipientEmail` (string, **required**) - Adresa email destinatar
- `groupings` (array of strings, optional) - Grupuri din care face parte reminderul (ex: ["masini", "casa"])

#### Response (200 OK)
```json
{
  "success": true,
  "message": "Reminder created successfully",
  "data": {
    "id": "550e8400-e29b-41d4-a716-446655440000",
    "emailTitle": "Reînnoire asigurare mașină",
    "emailMessage": "Nu uita să reînnoiești asigurarea pentru mașina X.",
    "expirationDate": "2025-06-15T00:00:00Z",
    "warningStartDate": "2025-05-15T00:00:00Z",
    "warningEmailCount": 5,
    "recipientEmail": "user@example.com",
    "groupings": ["masini", "asigurari"],
    "emailsSentCount": 0,
    "lastEmailSentAt": null,
    "expiredEmailSent": false,
    "active": true,
    "createdAt": "2025-01-15T10:30:00Z",
    "updatedAt": "2025-01-15T10:30:00Z"
  }
}
```

**Errors:**
- `400 Bad Request` - Date invalide sau lipsă

---

### 2. Get All Reminders

**GET** `/reminders`

Returnează toate reminderele.

#### Response (200 OK)
```json
{
  "success": true,
  "message": null,
  "data": [
    {
      "id": "550e8400-e29b-41d4-a716-446655440000",
      "emailTitle": "Reînnoire asigurare mașină",
      "emailMessage": "Nu uita să reînnoiești asigurarea...",
      "expirationDate": "2025-06-15T00:00:00Z",
      "warningStartDate": "2025-05-15T00:00:00Z",
      "warningEmailCount": 5,
      "recipientEmail": "user@example.com",
      "groupings": ["masini", "asigurari"],
      "emailsSentCount": 2,
      "lastEmailSentAt": "2025-05-20T09:00:00Z",
      "expiredEmailSent": false,
      "active": true,
      "createdAt": "2025-01-15T10:30:00Z",
      "updatedAt": "2025-01-15T10:30:00Z"
    }
  ]
}
```

---

### 3. Get Reminder by ID

**GET** `/reminders/{id}`

Returnează un reminder specific.

#### Path Parameters
- `id` (UUID, **required**) - ID-ul reminderului

#### Response (200 OK)
```json
{
  "success": true,
  "message": null,
  "data": {
    "id": "550e8400-e29b-41d4-a716-446655440000",
    "emailTitle": "Reînnoire asigurare mașină",
    "emailMessage": "Nu uita să reînnoiești asigurarea...",
    "expirationDate": "2025-06-15T00:00:00Z",
    "warningStartDate": "2025-05-15T00:00:00Z",
    "warningEmailCount": 5,
    "recipientEmail": "user@example.com",
    "groupings": ["masini", "asigurari"],
    "emailsSentCount": 2,
    "lastEmailSentAt": "2025-05-20T09:00:00Z",
    "expiredEmailSent": false,
    "active": true,
    "createdAt": "2025-01-15T10:30:00Z",
    "updatedAt": "2025-01-15T10:30:00Z"
  }
}
```

**Errors:**
- `404 Not Found` - Reminderul nu există

---

### 4. Get Reminders by Grouping

**GET** `/reminders/grouping/{grouping}`

Returnează toate reminderele dintr-un anumit grup.

#### Path Parameters
- `grouping` (string, **required**) - Numele grupului (ex: "masini", "casa")

#### Response (200 OK)
```json
{
  "success": true,
  "message": null,
  "data": [
    {
      "id": "550e8400-e29b-41d4-a716-446655440000",
      "emailTitle": "Reînnoire asigurare mașină",
      "groupings": ["masini", "asigurari"],
      ...
    }
  ]
}
```

---

### 5. Delete Reminder

**DELETE** `/reminders/{id}`

Șterge un reminder.

#### Path Parameters
- `id` (UUID, **required**) - ID-ul reminderului

#### Response (200 OK)
```json
{
  "success": true,
  "message": "Reminder deleted successfully",
  "data": null
}
```

**Errors:**
- `404 Not Found` - Reminderul nu există

---

### 6. Stop/Activate Reminder

**PUT** `/reminders/{id}/active?active={true|false}`

Oprește sau activează un reminder (util pentru remindere expirate).

#### Path Parameters
- `id` (UUID, **required**) - ID-ul reminderului

#### Query Parameters
- `active` (boolean, **required**) - `true` pentru activare, `false` pentru oprire

#### Examples
```
PUT /reminders/550e8400-e29b-41d4-a716-446655440000/active?active=false
PUT /reminders/550e8400-e29b-41d4-a716-446655440000/active?active=true
```

#### Response (200 OK)
```json
{
  "success": true,
  "message": "Reminder stopped successfully",
  "data": {
    "id": "550e8400-e29b-41d4-a716-446655440000",
    "active": false,
    ...
  }
}
```

**Errors:**
- `404 Not Found` - Reminderul nu există

---

## 📊 Model de Date

### CreateReminderDto
```typescript
interface CreateReminderDto {
  emailTitle: string;           // Titlul emailului (required)
  emailMessage: string;         // Mesajul emailului (required)
  expirationDate: string;       // ISO 8601 datetime (required)
  warningStartDate: string;     // ISO 8601 datetime (required)
  warningEmailCount: number;   // Număr de emailuri de avertizare (required)
  recipientEmail: string;       // Email destinatar (required)
  groupings?: string[];         // Listă de grupuri (optional)
}
```

### ReminderDto
```typescript
interface ReminderDto {
  id: string;                   // UUID
  emailTitle: string;
  emailMessage: string;
  expirationDate: string;       // ISO 8601 datetime
  warningStartDate: string;     // ISO 8601 datetime
  warningEmailCount: number;
  recipientEmail: string;
  groupings: string[];          // Poate fi array gol []
  emailsSentCount: number;      // Numărul de emailuri trimise până acum
  lastEmailSentAt: string | null; // ISO 8601 datetime sau null
  expiredEmailSent: boolean;    // true dacă a fost trimis email de expirare
  active: boolean;              // true dacă reminderul este activ
  createdAt: string;            // ISO 8601 datetime
  updatedAt: string;            // ISO 8601 datetime
}
```

### ApiResponse<T>
```typescript
interface ApiResponse<T> {
  success: boolean;
  message: string | null;
  data: T | null;
}
```

---

## 🔄 Comportament Sistem

### Emailuri de Avertizare (înainte de expirare)

1. Emailurile încep să fie trimise de la `warningStartDate`
2. Emailurile sunt distribuite uniform în perioada dintre `warningStartDate` și `expirationDate`
3. Numărul total de emailuri trimise este `warningEmailCount`
4. Intervalul dintre emailuri = `(expirationDate - warningStartDate) / (warningEmailCount + 1)`

**Exemplu:**
- `warningStartDate`: 2025-05-15
- `expirationDate`: 2025-06-15 (30 zile)
- `warningEmailCount`: 5
- Interval: ~5 zile
- Emailuri trimise: la ~5, ~10, ~15, ~20, ~25 zile după `warningStartDate`

### Emailuri După Expirare

1. După `expirationDate`, reminderele continuă să trimită emailuri
2. Folosesc **același interval** calculat din perioada de avertizare
3. Continuă la nesfârșit până când `active = false`
4. Mesajul include informații despre câte zile au trecut de la expirare

### Prefix și Sufix Email

Prefixul și sufixul sunt configurate global în `application.properties`:
```properties
app.reminder.email-prefix=[Reminder] 
app.reminder.extra-message-suffix=\n\n---\nAcest reminder a fost generat automat de sistem.
```

**Emailul final:**
- **Subject**: `[Reminder] {emailTitle}`
- **Body**: `{emailMessage}\n\n---\nAcest reminder a fost generat automat de sistem.\n\n⚠️ ATENȚIE: Acest reminder expiră în X zile...`

### Status Reminders

- **Active + înainte de expirare**: Trimite emailuri de avertizare
- **Active + după expirare**: Continuă să trimită emailuri la același interval
- **Inactive**: Nu trimite emailuri (oprit manual)

### Job Scheduler

- Rulează **o dată pe zi la 9:00 AM**
- Verifică toate reminderele active
- Trimite emailuri conform intervalelor calculate

---

## 🎯 Exemple de Utilizare

### Creare Reminder Simplu

```typescript
const createReminder = async () => {
  const reminder = {
    emailTitle: "Plată factură electricitate",
    emailMessage: "Nu uita să plătești factura de electricitate.",
    expirationDate: "2025-02-15T00:00:00Z",
    warningStartDate: "2025-02-01T00:00:00Z",
    warningEmailCount: 3,
    recipientEmail: "user@example.com",
    groupings: ["casa", "facturi"]
  };
  
  const response = await fetch('/reminders', {
    method: 'POST',
    headers: { 'Content-Type': 'application/json' },
    body: JSON.stringify(reminder)
  });
};
```

### Oprire Reminder Expirat

```typescript
const stopExpiredReminder = async (reminderId: string) => {
  const response = await fetch(`/reminders/${reminderId}/active?active=false`, {
    method: 'PUT'
  });
};
```

### Listare Reminders După Grup

```typescript
const getMasiniReminders = async () => {
  const response = await fetch('/reminders/grouping/masini');
  const { data } = await response.json();
  return data;
};
```

---

## ⚠️ Validări și Constricții

1. **Date**: 
   - `warningStartDate` trebuie să fie înainte de `expirationDate`
   - `warningEmailCount` trebuie să fie > 0

2. **Email**: 
   - `recipientEmail` trebuie să fie un email valid

3. **Groupings**: 
   - Pot fi stringuri libere (ex: "masini", "casa", "muncaX")
   - Nu sunt validate în BE, doar stocate ca stringuri

---

## 📝 Note pentru Frontend

1. **Status Vizual**: 
   - Afișează statusul reminderului (active/inactive, expirat/înainte de expirare)
   - Poți calcula dacă e expirat: `expirationDate < now`
   - Poți afișa câte emailuri au fost trimise: `emailsSentCount` / `warningEmailCount`

2. **Filtrare**: 
   - Poți filtra după groupings folosind endpoint-ul `/reminders/grouping/{grouping}`
   - Sau filtrezi local după `groupings` array

3. **Stop Reminder**: 
   - Pentru remindere expirate, afișează buton "Oprește" care setează `active = false`
   - Butonul "Activează" pentru remindere inactive

4. **Formular Creare**: 
   - Validare că `warningStartDate < expirationDate`
   - Validare email format
   - Input pentru groupings (poate fi multi-select sau tags)

5. **Vizualizare**: 
   - Afișează progresul: `emailsSentCount` / `warningEmailCount`
   - Afișează data ultimului email: `lastEmailSentAt`
   - Indică dacă e expirat și câte zile au trecut

