# Quick Start - Customer Service Only
# Run this script as Administrator

Write-Host "========================================" -ForegroundColor Cyan
Write-Host "  Starting Customer Service (Port 8081)" -ForegroundColor Cyan
Write-Host "========================================" -ForegroundColor Cyan
Write-Host ""

# Check if running as Administrator
$isAdmin = ([Security.Principal.WindowsPrincipal] [Security.Principal.WindowsIdentity]::GetCurrent()).IsInRole([Security.Principal.WindowsBuiltInRole]::Administrator)

if (-not $isAdmin) {
    Write-Host "⚠️  This script requires Administrator privileges!" -ForegroundColor Yellow
    Write-Host ""
    Write-Host "Right-click PowerShell → 'Run as Administrator'" -ForegroundColor Yellow
    Write-Host "Then run this script again" -ForegroundColor Yellow
    Write-Host ""
    Write-Host "Press any key to exit..."
    $null = $Host.UI.RawUI.ReadKey("NoEcho,IncludeKeyDown")
    exit 1
}

# Set location
$projectRoot = "d:\New folder (2)\INSURANCE POLICY\MyPolicy-Backend"
$servicePath = "$projectRoot\customer-service"

Set-Location $servicePath

Write-Host "✓ Running with Administrator privileges" -ForegroundColor Green
Write-Host ""

# Check Java
Write-Host "Checking Java..." -ForegroundColor Cyan
try {
    $javaVersion = java -version 2>&1
    Write-Host "✓ Java installed: $($javaVersion[0])" -ForegroundColor Green
} catch {
    Write-Host "✗ Java not found - Install Java 17 or higher" -ForegroundColor Red
    Write-Host "Press any key to exit..."
    $null = $Host.UI.RawUI.ReadKey("NoEcho,IncludeKeyDown")
    exit 1
}

# Check Maven
Write-Host "Checking Maven..." -ForegroundColor Cyan
$mavenInstalled = $false

try {
    mvn -version | Out-Null
    if ($LASTEXITCODE -eq 0) {
        Write-Host "✓ Maven is installed" -ForegroundColor Green
        $mavenInstalled = $true
    }
} catch {
    Write-Host "Maven not found, attempting to install..." -ForegroundColor Yellow
}

# Install Maven if needed
if (-not $mavenInstalled) {
    Write-Host ""
    Write-Host "Installing Maven via Chocolatey..." -ForegroundColor Cyan
    
    try {
        choco install maven -y
        
        # Refresh PATH
        $env:Path = [System.Environment]::GetEnvironmentVariable("Path","Machine") + ";" + [System.Environment]::GetEnvironmentVariable("Path","User")
        
        Write-Host "✓ Maven installed" -ForegroundColor Green
        Write-Host ""
        Write-Host "⚠️  IMPORTANT: Close this window and run the script again" -ForegroundColor Yellow
        Write-Host "   (Maven needs a fresh PowerShell session)" -ForegroundColor Yellow
        Write-Host ""
        Write-Host "Press any key to exit..."
        $null = $Host.UI.RawUI.ReadKey("NoEcho,IncludeKeyDown")
        exit 0
    } catch {
        Write-Host "✗ Failed to install Maven" -ForegroundColor Red
        Write-Host ""
        Write-Host "Install Chocolatey first, then run this script again:" -ForegroundColor Yellow
        Write-Host 'Set-ExecutionPolicy Bypass -Scope Process -Force; iex ((New-Object System.Net.WebClient).DownloadString("https://community.chocolatey.org/install.ps1"))' -ForegroundColor Gray
        Write-Host ""
        Write-Host "Press any key to exit..."
        $null = $Host.UI.RawUI.ReadKey("NoEcho,IncludeKeyDown")
        exit 1
    }
}

# Check PostgreSQL
Write-Host "Checking PostgreSQL..." -ForegroundColor Cyan
$pgRunning = Get-Service -Name "postgresql*" -ErrorAction SilentlyContinue | Where-Object {$_.Status -eq "Running"}
if ($pgRunning) {
    Write-Host "✓ PostgreSQL is running" -ForegroundColor Green
} else {
    Write-Host "⚠️  PostgreSQL not running - Please start it first" -ForegroundColor Yellow
    Write-Host "   Required: localhost:5432 with database 'mypolicy_db'" -ForegroundColor Gray
}

Write-Host ""
Write-Host "========================================" -ForegroundColor Cyan
Write-Host "  Building Customer Service" -ForegroundColor Cyan
Write-Host "========================================" -ForegroundColor Cyan
Write-Host ""

# Build the service
mvn clean install -DskipTests

if ($LASTEXITCODE -eq 0) {
    Write-Host ""
    Write-Host "✓ Build successful!" -ForegroundColor Green
    Write-Host ""
    Write-Host "========================================" -ForegroundColor Cyan
    Write-Host "  Starting Customer Service" -ForegroundColor Cyan
    Write-Host "========================================" -ForegroundColor Cyan
    Write-Host ""
    Write-Host "Service will start on: http://localhost:8081" -ForegroundColor Green
    Write-Host ""
    Write-Host "Features available:" -ForegroundColor White
    Write-Host "  • User registration & authentication" -ForegroundColor Gray
    Write-Host "  • Customer CRUD operations" -ForegroundColor Gray
    Write-Host "  • Customer data updates (NEW)" -ForegroundColor Green
    Write-Host "  • PII encryption (AES-256)" -ForegroundColor Gray
    Write-Host ""
    Write-Host "Press Ctrl+C to stop the service" -ForegroundColor Yellow
    Write-Host ""
    
    # Run the service
    mvn spring-boot:run
} else {
    Write-Host ""
    Write-Host "✗ Build failed!" -ForegroundColor Red
    Write-Host ""
    Write-Host "Check the error messages above" -ForegroundColor Yellow
    Write-Host ""
    Write-Host "Press any key to exit..."
    $null = $Host.UI.RawUI.ReadKey("NoEcho,IncludeKeyDown")
    exit 1
}
