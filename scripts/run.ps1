# Script PowerShell pentru rulare rapidă Total App
# Rulează cu: .\run.ps1

Write-Host "🚀 Starting Total App..." -ForegroundColor Green
Write-Host ""

# Verifică dacă Java 21 este instalat
Write-Host "Checking Java version..." -ForegroundColor Yellow
try {
    $javaVersion = java -version 2>&1 | Select-String "version" | ForEach-Object { $_.ToString() }
    Write-Host "✅ $javaVersion" -ForegroundColor Green
} catch {
    Write-Host "❌ Java not found! Please install JDK 21" -ForegroundColor Red
    exit 1
}

Write-Host ""

# Verifică dacă PostgreSQL rulează
Write-Host "Checking PostgreSQL..." -ForegroundColor Yellow
$pgService = Get-Service -Name "postgresql*" -ErrorAction SilentlyContinue
if ($pgService -and $pgService.Status -eq "Running") {
    Write-Host "✅ PostgreSQL is running" -ForegroundColor Green
} else {
    Write-Host "⚠️  PostgreSQL service not found or not running" -ForegroundColor Yellow
    Write-Host "Please make sure PostgreSQL is running on port 5432" -ForegroundColor Yellow
}

Write-Host ""
Write-Host "Starting Spring Boot application..." -ForegroundColor Cyan
Write-Host "Press Ctrl+C to stop" -ForegroundColor Gray
Write-Host ""

# Rulează aplicația
.\mvnw.cmd spring-boot:run -Dspring-boot.run.profiles=dev

