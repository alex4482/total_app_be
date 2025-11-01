# API Response Format

## Overview

**Toate endpoint-urile POST/PATCH/DELETE și GET pentru iteme specifice** returnează un format standardizat `ApiResponse<T>` pentru a permite Frontend-ului să gestioneze în mod consistent success/error și să afișeze mesaje clare utilizatorilor.

**⚠️ NOTA IMPORTANTĂ:** Endpoint-urile de tip **LIST** (ex. `GET /tenants`, `GET /buildings`) returnează **direct arrays** (NU `ApiResponse`) pentru backwards compatibility. Vezi [FE_API_MESSAGES.md](FE_API_MESSAGES.md) pentru detalii complete.

---

## Format Standard

### ApiResponse Structure

```typescript
interface ApiResponse<T> {
  success: boolean;        // true = success, false = error
  message: string;         // Mesaj pentru utilizator (afiseaza-l!)
  data: T | null;          // Datele răspunsului (poate fi null la erori)
}
```

---

## Exemple de Răspunsuri

### 1. Success Response

```json
{
  "success": true,
  "message": "Preset saved successfully",
  "data": {
    "id": 1,
    "name": "Facturi Lunare",
    "subject": "Factura pentru luna {luna}",
    "message": "Bună ziua...",
    "recipients": ["email1@example.com"],
    "keywords": ["factura"]
  }
}
```

### 2. Error Response (de la backend)

```json
{
  "success": false,
  "message": "Preset not found with id: 999",
  "data": null
}
```

### 3. Error Response (de la validare)

```json
{
  "success": false,
  "message": "Body is null when creating new tenant",
  "data": null
}
```

### 4. Partial Success (send emails)

```json
{
  "success": false,
  "message": "Failed to send 2 out of 5 emails",
  "data": [
    {
      "subject": "Factura Luna Ianuarie",
      "message": "...",
      "recipients": ["invalid-email"],
      "errorMessage": "Email invalid: invalid-email"
    },
    {
      "subject": "Reminder",
      "recipients": [],
      "errorMessage": "Nu există destinatari specificați"
    }
  ]
}
```

### 5. Full Success (send emails)

```json
{
  "success": true,
  "message": "All emails sent successfully",
  "data": null
}
```

---

## HTTP Status Codes

| Status Code | Când folosim |
|-------------|-------------|
| `200 OK` | Success (dar check `success: false` în body pentru erori business logic) |
| `400 Bad Request` | Validare failed (ex. body null, email invalid) |
| `404 Not Found` | Resursă inexistentă (ex. preset nu există) |
| `500 Internal Server Error` | Eroare neașteptată |

**⚠️ IMPORTANT:** Verifică **ÎNTÂI** câmpul `success` din response body, NU doar status code!

---

## Workflow pentru Frontend

### Pattern Standard

```typescript
async function callApi(endpoint: string, options: RequestInit) {
  try {
    const response = await fetch(endpoint, options);
    const data: ApiResponse<any> = await response.json();
    
    if (data.success) {
      // SUCCESS - afișează mesaj și folosește data
      console.log('✅', data.message);
      return { success: true, data: data.data, message: data.message };
    } else {
      // ERROR - afișează mesaj de eroare
      console.error('❌', data.message);
      return { success: false, error: data.message, data: data.data };
    }
  } catch (error) {
    // Network error sau JSON parse error
    console.error('Network error:', error);
    return { success: false, error: 'Network error. Please try again.' };
  }
}
```

### Exemple de Utilizare

#### Create Preset

```typescript
const response = await callApi('/email-presets', {
  method: 'POST',
  headers: { 'Content-Type': 'application/json' },
  body: JSON.stringify({
    name: "Facturi Lunare",
    subject: "Factura pentru luna {luna}",
    // ...
  })
});

if (response.success) {
  toast.success(response.message); // "Preset saved successfully"
  const preset = response.data as EmailPreset;
  // Folosește preset
} else {
  toast.error(response.error); // Afișează eroarea din backend
}
```

#### Update Preset

```typescript
const response = await callApi(`/email-presets/${id}`, {
  method: 'PUT',
  headers: { 'Content-Type': 'application/json' },
  body: JSON.stringify(updatedData)
});

if (response.success) {
  toast.success(response.message); // "Preset updated successfully"
} else {
  toast.error(response.error); // ex. "Preset not found with id: 999"
}
```

#### Send Emails

```typescript
const response = await callApi('/email-presets/send-emails', {
  method: 'POST',
  body: JSON.stringify({ data: emailData })
});

if (response.success) {
  toast.success(response.message); // "All emails sent successfully"
} else {
  // Partial success - afișează detaliile
  toast.error(response.error); // "Failed to send 2 out of 5 emails"
  const failedEmails = response.data as EmailData[];
  failedEmails.forEach(email => {
    console.error(`Failed: ${email.errorMessage}`);
  });
}
```

#### Delete Preset

```typescript
const response = await callApi(`/email-presets/${id}`, {
  method: 'DELETE'
});

if (response.success) {
  toast.success(response.message); // "Preset deleted successfully"
  // Reîmprospătează lista
} else {
  toast.error(response.error);
}
```

---

## Mesaje Standard

### Success Messages

| Operație | Mesaj |
|----------|-------|
| Create preset | `"Preset saved successfully"` |
| Update preset | `"Preset updated successfully"` |
| Delete preset | `"Preset deleted successfully"` |
| Save bulk presets | `"Presets saved successfully"` |
| Send all emails | `"All emails sent successfully"` |

### Error Messages

| Cauză | Mesaj | Data |
|-------|-------|------|
| Preset not found | `"Preset not found with id: {id}"` | null |
| Body null | `"Body is null when creating new tenant"` | null |
| Invalid email | `"Email invalid: {email}"` | email cu errorMessage |
| No recipients | `"Nu există destinatari specificați"` | email cu errorMessage |
| Partial send failure | `"Failed to send {n} out of {total} emails"` | lista failed emails |
| Generic error | `"An unexpected error occurred. Please try again."` | null |

---

## Exemple Complete de Răspunsuri

### GET `/email-presets`

**Success:**
```json
{
  "success": true,
  "message": "Operation completed successfully",
  "data": {
    "presets": [
      { "id": 1, "name": "Facturi", ... },
      { "id": 2, "name": "Reminders", ... }
    ]
  }
}
```

### POST `/email-presets`

**Success:**
```json
{
  "success": true,
  "message": "Preset saved successfully",
  "data": {
    "id": 1,
    "name": "Facturi Lunare",
    "subject": "Factura pentru luna {luna}",
    "message": "Bună ziua...",
    "recipients": ["client@example.com"],
    "keywords": ["factura"]
  }
}
```

### PUT `/email-presets/1`

**Success:**
```json
{
  "success": true,
  "message": "Preset updated successfully",
  "data": {
    "id": 1,
    "name": "Facturi Lunare Actualizat",
    ...
  }
}
```

**Error (404):**
```json
{
  "success": false,
  "message": "Preset not found with id: 999",
  "data": null
}
```

### DELETE `/email-presets/1`

**Success:**
```json
{
  "success": true,
  "message": "Preset deleted successfully",
  "data": null
}
```

### POST `/email-presets/send-emails`

**Full Success:**
```json
{
  "success": true,
  "message": "All emails sent successfully",
  "data": null
}
```

**Partial Failure:**
```json
{
  "success": false,
  "message": "Failed to send 2 out of 5 emails",
  "data": [
    {
      "subject": "Factura Luna Ianuarie",
      "message": "Bună ziua...",
      "recipients": ["invalid-email"],
      "errorMessage": "Email invalid: invalid-email"
    },
    {
      "subject": "Reminder",
      "recipients": [],
      "errorMessage": "Nu există destinatari specificați"
    }
  ]
}
```

---

## Recomandări pentru Frontend

1. **Verifică ÎNTÂI `success`** din body, nu doar HTTP status
2. **Afișează ÎNTOTDEAUNA** mesajul utilizatorului (`message` field)
3. **Folosește `data`** doar când `success === true`
4. **Gestionare network errors** (try-catch separat)
5. **Toast/Notifications** pentru feedback vizual
6. **Logging** pentru debugging (console.log cu ✅/❌)

---

## Exemplu Helper Function (TypeScript)

```typescript
interface ApiResponse<T> {
  success: boolean;
  message: string;
  data: T | null;
}

interface ApiResult<T> {
  success: boolean;
  data?: T;
  message: string;
  error?: string;
}

async function apiCall<T>(
  endpoint: string, 
  options: RequestInit = {}
): Promise<ApiResult<T>> {
  try {
    const response = await fetch(endpoint, {
      ...options,
      headers: {
        'Content-Type': 'application/json',
        ...options.headers
      }
    });

    const data: ApiResponse<T> = await response.json();

    if (data.success) {
      return {
        success: true,
        data: data.data as T,
        message: data.message
      };
    } else {
      return {
        success: false,
        error: data.message,
        message: data.message,
        data: data.data
      };
    }
  } catch (error) {
    return {
      success: false,
      error: 'Network error. Please check your connection.',
      message: 'Network error'
    };
  }
}

// Usage:
const result = await apiCall<EmailPreset>('/email-presets/1');
if (result.success) {
  toast.success(result.message);
  const preset = result.data!;
} else {
  toast.error(result.error);
}
```

