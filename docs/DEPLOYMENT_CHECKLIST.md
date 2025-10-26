# 🚀 Deployment Checklist

## ✅ Pre-Deployment (Înainte de Push)

### 1. Code Changes
- [ ] Toate modificările sunt testate local
- [ ] Tests passed: `./mvnw test`
- [ ] Build succeeded: `./mvnw clean package`
- [ ] No linter errors
- [ ] Code reviewed (dacă aplicabil)

### 2. Database Changes (Dacă aplicabil)

- [ ] **Entitățile modificate sunt testate în DEV**
  ```bash
  # În DEV, Hibernate a creat/modificat tabelele automat
  ./mvnw spring-boot:run -Dspring-boot.run.profiles=dev
  ```

- [ ] **Migrările Flyway sunt create**
  ```bash
  # Windows
  .\scripts\create-migration.ps1 "description_of_change"
  
  # Linux/Mac
  ./scripts/create-migration.sh "description_of_change"
  ```

- [ ] **SQL-ul migrării este complet și corect**
  ```sql
  -- Verifică în: src/main/resources/db/migration/V{N}__*.sql
  -- Asigură-te că SQL-ul este corect
  -- Adaugă comentarii pentru claritate
  ```

- [ ] **Migrarea este testată pe o copie a DB-ului**
  ```bash
  # Creează o copie a bazei de date
  createdb total_app_test_migration -T total_app_db_test
  
  # Testează migrarea
  # (activează temporar Flyway în dev.properties)
  spring.flyway.enabled=true
  spring.jpa.hibernate.ddl-auto=validate
  
  ./mvnw spring-boot:run
  
  # Verifică rezultatul
  psql total_app_test_migration -c "\d+ table_name"
  
  # Revertează configurația DEV
  spring.flyway.enabled=false
  spring.jpa.hibernate.ddl-auto=update
  ```

### 3. Configuration

- [ ] **Environment variables sunt documentate**
  ```bash
  # Dacă ai adăugat variabile noi, documentează-le
  # În README.md sau într-un .env.example
  ```

- [ ] **Secrets nu sunt commit-ate**
  ```bash
  # Verifică că nu sunt parole/tokens în cod
  git diff --cached | grep -i "password\|secret\|token\|key"
  ```

- [ ] **Properties files sunt corecte**
  - `application-dev.properties` - pentru development
  - `application-prod.properties` - pentru production

### 4. Documentation

- [ ] **API documentation updated** (dacă e caz)
  - Endpoint-uri noi în `guides/`
  - Exemple în `api-tests.http`

- [ ] **README updated** (dacă e caz)
  - Caracteristici noi
  - Instrucțiuni de instalare
  - Environment variables

- [ ] **CHANGELOG updated** (dacă există)
  ```markdown
  ## [Version] - YYYY-MM-DD
  ### Added
  - New feature X
  ### Changed
  - Modified behavior Y
  ### Fixed
  - Bug Z
  ```

### 5. Git

- [ ] **Branch is up to date**
  ```bash
  git checkout main
  git pull origin main
  ```

- [ ] **Commit message urmează convenția**
  ```bash
  git commit -m "feat: add phone number to tenant"
  git commit -m "fix: resolve email sending issue"
  git commit -m "docs: update API documentation"
  ```

- [ ] **Toate fișierele sunt add-ate**
  ```bash
  git status
  # Asigură-te că ai add-at:
  # - Entitățile modificate
  # - Migrările Flyway
  # - Configurările
  # - Documentația
  ```

## 🔄 During Deployment

### 1. Backup Production Database

```bash
# Conectează-te la server și fă backup
pg_dump -U total_app_admin -h localhost total_app_db > backup_$(date +%Y%m%d_%H%M%S).sql

# Sau folosește backup automat (dacă există)
```

### 2. Push to Main

```bash
git push origin main
```

### 3. Monitor Deployment

- [ ] **Build succeeded** (CI/CD logs)
- [ ] **Container started** (Docker logs)
- [ ] **Flyway migrations applied**
  ```bash
  # Verifică logs pentru:
  # "Successfully applied X migrations"
  docker logs total_app | grep -i flyway
  ```
- [ ] **Application started successfully**
  ```bash
  # Verifică pentru:
  # "Started TotalAppApplication in X seconds"
  docker logs total_app | tail -50
  ```

### 4. Verify Database Schema

```bash
# Conectează-te la DB production
psql -U total_app_admin -h production_host -d total_app_db

# Verifică schema history
SELECT * FROM flyway_schema_history ORDER BY installed_rank DESC LIMIT 5;

# Verifică tabelele modificate
\d+ table_name

# Verifică datele
SELECT COUNT(*) FROM table_name;
```

## ✅ Post-Deployment (După Deployment)

### 1. Smoke Tests

- [ ] **Health check**
  ```bash
  curl http://api.donix.ro/actuator/health
  # Expected: {"status":"UP"}
  ```

- [ ] **Login test**
  ```bash
  curl -X POST http://api.donix.ro/auth/login \
    -H "Content-Type: application/json" \
    -d '{"username":"admin","password":"your_password"}'
  # Expected: 200 OK cu tokens
  ```

- [ ] **Endpoint-uri critice**
  ```bash
  # Testează endpoint-urile principale
  curl -H "Authorization: Bearer $TOKEN" http://api.donix.ro/tenants
  curl -H "Authorization: Bearer $TOKEN" http://api.donix.ro/buildings
  ```

### 2. Feature Testing

- [ ] **Noua funcționalitate funcționează**
  - Testează manual feature-ul nou
  - Verifică în UI (dacă există)
  - Testează edge cases

- [ ] **Funcționalități existente nu sunt afectate**
  - Testează workflow-uri principale
  - Verifică că nu sunt regresii

### 3. Monitoring

- [ ] **Verifică logs pentru erori**
  ```bash
  docker logs total_app --tail 100 | grep -i error
  docker logs total_app --tail 100 | grep -i exception
  ```

- [ ] **Verifică performance**
  - Response times OK
  - Memory usage normal
  - CPU usage normal

- [ ] **Verifică database connections**
  ```sql
  -- În PostgreSQL
  SELECT count(*) FROM pg_stat_activity WHERE datname = 'total_app_db';
  ```

### 4. Notifications

- [ ] **Notify team about deployment**
  ```markdown
  ✅ Deployment complete!
  
  Version: v1.2.3
  Changes:
  - Added phone number to tenant
  - Fixed email sending bug
  
  Database migrations applied: V2__add_phone_to_tenant.sql
  
  All systems operational.
  ```

- [ ] **Update status page** (dacă există)
- [ ] **Close Jira tickets** (dacă aplicabil)

## 🚨 Rollback Plan (În caz de probleme)

### Quick Rollback

```bash
# 1. Stop container-ul curent
docker stop total_app

# 2. Revert la versiunea anterioară
docker run -d --name total_app_rollback previous_image:tag

# 3. Sau rollback git
git revert HEAD
git push origin main
```

### Database Rollback (Dacă migrarea a eșuat)

```sql
-- ATENȚIE: Folosește doar dacă este absolut necesar!

-- 1. Verifică ce migrare a fost aplicată
SELECT * FROM flyway_schema_history ORDER BY installed_rank DESC LIMIT 1;

-- 2. Șterge migrarea din history (dacă nu a reușit complet)
DELETE FROM flyway_schema_history WHERE version = 'X';

-- 3. Revert manual modificările (dacă a aplicat parțial)
-- Exemplu:
ALTER TABLE tenant DROP COLUMN phone_number;

-- 4. Restore din backup (ultima opțiune)
psql -U total_app_admin -h localhost -d total_app_db < backup_20251027_120000.sql
```

## 📊 Metrics to Track

### Deployment Metrics

- [ ] **Deployment duration**: _____ minutes
- [ ] **Downtime**: _____ seconds (target: 0)
- [ ] **Migrations applied**: _____ (count)
- [ ] **Migration duration**: _____ seconds
- [ ] **Build size**: _____ MB

### Post-Deployment Metrics

- [ ] **Error rate**: _____ % (target: < 1%)
- [ ] **Response time**: _____ ms (target: < 500ms)
- [ ] **Database connections**: _____ (normal: < 20)
- [ ] **Memory usage**: _____ MB (normal: < 2GB)

## 📝 Deployment Log Template

```markdown
# Deployment Log - [DATE]

## Version
- **Release**: v1.2.3
- **Branch**: main
- **Commit**: abc123def

## Changes
- feat: add phone number to tenant
- fix: resolve email attachment issue

## Database Migrations
- V2__add_phone_to_tenant.sql - ✅ Success (0.5s)

## Deployment Timeline
- 14:00 - Backup created
- 14:05 - Push to main
- 14:07 - Build started
- 14:10 - Build completed
- 14:11 - Container restarted
- 14:12 - Migrations applied
- 14:13 - Application started
- 14:15 - Smoke tests passed
- 14:20 - Feature tests passed
- 14:25 - Deployment complete ✅

## Issues Encountered
- None / [Describe any issues]

## Rollback Required
- No / Yes - [Reason]

## Next Steps
- Monitor for 24h
- Update documentation
```

## 🎯 Quick Reference

### Essential Commands

```bash
# Pre-deployment
./mvnw test
./mvnw clean package
./scripts/create-migration.ps1 "description"

# Deployment
git push origin main

# Post-deployment
docker logs total_app --tail 100
curl http://api.donix.ro/actuator/health
psql -c "SELECT * FROM flyway_schema_history;"

# Rollback
docker stop total_app
docker start previous_container
```

### Critical Files to Review

```
✅ src/main/resources/db/migration/V*__*.sql
✅ src/main/resources/application-prod.properties
✅ pom.xml (dependencies)
✅ Dockerfile (if changed)
```

---

**Folosește acest checklist pentru fiecare deployment pentru a asigura consistență și calitate!** 🚀

