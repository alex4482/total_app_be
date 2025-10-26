# 🔧 Scripts

Acest folder conține scripturi helper pentru development și operațiuni comune.

## 📋 Scripturi Disponibile

### 🚀 Application Scripts

#### `run.ps1` (Windows PowerShell)
Pornește aplicația în modul development.

```powershell
.\scripts\run.ps1
```

**Ce face:**
- Build-uiește proiectul cu Maven
- Pornește aplicația cu profilul DEV
- Configurează environment-ul corect

#### `run.sh` (Linux/Mac)
Echivalent pentru Linux/Mac.

```bash
./scripts/run.sh
```

---

### 🗄️ Database Migration Scripts

#### `create-migration.ps1` (Windows PowerShell)
Creează o nouă migrare Flyway.

```powershell
.\scripts\create-migration.ps1 "add_phone_to_tenant"
```

**Ce face:**
- Găsește ultimul număr de versiune
- Creează automat fișierul `V{N}__description.sql`
- Populează cu template SQL
- Oferă să deschidă fișierul în editor

**Output:**
```
src/main/resources/db/migration/V2__add_phone_to_tenant.sql
```

#### `create-migration.sh` (Linux/Mac)
Echivalent pentru Linux/Mac.

```bash
./scripts/create-migration.sh "add_phone_to_tenant"
```

---

## 💡 Usage Examples

### Pornire Aplicație

**Development (Hibernate DDL auto-update):**
```powershell
# Windows
.\scripts\run.ps1

# Linux/Mac
./scripts/run.sh
```

**Production (Manual):**
```bash
./mvnw spring-boot:run -Dspring-boot.run.profiles=prod
```

### Creare Migrări

**Exemplu 1: Adaugă coloană**
```powershell
.\scripts\create-migration.ps1 "add_email_to_building"
```

Editează fișierul creat:
```sql
ALTER TABLE building 
ADD COLUMN email VARCHAR(255);
```

**Exemplu 2: Creează tabel nou**
```powershell
.\scripts\create-migration.ps1 "create_invoice_table"
```

Editează fișierul creat:
```sql
CREATE TABLE invoice (
    id BIGSERIAL PRIMARY KEY,
    number VARCHAR(50) NOT NULL,
    ...
);
```

**Exemplu 3: Adaugă index**
```powershell
.\scripts\create-migration.ps1 "add_tenant_email_index"
```

Editează fișierul creat:
```sql
CREATE INDEX idx_tenant_email ON tenant(email);
```

---

## 🔧 Script Details

### Naming Convention

Toate migrările urmează formatul:
```
V{VERSION}__{DESCRIPTION}.sql
```

Exemple:
- `V1__baseline.sql`
- `V2__add_phone_to_tenant.sql`
- `V3__create_invoice_table.sql`
- `V4__add_indexes.sql`

### Migration Workflow

```
1. Dezvoltă în DEV cu Hibernate auto-update
   ↓
2. Rulează: .\scripts\create-migration.ps1 "description"
   ↓
3. Editează SQL-ul generat
   ↓
4. Testează migrarea pe o copie DB
   ↓
5. Commit și push
   ↓
6. Flyway aplică automat în PROD
```

---

## 📚 Related Documentation

- **[../docs/MIGRATION_SETUP.md](../docs/MIGRATION_SETUP.md)** - Setup complet database migrations
- **[../guides/07-database-migrations.md](../guides/07-database-migrations.md)** - Ghid detaliat migrări
- **[../docs/DEPLOYMENT_CHECKLIST.md](../docs/DEPLOYMENT_CHECKLIST.md)** - Checklist deployment

---

## 🆘 Troubleshooting

### Script nu rulează

**Windows:**
```powershell
# Verifică execution policy
Get-ExecutionPolicy

# Permite rularea scripturilor (dacă e nevoie)
Set-ExecutionPolicy -ExecutionPolicy RemoteSigned -Scope CurrentUser
```

**Linux/Mac:**
```bash
# Dă permisiuni de execuție
chmod +x scripts/*.sh
```

### Migrarea nu se creează

**Verifică că directorul există:**
```bash
ls -la src/main/resources/db/migration/
```

**Dacă lipsește:**
```bash
mkdir -p src/main/resources/db/migration
```

---

## 🔄 Updating Scripts

Pentru a modifica sau extinde script-urile:

1. **Editează** fișierul `.ps1` sau `.sh`
2. **Testează** local
3. **Documentează** modificările în acest README
4. **Commit** cu mesaj descriptiv

```bash
git add scripts/
git commit -m "feat: add new functionality to migration script"
```

---

## 📝 Notes

- Script-urile sunt cross-platform compatibile
- PowerShell pentru Windows (`.ps1`)
- Bash pentru Linux/Mac (`.sh`)
- Ambele au funcționalitate identică
- Versiunile sunt sincronizate automat (Flyway)

Pentru întrebări sau îmbunătățiri, contactează echipa de development.

