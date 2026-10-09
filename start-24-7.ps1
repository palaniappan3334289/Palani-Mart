# ==============================================================================
# PalaniMart 24/7 Always-Online Launcher & Monitor
# ==============================================================================

$scriptDir = Split-Path -Parent $MyInvocation.MyCommand.Path
Set-Location $scriptDir

$env:JAVA_HOME = "C:\Java\jdk-17.0.20.1+1"
$env:Path = "C:\apache-maven-3.9.16\bin;C:\Java\jdk-17.0.20.1+1\bin;" + $env:Path

# Load .env file if present
if (Test-Path "$scriptDir\.env") {
    Get-Content "$scriptDir\.env" | ForEach-Object {
        $line = $_.Trim()
        if ($line -and -not $line.StartsWith("#") -and $line.Contains("=")) {
            $parts = $line.Split("=", 2)
            [System.Environment]::SetEnvironmentVariable($parts[0].Trim(), $parts[1].Trim(), "Process")
        }
    }
}

Write-Host "======================================================================" -ForegroundColor Cyan
Write-Host "             PALANIMART 24/7 ALWAYS-ONLINE HOSTING SERVICE            " -ForegroundColor Green
Write-Host "======================================================================" -ForegroundColor Cyan

# 1. Build classpath if missing
if (-not (Test-Path "$scriptDir\target\classpath.txt")) {
    Write-Host "[*] Building classpath..." -ForegroundColor Yellow
    & "mvn.cmd" dependency:build-classpath "-Dmdep.outputFile=target/classpath.txt" "-DincludeScope=test"
}

# 2. Compile if missing
if (-not (Test-Path "$scriptDir\target\classes")) {
    Write-Host "[*] Compiling application..." -ForegroundColor Yellow
    & "mvn.cmd" compile test-compile -DskipTests
}

$cp = Get-Content "$scriptDir\target\classpath.txt"

# 3. Kill any old instances on port 8080 or cloudflared
Get-Process -Name "cloudflared" -ErrorAction SilentlyContinue | Stop-Process -Force

# 4. Start Tomcat in background
Write-Host "[*] Launching Tomcat Server on port 8080..." -ForegroundColor Green
$tomcatProc = Start-Process -FilePath "C:\Java\jdk-17.0.20.1+1\bin\java.exe" `
    -ArgumentList "-cp `"target\classes;target\test-classes;$cp`" com.palani.palanimart.TomcatServer 8080" `
    -WorkingDirectory $scriptDir `
    -WindowStyle Hidden `
    -PassThru

Start-Sleep -Seconds 4

# 5. Start Cloudflare Tunnel and capture log
$tunnelLog = "$scriptDir\tunnel.log"
if (Test-Path $tunnelLog) { Remove-Item $tunnelLog -Force }

Write-Host "[*] Connecting Cloudflare 24/7 Tunnel..." -ForegroundColor Green
$cfProc = Start-Process -FilePath "$scriptDir\cloudflared.exe" `
    -ArgumentList "tunnel --url http://localhost:8080 --logfile `"$tunnelLog`"" `
    -WorkingDirectory $scriptDir `
    -WindowStyle Hidden `
    -PassThru

# Wait for tunnel URL
$tunnelUrl = $null
$retries = 0
while (-not $tunnelUrl -and $retries -lt 30) {
    Start-Sleep -Seconds 1
    $retries++
    if (Test-Path $tunnelLog) {
        $content = Get-Content $tunnelLog -Raw -ErrorAction SilentlyContinue
        if ($content -match "https://[a-zA-Z0-9\-]+\.trycloudflare\.com") {
            $tunnelUrl = $matches[0]
        }
    }
}

if ($tunnelUrl) {
    $fullUrl = "$tunnelUrl/palanimart/"
    Set-Content -Path "$scriptDir\LIVE_URL.txt" -Value $fullUrl -Encoding utf8
    
    Write-Host "`n======================================================================" -ForegroundColor Green
    Write-Host "  PALANIMART IS LIVE ONLINE 24/7!" -ForegroundColor Yellow
    Write-Host "  URL: $fullUrl" -ForegroundColor White
    Write-Host "  (Saved to: $scriptDir\LIVE_URL.txt)" -ForegroundColor Gray
    Write-Host "======================================================================`n" -ForegroundColor Green

    # Launch browser once
    Start-Process $fullUrl
} else {
    Write-Host "[!] Could not extract tunnel URL. Check tunnel.log" -ForegroundColor Red
}

# 6. Infinite monitor loop to keep both alive
while ($true) {
    Start-Sleep -Seconds 15
    if ($tomcatProc.HasExited) {
        Write-Host "[*] Restarting Tomcat..." -ForegroundColor Yellow
        $tomcatProc = Start-Process -FilePath "C:\Java\jdk-17.0.20.1+1\bin\java.exe" `
            -ArgumentList "-cp `"target\classes;target\test-classes;$cp`" com.palani.palanimart.TomcatServer 8080" `
            -WorkingDirectory $scriptDir `
            -WindowStyle Hidden `
            -PassThru
    }
    if ($cfProc.HasExited) {
        Write-Host "[*] Restarting Cloudflare Tunnel..." -ForegroundColor Yellow
        $cfProc = Start-Process -FilePath "$scriptDir\cloudflared.exe" `
            -ArgumentList "tunnel --url http://localhost:8080 --logfile `"$tunnelLog`"" `
            -WorkingDirectory $scriptDir `
            -WindowStyle Hidden `
            -PassThru
    }
}
