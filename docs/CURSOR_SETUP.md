# 🎯 Rulare și Testare în Cursor IDE

Ghid rapid pentru a rula și testa aplicația **Total App** direct în Cursor.

## 📋 Prerequisite Rapide

### 1. **JDK 21**
```bash
# Verifică versiunea
java -version
```
Dacă nu ai JDK 21, descarcă de la: https://adoptium.net/

### 2. **PostgreSQL**
```bash
# Verifică dacă rulează
# Windows PowerShell:
Get-Service postgresql*

# Git Bash/Linux:
pg_isready -h localhost -p 5432
```

### 3. **Creează Database**
```sql
-- Conectează-te la PostgreSQL
psql -U postgres

-- Creează baza de date
CREATE DATABASE total_app_db_test;
\q
```

### 4. **Actualizează Parola DB**
Editează `src/main/resources/application-dev.properties`:
```properties
spring.datasource.password=PAROLA_TA_AICI  # <-- Schimbă asta!
```

## 🚀 Metode de Rulare în Cursor

### Metoda 1: Script Rapid (Recomandat! ⭐)

**Windows PowerShell:**
```powershell
.\run.ps1
```

**Git Bash / Linux:**
```bash
chmod +x run.sh
./run.sh
```

### Metoda 2: Tasks Cursor (Ctrl+Shift+P)

1. Apasă `Ctrl+Shift+P` (Command Palette)
2. Tastează: `Tasks: Run Task`
3. Alege:
   - **🚀 Run Spring Boot App (Dev)** - pornește aplicația
   - **🧹 Clean & Build** - curăță și build-uiește
   - **🧪 Run Tests** - rulează testele
   - **📦 Package JAR** - creează JAR executabil

### Metoda 3: Terminal Integrat

Deschide terminalul în Cursor (`Ctrl+` ` sau `View → Terminal`):

```bash
# Windows
.\mvnw.cmd spring-boot:run

# Linux/Mac/Git Bash
./mvnw spring-boot:run
```

### Metoda 4: Debug Mode 🐛

1. Apasă `F5` sau du-te la **Run and Debug** (Ctrl+Shift+D)
2. Selectează: **🚀 Debug Spring Boot App**
3. Click pe **Start Debugging** (butonul verde ▶)

**Avantaje Debug:**
- Pui breakpoint-uri (click în marginea stângă pe numărul liniei)
- Inspectezi variabile în timp real
- Execuție pas cu pas (F10, F11)

## 🧪 Testare API-uri

### Opțiunea 1: REST Client (Recomandat în Cursor!)

1. **Instalează extensia** (dacă nu o ai):
   - Apasă `Ctrl+Shift+X`
   - Caută: **REST Client**
   - Instalează (by Huachao Mao)

2. **Folosește fișierul de test**:
   - Deschide `api-tests.http`
   - Click pe **Send Request** deasupra fiecărui request
   - Vezi răspunsul în panoul din dreapta

**Exemplu rapid:**
```http
### Test dacă server-ul rulează
GET http://localhost:8080/ HTTP/1.1

### Login pentru a obține JWT token
POST http://localhost:8080/api/auth/login HTTP/1.1
Content-Type: application/json

{
  "username": "admin",
  "password": "admin123"
}
```

### Opțiunea 2: cURL din Terminal

```bash
# Test server
curl http://localhost:8080/

# Login
curl -X POST http://localhost:8080/api/auth/login \
  -H "Content-Type: application/json" \
  -d '{"username":"admin","password":"admin123"}'

# Get tenants (cu token)
curl http://localhost:8080/api/tenants \
  -H "Authorization: Bearer YOUR_JWT_TOKEN"
```

### Opțiunea 3: Postman / Insomnia

Import collection din `api-tests.http` sau creează manual requesturile.

## 🔥 Development Workflow

### 1. Pornește aplicația în Debug mode
```
F5 sau Ctrl+Shift+D → Start Debugging
```

### 2. Fă modificări în cod

### 3. Hot Reload (fără restart)

**Opțiunea A - Spring Boot DevTools** (recomandată):

Adaugă în `pom.xml`:
```xml
<dependency>
    <groupId>org.springframework.boot</groupId>
    <artifactId>spring-boot-devtools</artifactId>
    <scope>runtime</scope>
    <optional>true</optional>
</dependency>
```

Apoi doar salvează fișierul (`Ctrl+S`) și aplicația se reîncarcă automat! 🎉

**Opțiunea B - Rebuild manual:**
```
Ctrl+Shift+P → Tasks: Run Task → 🧹 Clean & Build
```
Apoi restart aplicația.

## 📝 Extensii Utile pentru Cursor

Recomandări pentru development Java/Spring Boot:

### Essential:
1. **Extension Pack for Java** - suport complet Java
2. **Spring Boot Extension Pack** - tools pentru Spring
3. **REST Client** - testare API-uri
4. **Lombok Annotations Support** - pentru Lombok

### Optional dar utile:
5. **Database Client** - vizualizare PostgreSQL
6. **GitLens** - Git enhanced
7. **Error Lens** - erori inline

**Instalare rapidă:**
```
Ctrl+Shift+X → caută numele → Install
```

## 🎨 Shortcuts Utile în Cursor

| Shortcut | Acțiune |
|----------|---------|
| `Ctrl+Shift+P` | Command Palette (comenzi) |
| `Ctrl+` ` | Toggle Terminal |
| `F5` | Start Debug |
| `Shift+F5` | Stop Debug |
| `Ctrl+Shift+D` | Run & Debug panel |
| `Ctrl+Shift+B` | Run Build Task |
| `Ctrl+K Ctrl+T` | Change Color Theme |
| `Ctrl+B` | Toggle Sidebar |
| `Ctrl+J` | Toggle Bottom Panel |

## 📊 Monitorizare și Logs

### Console Logs
Când aplicația rulează, vezi logurile direct în terminalul Cursor.

### Log Files
```
logs/app.log
```
Deschide în Cursor pentru a vedea logurile persistente.

### Database
Folosește pgAdmin 4 sau extensia **Database Client** în Cursor:

1. Instalează **Database Client JDBC**
2. Click pe iconița de database din sidebar
3. Add Connection:
   - Type: PostgreSQL
   - Host: localhost
   - Port: 5432
   - Database: total_app_db_test
   - User: postgres
   - Password: parola ta

## 🛠️ Comenzi Maven Rapide

În terminal Cursor:

```bash
# Curăță proiectul
./mvnw clean

# Build complet
./mvnw clean install

# Build fără teste (rapid)
./mvnw clean install -DskipTests

# Doar testele
./mvnw test

# Package în JAR
./mvnw package

# Rulează aplicația
./mvnw spring-boot:run

# Update dependencies
./mvnw dependency:resolve

# Verifică ce versiune Maven
./mvnw -version
```

## 🐛 Debugging Tips

### 1. Pune Breakpoint-uri
- Click în marginea stângă pe linia unde vrei să oprești
- Sau plasează cursorul și apasă `F9`

### 2. Step Through Code
- `F10` - Step Over (sari peste funcție)
- `F11` - Step Into (intră în funcție)
- `Shift+F11` - Step Out (ieși din funcție)

### 3. Inspectează Variabile
În Debug mode:
- **Variables** panel - vezi toate variabilele
- Hover peste variabile pentru preview
- Click dreapta → **Add to Watch** pentru monitorizare permanentă

### 4. Debug Console
Evaluează expresii în timpul debug-ului:
- Panelul **Debug Console**
- Scrie orice expresie Java și vezi rezultatul

## ❓ Troubleshooting

### Aplicația nu pornește?

**Verifică:**
```bash
# 1. Java instalat?
java -version

# 2. PostgreSQL rulează?
psql -U postgres -c "SELECT version();"

# 3. Baza de date există?
psql -U postgres -l | grep total_app

# 4. Portul 8080 liber?
netstat -ano | findstr :8080  # Windows
lsof -i :8080  # Linux/Mac
```

### Port 8080 ocupat?

Schimbă portul în `application.properties`:
```properties
server.port=8081
```

### Maven wrapper nu funcționează?

**Windows:**
```powershell
# Asigură-te că ai permisiuni
Set-ExecutionPolicy -Scope CurrentUser -ExecutionPolicy RemoteSigned

# Sau rulează direct cu Maven (dacă e instalat global)
mvn spring-boot:run
```

**Linux/Mac:**
```bash
# Dă permisiuni de execuție
chmod +x mvnw
./mvnw spring-boot:run
```

### Lombok nu funcționează?

1. Verifică că ai extensia **Lombok Annotations Support**
2. Restart Cursor
3. Rebuild proiectul: `Ctrl+Shift+P` → `Java: Clean Java Language Server Workspace`

## 🎯 Quick Reference

### Start și Stop

```bash
# Start
.\run.ps1                          # PowerShell
./run.sh                           # Bash
# SAU
F5 în Cursor                       # Debug mode

# Stop
Ctrl+C în terminal                 # Terminal
Shift+F5                          # Debug mode
```

### Test Rapid

```bash
# 1. Pornește app
.\run.ps1

# 2. În alt terminal/browser
curl http://localhost:8080/

# 3. Test API cu REST Client
# Deschide api-tests.http și click "Send Request"
```

## 📚 Resurse

- **Spring Boot Docs**: https://docs.spring.io/spring-boot/
- **Cursor Docs**: https://cursor.sh/docs
- **REST Client Extension**: https://marketplace.visualstudio.com/items?itemName=humao.rest-client

---

## 🎉 TL;DR - Start în 30 secunde

```bash
# 1. Asigură-te că PostgreSQL rulează și ai creat database-ul

# 2. Actualizează parola în application-dev.properties

# 3. În Cursor:
.\run.ps1

# 4. Testează:
curl http://localhost:8080/

# SAU deschide api-tests.http și click "Send Request"
```

**Gata! Aplicația rulează pe http://localhost:8080 🚀**

