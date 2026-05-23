# =============================================================================
# PriceHawk AI — Start All Services (Windows / PowerShell)
# Usage: powershell -ExecutionPolicy Bypass -File scripts\start-all.ps1
#
# Prerequisites:
#   - Docker Desktop running
#   - Java 21 + Maven 3.9+
#   - Python 3.11+ + pip
#   - Node.js 18+ + npm
# =============================================================================

param(
    [switch]$SkipBuild,     # -SkipBuild: skip Maven build (use if already built)
    [switch]$SkipSeed,      # -SkipSeed: skip inserting seed data
    [switch]$FrontendOnly   # -FrontendOnly: only start frontend dev server
)

$ROOT = Split-Path $PSScriptRoot -Parent
$ErrorActionPreference = "Stop"

function Write-Step([string]$msg) {
    Write-Host "`n===> $msg" -ForegroundColor Cyan
}

function Write-OK([string]$msg) {
    Write-Host "  [OK] $msg" -ForegroundColor Green
}

function Write-Warn([string]$msg) {
    Write-Host "  [!!] $msg" -ForegroundColor Yellow
}

function Start-ServiceWindow([string]$name, [string]$workdir, [string]$command) {
    $args = "-NoExit -Command `"cd '$workdir'; $command`""
    Start-Process powershell -ArgumentList $args -WindowStyle Normal
    Write-OK "Started: $name"
    Start-Sleep -Milliseconds 500
}

# ─── 1. Docker infrastructure ─────────────────────────────────────────────────
Write-Step "Starting Docker infrastructure (Postgres, Redis, RabbitMQ)..."
Set-Location $ROOT
docker-compose up -d
if ($LASTEXITCODE -ne 0) { Write-Warn "docker-compose warning (may already be running)" }

Write-Host "  Waiting 10s for Postgres to initialise..." -ForegroundColor Gray
Start-Sleep -Seconds 10
Write-OK "Infrastructure ready"

# ─── 2. Build Java services ───────────────────────────────────────────────────
if (-not $SkipBuild -and -not $FrontendOnly) {
    Write-Step "Building Java backend (this takes ~2 min)..."
    Set-Location "$ROOT\backend-java"
    mvn clean install -DskipTests -q
    if ($LASTEXITCODE -ne 0) { throw "Maven build failed. Check output above." }
    Write-OK "Maven build successful"
}

# ─── 3. Install Python dependencies ──────────────────────────────────────────
if (-not $FrontendOnly) {
    Write-Step "Installing Python dependencies..."
    Set-Location "$ROOT\backend-python"

    # Shared
    pip install -r shared/requirements.txt -q 2>&1 | Out-Null
    # Scraper
    pip install -r scraper-service/requirements.txt -q 2>&1 | Out-Null
    # AI service
    pip install -r ai-service/requirements.txt -q 2>&1 | Out-Null
    Write-OK "Python packages installed"
}

# ─── 4. Seed data ──────────────────────────────────────────────────────────────
if (-not $SkipSeed -and -not $FrontendOnly) {
    Write-Step "Inserting seed data..."
    $env:PGPASSWORD = "pricehawk_secret"
    psql -U pricehawk -h localhost -f "$ROOT\scripts\seed-data.sql" 2>&1
    if ($LASTEXITCODE -ne 0) {
        Write-Warn "Seed data may have partially failed (likely already seeded — OK to ignore)"
    } else {
        Write-OK "Seed data inserted"
    }
    Remove-Item Env:\PGPASSWORD -ErrorAction SilentlyContinue
}

# ─── 5. Start Java services (each in its own window) ─────────────────────────
if (-not $FrontendOnly) {
    Write-Step "Starting Java microservices..."

    $javaDir = "$ROOT\backend-java"

    Start-ServiceWindow "Gateway (8080)" $javaDir `
        "mvn spring-boot:run -pl pricehawk-gateway -am -DskipTests"

    Start-Sleep -Seconds 3

    Start-ServiceWindow "User Service (8081)" $javaDir `
        "mvn spring-boot:run -pl pricehawk-user-service -am -DskipTests"

    Start-ServiceWindow "Catalog Service (8082)" $javaDir `
        "mvn spring-boot:run -pl pricehawk-catalog-service -am -DskipTests"

    Start-ServiceWindow "Notification Service (8085)" $javaDir `
        "mvn spring-boot:run -pl pricehawk-notification-service -am -DskipTests"

    Write-Host "  Waiting 20s for Java services to start up..." -ForegroundColor Gray
    Start-Sleep -Seconds 20
}

# ─── 6. Start Python services ─────────────────────────────────────────────────
if (-not $FrontendOnly) {
    Write-Step "Starting Python microservices..."

    $scrPath = "$ROOT\backend-python\scraper-service"
    $aiPath  = "$ROOT\backend-python\ai-service"

    # Set PYTHONPATH so `shared` package is importable
    $pyEnv = "PYTHONPATH='$ROOT\backend-python'; "

    Start-ServiceWindow "Scraper Service (8083)" $scrPath `
        "Set-Item Env:\PYTHONPATH '$ROOT\backend-python'; uvicorn app.main:app --host 0.0.0.0 --port 8083 --reload"

    Start-ServiceWindow "AI Analyzer Service (8084)" $aiPath `
        "Set-Item Env:\PYTHONPATH '$ROOT\backend-python'; uvicorn app.main:app --host 0.0.0.0 --port 8084 --reload"
}

# ─── 7. Start frontend dev server ─────────────────────────────────────────────
Write-Step "Starting Next.js frontend (http://localhost:3000)..."
Start-ServiceWindow "Frontend (3000)" "$ROOT\frontend" "npm run dev"

# ─── Summary ──────────────────────────────────────────────────────────────────
Write-Host ""
Write-Host "============================================================" -ForegroundColor Magenta
Write-Host "  PriceHawk AI — All services starting up!" -ForegroundColor Magenta
Write-Host "============================================================" -ForegroundColor Magenta
Write-Host ""
Write-Host "  Frontend:       http://localhost:3000" -ForegroundColor White
Write-Host "  API Gateway:    http://localhost:8080" -ForegroundColor White
Write-Host "  RabbitMQ UI:    http://localhost:15672  (pricehawk / pricehawk_secret)" -ForegroundColor White
Write-Host ""
Write-Host "  Test accounts:" -ForegroundColor White
Write-Host "    test@pricehawk.vn     / Test123456  (FREE user)" -ForegroundColor Gray
Write-Host "    premium@pricehawk.vn  / Test123456  (PREMIUM user)" -ForegroundColor Gray
Write-Host "    admin@pricehawk.vn    / Test123456  (ADMIN)" -ForegroundColor Gray
Write-Host ""
Write-Host "  Sample product slugs:" -ForegroundColor White
Write-Host "    /products/apple-iphone-15-pro-max-256gb" -ForegroundColor Gray
Write-Host "    /products/samsung-galaxy-s24-ultra-256gb" -ForegroundColor Gray
Write-Host "    /products/apple-macbook-pro-14-m3-pro-18gb-512gb" -ForegroundColor Gray
Write-Host "    /products/apple-airpods-pro-gen-2-magsafe-usb-c" -ForegroundColor Gray
Write-Host "    /products/sony-wh-1000xm5-wireless-noise-canceling" -ForegroundColor Gray
Write-Host ""
Write-Host "  Wait ~30s for all services to be healthy before browsing." -ForegroundColor Yellow
Write-Host ""
