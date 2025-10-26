#!/usr/bin/env pwsh
# Script pentru crearea unei noi migrări Flyway
# Usage: .\create-migration.ps1 "add_phone_to_tenant"

param(
    [Parameter(Mandatory=$true)]
    [string]$Description
)

$ErrorActionPreference = "Stop"

# Directorul migrations
$migrationsDir = "src/main/resources/db/migration"

# Verifică că directorul există
if (-not (Test-Path $migrationsDir)) {
    Write-Host "❌ Directorul $migrationsDir nu există!" -ForegroundColor Red
    Write-Host "   Rulează mai întâi: mkdir -p $migrationsDir" -ForegroundColor Yellow
    exit 1
}

# Găsește ultimul număr de versiune
$existingMigrations = Get-ChildItem -Path $migrationsDir -Filter "V*.sql" -File | 
    Where-Object { $_.Name -match '^V(\d+)__' } |
    ForEach-Object { 
        if ($_.Name -match '^V(\d+)__') { 
            [PSCustomObject]@{
                File = $_
                Version = [int]$matches[1]
            }
        }
    } |
    Sort-Object -Property Version

$lastVersion = 0
if ($existingMigrations) {
    $lastVersion = ($existingMigrations | Select-Object -Last 1).Version
}

$nextVersion = $lastVersion + 1

# Normalizează descrierea (înlocuiește spații cu underscore)
$normalizedDescription = $Description -replace '\s+', '_' -replace '[^a-zA-Z0-9_]', ''

# Numele fișierului
$filename = "V${nextVersion}__${normalizedDescription}.sql"
$filepath = Join-Path $migrationsDir $filename

# Template-ul migrării
$template = @"
-- Migration: $Description
-- Version: V$nextVersion
-- Date: $(Get-Date -Format 'yyyy-MM-dd HH:mm')
-- Author: $(whoami)
--
-- Description:
-- Add your description here
--

-- Your SQL commands here:

-- Example: Add column
-- ALTER TABLE table_name 
-- ADD COLUMN column_name data_type;

-- Example: Create index
-- CREATE INDEX idx_table_column ON table_name(column_name);

-- Example: Add constraint
-- ALTER TABLE table_name 
-- ADD CONSTRAINT constraint_name 
-- FOREIGN KEY (column_name) REFERENCES other_table(id);

-- TODO: Replace examples with actual SQL commands
"@

# Creează fișierul
Set-Content -Path $filepath -Value $template -Encoding UTF8

Write-Host ""
Write-Host "✅ Migrare creată cu succes!" -ForegroundColor Green
Write-Host ""
Write-Host "📁 Fișier: " -NoNewline
Write-Host $filename -ForegroundColor Cyan
Write-Host "📍 Path: " -NoNewline
Write-Host $filepath -ForegroundColor Cyan
Write-Host "🔢 Versiune: " -NoNewline
Write-Host "V$nextVersion" -ForegroundColor Yellow
Write-Host ""
Write-Host "📝 Următorii pași:" -ForegroundColor Magenta
Write-Host "   1. Deschide fișierul și adaugă SQL-ul tău" -ForegroundColor White
Write-Host "   2. Testează migrarea pe o copie a bazei de date" -ForegroundColor White
Write-Host "   3. Commit și push pe main" -ForegroundColor White
Write-Host ""

# Oferă să deschidă fișierul
$response = Read-Host "Vrei să deschizi fișierul acum? (y/n)"
if ($response -eq 'y' -or $response -eq 'Y') {
    if (Get-Command code -ErrorAction SilentlyContinue) {
        code $filepath
        Write-Host "✅ Fișier deschis în VS Code" -ForegroundColor Green
    } else {
        Start-Process $filepath
        Write-Host "✅ Fișier deschis în editor default" -ForegroundColor Green
    }
}

Write-Host ""
Write-Host "📚 Pentru mai multe informații:" -ForegroundColor Blue
Write-Host "   - docs/MIGRATION_SETUP.md" -ForegroundColor Cyan
Write-Host "   - guides/07-database-migrations.md" -ForegroundColor Cyan
Write-Host ""

