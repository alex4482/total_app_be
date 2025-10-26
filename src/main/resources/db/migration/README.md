# Database Migrations cu Flyway

## 📋 Workflow Dezvoltare → Producție

### 🔧 În DEV (development):
- **Hibernate `ddl-auto=update`** este activ
- **Flyway este DEZACTIVAT**
- Modifici entitățile și Hibernate updatează automat tabelele
- Dezvoltare rapidă, fără scripturi SQL

### 🚀 În PROD (production):
- **Hibernate `ddl-auto=validate`** - doar verifică schema
- **Flyway este ACTIV** - gestionează toate migrările
- Structura bazei de date se modifică DOAR prin migrări Flyway

## 📝 Cum să creezi o migrare nouă

### Pasul 1: Dezvoltă în DEV
```java
// Modifici entitatea, de ex. adaugi un câmp nou
@Entity
public class Tenant {
    // ... câmpuri existente
    
    @Column(name = "phone_number")
    private String phoneNumber;  // CÂMP NOU
}
```

Pornești app-ul în DEV → Hibernate creează coloana automat ✅

### Pasul 2: Creează migrarea pentru PROD

Folosește helper script-ul:
```bash
# Windows
.\scripts\create-migration.ps1 "add_phone_to_tenant"

# Linux/Mac
./scripts/create-migration.sh "add_phone_to_tenant"
```

Acest lucru va crea automat `V2__add_phone_to_tenant.sql` în acest director.

Conținut:
```sql
-- Add phone number to tenant table
ALTER TABLE tenant 
ADD COLUMN phone_number VARCHAR(50);
```

### Pasul 3: Testează migrarea

Înainte de push pe main:
1. Creează o bază de date de test curată
2. Activează Flyway temporar în dev.properties
3. Rulează app-ul - Flyway va aplica migrările
4. Verifică că totul funcționează

### Pasul 4: Push pe main
- Codul ajunge pe production
- Flyway detectează migrarea nouă (V2)
- O aplică automat la pornirea aplicației
- Baza de date este actualizată ✅

## 📐 Convenții de denumire

Fișierele de migrare trebuie să urmeze formatul:
```
V{VERSION}__{DESCRIPTION}.sql
```

Exemple:
- `V1__baseline.sql` - baseline inițial
- `V2__add_phone_to_tenant.sql` - adaugă coloană
- `V3__create_invoice_table.sql` - tabel nou
- `V4__add_tenant_indexes.sql` - indecși
- `V5__rename_column_xyz.sql` - redenumire

## ⚠️ Reguli importante

1. **NICIODATĂ nu modifica o migrare deja aplicată** - Flyway o va detecta și va da eroare
2. **ÎNTOTDEAUNA creează migrări noi** - chiar și pentru fix-uri
3. **Testează migrările** înainte de push
4. **Fă backup** la baza de date înainte de deploy-uri importante

## 🛠️ Comenzi utile

### Generare hash BCrypt pentru parole:
```java
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;

public class HashGenerator {
    public static void main(String[] args) {
        BCryptPasswordEncoder encoder = new BCryptPasswordEncoder();
        String hash = encoder.encode("parola_ta");
        System.out.println(hash);
    }
}
```

### Verificare status Flyway (via Maven):
```bash
mvn flyway:info
```

### Forțare baseline (dacă e nevoie):
```bash
mvn flyway:baseline
```

## 📊 Flow complet exemplu

```
1. Modifici entitatea în IntelliJ
   ↓
2. Testezi în DEV (Hibernate face update)
   ↓
3. Creezi migrare SQL (V2__xxx.sql)
   ↓
4. Testezi migrarea pe o DB curată
   ↓
5. Push pe main
   ↓
6. Deploy pe PROD
   ↓
7. Flyway aplică V2 automat
   ↓
8. ✅ Success!
```

## 🔍 Verificare stare curentă

Tabelul `flyway_schema_history` ține evidența migrărilor aplicate:
```sql
SELECT * FROM flyway_schema_history ORDER BY installed_rank;
```

## 🆘 Troubleshooting

### Eroare: "Validate failed: Migrations have failed validation"
- O migrare aplicată a fost modificată
- Soluție: Revertește modificarea sau creează o migrare nouă de fix

### Eroare: "Found non-empty schema(s) without schema history table"
- Prima rulare pe o bază existentă
- Flyway va folosi baseline-on-migrate (deja configurat) ✅

### Vreau să resetez totul în DEV
```sql
DROP SCHEMA public CASCADE;
CREATE SCHEMA public;
```
Apoi repornește app-ul - Flyway va rula toate migrările de la început.

