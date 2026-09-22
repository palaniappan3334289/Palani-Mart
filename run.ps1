# PalaniMart Localhost Server Launcher
$ErrorActionPreference = "Stop"

$env:JAVA_HOME = "C:\Java\jdk-17.0.20.1+1"
$env:Path = "C:\apache-maven-3.9.16\bin;C:\Java\jdk-17.0.20.1+1\bin;" + $env:Path

Write-Host "======================================================================" -ForegroundColor Cyan
Write-Host "                  PALANIMART MARKETPLACE LAUNCHER                     " -ForegroundColor Green
Write-Host "======================================================================" -ForegroundColor Cyan

if (-not (Test-Path "target\classpath.txt")) {
    Write-Host "[*] Building dependency classpath..." -ForegroundColor Yellow
    .\mvn.cmd dependency:build-classpath "-Dmdep.outputFile=target/classpath.txt" "-DincludeScope=test"
}

if (-not (Test-Path "target\classes")) {
    Write-Host "[*] Compiling application..." -ForegroundColor Yellow
    .\mvn.cmd compile test-compile -DskipTests
}

$cp = Get-Content "target\classpath.txt"

Write-Host "`n[*] Starting Apache Tomcat at http://localhost:8080/palanimart ...`n" -ForegroundColor Green
Write-Host "======================================================================" -ForegroundColor Yellow
Write-Host "  URL: http://localhost:8080/palanimart/" -ForegroundColor White
Write-Host "  Demo Accounts:" -ForegroundColor White
Write-Host "    - Admin:  admin@palanimart.com  / admin123" -ForegroundColor White
Write-Host "    - Seller: seller1@palanimart.com / seller123" -ForegroundColor White
Write-Host "    - Buyer:  buyer1@palanimart.com  / buyer123" -ForegroundColor White
Write-Host "======================================================================`n" -ForegroundColor Yellow

Start-Process "http://localhost:8080/palanimart/"

& "C:\Java\jdk-17.0.20.1+1\bin\java.exe" -cp "target\classes;target\test-classes;$cp" com.palani.palanimart.TomcatServer 8080
