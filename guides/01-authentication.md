# Authentication API

Base URL: `/auth`

## Endpoints

### 1. Login
**POST** `/auth/login`

Autentifică un utilizator și returnează token-uri JWT.

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
    "accessToken": "string",
    "refreshToken": "string"
  }
}
```

**Error**
- `401 Unauthorized` - Credențiale invalide
- `403 Forbidden` - Request invalid (lipsește password)

---

### 2. Refresh Token
**POST** `/auth/refresh-token`

Reîmprospătează token-ul de acces folosind refresh token-ul.

#### Request Body
```json
{
  "refreshToken": "string"
}
```

#### Response
**Success (202 Accepted)**
```json
{
  "tokens": {
    "accessToken": "string",
    "refreshToken": "string"
  }
}
```

**Error**
- `403 Forbidden` - Refresh token invalid sau expirat

---

## Note pentru Frontend
- Token-ul de acces trebuie inclus în header-ul `Authorization: Bearer {accessToken}` pentru toate request-urile autentificate
- Când primești `401 Unauthorized`, încearcă să refreshezi token-ul înainte de a redirecta utilizatorul la login
- Stochează token-urile în `localStorage` sau `sessionStorage` (preferabil `httpOnly` cookies pentru securitate)

