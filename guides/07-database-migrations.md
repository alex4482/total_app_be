# 🗄️ Database Migrations - Ghid Complet

## 📌 Rezumat Rapid

**DEV**: Hibernate face tot → dezvoltare rapidă
**PROD**: Flyway gestionează schema → migrări controlate

## 🎯 Configurare Actuală

### Development (application-dev.properties)
```properties
spring.jpa.hibernate.ddl-auto=update  # Hibernate actualizează schema automat
spring.flyway.enabled=false           # Flyway dezactivat
```

### Production (application-prod.properties)
```properties
spring.jpa.hibernate.ddl-auto=validate # Doar validează, NU modifică
spring.flyway.enabled=true             # Flyway activ
spring.flyway.baseline-on-migrate=true # Acceptă baze existente
```

## 🔄 Workflow Complet: DEV → PROD

### Pasul 1: Dezvoltă feature-ul în DEV

```java
// Exemplu: Adaugi un câmp la Tenant
@Entity
public class Tenant {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    
    private String name;
    
    // 🆕 CÂMP NOU
    @Column(name = "phone_number", length = 20)
    private String phoneNumber;
    
    @Column(name = "email", unique = true)
    private String email;
}
```

**Ce se întâmplă:**
- Pornești app-ul în DEV
- Hibernate vede câmpul nou `phoneNumber`
- Creează automat coloana `phone_number` în tabel
- ✅ Totul funcționează

### Pasul 2: Creează migrarea SQL

Când feature-ul e gata, creezi migrarea pentru PROD:

**Fișier:** `src/main/resources/db/migration/V2__add_phone_to_tenant.sql`

```sql
-- Add phone number column to tenant table
-- Author: Your Name
-- Date: 2025-10-27
-- Jira: PROJ-123

ALTER TABLE tenant 
ADD COLUMN phone_number VARCHAR(20);

-- Optional: Add index if needed
CREATE INDEX idx_tenant_phone ON tenant(phone_number);

-- Optional: Add comment
COMMENT ON COLUMN tenant.phone_number IS 'Contact phone number for tenant';
```

### Pasul 3: Testează migrarea LOCAL

Înainte de push, testează că migrarea funcționează:

```bash
# 1. Creează o copie a bazei de date DEV
createdb total_app_test_migration -T total_app_db_test

# 2. Modifică temporar application-dev.properties
spring.flyway.enabled=true
spring.jpa.hibernate.ddl-auto=validate

# 3. Pornește app-ul - Flyway va aplica V2
./mvnw spring-boot:run

# 4. Verifică că a mers
psql total_app_test_migration -c "\d tenant"

# 5. Revertează configurația DEV
spring.flyway.enabled=false
spring.jpa.hibernate.ddl-auto=update
```

### Pasul 4: Push pe main

```bash
git add src/main/resources/db/migration/V2__add_phone_to_tenant.sql
git add src/main/java/com/work/total_app/models/tenant/Tenant.java
git commit -m "feat: add phone number to tenant"
git push origin main
```

### Pasul 5: Deploy pe PROD

Când codul ajunge pe server:

1. **Docker build** creează imaginea nouă
2. **Container pornește** → Spring Boot se lansează
3. **Flyway verifică** baza de date
4. **Detectează V2** (nouă, neaplicată)
5. **Aplică migrarea** → coloana `phone_number` este adăugată
6. **Hibernate validează** că schema match-uiește entitățile
7. ✅ **App-ul pornește** cu succes

## 📊 Structura Migrărilor

```
src/main/resources/db/migration/
├── V1__baseline.sql                    # Baseline inițial
├── V2__add_phone_to_tenant.sql         # Prima ta migrare
├── V3__create_invoice_table.sql        # Tabel nou
├── V4__add_indexes.sql                 # Performance
├── V5__update_tenant_status.sql        # Update date
└── README.md                           # Documentație
```

## 🎨 Template-uri de Migrări

### Template 1: Adaugă coloană simplă
```sql
-- V{N}__add_{column}_to_{table}.sql
ALTER TABLE {table_name}
ADD COLUMN {column_name} {data_type};
```

### Template 2: Adaugă coloană cu constraint
```sql
-- V{N}__add_{column}_with_constraint.sql
ALTER TABLE {table_name}
ADD COLUMN {column_name} {data_type} NOT NULL DEFAULT {default_value};

-- Optional: Remove default after migration
ALTER TABLE {table_name}
ALTER COLUMN {column_name} DROP DEFAULT;
```

### Template 3: Creează tabel nou
```sql
-- V{N}__create_{table}.sql
CREATE TABLE {table_name} (
    id BIGSERIAL PRIMARY KEY,
    name VARCHAR(255) NOT NULL,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP
);

CREATE INDEX idx_{table}_{column} ON {table_name}({column_name});
```

### Template 4: Adaugă foreign key
```sql
-- V{N}__add_fk_{table}_to_{referenced_table}.sql
ALTER TABLE {table_name}
ADD CONSTRAINT fk_{table}_{referenced_table}
FOREIGN KEY ({column_name}) REFERENCES {referenced_table}(id)
ON DELETE CASCADE;  -- sau ON DELETE SET NULL, etc.
```

### Template 5: Redenumește coloană
```sql
-- V{N}__rename_{old_name}_to_{new_name}.sql
ALTER TABLE {table_name}
RENAME COLUMN {old_name} TO {new_name};
```

### Template 6: Modifică tip coloană
```sql
-- V{N}__change_{column}_type.sql
-- Be careful! This might fail if data is incompatible
ALTER TABLE {table_name}
ALTER COLUMN {column_name} TYPE {new_type} USING {column_name}::{new_type};
```

### Template 7: Migrare date
```sql
-- V{N}__migrate_data_{description}.sql
-- Backup: Always test data migrations thoroughly!

-- Update existing records
UPDATE {table_name}
SET {new_column} = {expression}
WHERE {condition};

-- Clean up old data if needed
DELETE FROM {table_name}
WHERE {condition};
```

## 🔐 Securitate: Hash Parola Universală

### Unde să salvezi hash-ul:

**❌ NU salva parola în plain text nicăieri!**

**✅ Opțiunea 1: În application.properties (recomandat pentru dev/testing)**
```properties
# application-dev.properties
app.auth.universal-password-hash=$2a$12$t2Fz8QGcAfN0Io9VlnYsQ.Ql164qcz12STZ.HBrrdT.sbFEcxjSeO
```

**✅ Opțiunea 2: Environment variable (recomandat pentru prod)**
```bash
# În Docker Compose / Kubernetes
UNIVERSAL_PASSWORD_HASH=$2a$12$pjaxSp9ucuYPKseyAY1QYuToSFzSGdJGODcYBniro.R8LpulHU6bm
```

**✅ Opțiunea 3: Hibrid (cel mai bun)**
```properties
# application-prod.properties
app.auth.universal-password-hash=${UNIVERSAL_PASSWORD_HASH:$2a$12$defaultHashForFallback}
```

### Generare hash nou:

```bash
# Metoda 1: Cu clasa BCryptHashGenerator
cd src/main/java
javac -cp ".:../../../target/*" com/work/total_app/utils/BCryptHashGenerator.java
java -cp ".:../../../target/*" com.work.total_app.utils.BCryptHashGenerator

# Metoda 2: În aplicație (Controller temporar sau unit test)
BCryptPasswordEncoder encoder = new BCryptPasswordEncoder(12);
String hash = encoder.encode("parola_ta_noua");
System.out.println(hash);
```

### De ce hash-ul BCrypt e sigur în repository:

1. **Nu poate fi reversat** - BCrypt folosește one-way hashing
2. **Salted** - fiecare hash are salt unic
3. **Computațional expensive** - rezistent la brute force
4. **Standard în industrie** - folosit de milioane de app-uri

**Dar:** Hash-ul protejează doar parola. Dacă cineva are acces la repository, poate vedea hash-ul și îl poate înlocui cu al lui. Pentru securitate maximă, folosește environment variables în producție.

## ⚠️ Probleme Comune și Soluții

### Problema 1: "Validate failed: Migrations have failed validation"

**Cauză:** Ai modificat o migrare deja aplicată.

**Soluție:**
```bash
# NU face asta în producție!
# Doar în DEV, dacă chiar trebuie:
DELETE FROM flyway_schema_history WHERE version = '{versiunea_problematică}';
```

**Soluția corectă:** Creează o migrare nouă care să corecteze problema.

### Problema 2: "Found non-empty schema without schema history table"

**Cauză:** Prima rulare Flyway pe o bază existentă.

**Soluție:** Deja configurată cu `baseline-on-migrate=true` ✅

### Problema 3: Entitatea are câmpuri noi, dar baza nu

**Cauză:** Ai uitat să creezi migrarea pentru PROD.

**Soluție:**
1. Creează migrarea SQL
2. Deploy-o
3. Flyway va aplica-o automat

### Problema 4: "Column already exists"

**Cauză:** Hibernate a creat deja coloana în DEV, apoi ai încercat să rulezi migrarea.

**Soluție:**
```sql
-- Folosește IF NOT EXISTS
ALTER TABLE tenant 
ADD COLUMN IF NOT EXISTS phone_number VARCHAR(20);
```

Sau în migration:
```sql
DO $$ 
BEGIN
    IF NOT EXISTS (
        SELECT 1 FROM information_schema.columns 
        WHERE table_name='tenant' AND column_name='phone_number'
    ) THEN
        ALTER TABLE tenant ADD COLUMN phone_number VARCHAR(20);
    END IF;
END $$;
```

## 🚀 Best Practices

### 1. **O migrare = O schimbare logică**
❌ Rău:
```sql
-- V2__multiple_changes.sql
ALTER TABLE tenant ADD COLUMN phone VARCHAR(20);
ALTER TABLE building ADD COLUMN address TEXT;
CREATE TABLE invoice (...);
```

✅ Bine:
```sql
-- V2__add_phone_to_tenant.sql
ALTER TABLE tenant ADD COLUMN phone VARCHAR(20);
```
```sql
-- V3__add_address_to_building.sql
ALTER TABLE building ADD COLUMN address TEXT;
```
```sql
-- V4__create_invoice_table.sql
CREATE TABLE invoice (...);
```

### 2. **Mereu testează pe date reale**
```bash
# Creează o copie a producției
pg_dump production_db > backup.sql
createdb test_migration
psql test_migration < backup.sql

# Testează migrarea
# Apply migrations și verifică
```

### 3. **Backup înainte de fiecare deploy important**
```bash
# Automated backup script
pg_dump -U total_app_admin -h localhost total_app_db > "backup_$(date +%Y%m%d_%H%M%S).sql"
```

### 4. **Comentează migrările complexe**
```sql
-- V5__complex_data_migration.sql
-- 
-- Purpose: Migrate old pricing structure to new format
-- Impact: ~50,000 rows in tenant_rental_data table
-- Estimated time: ~30 seconds
-- Rollback: See V5_rollback.sql (manual)
--
-- Author: Alex
-- Date: 2025-10-27
-- Reviewed by: Team Lead

BEGIN;

-- Step 1: Create temp table
CREATE TEMP TABLE ...;

-- Step 2: Migrate data
UPDATE ...;

-- Step 3: Verify
DO $$ ... $$;

COMMIT;
```

### 5. **Versioning semantic**
```
V1__baseline.sql
V2__add_feature_x.sql
V3__add_feature_x_indexes.sql
V4__fix_feature_x_data.sql
V10__major_refactor_start.sql
V11__major_refactor_continue.sql
```

### 6. **Rollback manual**
Flyway nu suportă rollback automat. Creează scripturi manuale:
```sql
-- V5_rollback.sql (nu e detectat de Flyway, doar pentru referință)
ALTER TABLE tenant DROP COLUMN phone_number;
```

## 📋 Checklist pentru Deployment

Înainte de fiecare push pe main cu schimbări de DB:

- [ ] Entitatea e modificată și testată în DEV
- [ ] Migrarea SQL e creată și testată local
- [ ] Migrarea e testată pe o copie a bazei de producție
- [ ] Backup al producției e făcut (sau programat automat)
- [ ] Codul e reviewed
- [ ] Documentația e updatată (dacă e caz)
- [ ] Team-ul e notificat despre deployment
- [ ] Rollback plan e pregătit (pentru migrări complexe)

## 🔍 Monitorizare Post-Deployment

După deploy:
```sql
-- Verifică că migrarea a fost aplicată
SELECT * FROM flyway_schema_history ORDER BY installed_rank DESC LIMIT 5;

-- Verifică structura tabelului
\d+ tenant

-- Verifică datele
SELECT COUNT(*) FROM tenant;
SELECT phone_number FROM tenant LIMIT 5;

-- Check pentru null values (dacă coloana e NOT NULL)
SELECT COUNT(*) FROM tenant WHERE phone_number IS NULL;
```

## 📚 Resurse Utile

- **Flyway Docs:** https://flywaydb.org/documentation/
- **PostgreSQL ALTER TABLE:** https://www.postgresql.org/docs/current/sql-altertable.html
- **JPA/Hibernate Best Practices:** https://vladmihalcea.com/

## 🎓 Exemple Reale

Vezi folder: `src/main/resources/db/migration/`
- `V1__baseline.sql` - Baseline
- `V2__example_migration.sql.example` - Template-uri

Utilitar: `src/main/java/com/work/total_app/utils/BCryptHashGenerator.java`

