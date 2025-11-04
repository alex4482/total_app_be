# 📦 Backup și Restore - Documentație

## Prezentare Generală

Sistemul de backup oferă funcționalități complete pentru salvarea și restaurarea bazei de date.

### Caracteristici:
- ✅ Backup automat periodic (la fiecare 5 zile)
- ✅ Backup manual prin API
- ✅ Export în format JSON (complet, pentru restaurare exactă)
- ✅ Export în format Excel (ușor de citit și modificat)
- ✅ Upload automat la Google Drive (opțional)
- ✅ Restaurare din backup JSON sau Excel
- ✅ Descărcare backup-uri ca ZIP sau Excel
- ✅ Metadata despre fiecare backup în baza de date

---

## 🔧 Configurare

### application.properties

```properties
# Backup configuration
app.backup.local-dir=/DATA/backups
app.backup.google-drive.enabled=false
app.backup.google-drive.folder-id=
app.backup.google-drive.credentials-path=
```

### Configurare Google Drive (Opțional)

Pentru a activa upload-ul automat la Google Drive:

1. **Activează Google Drive API** în Google Cloud Console
2. **Creează credențiale OAuth2** și descarcă fișierul JSON
3. **Actualizează configurația**:
```properties
app.backup.google-drive.enabled=true
app.backup.google-drive.folder-id=YOUR_FOLDER_ID
app.backup.google-drive.credentials-path=/path/to/credentials.json
```

4. **Adaugă dependințe în pom.xml** (dacă vrei integrare completă):
```xml
<dependency>
    <groupId>com.google.apis</groupId>
    <artifactId>google-api-services-drive</artifactId>
    <version>v3-rev20220815-2.0.0</version>
</dependency>
<dependency>
    <groupId>com.google.auth</groupId>
    <artifactId>google-auth-library-oauth2-http</artifactId>
    <version>1.19.0</version>
</dependency>
```

---

## 📡 API Endpoints

### 1. **Listare Backup-uri**

**GET** `/api/backups`

Returnează lista tuturor backup-urilor cu metadata.

**Response:**
```json
{
  "success": true,
  "data": [
    {
      "id": 1,
      "backupName": "backup_2025-11-03_14-30-15_manual",
      "backupType": "MANUAL",
      "createdAt": "2025-11-03T14:30:15Z",
      "localPath": "/DATA/backups/backup_2025-11-03_14-30-15_manual.zip",
      "googleDriveFileId": "gdrive_1234567890",
      "sizeBytes": 52428800,
      "description": "Manual backup"
    }
  ]
}
```

---

### 2. **Creare Backup Manual**

**POST** `/api/backups/create`

Creează un backup manual și îl salvează local + Google Drive (dacă e configurat).

**Query Parameters:**
- `format` (optional): `zip` (default) sau `excel` - determină ce fișier e returnat
- `description` (optional): Descriere pentru backup
- `returnFile` (optional): `true` (default) sau `false` - returnează fișierul sau doar metadata

**Exemple:**

```bash
# Creează backup și returnează ZIP-ul
POST /api/backups/create?format=zip&description=Backup%20inainte%20de%20update

# Creează backup și returnează Excel-ul
POST /api/backups/create?format=excel&description=Backup%20pentru%20raport

# Creează backup și returnează doar metadata (nu descarcă fișierul)
POST /api/backups/create?returnFile=false
```

**Response (când returnFile=false):**
```json
{
  "success": true,
  "data": {
    "id": 2,
    "backupName": "backup_2025-11-03_15-00-00_manual",
    "backupType": "MANUAL",
    "createdAt": "2025-11-03T15:00:00Z",
    "localPath": "/DATA/backups/backup_2025-11-03_15-00-00_manual.zip",
    "sizeBytes": 52428800
  }
}
```

**Response (când returnFile=true):**
Descarcă direct fișierul (ZIP sau Excel).

---

### 3. **Descărcare Backup ca ZIP**

**GET** `/api/backups/{backupName}/download/zip`

Descarcă backup-ul specificat ca fișier ZIP.

**Exemplu:**
```bash
GET /api/backups/backup_2025-11-03_14-30-15_manual/download/zip
```

**Response:** Fișier ZIP cu structura:
```
backup_2025-11-03_14-30-15_manual.zip
├── json/
│   ├── tenants.json
│   ├── buildings.json
│   ├── rooms.json
│   ├── rental_spaces.json
│   ├── index_counters.json
│   ├── index_data.json
│   ├── email_presets.json
│   ├── file_assets.json
│   └── tenant_rental_data.json
└── backup.xlsx
```

---

### 4. **Descărcare Backup ca Excel**

**GET** `/api/backups/{backupName}/download/excel`

Descarcă doar fișierul Excel din backup.

**Exemplu:**
```bash
GET /api/backups/backup_2025-11-03_14-30-15_manual/download/excel
```

**Response:** Fișier Excel (.xlsx) cu foi (sheets):
- Tenants
- Buildings
- Rooms
- RentalSpaces
- IndexCounters
- IndexData
- EmailPresets
- TenantRentalData
- FileAssets

---

### 5. **Restaurare Bază de Date**

**POST** `/api/backups/restore`

⚠️ **ATENȚIE**: Această operațiune **șterge toate datele existente** și le înlocuiește cu cele din backup!

✅ **SIGURANȚĂ**: Înainte de a șterge datele, sistemul creează automat un **safety backup**. Dacă restaurarea eșuează, datele sunt restaurate automat la starea anterioară (automatic rollback)!

**Request Body:**
```json
{
  "backupName": "backup_2025-11-03_14-30-15_manual",
  "fromExcel": false
}
```

**Parametri:**
- `backupName`: Numele backup-ului de restaurat
- `fromExcel`: 
  - `false` (default): Restaurează din fișierele JSON (restaurare completă și exactă)
  - `true`: Restaurează din fișierul Excel (permite modificări manuale)

**Response (Success):**
```json
{
  "success": true,
  "data": "Database restored successfully from backup: backup_2025-11-03_14-30-15_manual. Safety backup available: backup_2025-11-03_15-00-00_manual"
}
```

**Response (Failure with Rollback):**
```json
{
  "success": false,
  "error": "Restore failed but database was rolled back to previous state. Error: Invalid JSON format in tenants.json"
}
```

**Response (Critical Failure):**
```json
{
  "success": false,
  "error": "CRITICAL: Restore failed AND rollback failed! Safety backup available at: backup_2025-11-03_15-00-00_manual. Original error: ..."
}
```

### Mecanismul de Siguranță (Safety Backup & Rollback)

Procesul de restaurare include un mecanism automat de protecție:

1. **Înainte de restaurare**: Se creează automat un "safety backup" cu datele curente
2. **În timpul restaurării**: Datele sunt șterse și înlocuite cu cele din backup
3. **Dacă restaurarea eșuează**: 
   - Sistemul detectează eroarea
   - Face **automatic rollback** la safety backup
   - Datele tale sunt restaurate la starea de dinainte
4. **Dacă totul merge bine**: Safety backup-ul rămâne disponibil pentru siguranță

**Scenarii posibile:**
- ✅ **Restaurare reușită**: Datele sunt restaurate, safety backup disponibil
- ⚠️ **Restaurare eșuată**: Rollback automat, datele tale sunt în siguranță
- 🔴 **Rollback eșuat** (extrem de rar): Safety backup disponibil pentru restaurare manuală

---

### 6. **Șterge Backup**

**DELETE** `/api/backups/{backupName}`

Șterge un backup (local și din Google Drive dacă există).

**Exemplu:**
```bash
DELETE /api/backups/backup_2025-11-03_14-30-15_manual
```

**Response:**
```json
{
  "success": true,
  "data": "Backup deleted successfully"
}
```

---

## 🤖 Backup Automat

### Programare

Backup-ul automat rulează conform cron expression:
```
0 0 2 */5 * ?
```
**Tradus:** La ora 02:00 AM, la fiecare 5 zile.

### Proces Automat

1. Creează backup cu tipul `AUTOMATIC`
2. Salvează local în `/DATA/backups/`
3. Dacă Google Drive este configurat:
   - Uploadează ZIP-ul la Google Drive
   - Salvează `googleDriveFileId` în metadata

### Modificare Frecvență

Pentru a schimba frecvența backup-ului automat, editează:

**Fișier:** `src/main/java/com/work/total_app/jobs/BackupScheduledJob.java`

```java
@Scheduled(cron = "0 0 2 */5 * ?")  // La fiecare 5 zile
// sau
@Scheduled(cron = "0 0 2 * * ?")     // Zilnic la 02:00
// sau
@Scheduled(cron = "0 0 2 * * SUN")   // Duminică la 02:00
```

---

## 📊 Structura Backup-ului

### Format JSON

Fiecare entitate este salvată într-un fișier JSON separat:
- `tenants.json` - Chiriași
- `buildings.json` - Clădiri
- `rooms.json` - Încăperi
- `rental_spaces.json` - Spații de închiriat
- `index_counters.json` - Contoare
- `index_data.json` - Citiri contoare
- `email_presets.json` - Template-uri email
- `file_assets.json` - Metadata fișiere
- `tenant_rental_data.json` - Date contracte chirie

### Format Excel

Un singur fișier Excel cu foi (sheets) pentru fiecare tip de date.
Ideal pentru:
- Vizualizare rapidă a datelor
- Modificări manuale înainte de restaurare
- Raportare și analiză

---

## 🔄 Workflow-uri Recomandate

### 1. Backup Înainte de Update Major

```bash
# Creează backup manual
curl -X POST "http://localhost:8080/api/backups/create?description=Inainte%20de%20update%20v2.0&returnFile=false" \
  -H "Authorization: Bearer YOUR_TOKEN"

# Salvează numele backup-ului returnat pentru restaurare rapidă
```

### 2. Export Date pentru Raport

```bash
# Descarcă Excel pentru a vedea datele
curl -X GET "http://localhost:8080/api/backups/backup_2025-11-03_14-30-15_manual/download/excel" \
  -H "Authorization: Bearer YOUR_TOKEN" \
  -o backup_raport.xlsx
```

### 3. Modificare Date prin Excel + Restaurare

```bash
# 1. Creează backup
curl -X POST "http://localhost:8080/api/backups/create?format=excel" \
  -H "Authorization: Bearer YOUR_TOKEN" \
  -o backup_to_modify.xlsx

# 2. Modifică Excel-ul local

# 3. Upload-ul Excel-ului modificat + Restaurare
# (Trebuie să uploadezi Excel-ul înapoi și să restaurezi)
curl -X POST "http://localhost:8080/api/backups/restore" \
  -H "Authorization: Bearer YOUR_TOKEN" \
  -H "Content-Type: application/json" \
  -d '{"backupName": "backup_2025-11-03_14-30-15_manual", "fromExcel": true}'
```

### 4. Restaurare după Eroare

```bash
# Restaurează din ultimul backup (are rollback automat dacă eșuează)
curl -X POST "http://localhost:8080/api/backups/restore" \
  -H "Authorization: Bearer YOUR_TOKEN" \
  -H "Content-Type: application/json" \
  -d '{"backupName": "backup_2025-11-03_14-30-15_manual", "fromExcel": false}'

# Dacă restaurarea eșuează, datele tale vor fi restaurate automat
# Dacă primești eroare CRITICAL (extrem de rar), restaurează manual din safety backup:
curl -X POST "http://localhost:8080/api/backups/restore" \
  -H "Authorization: Bearer YOUR_TOKEN" \
  -H "Content-Type: application/json" \
  -d '{"backupName": "backup_2025-11-03_15-00-00_manual", "fromExcel": false}'
```

---

## ⚠️ Precauții

1. **Restaurarea șterge toate datele curente** - dar creează automat safety backup și face rollback dacă eșuează
2. **Safety backups** - sunt create automat la fiecare restore; poți să le ștergi manual după ce verifici că totul e OK
3. **Backup-urile consumă spațiu** - monitorizează `/DATA/backups/` (include și fișierele fizice din `/FISIERE/`)
4. **Google Drive are limite** - verifică quota-ul disponibilă (backup-urile cu multe fișiere pot fi mari)
5. **Excel nu salvează toate relațiile** - pentru restaurare completă folosește JSON
6. **Fișierele fizice sunt incluse** - backup-ul include TOATE fișierele din `/FISIERE/`

---

## 🐛 Debugging

### Verificare Backup-uri

```bash
# Listează toate backup-urile
curl -X GET "http://localhost:8080/api/backups" \
  -H "Authorization: Bearer YOUR_TOKEN"
```

### Log-uri

Caută în log-uri pentru:
```
[BackupService] Starting backup creation
[BackupScheduledJob] Starting automatic backup
[RestoreService] Starting restore from backup
```

### Verificare Spațiu Disk

```bash
du -sh /DATA/backups/
```

---

## 📝 Note

- Backup-urile sunt comprimate în format ZIP pentru a economisi spațiu
- Metadata despre backup-uri este stocată în tabela `backup_metadata`
- Fișierele JSON folosesc format pretty-printed pentru lizibilitate
- Excel folosește autosize pentru coloane
- Google Drive integration este opțională și poate fi dezactivată

---

**Ultima actualizare:** 03 Noiembrie 2025

