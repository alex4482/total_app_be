# File Storage Architecture

## Overview

This application uses a **filesystem-based storage** approach for file content, with only **metadata stored in PostgreSQL**.

## Architecture

```
┌─────────────────────────────┐
│      PostgreSQL DB          │
│   (file_asset table)        │
│                             │
│  - id (UUID)                │
│  - owner_type               │
│  - owner_id                 │
│  - original_filename        │
│  - content_type             │
│  - size_bytes               │
│  - checksum (SHA-256)       │
│  - uploaded_at              │
│  - modified_at              │
└─────────────────────────────┘
         │
         │ (metadata only)
         │
         ▼
┌─────────────────────────────┐
│     Filesystem              │
│     /FISIERE/               │
│                             │
│  - Actual file content      │
│  - Organized by owner type  │
│  - Path templates used      │
└─────────────────────────────┘
```

## Why Filesystem Storage?

### ✅ Advantages

1. **Performance**
   - Much faster reads (10-100x faster than BLOB)
   - No database overhead for large files
   - OS-level caching

2. **Scalability**
   - Database stays small and fast
   - Easy to move to NFS, S3, or CDN
   - No database size limits

3. **Maintainability**
   - Files visible on disk (easy debugging)
   - Simple backup strategy (separate DB and files)
   - Can use standard filesystem tools

4. **Cost**
   - Database backups are small and fast
   - Cheaper storage (disk vs database storage)
   - Less memory usage

### ❌ Why NOT BLOB Storage?

1. **Hibernate Issues**
   - `Unable to access lob stream` errors
   - Lazy loading problems after transaction closes
   - Memory leaks with large files

2. **Performance Problems**
   - Every query loads BLOB (even if EAGER)
   - Slow database operations
   - High memory consumption

3. **Size Limits**
   - PostgreSQL: ~1GB per BLOB
   - MySQL: 16MB-4GB depending on config
   - Filesystem: practically unlimited

## File Path Structure

Files are organized using **path templates** defined in `application.properties`:

```properties
app.storage.templates.TENANT=CHIRIASI/{tenantName}/
app.storage.templates.BUILDING=CLADIRI/{buildingLocation}/{buildingName}/
app.storage.templates.ROOM=CLADIRI/{buildingLocation}/{buildingName}/INCAPERI/{roomName}/
app.storage.templates.RENTAL_SPACE=CLADIRI/{buildingLocation}/{buildingName}/SPATII-CHIRIE/{roomName}/
app.storage.templates.EMAIL_DATA=EMAILURI/{emailThreadKey}/
app.storage.templates.BUILDING_LOCATION=LOCATII/{buildingLocation}/
app.storage.templates.FIRM=FIRME/{firmName}/
app.storage.templates.CAR=MASINI/{licensePlate}/
app.storage.templates.OTHER=ALTELE/{ownerType}/{ownerId}/
```

### Example Path

```
/FISIERE/CHIRIASI/Ion_Popescu/contract_123.pdf
```

For file with:
- Owner Type: `TENANT`
- Owner ID: `42`
- Tenant Name: `Ion Popescu`
- Filename: `contract_123.pdf`

## Database Schema

### `file_asset` Table

```sql
CREATE TABLE file_asset (
    id UUID PRIMARY KEY,
    owner_type VARCHAR(32) NOT NULL,
    owner_id BIGINT NOT NULL,
    original_filename VARCHAR(255) NOT NULL,
    content_type VARCHAR(255),
    size_bytes BIGINT,
    checksum VARCHAR(64),
    modified_at TIMESTAMP,
    uploaded_at TIMESTAMP NOT NULL,
    
    CONSTRAINT uq_owner_name UNIQUE (owner_type, owner_id, original_filename),
    CONSTRAINT uq_owner_checksum UNIQUE (owner_type, owner_id, checksum)
);
```

**Note:** The `data` BLOB column was removed in migration `V4__remove_blob_column.sql`.

## File Operations

### Upload Flow

1. **Temporary Stage**
   - File uploaded to `/TEMP/` directory
   - `TempUpload` record created in DB
   - Expires after 6 hours if not committed

2. **Commit**
   - Validate owner and permissions
   - Check for duplicates (by checksum)
   - Create `FileAsset` metadata record in DB
   - Move file from `/TEMP/` to permanent location
   - Delete `TempUpload` record

3. **Deduplication**
   - If file with same checksum exists for same owner
   - Return existing file (no duplicate created)
   - Delete temporary file

### Download Flow

1. Lookup `FileAsset` by ID in database
2. Build file path using `FileSystemHelper`
3. Read file content from filesystem
4. Stream to client

### Delete Flow

1. Delete `FileAsset` record from database
2. Delete file from filesystem
3. If delete fails, log warning (orphaned file)

## Configuration

```properties
# Base directory for file storage
app.storage.baseDir=/FISIERE

# Temporary upload directory
app.storage.tempDir=/TEMP
```

## Backup Strategy

### Database Backup
```bash
# Small and fast - only metadata
pg_dump -Fc mydb > backup_metadata.dump
```

### File Backup
```bash
# Separate backup for file content
rsync -av /FISIERE/ /backup/files/
# Or use backup system (e.g., BackupService.java)
```

## Migration Notes

### V4 Migration (2025-11-04)

**Removed BLOB storage:**
- Dropped `data` column from `file_asset` table
- All existing files already on filesystem
- No data migration needed (column was always NULL)

**Impact:**
- Database size reduced significantly
- Queries are faster
- No more Hibernate LOB errors
- Cleaner separation of concerns

## API Endpoints

### File Upload
```
POST /api/files/temp
→ Returns tempId for later commit
```

### File Commit
```
POST /api/files/commit
→ Moves from temp to permanent storage
```

### File Download
```
GET /api/files/{fileId}
→ Streams file from filesystem
```

### File List
```
GET /api/files?ownerType=TENANT&ownerId=123
→ Returns metadata list
```

## Error Handling

### File Not Found on Filesystem
- Return 404 to client
- Log warning with file ID and expected path
- Keep metadata in DB (can be manually restored)

### Orphaned Files
- Files on disk without DB record
- Cleaned by periodic cleanup job
- Or manually identified and removed

### Missing Files
- DB record exists but file missing
- Return 404 on download
- Can be restored from backup

## Security

### Path Validation
- All paths validated to be within `baseDir`
- No directory traversal attacks (`../` blocked)
- Owner permissions checked before access

### File Access Control
- File ownership enforced by `owner_type` and `owner_id`
- User must have access to owner entity
- Checked in controller layer

## Performance Tips

1. **OS Cache**: Files frequently accessed are cached by OS
2. **NGINX**: Can serve files directly (X-Accel-Redirect)
3. **CDN**: Can move to CDN for public files
4. **Compression**: GZIP files before storage if needed

## Future Enhancements

- [ ] Move to S3/MinIO for cloud storage
- [ ] Implement CDN integration
- [ ] Add thumbnail generation for images
- [ ] Virus scanning on upload
- [ ] File versioning support

