@echo off
title PalaniMart 24-7 Host
cd /d "%~dp0"
powershell.exe -ExecutionPolicy Bypass -NoProfile -File "%~dp0start-24-7.ps1"
pause
