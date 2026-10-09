@echo off
echo [*] Installing PalaniMart to Windows Auto-Startup...

set SCRIPT_DIR=%~dp0
set TARGET_VBS=%SCRIPT_DIR%start-silent.vbs
set SHORTCUT_PATH=%APPDATA%\Microsoft\Windows\Start Menu\Programs\Startup\PalaniMart247.vbs

copy /y "%TARGET_VBS%" "%SHORTCUT_PATH%" >nul

if %ERRORLEVEL% EQU 0 (
    echo [OK] PalaniMart is now configured to start automatically whenever your PC turns on!
    echo [OK] Startup path: %SHORTCUT_PATH%
) else (
    echo [!] Could not copy to startup folder.
)

pause
