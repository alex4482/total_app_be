# Admin API - User & Email Whitelist Management

Base URL: `/admin`

## Overview

API-uri pentru administrarea utilizatorilor și whitelist-ului de emailuri.

**IMPORTANT**: Aceste endpoint-uri ar trebui protejate cu rol de ADMIN în producție.

---

## Email Whitelist Management

### 1. Get All Whitelisted Emails
**GET** `/admin/email-whitelist`

Returnează toate emailurile din whitelist (active și inactive).

#### Response
```json
[
  {
    "id": "uuid",
    "email": "user@example.com",
    "active": true,
    "description": "Admin principal",
    "createdAt": "2024-01-01T10:00:00Z"
  }
]
```

---

### 2. Get Active Whitelisted Emails
**GET** `/admin/email-whitelist/active`

Returnează doar emailurile active din whitelist.

---

### 3. Add Email to Whitelist
**POST** `/admin/email-whitelist`

Adaugă un email în whitelist.

#### Request Body
```json
{
  "email": "newemail@example.com",
  "description": "Description optional"
}
```

#### Response
```json
{
  "message": "Email added to whitelist successfully"
}
```

---

### 4. Remove Email from Whitelist
**DELETE** `/admin/email-whitelist/{id}`

Dezactivează un email din whitelist (soft delete).

#### Response
```json
{
  "message": "Email removed from whitelist"
}
```

---

### 5. Reactivate Email
**PUT** `/admin/email-whitelist/{id}/activate`

Reactivează un email dezactivat.

---

### 6. Check if Email is Whitelisted
**GET** `/admin/email-whitelist/check/{email}`

Verifică dacă un email este în whitelist.

#### Response
```json
{
  "whitelisted": true
}
```

---

## User Management

### 1. Get All Users
**GET** `/admin/users`

Returnează toți utilizatorii (fără password hash-uri).

#### Response
```json
[
  {
    "id": "uuid",
    "username": "admin",
    "email": "admin@example.com",
    "enabled": true,
    "accountLocked": false,
    "failedLoginAttempts": 0,
    "lastFailedLoginAt": null,
    "accountLockedUntil": null,
    "lastSuccessfulLoginAt": "2024-01-15T10:00:00Z",
    "lastLoginIp": "192.168.1.1",
    "requiresEmailVerification": false,
    "createdAt": "2024-01-01T10:00:00Z",
    "updatedAt": "2024-01-15T10:00:00Z"
  }
]
```

---

### 2. Get User by ID
**GET** `/admin/users/{id}`

Returnează detaliile unui utilizator specific.

---

### 3. Enable/Disable User
**PUT** `/admin/users/{id}/enabled?enabled=true`

Activează sau dezactivează un utilizator.

#### Query Parameters
- `enabled` (boolean): `true` pentru activare, `false` pentru dezactivare

#### Response
```json
{
  "message": "User enabled"
}
```

---

### 4. Unlock User Account
**POST** `/admin/users/{id}/unlock`

Deblochează un cont de utilizator și resetează toate flag-urile de securitate.

Această acțiune:
- Deblochează contul (`accountLocked = false`)
- Resetează contorul de încercări eșuate
- Elimină cerința de verificare prin email
- Șterge timestamp-ul de blocare

#### Response
```json
{
  "message": "User unlocked successfully"
}
```

---

### 5. Reset Email Verification Requirement
**POST** `/admin/users/{id}/reset-email-verification`

Resetează flag-ul de verificare prin email pentru un utilizator.

---

### 6. Delete User
**DELETE** `/admin/users/{id}`

Șterge complet un utilizator din sistem.

**ATENȚIE**: Această acțiune este ireversibilă!

---

### 7. Get User Statistics
**GET** `/admin/users/stats`

Returnează statistici despre utilizatori.

#### Response
```json
{
  "totalUsers": 10,
  "enabledUsers": 9,
  "lockedUsers": 1,
  "requiresEmailVerification": 2
}
```

---

## cURL Examples

### Add Email to Whitelist
```bash
curl -X POST http://localhost:8080/admin/email-whitelist \
  -H "Authorization: Bearer YOUR_ACCESS_TOKEN" \
  -H "Content-Type: application/json" \
  -d '{
    "email": "newemail@example.com",
    "description": "New user email"
  }'
```

### Unlock User
```bash
curl -X POST http://localhost:8080/admin/users/{userId}/unlock \
  -H "Authorization: Bearer YOUR_ACCESS_TOKEN"
```

### Get User Stats
```bash
curl -X GET http://localhost:8080/admin/users/stats \
  -H "Authorization: Bearer YOUR_ACCESS_TOKEN"
```

### Check if Email is Whitelisted
```bash
curl -X GET http://localhost:8080/admin/email-whitelist/check/user@example.com \
  -H "Authorization: Bearer YOUR_ACCESS_TOKEN"
```

---

## Security Notes

1. **IMPORTANT**: În producție, aceste endpoint-uri trebuie protejate cu rol de ADMIN
2. Actualizează `SecurityConfig.java` pentru a adăuga verificare de rol:

```java
.authorizeHttpRequests(reg -> reg
    .requestMatchers(HttpMethod.OPTIONS, "/**").permitAll()
    .requestMatchers("/auth/**").permitAll()
    .requestMatchers("/admin/**").hasRole("ADMIN")  // <-- Add this
    .anyRequest().authenticated())
```

3. Pentru implementare completă a rolurilor, ar trebui:
   - Adăugat un tabel `user_roles`
   - Adăugat verificare în `LocalTokenVerifier`
   - Updatat `UserPrincipal` cu lista de roluri

