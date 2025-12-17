# 🚀 Quick Start - Authentication System

## TL;DR

Ai un sistem complet de autentificare implementat cu:
- ✅ Multi-user cu username/parolă
- ✅ 2FA prin email după 6 încercări eșuate  
- ✅ Rate limiting și protecție brute-force
- ✅ HttpOnly cookies și JWT tokens
- ✅ Admin API pentru management

---

## 📝 Setup în 5 Pași

### 1️⃣ Setează JWT Secret
```bash
# În environment variables sau .env
export JWT_SECRET="your-super-secret-key-at-least-32-characters-long"

# SAU direct în application-dev.properties (DOAR pentru development)
# app.jwt.secret=your-secret-key-here
```

**IMPORTANT**: În producție, folosește ÎNTOTDEAUNA environment variables!

### 2️⃣ Rulează Migrarea DB
```bash
psql -U postgres -d total_app < database/migration_auth_system.sql
```

### 3️⃣ Creează Primul User
```bash
# Generează hash BCrypt pentru parola "Admin123!"
# Folosește: https://bcrypt-generator.com/

# Apoi:
psql -U postgres -d total_app

INSERT INTO users (username, password_hash, email, enabled) 
VALUES ('admin', '$2a$10$[HASH-UL-TAU-AICI]', 'admin@example.com', true);
```

### 4️⃣ Adaugă Email în Whitelist
```sql
INSERT INTO email_whitelist (email, active, description) 
VALUES ('admin@example.com', true, 'Admin principal');
```

### 5️⃣ Restart Aplicația
```bash
mvn clean install
mvn spring-boot:run
```

---

## 🧪 Test Rapid

```bash
# Login
curl -X POST http://localhost:8080/auth/login \
  -H "Content-Type: application/json" \
  -c cookies.txt \
  -d '{"username": "admin", "password": "Admin123!"}'

# Dacă primești token → ✅ FUNCȚIONEAZĂ!
```

---

## 📚 Documentație Completă

| Fișier | Descriere |
|--------|-----------|
| **MIGRATION_GUIDE.md** | Ghid detaliat de migrare |
| **AUTH_IMPLEMENTATION_SUMMARY.md** | Ce a fost implementat |
| **TESTING_GUIDE.md** | Teste complete |
| **guides/02-authentication-complete.md** | API Reference |
| **guides/03-admin-api.md** | Admin endpoints |

---

## 🔑 Endpoint-uri Principale

```bash
# Autentificare
POST /auth/register              # Înregistrare user nou
POST /auth/login                 # Login standard
POST /auth/login-with-email      # Login cu 2FA (după 6 eșecuri)
POST /auth/request-email-code    # Solicită cod verificare
POST /auth/refresh-token         # Refresh token
POST /auth/logout                # Logout
POST /auth/change-password       # Schimbă parola

# Admin - Users
GET    /admin/users              # Lista useri
PUT    /admin/users/{id}/enabled # Enable/disable user
POST   /admin/users/{id}/unlock  # Deblochează user
DELETE /admin/users/{id}         # Șterge user
GET    /admin/users/stats        # Statistici

# Admin - Email Whitelist
GET    /admin/email-whitelist    # Lista emailuri
POST   /admin/email-whitelist    # Adaugă email
DELETE /admin/email-whitelist/{id} # Șterge email
```

---

## 🛠️ Troubleshooting Quick Fixes

### "JWT secret must be at least 32 chars"
```bash
# Option 1: Environment variable (recommended)
export JWT_SECRET="un-secret-foarte-lung-de-cel-putin-32-de-caractere"

# Option 2: În application-dev.properties (doar pentru development)
app.jwt.secret=un-secret-foarte-lung-de-cel-putin-32-de-caractere
```

**NOTA**: Nu mai este nevoie de `app.auth.universal-password-hash` - sistemul nou folosește autentificare multi-user!

### "Email nu este în whitelist"
```sql
INSERT INTO email_whitelist (email, active, description) 
VALUES ('youremail@example.com', true, 'Your description');
```

### "Cont blocat"
```sql
UPDATE users 
SET account_locked = false, 
    failed_login_attempts = 0, 
    requires_email_verification = false 
WHERE username = 'admin';
```

### "Nu se trimit emailuri"
Verifică `application.properties`:
```properties
spring.mail.host=smtp.gmail.com
spring.mail.port=587
spring.mail.username=your-email@gmail.com
spring.mail.password=your-app-password
```

---

## 🎯 Flow 2FA (după 6 eșecuri)

```
1. User încearcă login → 6 eșecuri consecutive
2. User primește 403: "Necesită verificare email"
3. User solicită cod: POST /auth/request-email-code
4. Primește email cu cod de 6 cifre
5. Login cu cod: POST /auth/login-with-email
6. Success → Reset counter încercări eșuate
```

---

## 📊 Verificare Status în DB

```sql
-- Vezi userii și statusul lor
SELECT username, enabled, failed_login_attempts, requires_email_verification 
FROM users;

-- Vezi ultimele încercări de login
SELECT username, successful, failure_reason, created_at 
FROM login_attempts 
ORDER BY created_at DESC 
LIMIT 10;

-- Vezi emailurile în whitelist
SELECT email, active FROM email_whitelist WHERE active = true;
```

---

## ⚡ One-Liner pentru Test Complet

```bash
# Testează tot flow-ul
curl -X POST http://localhost:8080/auth/register \
  -H "Content-Type: application/json" \
  -d '{"username":"test1","password":"Test1234","email":"test@example.com"}' && \
curl -X POST http://localhost:8080/auth/login \
  -H "Content-Type: application/json" \
  -c /tmp/auth.txt \
  -d '{"username":"test1","password":"Test1234"}' | jq && \
curl -X POST http://localhost:8080/auth/refresh-token \
  -b /tmp/auth.txt | jq
```

---

## 🎉 Dacă Tot Merge

**Congrats! Sistemul tău de autentificare este functional!** 

Citește documentația completă pentru features avansate:
- Email verification 2FA
- Rate limiting configuration  
- Admin API usage
- Production best practices

---

## 📞 Need Help?

1. **Vezi log-urile**: `tail -f logs/application.log | grep AUTH`
2. **Citește**: `MIGRATION_GUIDE.md`
3. **Testează**: `TESTING_GUIDE.md`
4. **API Docs**: `guides/02-authentication-complete.md`

---

**Built with ❤️ - Ready for Production 🚀**

