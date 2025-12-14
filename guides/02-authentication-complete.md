# Authentication & User Management API

Base URL: `/auth`

## Overview

Sistemul de autentificare suportă:
- **Multi-user authentication** cu username și parolă
- **Rate limiting** pentru protecție împotriva brute-force
- **Two-factor authentication (2FA) prin email** după 6 încercări eșuate
- **Email whitelist** pentru coduri de verificare
- **HttpOnly cookies** pentru refresh tokens
- **IP și User Agent tracking** pentru securitate îmbunătățită
- **Audit logging** complet

---

## Endpoints

### 1. Register User
**POST** `/auth/register`

Înregistrează un utilizator nou în sistem.

#### Request Body
```json
{
  "username": "string",
  "password": "string",      // Minim 8 caractere
  "email": "string"          // Opțional
}
```

#### Response
**Success (201 Created)**
```json
{
  "message": "User created successfully",
  "username": "string"
}
```

**Error**
- `400 Bad Request` - Date invalide (parolă prea scurtă, username lipsă)
- `409 Conflict` - Username sau email deja existent

---

### 2. Login Standard
**POST** `/auth/login`

Autentifică un utilizator cu username și parolă.

**IMPORTANT**: După 6 încercări eșuate, utilizatorul va fi obligat să folosească `/auth/login-with-email`.

#### Request Body
```json
{
  "username": "string",
  "password": "string"
}
```

#### Response
**Success (202 Accepted)**
```json
{
  "tokens": {
    "accessToken": "string",    // JWT, valabil 15 minute
    "refreshToken": null,       // Trimis ca HttpOnly cookie
    "sessionId": "string"
  }
}
```

**Headers Set**:
- `Set-Cookie: refreshToken=...; HttpOnly; Secure; SameSite=Strict; Path=/auth; Max-Age=604800`

**Error**
- `401 Unauthorized` - Credențiale invalide
- `403 Forbidden` - Cont dezactivat SAU necesită verificare prin email
- `429 Too Many Requests` - Prea multe încercări eșuate de pe același IP

---

### 3. Request Email Verification Code
**POST** `/auth/request-email-code`

Solicită un cod de verificare trimis pe email. Utilizat când userul necesită 2FA.

**IMPORTANT**: Emailul trebuie să fie în whitelist-ul din baza de date.

#### Request Body
```json
{
  "username": "string",
  "password": "string",
  "email": "string"          // Trebuie să fie în whitelist
}
```

#### Response
**Success (200 OK)**
```json
{
  "message": "Cod de verificare trimis pe email"
}
```

**Error**
- `400 Bad Request` - Email nu este în whitelist SAU prea multe coduri solicitate (max 5/oră)
- `401 Unauthorized` - Credențiale invalide

---

### 4. Login With Email Verification
**POST** `/auth/login-with-email`

Autentifică un utilizator folosind codul de verificare trimis pe email.

#### Request Body
```json
{
  "username": "string",
  "password": "string",
  "email": "string",
  "verificationCode": "string"  // Cod de 6 cifre
}
```

#### Response
**Success (202 Accepted)**
```json
{
  "tokens": {
    "accessToken": "string",
    "refreshToken": null,       // Trimis ca HttpOnly cookie
    "sessionId": "string"
  }
}
```

**Error**
- `401 Unauthorized` - Credențiale invalide SAU cod de verificare invalid/expirat
- `403 Forbidden` - Cont dezactivat

**Note**:
- Codul de verificare expiră după 15 minute
- După login reușit, contorul de încercări eșuate este resetat

---

### 5. Refresh Token
**POST** `/auth/refresh-token`

Reîmprospătează token-ul de acces folosind refresh token-ul.

#### Request
- **Option 1**: Cookie `refreshToken` (recomandat, automat trimis de browser)
- **Option 2**: Body JSON (pentru compatibilitate backwards)

```json
{
  "refreshToken": "string"     // Opțional dacă se folosește cookie
}
```

#### Response
**Success (202 Accepted)**
```json
{
  "tokens": {
    "accessToken": "string",    // JWT nou
    "refreshToken": null,       // Rotated și trimis ca cookie
    "sessionId": "string"
  }
}
```

**Error**
- `401 Unauthorized` - Refresh token invalid, expirat sau reutilizat (compromise detection)
- `403 Forbidden` - Lipsește refresh token

**Note**:
- Refresh token-ul este **rotated** la fiecare request (vechiul devine invalid)
- Suportă **idempotent retry**: token-ul anterior poate fi folosit o singură dată

---

### 6. Logout
**POST** `/auth/logout`

Invalidează sesiunea curentă și refresh token-ul.

#### Headers Required
```
Authorization: Bearer {accessToken}
```

#### Response
**Success (200 OK)**
```json
{
  "message": "Logout successful"
}
```

**Note**:
- Invalidează toate token-urile asociate sesiunii
- Cookie-ul `refreshToken` este șters

---

### 7. Change Password
**POST** `/auth/change-password`

Schimbă parola utilizatorului autentificat.

#### Headers Required
```
Authorization: Bearer {accessToken}
```

#### Request Body
```json
{
  "oldPassword": "string",
  "newPassword": "string"      // Minim 8 caractere
}
```

#### Response
**Success (200 OK)**
```json
{
  "message": "Password changed successfully"
}
```

**Error**
- `401 Unauthorized` - Parola veche incorectă SAU utilizator neautentificat
- `400 Bad Request` - Parola nouă prea scurtă

---

## Security Features

### 1. Rate Limiting

#### IP-based Rate Limiting
- **10 încercări eșuate** în 15 minute → IP blocat pentru 15 minute
- Protecție împotriva brute-force attacks

#### User-based Failed Attempts
- **6 încercări eșuate** → Necesită verificare prin email la următorul login
- **10 încercări eșuate** → Cont blocat pentru 30 minute

### 2. Email Verification (2FA)

Activat automat după 6 încercări eșuate de login.

**Flow**:
1. User încearcă login standard → Primește `403 Forbidden` cu mesaj explicit
2. User apelează `/auth/request-email-code` cu username, parolă și email
3. Sistemul verifică dacă emailul este în whitelist
4. Dacă DA → Trimite cod de 6 cifre pe email (valabil 15 minute)
5. User apelează `/auth/login-with-email` cu codul primit
6. După login reușit → Flag-ul de verificare email este resetat

**Rate Limits**:
- Max **5 coduri de verificare** per oră per user

### 3. Email Whitelist

Pentru securitate, codurile de verificare sunt trimise doar pe emailuri din whitelist.

**Administrare whitelist**:
```sql
-- Adaugă email în whitelist
INSERT INTO email_whitelist (id, email, active, description, created_at) 
VALUES (gen_random_uuid(), 'user@example.com', true, 'User principal', NOW());

-- Vizualizează whitelist
SELECT * FROM email_whitelist WHERE active = true;

-- Dezactivează un email
UPDATE email_whitelist SET active = false WHERE email = 'user@example.com';
```

### 4. Session Management

Fiecare login creează o sesiune unică cu:
- **sessionId**: UUID unic
- **IP address**: Adresa IP a clientului
- **User Agent**: Browser/device info
- **Timestamps**: created_at, last_login, etc.

**Invalidare sesiune**:
- Logout manual
- Expirarea refresh token-ului (7 zile)
- Detecție compromise (refresh token reuse)

### 5. Audit Logging

Toate acțiunile de autentificare sunt logate:
- Login attempts (success/failure)
- Refresh token operations
- Password changes
- Email verification code requests

**Log levels**:
- `INFO`: Operațiuni normale
- `WARN`: Încercări eșuate, rate limiting
- `ERROR`: Erori de sistem

---

## Data Models

### User
```json
{
  "id": "uuid",
  "username": "string",
  "passwordHash": "string (bcrypt)",
  "email": "string",
  "enabled": boolean,
  "accountLocked": boolean,
  "failedLoginAttempts": number,
  "lastFailedLoginAt": "timestamp",
  "accountLockedUntil": "timestamp",
  "lastSuccessfulLoginAt": "timestamp",
  "lastLoginIp": "string",
  "requiresEmailVerification": boolean,
  "createdAt": "timestamp",
  "updatedAt": "timestamp"
}
```

### Login Attempt
```json
{
  "id": "uuid",
  "username": "string",
  "ipAddress": "string",
  "userAgent": "string",
  "successful": boolean,
  "failureReason": "string",
  "createdAt": "timestamp"
}
```

### Email Verification Code
```json
{
  "id": "uuid",
  "userId": "uuid",
  "code": "string (6 digits)",
  "email": "string",
  "expiresAt": "timestamp",
  "used": boolean,
  "usedAt": "timestamp",
  "createdAt": "timestamp",
  "requestIp": "string"
}
```

---

## Frontend Integration Guide

### 1. Configurare Axios

```javascript
import axios from 'axios';

const api = axios.create({
  baseURL: 'http://localhost:8080',
  withCredentials: true, // IMPORTANT pentru HttpOnly cookies
  headers: {
    'Content-Type': 'application/json'
  }
});

// Interceptor pentru access token
api.interceptors.request.use(config => {
  const accessToken = localStorage.getItem('accessToken');
  if (accessToken) {
    config.headers.Authorization = `Bearer ${accessToken}`;
  }
  return config;
});

// Interceptor pentru auto-refresh
api.interceptors.response.use(
  response => response,
  async error => {
    if (error.response?.status === 401) {
      // Încearcă refresh
      try {
        const { data } = await axios.post(
          'http://localhost:8080/auth/refresh-token',
          {},
          { withCredentials: true }
        );
        
        localStorage.setItem('accessToken', data.tokens.accessToken);
        
        // Retry original request
        error.config.headers.Authorization = `Bearer ${data.tokens.accessToken}`;
        return axios.request(error.config);
      } catch (refreshError) {
        // Redirect to login
        localStorage.removeItem('accessToken');
        window.location.href = '/login';
      }
    }
    return Promise.reject(error);
  }
);
```

### 2. Login Flow

```javascript
// Login standard
async function login(username, password) {
  try {
    const { data } = await api.post('/auth/login', { username, password });
    localStorage.setItem('accessToken', data.tokens.accessToken);
    return { success: true };
  } catch (error) {
    if (error.response?.status === 403) {
      // Necesită verificare email
      return { 
        success: false, 
        requiresEmailVerification: true,
        message: 'Prea multe încercări eșuate. Te rugăm să folosești codul din email.'
      };
    }
    return { success: false, message: 'Credențiale invalide' };
  }
}

// Request email code
async function requestEmailCode(username, password, email) {
  const { data } = await api.post('/auth/request-email-code', {
    username,
    password,
    email
  });
  return data.message;
}

// Login cu email
async function loginWithEmail(username, password, email, code) {
  const { data } = await api.post('/auth/login-with-email', {
    username,
    password,
    email,
    verificationCode: code
  });
  localStorage.setItem('accessToken', data.tokens.accessToken);
  return { success: true };
}

// Logout
async function logout() {
  await api.post('/auth/logout');
  localStorage.removeItem('accessToken');
  window.location.href = '/login';
}
```

### 3. React Component Example

```jsx
import { useState } from 'react';

function LoginPage() {
  const [username, setUsername] = useState('');
  const [password, setPassword] = useState('');
  const [email, setEmail] = useState('');
  const [code, setCode] = useState('');
  const [needsEmailVerification, setNeedsEmailVerification] = useState(false);
  const [codeSent, setCodeSent] = useState(false);
  
  const handleStandardLogin = async (e) => {
    e.preventDefault();
    const result = await login(username, password);
    
    if (result.success) {
      window.location.href = '/dashboard';
    } else if (result.requiresEmailVerification) {
      setNeedsEmailVerification(true);
      alert(result.message);
    } else {
      alert('Login failed: ' + result.message);
    }
  };
  
  const handleRequestCode = async () => {
    try {
      const message = await requestEmailCode(username, password, email);
      setCodeSent(true);
      alert(message);
    } catch (error) {
      alert('Nu s-a putut trimite codul. Verifică dacă emailul este valid.');
    }
  };
  
  const handleEmailLogin = async (e) => {
    e.preventDefault();
    try {
      await loginWithEmail(username, password, email, code);
      window.location.href = '/dashboard';
    } catch (error) {
      alert('Cod invalid sau expirat');
    }
  };
  
  if (needsEmailVerification) {
    return (
      <div>
        <h2>Verificare prin Email</h2>
        <input 
          type="email" 
          placeholder="Email" 
          value={email}
          onChange={(e) => setEmail(e.target.value)}
        />
        {!codeSent ? (
          <button onClick={handleRequestCode}>Trimite Cod</button>
        ) : (
          <>
            <input 
              type="text" 
              placeholder="Cod (6 cifre)" 
              value={code}
              onChange={(e) => setCode(e.target.value)}
            />
            <button onClick={handleEmailLogin}>Login</button>
          </>
        )}
      </div>
    );
  }
  
  return (
    <div>
      <h2>Login</h2>
      <form onSubmit={handleStandardLogin}>
        <input 
          type="text" 
          placeholder="Username" 
          value={username}
          onChange={(e) => setUsername(e.target.value)}
        />
        <input 
          type="password" 
          placeholder="Password" 
          value={password}
          onChange={(e) => setPassword(e.target.value)}
        />
        <button type="submit">Login</button>
      </form>
    </div>
  );
}
```

---

## Database Schema

### Table: `users`
```sql
CREATE TABLE users (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    username VARCHAR(100) UNIQUE NOT NULL,
    password_hash VARCHAR(255) NOT NULL,
    email VARCHAR(255),
    enabled BOOLEAN NOT NULL DEFAULT true,
    account_locked BOOLEAN NOT NULL DEFAULT false,
    failed_login_attempts INT NOT NULL DEFAULT 0,
    last_failed_login_at TIMESTAMP,
    account_locked_until TIMESTAMP,
    last_successful_login_at TIMESTAMP,
    last_login_ip VARCHAR(45),
    requires_email_verification BOOLEAN NOT NULL DEFAULT false,
    created_at TIMESTAMP NOT NULL DEFAULT NOW(),
    updated_at TIMESTAMP NOT NULL DEFAULT NOW()
);
```

### Table: `email_whitelist`
```sql
CREATE TABLE email_whitelist (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    email VARCHAR(255) UNIQUE NOT NULL,
    active BOOLEAN NOT NULL DEFAULT true,
    description TEXT,
    created_at TIMESTAMP NOT NULL DEFAULT NOW()
);
```

### Table: `email_verification_codes`
```sql
CREATE TABLE email_verification_codes (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    user_id UUID NOT NULL REFERENCES users(id) ON DELETE CASCADE,
    code VARCHAR(10) NOT NULL,
    email VARCHAR(255) NOT NULL,
    expires_at TIMESTAMP NOT NULL,
    used BOOLEAN NOT NULL DEFAULT false,
    used_at TIMESTAMP,
    created_at TIMESTAMP NOT NULL DEFAULT NOW(),
    request_ip VARCHAR(45)
);
```

### Table: `login_attempts`
```sql
CREATE TABLE login_attempts (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    username VARCHAR(100),
    ip_address VARCHAR(45) NOT NULL,
    user_agent VARCHAR(500),
    successful BOOLEAN NOT NULL,
    failure_reason VARCHAR(100),
    created_at TIMESTAMP NOT NULL DEFAULT NOW()
);

CREATE INDEX idx_ip_timestamp ON login_attempts(ip_address, created_at);
CREATE INDEX idx_username_timestamp ON login_attempts(username, created_at);
```

### Table: `refresh_token_state`
```sql
CREATE TABLE refresh_token_state (
    session_id VARCHAR(36) PRIMARY KEY,
    token_hash VARCHAR(64) NOT NULL UNIQUE,
    previous_token_hash VARCHAR(64),
    expires_at TIMESTAMP NOT NULL,
    created_at TIMESTAMP NOT NULL DEFAULT NOW(),
    revoked_after TIMESTAMP NOT NULL DEFAULT '1970-01-01 00:00:00',
    ip_address VARCHAR(45),
    user_agent VARCHAR(500),
    version BIGINT NOT NULL DEFAULT 0
);

CREATE INDEX idx_rts_token_hash ON refresh_token_state(token_hash);
CREATE INDEX idx_rts_prev_hash ON refresh_token_state(previous_token_hash);
```

---

## Initial Setup

### 1. Creează primul user
```sql
-- Password: "password123" (hashed cu BCrypt)
INSERT INTO users (username, password_hash, email, enabled) 
VALUES (
    'admin',
    '$2a$10$rN3qJ8xqF5JqLJ5JxqJ5Je5JqJ5JxqJ5Je5JqJ5JxqJ5Je5JqJ5Jx',  -- Replace cu hash real
    'admin@example.com',
    true
);
```

### 2. Adaugă emailuri în whitelist
```sql
INSERT INTO email_whitelist (email, active, description) VALUES
    ('admin@example.com', true, 'Admin principal'),
    ('support@company.com', true, 'Support team'),
    ('dev@company.com', true, 'Development team');
```

### 3. Configurare environment variables
```bash
# .env sau docker-compose.yml
JWT_SECRET=your-super-secret-key-min-32-characters-long
UNIVERSAL_PASSWORD_HASH=legacy-support-optional
```

---

## Security Best Practices

1. **HTTPS Only**: Setează cookie-uri cu flag-ul `Secure` doar în producție
2. **CORS**: Configurează domenii permise în `SecurityConfig`
3. **JWT Secret**: Folosește un secret lung (min 32 caractere) și rotează-l periodic
4. **Database**: Folosește prepared statements (JPA o face automat)
5. **Passwords**: BCrypt cu cost factor 10+
6. **Email Whitelist**: Actualizează periodic lista de emailuri permise
7. **Monitoring**: Monitorizează log-urile pentru pattern-uri suspecte

---

## Troubleshooting

### "Prea multe încercări eșuate"
- **Cauză**: 10 login-uri eșuate în 15 minute de pe același IP
- **Soluție**: Așteaptă 15 minute sau folosește alt IP

### "Contul este blocat temporar"
- **Cauză**: 10 încercări eșuate pentru același user
- **Soluție**: Așteaptă 30 minute sau contactează admin pentru deblocare manuală

### "Email nu este în whitelist"
- **Cauză**: Emailul furnizat nu este în tabelul `email_whitelist`
- **Soluție**: Adaugă emailul în whitelist prin SQL

### "Cod de verificare invalid"
- **Cauză**: Cod greșit, expirat (>15 min) sau deja folosit
- **Soluție**: Solicită un cod nou

### "Refresh token invalid"
- **Cauză**: Token expirat, revocat sau compromise detection
- **Soluție**: Relogare necesară

---

## Status Codes

- `200 OK`: Succes (GET requests)
- `201 Created`: User creat cu succes
- `202 Accepted`: Autentificare reușită
- `400 Bad Request`: Date invalide
- `401 Unauthorized`: Credențiale invalide sau token expirat
- `403 Forbidden`: Acces interzis (cont blocat, necesită email verification)
- `409 Conflict`: Username/email deja existent
- `429 Too Many Requests`: Rate limit depășit
- `500 Internal Server Error`: Eroare server

---

## Contact & Support

Pentru probleme sau întrebări despre API, contactează echipa de development.

