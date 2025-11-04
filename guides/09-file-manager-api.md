# File Manager API Documentation

## Overview

The File Manager API provides endpoints for browsing and managing the file system structure. It allows you to retrieve hierarchical file trees, flat file lists, and storage statistics.

All files are stored in the base directory configured by `app.storage.baseDir` (default: `/FISIERE`).

## Base URL

```
/api/file-manager
```

## Endpoints

### 1. Get Complete File Tree

Retrieves the complete hierarchical structure of all files and folders.

**Endpoint:** `GET /api/file-manager/tree`

**Response:**
```json
{
  "success": true,
  "data": {
    "name": "FISIERE",
    "path": "/",
    "type": "folder",
    "size": null,
    "lastModified": "2025-11-04T00:00:00Z",
    "extension": null,
    "mimeType": null,
    "itemCount": 3,
    "expanded": null,
    "children": [
      {
        "name": "documents",
        "path": "/documents",
        "type": "folder",
        "size": null,
        "lastModified": "2025-11-04T00:00:00Z",
        "extension": null,
        "mimeType": null,
        "itemCount": 2,
        "children": [
          {
            "name": "report.pdf",
            "path": "/documents/report.pdf",
            "type": "file",
            "size": 1024567,
            "lastModified": "2025-11-04T00:00:00Z",
            "extension": "pdf",
            "mimeType": "application/pdf",
            "children": []
          }
        ]
      },
      {
        "name": "images",
        "path": "/images",
        "type": "folder",
        "size": null,
        "lastModified": "2025-11-04T00:00:00Z",
        "itemCount": 5,
        "children": [...]
      }
    ]
  }
}
```

### 2. Get File Tree for Specific Path

Retrieves the file tree starting from a specific subdirectory.

**Endpoint:** `GET /api/file-manager/tree/path`

**Query Parameters:**
- `path` (optional, default: ""): Relative path from storage base directory

**Examples:**
```
GET /api/file-manager/tree/path?path=documents
GET /api/file-manager/tree/path?path=documents/2025
GET /api/file-manager/tree/path (empty path = root)
```

**Response:** Same structure as complete file tree, but starting from the specified path.

**Security:** The API validates that the requested path is within the storage directory. Attempts to access paths outside the storage directory will result in a 403 Forbidden error.

### 3. Get All Files (Flat List)

Retrieves a flat list of all files without folder structure. Useful for searching or displaying a simple file list.

**Endpoint:** `GET /api/file-manager/files`

**Response:**
```json
{
  "success": true,
  "data": [
    {
      "name": "report.pdf",
      "path": "/documents/report.pdf",
      "type": "file",
      "size": 1024567,
      "lastModified": "2025-11-04T00:00:00Z",
      "extension": "pdf",
      "mimeType": "application/pdf",
      "children": []
    },
    {
      "name": "photo.jpg",
      "path": "/images/photo.jpg",
      "type": "file",
      "size": 2048000,
      "lastModified": "2025-11-03T12:30:00Z",
      "extension": "jpg",
      "mimeType": "image/jpeg",
      "children": []
    }
  ]
}
```

**Note:** This endpoint only returns files, not folders. The list is sorted by path.

### 4. Get Storage Statistics

Returns information about total storage usage, file count, and folder count.

**Endpoint:** `GET /api/file-manager/stats`

**Response:**
```json
{
  "success": true,
  "data": {
    "totalSizeBytes": 52428800,
    "fileCount": 127,
    "folderCount": 15
  }
}
```

**Fields:**
- `totalSizeBytes`: Total size of all files in bytes
- `fileCount`: Number of files
- `folderCount`: Number of folders (excluding root)

## Data Models

### FileTreeNodeDto

Represents a file or folder in the tree structure.

**Fields:**

| Field | Type | Description |
|-------|------|-------------|
| `name` | string | Name of the file or folder |
| `path` | string | Full path relative to storage base directory (starts with `/`) |
| `type` | string | Either `"file"` or `"folder"` |
| `size` | number \| null | File size in bytes (null for folders) |
| `lastModified` | string (ISO 8601) | Last modified timestamp |
| `extension` | string \| null | File extension in lowercase (null for folders) |
| `mimeType` | string \| null | MIME type (null for folders or if cannot be determined) |
| `children` | FileTreeNodeDto[] | Array of child nodes (empty for files) |
| `expanded` | boolean \| null | Optional field for UI state management |
| `itemCount` | number \| null | Number of items in folder (null for files) |

### StorageStatsDto

Storage statistics information.

**Fields:**

| Field | Type | Description |
|-------|------|-------------|
| `totalSizeBytes` | number | Total size of all files in bytes |
| `fileCount` | number | Total number of files |
| `folderCount` | number | Total number of folders |

## TypeScript Interfaces

```typescript
interface FileTreeNodeDto {
  name: string;
  path: string;
  type: 'file' | 'folder';
  size: number | null;
  lastModified: string; // ISO 8601 timestamp
  extension: string | null;
  mimeType: string | null;
  children: FileTreeNodeDto[];
  expanded?: boolean | null;
  itemCount?: number | null;
}

interface StorageStatsDto {
  totalSizeBytes: number;
  fileCount: number;
  folderCount: number;
}

interface ApiResponse<T> {
  success: boolean;
  data?: T;
  error?: string;
}
```

## Frontend Integration Examples

### React Example - File Tree Component

```typescript
import React, { useEffect, useState } from 'react';
import axios from 'axios';

interface FileTreeProps {
  onFileSelect?: (file: FileTreeNodeDto) => void;
}

const FileTree: React.FC<FileTreeProps> = ({ onFileSelect }) => {
  const [tree, setTree] = useState<FileTreeNodeDto | null>(null);
  const [loading, setLoading] = useState(true);
  const [error, setError] = useState<string | null>(null);

  useEffect(() => {
    loadFileTree();
  }, []);

  const loadFileTree = async () => {
    try {
      setLoading(true);
      const response = await axios.get<ApiResponse<FileTreeNodeDto>>(
        '/api/file-manager/tree'
      );
      
      if (response.data.success) {
        setTree(response.data.data);
      } else {
        setError(response.data.error || 'Failed to load file tree');
      }
    } catch (err) {
      setError('Error loading file tree');
      console.error(err);
    } finally {
      setLoading(false);
    }
  };

  const renderNode = (node: FileTreeNodeDto, level: number = 0) => {
    const isFolder = node.type === 'folder';
    const icon = isFolder ? '📁' : '📄';
    
    return (
      <div key={node.path} style={{ marginLeft: level * 20 }}>
        <div
          onClick={() => !isFolder && onFileSelect?.(node)}
          style={{ cursor: isFolder ? 'default' : 'pointer' }}
        >
          {icon} {node.name}
          {isFolder && ` (${node.itemCount} items)`}
          {!isFolder && ` (${formatFileSize(node.size)})`}
        </div>
        {isFolder && node.children.map(child => renderNode(child, level + 1))}
      </div>
    );
  };

  const formatFileSize = (bytes: number | null): string => {
    if (bytes === null) return '';
    if (bytes < 1024) return bytes + ' B';
    if (bytes < 1024 * 1024) return (bytes / 1024).toFixed(1) + ' KB';
    return (bytes / (1024 * 1024)).toFixed(1) + ' MB';
  };

  if (loading) return <div>Loading file tree...</div>;
  if (error) return <div>Error: {error}</div>;
  if (!tree) return <div>No files found</div>;

  return (
    <div className="file-tree">
      <h3>File Manager</h3>
      {renderNode(tree)}
    </div>
  );
};

export default FileTree;
```

### Storage Statistics Component

```typescript
import React, { useEffect, useState } from 'react';
import axios from 'axios';

const StorageStats: React.FC = () => {
  const [stats, setStats] = useState<StorageStatsDto | null>(null);

  useEffect(() => {
    loadStats();
  }, []);

  const loadStats = async () => {
    try {
      const response = await axios.get<ApiResponse<StorageStatsDto>>(
        '/api/file-manager/stats'
      );
      
      if (response.data.success) {
        setStats(response.data.data);
      }
    } catch (err) {
      console.error('Error loading storage stats:', err);
    }
  };

  if (!stats) return null;

  const formatBytes = (bytes: number): string => {
    if (bytes < 1024) return bytes + ' B';
    if (bytes < 1024 * 1024) return (bytes / 1024).toFixed(2) + ' KB';
    if (bytes < 1024 * 1024 * 1024) 
      return (bytes / (1024 * 1024)).toFixed(2) + ' MB';
    return (bytes / (1024 * 1024 * 1024)).toFixed(2) + ' GB';
  };

  return (
    <div className="storage-stats">
      <h4>Storage Statistics</h4>
      <p>Total Size: {formatBytes(stats.totalSizeBytes)}</p>
      <p>Files: {stats.fileCount}</p>
      <p>Folders: {stats.folderCount}</p>
    </div>
  );
};

export default StorageStats;
```

## Error Handling

All endpoints return a standard `ApiResponse` format:

**Success Response:**
```json
{
  "success": true,
  "data": { ... }
}
```

**Error Response:**
```json
{
  "success": false,
  "error": "Error message here"
}
```

**HTTP Status Codes:**
- `200 OK`: Success
- `403 Forbidden`: Security violation (path outside storage directory)
- `404 Not Found`: Requested path does not exist
- `500 Internal Server Error`: Server error

## Performance Considerations

1. **Large Directory Trees**: For very large directory structures, consider using the `/tree/path` endpoint to load subdirectories on demand instead of loading the entire tree at once.

2. **Caching**: Consider implementing client-side caching for the file tree, especially if the file system doesn't change frequently.

3. **Pagination**: For the `/files` endpoint with many files, consider implementing pagination in the frontend.

4. **Lazy Loading**: Implement lazy loading in the UI to only expand and load subdirectories when the user clicks on them.

## Security

- All paths are validated to ensure they are within the storage base directory
- Attempts to access paths outside the storage directory (e.g., using `..`) will result in a 403 Forbidden error
- The API does not provide file deletion or modification capabilities (read-only)

## Configuration

The storage base directory can be configured in `application.properties`:

```properties
app.storage.baseDir=/FISIERE
```

## Notes

- Folders are sorted before files in the tree structure
- Within each category (folders/files), items are sorted alphabetically (case-insensitive)
- The `expanded` field in `FileTreeNodeDto` is optional and can be used by the frontend to maintain UI state
- MIME type detection is best-effort and may be null for some file types
- All paths use forward slashes (`/`) regardless of the operating system

