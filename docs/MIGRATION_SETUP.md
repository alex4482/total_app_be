# ✅ Database Migrations - Setup Complete

## 🎉 Ce s-a configurat

### 1. **Flyway Dependencies** (pom.xml)
```xml
<dependency>
    <groupId>org.flywaydb</groupId>
    <artifactId>flyway-core</artifactId>
</dependency>
<dependency>
    <groupId>org.flywaydb</groupId>
    <artifactId>flyway-database-postgresql</artifactId>
</dependency>
```

### 2. **Configurare Environment-Specific**

**DEV** (`application-dev.properties`):
```properties
spring.jpa.hibernate.ddl-auto=update  # Hibernate face UPDATE automat
spring.flyway.enabled=false           # Flyway DEZACTIVAT
```
→ **Dezvoltare rapidă**: modifici entitatea → restart → schema se updatează

**PROD** (`application-prod.properties`):
```properties
spring.jpa.hibernate.ddl-auto=validate  # Hibernate doar VALIDEAZĂ
spring.flyway.enabled=true              # Flyway ACTIV
spring.flyway.baseline-on-migrate=true  # Acceptă DB existente
```
→ **Migrări controlate**: modifici entitatea → creezi migration SQL → deploy → Flyway aplică

### 3. **Securitate Parola Universală**

Configurată cu fallback la environment variable:
```properties
app.auth.universal-password-hash=${UNIVERSAL_PASSWORD_HASH:$2a$12$defaultHash...}
```

**În producție**, setează:
```bash
export UNIVERSAL_PASSWORD_HASH="$2a$12$your_production_hash_here"
```

### 4. **Structura Creată**

```
src/main/resources/db/migration/
├── V1__baseline.sql                    # Baseline migration (gol)
├── V2__example_migration.sql.example   # Template-uri și exemple
└── README.md                           # Documentație detaliată

guides/
└── 07-database-migrations.md           # Ghid complet workflow

src/main/java/.../utils/
└── BCryptHashGenerator.java            # Utilitar generare hash-uri

scripts/                                # Helper scripts
├── create-migration.ps1                # PowerShell script
├── create-migration.sh                 # Bash script
├── run.ps1                             # Start app (Windows)
└── run.sh                              # Start app (Linux/Mac)
```

## 🚀 Quick Start

### Creează o migrare nouă

**Windows (PowerShell):**
```powershell
.\scripts\create-migration.ps1 "add_phone_to_tenant"
```

**Linux/Mac:**
```bash
./scripts/create-migration.sh "add_phone_to_tenant"
```

Script-ul va crea automat `V2__add_phone_to_tenant.sql` cu template.

### Workflow complet

1. **Dezvoltă în DEV:**
   ```java
   @Entity
   public class Tenant {
       // ... câmpuri existente
       
       @Column(name = "phone_number")
       private String phoneNumber;  // NOU!
   }
   ```
   → Pornești app → Hibernate creează coloana automat ✅

2. **Creează migrarea pentru PROD:**
   ```bash
   ./scripts/create-migration.ps1 "add_phone_to_tenant"
   ```
   
3. **Editează SQL-ul:**
   ```sql
   -- V2__add_phone_to_tenant.sql
   ALTER TABLE tenant 
   ADD COLUMN phone_number VARCHAR(20);
   ```

4. **Testează local** (vezi ghid complet)

5. **Push pe main:**
   ```bash
   git add .
   git commit -m "feat: add phone to tenant"
   git push origin main
   ```

6. **Deploy pe PROD:**
   → Flyway detectează V2
   → Aplică migrarea automat
   → App pornește cu schema nouă ✅

## 📚 Documentație Completă

Pentru detalii complete, vezi:
- **[guides/07-database-migrations.md](guides/07-database-migrations.md)** - Ghid complet cu:
  - Workflow detaliat DEV → PROD
  - Template-uri SQL pentru toate scenariile
  - Best practices și troubleshooting
  - Securitate și backup strategies
  - Exemple complete

## 🛠️ Utilitare

### Generare hash BCrypt

**Metoda 1 - Rulează clasa Java:**
```bash
# Editează BCryptHashGenerator.java, modifică parola
# Apoi rulează:
mvn compile exec:java -Dexec.mainClass="com.work.total_app.utils.BCryptHashGenerator"
```

**Metoda 2 - În aplicație (test):**
```java
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;

@Test
public void generateHash() {
    BCryptPasswordEncoder encoder = new BCryptPasswordEncoder(12);
    String hash = encoder.encode("your_password");
    System.out.println(hash);
}
```

### Verificare status Flyway

```sql
-- Conectează-te la DB și rulează:
SELECT * FROM flyway_schema_history ORDER BY installed_rank;
```

## ⚡ TL;DR

**DEV:**
- Modifici entitatea → restart → funcționează
- Flyway dezactivat
- Hibernate face tot

**PROD:**
- Modifici entitatea → creezi migration SQL → push
- Flyway activ, aplică automat
- Schema sigură și versionată

**Securitate:**
- Hash-ul BCrypt e safe în `.properties`
- Pentru extra securitate în PROD: environment variable

**Helper scripts:**
- `create-migration.ps1` / `.sh` → creare migrări rapide

**Documentație:**
- `guides/07-database-migrations.md` → ghid complet

## ✅ Totul e gata!

Acum când modifici o entitate JPA și dai push pe main:
1. ✅ În DEV: Hibernate updatează automat (dezvoltare rapidă)
2. ✅ În PROD: Flyway aplică migrările tale (migrări controlate)
3. ✅ Zero downtime, zero probleme!

**Next steps:** Citește ghidul complet și încearcă să creezi prima migrare! 🚀

