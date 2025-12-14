# 🏢 Total App - Backend

Backend API pentru gestionarea chiriilor, clădirilor, tenants, fișiere și comunicare prin email.

> 📁 **Vezi structura completă:** [PROJECT_STRUCTURE.md](PROJECT_STRUCTURE.md)

## 🚀 Quick Start

### Pornire Rapidă

**Windows (PowerShell):**
```powershell
.\scripts\run.ps1
```

**Linux/Mac:**
```bash
./scripts/run.sh
```

### Pornire Manuală

```bash
# Build
./mvnw clean package -DskipTests

# Run DEV
./mvnw spring-boot:run -Dspring-boot.run.profiles=dev

# Run PROD
./mvnw spring-boot:run -Dspring-boot.run.profiles=prod
```

## 📋 Caracteristici

- 🔐 **Autentificare Avansată** 
  - Multi-user authentication cu JWT
  - 2FA prin email după 6 încercări eșuate
  - Rate limiting și brute-force protection
  - HttpOnly cookies pentru refresh tokens
  - IP & User Agent tracking
  - Email whitelist pentru coduri de verificare
- 👥 **Gestionare Tenants** - CRUD complet, import Excel, bulk operations
- 🏗️ **Buildings & Rental Spaces** - Gestiune clădiri și spații de închiriat
- 📁 **File Management** - Upload, download, ZIP, filesystem storage (metadata în DB)
- 📧 **Email System** - Template-uri email, trimitere în bulk cu atașamente
- 📊 **Index Counters** - Gestiune contoare (apă, gaz, electricitate) și citiri
- 💾 **Backup & Restore** - Backup automat/manual, export Excel/JSON, Google Drive integration
- 🗄️ **Database Migrations** - Flyway pentru migrări controlate în producție
- 👮 **Admin API** - Management utilizatori și email whitelist

## 🛠️ Tech Stack

- **Java 21**
- **Spring Boot 3.5.3**
  - Spring Security (JWT)
  - Spring Data JPA
  - Spring Data REST
- **PostgreSQL**
- **Flyway** (database migrations)
- **Maven**
- **Lombok**
- **Apache POI** (Excel import/export)
- **SimpleJavaMail** (email sending)
- **Log4j2**

## 📁 Structura Proiectului

```
total_app/
├── src/main/java/com/work/total_app/
│   ├── config/          # Configurări (Security, Storage, etc.)
│   ├── controllers/     # REST Controllers
│   ├── services/        # Business logic
│   ├── repositories/    # JPA Repositories
│   ├── models/          # Entities & DTOs
│   ├── helpers/         # Helper classes (Email, Files, etc.)
│   ├── utils/           # Utilitare (BCrypt hash generator, etc.)
│   └── jobs/            # Scheduled jobs
│
├── src/main/resources/
│   ├── db/migration/    # Flyway migrations
│   ├── application*.properties
│   └── log4j2.xml
│
├── docs/                # Documentație și ghiduri setup
│   ├── CURSOR_SETUP.md
│   ├── MIGRATION_SETUP.md
│   ├── DEPLOYMENT_CHECKLIST.md
│   └── api-tests.http
│
├── guides/              # Documentație API
│   ├── 01-authentication.md (legacy)
│   ├── 02-authentication-complete.md (NEW - Complete Auth System)
│   ├── 03-admin-api.md (NEW - Admin Management)
│   ├── 02-tenants.md
│   ├── 03-buildings.md
│   ├── 04-files.md
│   ├── 05-email-presets.md
│   ├── 06-index-counters.md
│   ├── 07-database-migrations.md
│   └── 08-backup-restore.md
│
├── database/            # Database scripts
│   └── migration_auth_system.sql (NEW)
│
├── MIGRATION_GUIDE.md   # NEW - Auth migration guide
├── QUICKSTART.md        # NEW - Quick setup
├── TESTING_GUIDE.md     # NEW - Complete test suite
└── AUTH_IMPLEMENTATION_SUMMARY.md  # NEW - Implementation details
│
└── scripts/             # Helper scripts
    ├── run.ps1          # Script pornire Windows
    ├── run.sh           # Script pornire Linux/Mac
    ├── create-migration.ps1
    └── create-migration.sh
```

## ⚙️ Configurare

### Development Environment

Configurare în `src/main/resources/application-dev.properties`:

```properties
# Database
spring.datasource.url=jdbc:postgresql://localhost:5432/total_app_db_test
spring.datasource.username=postgres
spring.datasource.password=your_password

# Hibernate
spring.jpa.hibernate.ddl-auto=update  # Auto-update schema în DEV

# Flyway
spring.flyway.enabled=false  # Dezactivat în DEV pentru dezvoltare rapidă

# Email (pentru testing)
emailer.from=your@gmail.com
emailer.password=your_app_password
emailer.server.address=smtp.gmail.com
emailer.server.port=587

# Universal password
app.auth.universal-password-hash=$2a$12$your_bcrypt_hash
```

### Production Environment

Configurare în `src/main/resources/application-prod.properties`:

```properties
# Database (folosește environment variables)
spring.datasource.url=jdbc:postgresql://${DB_HOST:172.18.0.1}:5432/total_app_db
spring.datasource.username=total_app_admin
spring.datasource.password=total_password

# Hibernate
spring.jpa.hibernate.ddl-auto=validate  # Doar validare în PROD

# Flyway
spring.flyway.enabled=true  # Activ pentru migrări controlate
spring.flyway.baseline-on-migrate=true

# Email (via environment variables)
emailer.from=${EMAIL_FROM}
emailer.password=${EMAIL_PASSWORD}
emailer.server.address=${EMAIL_SERVER}
emailer.server.port=${EMAIL_PORT}

# Universal password (poate fi override prin env var)
app.auth.universal-password-hash=${UNIVERSAL_PASSWORD_HASH:$2a$12$default_hash}
```

### Environment Variables (Production)

```bash
# Database
export DB_HOST=your_db_host

# Email
export EMAIL_FROM=your@email.com
export EMAIL_PASSWORD=your_password
export EMAIL_SERVER=smtp.gmail.com
export EMAIL_PORT=587

# Authentication (REQUIRED)
export JWT_SECRET="your-super-secret-key-at-least-32-characters-long"

# Legacy support (optional)
export UNIVERSAL_PASSWORD_HASH="$2a$12$your_production_hash"
```

## 📊 Database Setup

### PostgreSQL Installation

**Windows:**
```powershell
# Download și instalează de pe postgresql.org
# Sau folosește Docker:
docker run --name total_app_db -e POSTGRES_PASSWORD=postgres -p 5432:5432 -d postgres:16
```

**Linux:**
```bash
sudo apt update
sudo apt install postgresql postgresql-contrib
sudo -u postgres createdb total_app_db_test
```

### Database Migrations

Aplicația folosește **Flyway** pentru migrări în producție și **Hibernate DDL auto-update** în development.

**Creează o migrare nouă:**
```bash
# Windows
.\scripts\create-migration.ps1 "add_phone_to_tenant"

# Linux/Mac
./scripts/create-migration.sh "add_phone_to_tenant"
```

**Pentru detalii complete:** Vezi [guides/07-database-migrations.md](guides/07-database-migrations.md)

## 🔐 Autentificare - Sistem Nou Multi-User

### ⚡ Quick Setup

```bash
# 1. Setează JWT Secret
export JWT_SECRET="your-super-secret-key-at-least-32-characters-long"

# 2. Rulează migrarea DB
psql -U postgres -d total_app < database/migration_auth_system.sql

# 3. Creează primul user (vezi QUICKSTART.md)
```

**Pentru setup complet:** Vezi **[QUICKSTART.md](QUICKSTART.md)** și **[MIGRATION_GUIDE.md](MIGRATION_GUIDE.md)**

### 🎯 Funcționalități

- ✅ **Multi-user** - Fiecare utilizator are propriul cont
- ✅ **2FA prin email** - Activat automat după 6 încercări eșuate
- ✅ **Rate limiting** - 10 încercări/15min pe IP, blocare 30 min după 10 eșecuri
- ✅ **Email whitelist** - Codurile se trimit doar pe emailuri aprobate
- ✅ **HttpOnly cookies** - Protecție XSS pentru refresh tokens
- ✅ **IP tracking** - Fiecare sesiune are IP și User Agent
- ✅ **Audit logging** - Toate acțiunile sunt logate

### 📚 Documentație Completă

- **[QUICKSTART.md](QUICKSTART.md)** - Setup în 5 pași
- **[MIGRATION_GUIDE.md](MIGRATION_GUIDE.md)** - Ghid detaliat de migrare
- **[guides/02-authentication-complete.md](guides/02-authentication-complete.md)** - API Reference complet
- **[guides/03-admin-api.md](guides/03-admin-api.md)** - Admin endpoints
- **[TESTING_GUIDE.md](TESTING_GUIDE.md)** - Test suite complet
- **[AUTH_IMPLEMENTATION_SUMMARY.md](AUTH_IMPLEMENTATION_SUMMARY.md)** - Ce a fost implementat
- **[FRONTEND_INTEGRATION_GUIDE.md](FRONTEND_INTEGRATION_GUIDE.md)** ⭐ - Ghid complet pentru Frontend
- **[FRONTEND_SUMMARY.md](FRONTEND_SUMMARY.md)** ⭐ - Quick summary pentru FE Dev

### 🧪 Test Rapid

```bash
# Login
curl -X POST http://localhost:8080/auth/login \
  -H "Content-Type: application/json" \
  -c cookies.txt \
  -d '{"username": "admin", "password": "Admin123!"}'
```

## 📚 API Documentation

Documentație completă pentru fiecare endpoint în folder-ul `guides/`:

### API Endpoints
- **[Authentication API](guides/02-authentication-complete.md)** ⭐ NEW - Multi-user, 2FA, rate limiting
- **[Admin API](guides/03-admin-api.md)** ⭐ NEW - User & email whitelist management
- **[Tenants API](guides/02-tenants.md)** - CRUD, import Excel, bulk delete
- **[Buildings API](guides/03-buildings.md)** - Clădiri, rooms, rental spaces
- **[Files API](guides/04-files.md)** - Upload, download, ZIP archives
- **[Email Presets API](guides/05-email-presets.md)** - Template-uri și trimitere email
- **[Index Counters API](guides/06-index-counters.md)** - Contoare și citiri
- **[Backup & Restore API](guides/08-backup-restore.md)** - Backup automat/manual, restaurare din JSON/Excel

### Development & Deployment
- **[Database Migrations](guides/07-database-migrations.md)** - Workflow Flyway, best practices

### Quick API Test

Fișier `api-tests.http` conține exemple pentru toate endpoint-urile:
```http
### Login
POST http://localhost:8080/auth/login
Content-Type: application/json

{
  "username": "admin",
  "password": "your_password"
}
```

## 🐳 Docker Deployment

### Build Image
```bash
docker build -t total_app:latest .
```

### Run Container
```bash
docker run -d \
  --name total_app \
  -p 8080:8080 \
  -e DB_HOST=postgres_host \
  -e EMAIL_FROM=your@email.com \
  -e EMAIL_PASSWORD=your_password \
  -e EMAIL_SERVER=smtp.gmail.com \
  -e EMAIL_PORT=587 \
  -e UNIVERSAL_PASSWORD_HASH='$2a$12$your_hash' \
  total_app:latest
```

### Docker Compose
```yaml
version: '3.8'
services:
  postgres:
    image: postgres:16
    environment:
      POSTGRES_DB: total_app_db
      POSTGRES_USER: total_app_admin
      POSTGRES_PASSWORD: total_password
    volumes:
      - postgres_data:/var/lib/postgresql/data
    ports:
      - "5432:5432"

  app:
    build: .
    ports:
      - "8080:8080"
    environment:
      DB_HOST: postgres
      EMAIL_FROM: ${EMAIL_FROM}
      EMAIL_PASSWORD: ${EMAIL_PASSWORD}
      UNIVERSAL_PASSWORD_HASH: ${UNIVERSAL_PASSWORD_HASH}
    depends_on:
      - postgres

volumes:
  postgres_data:
```

## 🧪 Testing

```bash
# Run all tests
./mvnw test

# Run specific test
./mvnw test -Dtest=TotalAppApplicationTests

# Skip tests during build
./mvnw clean package -DskipTests
```

## 📝 Logging

Logs sunt configurate în `src/main/resources/log4j2.xml`:
- **Console**: INFO level
- **File**: `logs/app.log` (DEBUG level, rotație automată)

```bash
# Vezi logs în timp real
tail -f logs/app.log
```

## 🔧 Development Workflow

### 1. Modificare Entitate
```java
@Entity
public class Tenant {
    // ... câmpuri existente
    
    @Column(name = "phone_number")
    private String phoneNumber;  // CÂMP NOU
}
```

### 2. Restart App în DEV
```bash
./mvnw spring-boot:run
```
→ Hibernate va crea coloana automat ✅

### 3. Creează Migrare pentru PROD
```bash
.\scripts\create-migration.ps1 "add_phone_to_tenant"
```

### 4. Editează SQL
```sql
-- V2__add_phone_to_tenant.sql
ALTER TABLE tenant 
ADD COLUMN phone_number VARCHAR(20);
```

### 5. Push pe Main
```bash
git add .
git commit -m "feat: add phone to tenant"
git push origin main
```

→ Deploy pe PROD → Flyway aplică migrarea automat ✅

## 🤝 Contributing

1. Fork repository-ul
2. Creează branch pentru feature: `git checkout -b feature/amazing-feature`
3. Commit changes: `git commit -m 'feat: add amazing feature'`
4. Push pe branch: `git push origin feature/amazing-feature`
5. Deschide Pull Request

### Commit Message Convention
```
feat: add new feature
fix: bug fix
docs: documentation changes
refactor: code refactoring
test: add tests
chore: maintenance tasks
```

## 📄 License

Proprietar - Toate drepturile rezervate.

## 🆘 Troubleshooting

### App nu pornește

**Eroare: "Connection refused" la database**
```bash
# Verifică că PostgreSQL rulează
sudo systemctl status postgresql  # Linux
# sau
Get-Service postgresql*  # Windows

# Testează conexiunea
psql -U postgres -h localhost -p 5432
```

**Eroare: "Port 8080 already in use"**
```bash
# Găsește procesul
netstat -ano | findstr :8080  # Windows
lsof -i :8080  # Linux/Mac

# Kill procesul
taskkill /PID <pid> /F  # Windows
kill -9 <pid>  # Linux/Mac
```

### Flyway Errors

**Eroare: "Validate failed"**
```sql
-- Vezi history-ul migrărilor
SELECT * FROM flyway_schema_history ORDER BY installed_rank;

-- Dacă e nevoie, șterge migrarea problematică (DOAR ÎN DEV!)
DELETE FROM flyway_schema_history WHERE version = '2';
```

**Pentru mai multe soluții:** [guides/07-database-migrations.md#troubleshooting](guides/07-database-migrations.md)

### Email nu se trimite

**Gmail: "Invalid credentials"**
- Folosește App Password, nu parola normală
- Activează 2FA pe contul Google
- Generează App Password: https://myaccount.google.com/apppasswords

## 📞 Contact & Support

Pentru întrebări sau probleme:
- **Documentație:** Folder `guides/` și `docs/`
- **Setup:** `docs/CURSOR_SETUP.md` și `docs/MIGRATION_SETUP.md`
- **API Tests:** `docs/api-tests.http`
- **Scripts:** Folder `scripts/`

---

**Versiune:** 2.0-SNAPSHOT (with Advanced Authentication System)  
**Java:** 21  
**Spring Boot:** 3.5.3  
**Ultima actualizare:** Decembrie 2025

---

## 🆕 What's New in v2.0

### 🔐 Advanced Authentication System

- ✅ **Multi-user support** - Individual user accounts with username/password
- ✅ **Two-Factor Authentication** - Email-based 2FA after 6 failed attempts
- ✅ **Rate Limiting** - IP-based (10/15min) and user-based protection
- ✅ **Email Whitelist** - Only approved emails receive verification codes
- ✅ **HttpOnly Cookies** - XSS protection for refresh tokens
- ✅ **Session Tracking** - IP address and User Agent for each session
- ✅ **Comprehensive Audit Logging** - All authentication events logged
- ✅ **Admin Management API** - User and whitelist administration

**Migration:** See [MIGRATION_GUIDE.md](MIGRATION_GUIDE.md) for upgrading from v1.0
