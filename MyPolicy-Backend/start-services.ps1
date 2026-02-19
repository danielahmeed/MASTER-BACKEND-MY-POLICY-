# MyPolicy Backend - Service Startup Script
# Run this script as Administrator

Write-Host "========================================" -ForegroundColor Cyan
Write-Host "  MyPolicy Backend - Service Manager" -ForegroundColor Cyan
Write-Host "========================================" -ForegroundColor Cyan
Write-Host ""

# Check if running as Administrator
$isAdmin = ([Security.Principal.WindowsPrincipal] [Security.Principal.WindowsIdentity]::GetCurrent()).IsInRole([Security.Principal.WindowsBuiltInRole]::Administrator)

if (-not $isAdmin) {
    Write-Host "⚠️  This script requires Administrator privileges!" -ForegroundColor Yellow
    Write-Host ""
    Write-Host "Please right-click PowerShell and select 'Run as Administrator'" -ForegroundColor Yellow
    Write-Host ""
    Write-Host "Press any key to exit..."
    $null = $Host.UI.RawUI.ReadKey("NoEcho,IncludeKeyDown")
    exit 1
}

Write-Host "✓ Running with Administrator privileges" -ForegroundColor Green
Write-Host ""

# Set project root directory
$projectRoot = "d:\New folder (2)\INSURANCE POLICY\MyPolicy-Backend"
Set-Location $projectRoot

# Check if Maven is installed
Write-Host "Checking Maven installation..." -ForegroundColor Cyan
$mavenInstalled = $false

try {
    $mavenVersion = mvn -version 2>&1
    if ($LASTEXITCODE -eq 0) {
        Write-Host "✓ Maven is already installed" -ForegroundColor Green
        Write-Host $mavenVersion[0] -ForegroundColor Gray
        $mavenInstalled = $true
    }
} catch {
    Write-Host "Maven not found" -ForegroundColor Yellow
}

# Install Maven if not present
if (-not $mavenInstalled) {
    Write-Host ""
    Write-Host "Installing Apache Maven..." -ForegroundColor Cyan
    
    # Check if Chocolatey is installed
    try {
        choco --version | Out-Null
        Write-Host "✓ Chocolatey found" -ForegroundColor Green
        
        Write-Host "Installing Maven via Chocolatey..." -ForegroundColor Cyan
        choco install maven -y
        
        # Refresh environment variables
        $env:Path = [System.Environment]::GetEnvironmentVariable("Path","Machine") + ";" + [System.Environment]::GetEnvironmentVariable("Path","User")
        
        Write-Host "✓ Maven installed successfully" -ForegroundColor Green
        Write-Host "⚠️  You may need to restart PowerShell for Maven to work properly" -ForegroundColor Yellow
    } catch {
        Write-Host "✗ Chocolatey not found" -ForegroundColor Red
        Write-Host ""
        Write-Host "Please install Chocolatey first:" -ForegroundColor Yellow
        Write-Host "  Run this command in Administrator PowerShell:" -ForegroundColor White
        Write-Host '  Set-ExecutionPolicy Bypass -Scope Process -Force; [System.Net.ServicePointManager]::SecurityProtocol = [System.Net.ServicePointManager]::SecurityProtocol -bor 3072; iex ((New-Object System.Net.WebClient).DownloadString("https://community.chocolatey.org/install.ps1"))' -ForegroundColor Gray
        Write-Host ""
        Write-Host "Or download Maven manually from: https://maven.apache.org/download.cgi" -ForegroundColor Yellow
        Write-Host ""
        Write-Host "Press any key to exit..."
        $null = $Host.UI.RawUI.ReadKey("NoEcho,IncludeKeyDown")
        exit 1
    }
}

Write-Host ""
Write-Host "========================================" -ForegroundColor Cyan
Write-Host "  Checking Prerequisites" -ForegroundColor Cyan
Write-Host "========================================" -ForegroundColor Cyan
Write-Host ""

# Check Java
Write-Host "Checking Java..." -ForegroundColor Cyan
try {
    $javaVersion = java -version 2>&1
    Write-Host "✓ Java is installed: $($javaVersion[0])" -ForegroundColor Green
} catch {
    Write-Host "✗ Java not found - Please install Java 17 or higher" -ForegroundColor Red
    exit 1
}

# Check PostgreSQL
Write-Host "Checking PostgreSQL..." -ForegroundColor Cyan
$pgRunning = Get-Service -Name "postgresql*" -ErrorAction SilentlyContinue | Where-Object {$_.Status -eq "Running"}
if ($pgRunning) {
    Write-Host "✓ PostgreSQL is running" -ForegroundColor Green
} else {
    Write-Host "⚠️  PostgreSQL service not detected or not running" -ForegroundColor Yellow
    Write-Host "   Please ensure PostgreSQL is installed and running on localhost:5432" -ForegroundColor Yellow
}

# Check MongoDB
Write-Host "Checking MongoDB..." -ForegroundColor Cyan
$mongoRunning = Get-Service -Name "MongoDB" -ErrorAction SilentlyContinue | Where-Object {$_.Status -eq "Running"}
if ($mongoRunning) {
    Write-Host "✓ MongoDB is running" -ForegroundColor Green
} else {
    Write-Host "⚠️  MongoDB service not detected or not running" -ForegroundColor Yellow
    Write-Host "   Please ensure MongoDB is installed and running on localhost:27017" -ForegroundColor Yellow
}

Write-Host ""
Write-Host "========================================" -ForegroundColor Cyan
Write-Host "  Service Startup Options" -ForegroundColor Cyan
Write-Host "========================================" -ForegroundColor Cyan
Write-Host ""
Write-Host "Which services would you like to start?" -ForegroundColor White
Write-Host ""
Write-Host "1. Customer Service (Port 8081) - With Customer Update Feature" -ForegroundColor White
Write-Host "2. BFF Service (Port 8080) - API Gateway" -ForegroundColor White
Write-Host "3. Ingestion Service (Port 8082)" -ForegroundColor White
Write-Host "4. Metadata Service (Port 8083)" -ForegroundColor White
Write-Host "5. Processing Service (Port 8084)" -ForegroundColor White
Write-Host "6. Policy Service (Port 8085)" -ForegroundColor White
Write-Host "7. Matching Engine (Port 8086)" -ForegroundColor White
Write-Host "8. Start ALL Services" -ForegroundColor Green
Write-Host "9. Exit" -ForegroundColor Red
Write-Host ""

$choice = Read-Host "Enter your choice (1-9)"

function Start-Service {
    param(
        [string]$ServiceName,
        [string]$ServicePath,
        [int]$Port
    )
    
    Write-Host ""
    Write-Host "========================================" -ForegroundColor Cyan
    Write-Host "  Starting $ServiceName (Port $Port)" -ForegroundColor Cyan
    Write-Host "========================================" -ForegroundColor Cyan
    Write-Host ""
    
    Set-Location "$projectRoot\$ServicePath"
    
    Write-Host "Building $ServiceName..." -ForegroundColor Yellow
    mvn clean install -DskipTests
    
    if ($LASTEXITCODE -eq 0) {
        Write-Host "✓ Build successful" -ForegroundColor Green
        Write-Host ""
        Write-Host "Starting $ServiceName..." -ForegroundColor Yellow
        Write-Host "Press Ctrl+C to stop the service" -ForegroundColor Gray
        Write-Host ""
        
        Start-Process powershell -ArgumentList "-NoExit", "-Command", "cd '$projectRoot\$ServicePath'; mvn spring-boot:run"
        
        Start-Sleep -Seconds 2
        Write-Host "✓ $ServiceName started in new window" -ForegroundColor Green
    } else {
        Write-Host "✗ Build failed for $ServiceName" -ForegroundColor Red
    }
    
    Set-Location $projectRoot
}

switch ($choice) {
    "1" {
        Start-Service -ServiceName "Customer Service" -ServicePath "customer-service" -Port 8081
    }
    "2" {
        Start-Service -ServiceName "BFF Service" -ServicePath "bff-service" -Port 8080
    }
    "3" {
        Start-Service -ServiceName "Ingestion Service" -ServicePath "ingestion-service" -Port 8082
    }
    "4" {
        Start-Service -ServiceName "Metadata Service" -ServicePath "metadata-service" -Port 8083
    }
    "5" {
        Start-Service -ServiceName "Processing Service" -ServicePath "processing-service" -Port 8084
    }
    "6" {
        Start-Service -ServiceName "Policy Service" -ServicePath "policy-service" -Port 8085
    }
    "7" {
        Start-Service -ServiceName "Matching Engine" -ServicePath "matching-engine" -Port 8086
    }
    "8" {
        Write-Host ""
        Write-Host "Starting ALL services..." -ForegroundColor Green
        Write-Host "This will open 7 PowerShell windows" -ForegroundColor Yellow
        Write-Host ""
        
        $services = @(
            @{Name="Customer Service"; Path="customer-service"; Port=8081},
            @{Name="BFF Service"; Path="bff-service"; Port=8080},
            @{Name="Ingestion Service"; Path="ingestion-service"; Port=8082},
            @{Name="Metadata Service"; Path="metadata-service"; Port=8083},
            @{Name="Processing Service"; Path="processing-service"; Port=8084},
            @{Name="Policy Service"; Path="policy-service"; Port=8085},
            @{Name="Matching Engine"; Path="matching-engine"; Port=8086}
        )
        
        foreach ($service in $services) {
            Start-Service -ServiceName $service.Name -ServicePath $service.Path -Port $service.Port
            Start-Sleep -Seconds 5
        }
        
        Write-Host ""
        Write-Host "✓ All services started!" -ForegroundColor Green
    }
    "9" {
        Write-Host "Exiting..." -ForegroundColor Yellow
        exit 0
    }
    default {
        Write-Host "Invalid choice" -ForegroundColor Red
    }
}

Write-Host ""
Write-Host "========================================" -ForegroundColor Cyan
Write-Host "  Service URLs" -ForegroundColor Cyan
Write-Host "========================================" -ForegroundColor Cyan
Write-Host ""
Write-Host "BFF Service:        http://localhost:8080" -ForegroundColor White
Write-Host "Customer Service:   http://localhost:8081" -ForegroundColor White
Write-Host "Ingestion Service:  http://localhost:8082" -ForegroundColor White
Write-Host "Metadata Service:   http://localhost:8083" -ForegroundColor White
Write-Host "Processing Service: http://localhost:8084" -ForegroundColor White
Write-Host "Policy Service:     http://localhost:8085" -ForegroundColor White
Write-Host "Matching Engine:    http://localhost:8086" -ForegroundColor White
Write-Host ""
Write-Host "Press any key to exit this window..."
$null = $Host.UI.RawUI.ReadKey("NoEcho,IncludeKeyDown")
