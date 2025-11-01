# Frontend API Messages Reference

## Rezumat Executiv

**Ce să aștepți de la API:**

**TOATE endpoint-urile returnează `ApiResponse<T>` pentru success și error!**

- ✅ **Excepții → ApiResponse automat** prin `GlobalControllerExceptionHandler`
- ✅ **Success responses → ApiResponse** cu mesaje clare
- ✅ **Error responses → ApiResponse** cu mesaje detaliate
- ⚠️ **EXCEPȚIE: Liste GET** → returnează direct arrays (pentru backwards compatibility)

---

## Endpoints Convertite Complet la ApiResponse

### ✅ Email Presets API (`/email-presets`)

**TOATE** endpoint-urile returnează `ApiResponse<T>`:

| Endpoint | Method | Success Message | Error Messages |
|----------|--------|----------------|----------------|
| `GET /email-presets` | GET | `"Operation completed successfully"` | - |
| `POST /email-presets` | POST | `"Preset saved successfully"` | - |
| `PUT /email-presets/{id}` | PUT | `"Preset updated successfully"` | `"Preset not found with id: {id}"` |
| `POST /email-presets/bulk` | POST | `"Presets saved successfully"` | `"Failed to save all presets"` |
| `DELETE /email-presets/{id}` | DELETE | `"Preset deleted successfully"` | `"Preset not found with id: {id}"` |
| `POST /email-presets/send-emails` | POST | `"All emails sent successfully"` | `"Failed to send {n} out of {total} emails"` |

**Validări pentru Send Emails:**
- `"Email invalid: {email}"` - pentru emailuri invalide
- `"Nu există destinatari specificați"` - pentru destinatari lipsă
- `"Subject-ul este obligatoriu"` - pentru subject gol
- `"Mesajul este obligatoriu"` - pentru message gol
- `"Eroare la trimiterea emailului"` - pentru erori SMTP/IMAP

---

### ✅ Tenants API (`/tenants`)

**TOATE** endpoint-urile returnează `ApiResponse<T>`:

| Endpoint | Method | Success Message | Error Messages |
|----------|--------|----------------|----------------|
| `GET /tenants/{id}` | GET | `"Operation completed successfully"` | `"Invalid tenant ID"`, `"Tenant not found with id: {id}"` |
| `POST /tenants` | POST | `"Tenant created successfully"` | `"Failed to create tenant"` |
| `PATCH /tenants/{id}` | PATCH | `"Tenant updated successfully"` | `"Failed to update tenant"` |
| `DELETE /tenants/{id}` | DELETE | `"Tenant deleted successfully"` | `"Invalid tenant ID"`, `"Tenant not found"`, `"Cannot delete tenant: it is still referenced by other records"` |
| `POST /tenants/new-rental-agreement` | POST | `"Rental agreement created successfully"` | `"Failed to create rental agreement"` |
| `POST /tenants/import` | POST | `"Import completed successfully"` | `"No file provided or file is empty"`, `"Invalid file type. Only .xlsx and .xls files are allowed"`, `"Failed to import tenants: {error}"` |
| `DELETE /tenants/bulk-delete` | DELETE | `"Bulk delete completed"` | `"No tenant IDs provided"`, `"Failed to delete tenants: {error}"` |

**⚠️ NOTĂ:** `GET /tenants` (list all) returnează **DIRECT** array `List<Tenant>` (NU ApiResponse)

---

### ✅ Buildings API (`/buildings`)

**TOATE** endpoint-urile returnează `ApiResponse<T>`:

| Endpoint | Method | Success Message | Error Messages |
|----------|--------|----------------|----------------|
| `GET /buildings/{id}` | GET | `"Operation completed successfully"` | `"Building not found with id: {id}"` |
| `POST /buildings` | POST | `"Building created successfully"` | - (aruncă ValidationException) |
| `PATCH /buildings/{id}` | PATCH | `"Building updated successfully"` | - (aruncă ValidationException) |
| `DELETE /buildings/{id}` | DELETE | `"Building deleted successfully"` | - (aruncă ValidationException) |
| `GET /buildings/spaces/{sid}` | GET | `"Operation completed successfully"` | - (aruncă NotFoundException) |
| `POST /buildings/spaces` | POST | `"Space created successfully"` | - (aruncă ValidationException) |
| `PATCH /buildings/spaces/{spaceId}` | PATCH | `"Space updated successfully"` | - (aruncă ValidationException) |
| `DELETE /buildings/spaces/{spaceId}` | DELETE | `"Space deleted successfully"` | - (aruncă ValidationException) |
| `POST /buildings/import` | POST | `"Import completed successfully"` | `"Failed to import locations: {error}"` |

**⚠️ NOTĂ:** `GET /buildings`, `GET /buildings/spaces`, etc. (list all) returnează **DIRECT** arrays (NU ApiResponse)

---

### ✅ Index Counters API (`/index-counters`)

**TOATE** endpoint-urile returnează `ApiResponse<T>`:

| Endpoint | Method | Success Message | Error Messages |
|----------|--------|----------------|----------------|
| `GET /index-counters/{id}` | GET | `"Operation completed successfully"` | - (aruncă excepții) |
| `POST /index-counters` | POST | `"Counter created successfully"` | - (aruncă NotFoundException) |
| `POST /index-counters/data` | POST | `"Index data added successfully"` | - (aruncă NotFoundException) |

**⚠️ NOTĂ:** `GET /index-counters` (list all) returnează **DIRECT** array (NU ApiResponse)

---

### ✅ Files API (`/files`)

**TOATE** endpoint-urile returnează `ApiResponse<T>`:

| Endpoint | Method | Success Message | Error Messages |
|----------|--------|----------------|----------------|
| `GET /files` | GET | `"Operation completed successfully"` | `"Owner type is required"`, `"Invalid owner type: {type}"` |
| `POST /files/commit` | POST | `"Files committed successfully"` | `"Missing required parameters: ownerType, ownerId, and tempIds"`, `"Invalid ownerType"`, `"Invalid request: {error}"`, `"File conflict: {error}"`, `"Failed to commit files: {error}"` |

---

## Endpoints cu Excepții Automate (Prins de GlobalControllerExceptionHandler)

Orice endpoint care **aruncă excepții** va fi convertit automat la `ApiResponse`.

### ValidationException (400 Bad Request)

| Mesaj | Când apare |
|-------|-----------|
| `"Body is null when creating new tenant"` | POST cu body null |
| `"No location/name for given building to add."` | Building fără location/name |
| `"Building with given name already exists: {name}"` | Duplicate building name |
| `"No building id provided for update."` | Update fără ID |
| `"Building not found with id: {id}"` | ID inexistent |
| `"Preset not found with id: {id}"` | Preset ID inexistent |

### NotFoundException (404 Not Found)

| Mesaj | Când apare |
|-------|-----------|
| `"No location with this id exists: {id}"` | Location inexistentă |
| `"Rental space not found by id: {id}"` | Space inexistent |

### IllegalStateException (400 Bad Request)

| Mesaj | Când apare |
|-------|-----------|
| `"Filename already exists for owner: {filename}"` | File conflict |
| `"Preset not found with id: {id}"` | Preset inexistent |

### IllegalArgumentException (400 Bad Request)

| Mesaj | Când apare |
|-------|-----------|
| `"Invalid tempId: {id}"` | Temp file invalid |
| Orice mesaj din `IllegalArgumentException` | - |

### Exception Generic (500 Internal Server Error)

| Mesaj | Când apare |
|-------|-----------|
| `"An unexpected error occurred. Please try again."` | Erori neprevăzute |

---

## ⚠️ EXCEPȚIE: Endpoints cu Liste (Rămân compatibile cu vechiul format)

### GET List Endpoints - Returnează Direct Arrays

**DOAR** aceste endpoint-uri **NU** returnează `ApiResponse`, ci direct arrays:

- `GET /tenants` → `List<Tenant>` (direct)
- `GET /buildings` → `List<Building>` (direct)
- `GET /buildings/spaces` → `List<RentalSpace>` (direct)
- `GET /buildings/{bid}/spaces` → `List<RentalSpace>` (direct)
- `GET /buildings/spaces/{tid}` → `List<RentalSpace>` (direct)
- `GET /index-counters` → `List<IndexCounter>` (direct)

**Toate celelalte endpoint-uri** returnează `ApiResponse<T>`:
- `GET /tenants/{id}` → `ApiResponse<Tenant>`
- `GET /buildings/{id}` → `ApiResponse<Building>`
- `GET /buildings/spaces/{sid}` → `ApiResponse<RentalSpace>`
- `POST /tenants` → `ApiResponse<Tenant>`
- `PATCH /tenants/{id}` → `ApiResponse<Tenant>`
- `DELETE /tenants/{id}` → `ApiResponse<Void>`
- etc.

**Verificare pentru liste:**
```typescript
const response = await fetch('/tenants');
if (response.ok) {
  const tenants: Tenant[] = await response.json(); // Array direct!
  // NU este ApiResponse!
}
```

---

## Workflow Recomandat pentru FE

### Pattern Universal

```typescript
interface ApiResponse<T> {
  success: boolean;
  message: string;
  data: T | null;
}

async function apiCall<T>(
  endpoint: string, 
  options: RequestInit = {}
): Promise<{ success: boolean; data?: T; error?: string; raw?: any }> {
  try {
    const response = await fetch(endpoint, options);
    const contentType = response.headers.get('content-type');
    
    // Check if it's ApiResponse or direct data
    if (contentType?.includes('application/json')) {
      const data = await response.json();
      
      // Check if it's ApiResponse format
      if ('success' in data && 'message' in data) {
        const apiResponse: ApiResponse<T> = data;
        return {
          success: apiResponse.success,
          data: apiResponse.success ? apiResponse.data as T : undefined,
          error: apiResponse.success ? undefined : apiResponse.message
        };
      } else {
        // Direct data response
        return {
          success: response.ok,
          raw: data
        };
      }
    } else {
      // Non-JSON response (ex. 204 No Content)
      return {
        success: response.ok
      };
    }
  } catch (error) {
    return {
      success: false,
      error: 'Network error'
    };
  }
}

// Usage
const result = await apiCall<Tenant[]>('/tenants');
if (result.success) {
  const tenants = result.raw as Tenant[];
  // Use tenants
} else {
  toast.error(result.error);
}
```

---

## Tabel Complet de Mesaje

### Email Presets (100% ApiResponse)

| Endpoint | Success | Error |
|----------|---------|-------|
| GET | `"Operation completed successfully"` | - |
| POST single | `"Preset saved successfully"` | - |
| PUT | `"Preset updated successfully"` | `"Preset not found with id: {id}"` |
| POST bulk | `"Presets saved successfully"` | `"Failed to save all presets"` |
| DELETE | `"Preset deleted successfully"` | `"Preset not found with id: {id}"` |
| POST send | `"All emails sent successfully"` | `"Failed to send {n} out of {total} emails"` |

### Validări Email Send

| Cauză | Mesaj |
|-------|-------|
| No recipients | `"Nu există destinatari specificați"` |
| Invalid email | `"Email invalid: {email}"` |
| Empty subject | `"Subject-ul este obligatoriu"` |
| Empty message | `"Mesajul este obligatoriu"` |
| SMTP error | `"Eroare la trimiterea emailului"` |

### Generic Exceptions (Toate Endpoints)

| Tip | Mesaj |
|-----|-------|
| ValidationException | Mesaj specific (ex. `"Body is null..."`) |
| NotFoundException | Mesaj specific (ex. `"Building not found..."`) |
| IllegalStateException | Mesaj specific |
| IllegalArgumentException | Mesaj specific |
| Generic Exception | `"An unexpected error occurred. Please try again."` |

---

## Testing Scenarios

### 1. Email Send - Success

```json
{
  "success": true,
  "message": "All emails sent successfully",
  "data": null
}
```

### 2. Email Send - Partial Failure

```json
{
  "success": false,
  "message": "Failed to send 2 out of 5 emails",
  "data": [
    {
      "subject": "Test",
      "recipients": ["invalid-email"],
      "errorMessage": "Email invalid: invalid-email"
    }
  ]
}
```

### 3. Update Preset - NotFound

```json
{
  "success": false,
  "message": "Preset not found with id: 999",
  "data": null
}
```

### 4. Create Building - Validation

```json
{
  "success": false,
  "message": "No location/name for given building to add.",
  "data": null
}
```

---

## Concluzie

✅ **TOATE endpoint-urile POST/PATCH/DELETE:** 100% ApiResponse  
✅ **TOATE endpoint-urile GET pentru iteme specifice:** 100% ApiResponse  
✅ **Erori:** 100% ApiResponse prin GlobalControllerExceptionHandler  
⚠️ **EXCEPȚIE: Liste GET** (ex. `GET /tenants`): Returnează direct arrays (NU ApiResponse)  

**Recomandare:** Folosește funcția helper de mai jos pentru a gestiona ambele tipuri de răspunsuri!

