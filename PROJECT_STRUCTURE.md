# 📁 Project Structure

Structura organizată a proiectului Total App.

## 🗂️ Organizare Folder-e

```
total_app/
│
├── 📄 README.md                          # Documentație principală
├── 📄 PROJECT_STRUCTURE.md               # Acest fișier
├── 📄 pom.xml                            # Maven configuration
├── 📄 Dockerfile                         # Docker image configuration
│
├── 📁 docs/                              # 📚 DOCUMENTAȚIE & SETUP
│   ├── README.md                         # Index documentație
│   ├── CURSOR_SETUP.md                   # Setup în Cursor IDE
│   ├── MIGRATION_SETUP.md                # Setup database migrations
│   ├── DEPLOYMENT_CHECKLIST.md           # Checklist deployment
│   └── api-tests.http                    # Colecție teste API
│
├── 📁 guides/                            # 📖 GHIDURI API
│   ├── README.md                         # Index ghiduri
│   ├── 01-authentication.md              # API: Autentificare
│   ├── 02-tenants.md                     # API: Tenants
│   ├── 03-buildings.md                   # API: Buildings
│   ├── 04-files.md                       # API: Files
│   ├── 05-email-presets.md               # API: Email
│   ├── 06-index-counters.md              # API: Counters
│   └── 07-database-migrations.md         # Ghid complet migrări DB
│
├── 📁 scripts/                           # 🔧 HELPER SCRIPTS
│   ├── README.md                         # Documentație scripts
│   ├── run.ps1                           # Start app (Windows)
│   ├── run.sh                            # Start app (Linux/Mac)
│   ├── create-migration.ps1              # Creare migrări (Windows)
│   └── create-migration.sh               # Creare migrări (Linux/Mac)
│
└── 📁 src/
    ├── 📁 main/
    │   ├── 📁 java/com/work/total_app/
    │   │   ├── 📁 config/                # Configurări (Security, Storage)
    │   │   ├── 📁 controllers/           # REST Controllers
    │   │   ├── 📁 services/              # Business logic
    │   │   ├── 📁 repositories/          # JPA Repositories
    │   │   ├── 📁 models/                # Entities & DTOs
    │   │   ├── 📁 helpers/               # Helper classes
    │   │   ├── 📁 utils/                 # Utilitare
    │   │   │   └── BCryptHashGenerator.java
    │   │   ├── 📁 jobs/                  # Scheduled jobs
    │   │   └── TotalAppApplication.java  # Main class
    │   │
    │   └── 📁 resources/
    │       ├── application.properties
    │       ├── application-dev.properties
    │       ├── application-prod.properties
    │       ├── 📁 db/migration/          # 🗄️ FLYWAY MIGRATIONS
    │       │   ├── README.md
    │       │   ├── V1__baseline.sql
    │       │   └── V2__example_migration.sql.example
    │       ├── emailer.properties
    │       └── log4j2.xml
    │
    └── 📁 test/
        └── 📁 java/com/work/total_app/
            └── TotalAppApplicationTests.java
```

## 📚 Documentație

### Unde să găsești ce trebuie

| Caut... | Unde găsesc? |
|---------|--------------|
| **Setup inițial** | `docs/CURSOR_SETUP.md` |
| **Database migrations** | `docs/MIGRATION_SETUP.md` |
| **Deployment** | `docs/DEPLOYMENT_CHECKLIST.md` |
| **API endpoints** | `guides/01-*.md` - `guides/06-*.md` |
| **Database workflow** | `guides/07-database-migrations.md` |
| **Teste API** | `docs/api-tests.http` |
| **Start app** | `scripts/run.ps1` sau `scripts/run.sh` |
| **Creează migrare** | `scripts/create-migration.ps1` |

## 🎯 Quick Reference

### Pentru Development

```bash
# Start app
.\scripts\run.ps1                        # Windows
./scripts/run.sh                         # Linux/Mac

# Creează migrare
.\scripts\create-migration.ps1 "description"
```

### Pentru Documentation

```bash
# Setup documentation
docs/
├── CURSOR_SETUP.md              # IDE setup
├── MIGRATION_SETUP.md           # DB migrations quick start
└── DEPLOYMENT_CHECKLIST.md      # Deployment guide

# API documentation  
guides/
├── 01-authentication.md         # Login, JWT
├── 02-tenants.md               # Tenants CRUD
├── 03-buildings.md             # Buildings & spaces
├── 04-files.md                 # File management
├── 05-email-presets.md         # Email templates
├── 06-index-counters.md        # Counters & readings
└── 07-database-migrations.md   # Complete DB guide
```

## 🔧 Convenții

### Denumire Fișiere

**Documentație:**
- `UPPERCASE_WORDS.md` - documentație generală (README, SETUP)
- `##-lowercase-words.md` - ghiduri numerotate (API guides)

**Scripts:**
- `kebab-case.ps1` - PowerShell (Windows)
- `kebab-case.sh` - Bash (Linux/Mac)

**Migrări:**
- `V{N}__{description}.sql` - Flyway migrations
- Exemplu: `V2__add_phone_to_tenant.sql`

### Organizare Logică

```
docs/       → Documentație setup, deployment, testing
guides/     → Documentație API (pentru frontend developers)
scripts/    → Helper scripts pentru operațiuni comune
src/        → Codul aplicației
```

## 📖 Flow pentru Noi Dezvoltatori

### 1. **Setup Initial**
```
1. Citește: README.md
2. Setup environment: docs/CURSOR_SETUP.md  
3. Setup DB migrations: docs/MIGRATION_SETUP.md
```

### 2. **Development**
```
1. Pornește app: scripts/run.ps1
2. Testează API: docs/api-tests.http
3. Citește API guides: guides/
```

### 3. **Modificări Database**
```
1. Modifică entity în dev
2. Creează migrare: scripts/create-migration.ps1
3. Editează SQL generat
4. Vezi ghidul: guides/07-database-migrations.md
```

### 4. **Deployment**
```
1. Urmează checklist: docs/DEPLOYMENT_CHECKLIST.md
2. Push pe main
3. Verifică că Flyway aplică migrările
```

## 🆕 Adăugare Documentație Nouă

### Pentru API nou:
```
guides/08-nume-api.md
```

Adaugă link în:
- `guides/README.md`
- `README.md` (dacă e relevant)

### Pentru setup/deployment:
```
docs/NUME_DOCUMENT.md
```

Adaugă link în:
- `docs/README.md`
- `README.md` (dacă e relevant)

### Pentru script nou:
```
scripts/nume-script.ps1  (Windows)
scripts/nume-script.sh   (Linux/Mac)
```

Documentează în:
- `scripts/README.md`

## 📊 Statistici

```
Folder        Scop                           Fișiere
----------------------------------------------------------
docs/         Documentație setup             5 files
guides/       Documentație API               8 files  
scripts/      Helper scripts                 5 files
src/          Cod aplicație                  100+ files
```

## 🔍 Căutare Rapidă

### Căutare după scop:

**Vreau să:**
- ✅ Pornesc app-ul → `scripts/run.ps1`
- ✅ Fac setup → `docs/CURSOR_SETUP.md`
- ✅ Creez migrare DB → `scripts/create-migration.ps1`
- ✅ Văd API-uri → `guides/`
- ✅ Testez endpoint → `docs/api-tests.http`
- ✅ Fac deploy → `docs/DEPLOYMENT_CHECKLIST.md`
- ✅ Văd structura → acest fișier

### Căutare după tehnologie:

**Caut despre:**
- 🔐 Autentificare → `guides/01-authentication.md`
- 🗄️ Database → `guides/07-database-migrations.md`
- 📁 Fișiere → `guides/04-files.md`
- 📧 Email → `guides/05-email-presets.md`
- 🏗️ Buildings → `guides/03-buildings.md`
- 👥 Tenants → `guides/02-tenants.md`

## 🎨 Vizualizare Simplificată

```
total_app/
│
├── 📚 docs/          ← Setup & Deployment
├── 📖 guides/        ← API Documentation  
├── 🔧 scripts/       ← Helper Tools
└── 💻 src/           ← Application Code
    └── resources/
        └── db/migration/  ← Database Migrations
```

---

**Pentru mai multe detalii:** Vezi `README.md` sau folder-ele specifice.

**Ultima actualizare:** Octombrie 2025

