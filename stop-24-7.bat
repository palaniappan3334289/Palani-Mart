@echo off
echo [*] Stopping PalaniMart 24/7 processes...

taskkill /f /im cloudflared.exe >nul 2>&1
for /f "tokens=5" %%a in ('netstat -aon ^| findstr :8080 ^| findstr LISTENING') do (
    taskkill /f /pid %%a >nul 2>&1
)

echo [OK] PalaniMart and Cloudflare processes stopped.
pause
