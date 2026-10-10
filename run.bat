@echo off
title PalaniMart - AI Shopping Assistant
cd /d "%~dp0"

echo ======================================================================
echo                     STARTING PALANIMART WEB APP
echo ======================================================================
echo.

if "%1"=="local" goto :run_local
if "%1"=="h2" goto :run_h2
if "%1"=="stop" goto :run_stop

REM Default: Launch full 24/7 server + Cloudflare Tunnel + Auto-Recovery
powershell.exe -ExecutionPolicy Bypass -NoProfile -File "%~dp0start-24-7.ps1"
goto :eof

:run_local
echo [*] Starting in Local-Only mode on port 8080...
set JAVA_HOME=C:\Java\jdk-17.0.20.1+1
set PATH=C:\apache-maven-3.9.16\bin;C:\Java\jdk-17.0.20.1+1\bin;%PATH%

if exist "%~dp0.env" (
    for /f "usebackq eol=# tokens=1* delims==" %%A in ("%~dp0.env") do set "%%A=%%B"
)

if "%JDBC_URL%"=="" set JDBC_URL=jdbc:h2:file:./data/palanimart;DB_CLOSE_DELAY=-1;MODE=PostgreSQL;AUTO_SERVER=TRUE
if "%GEMINI_MODEL%"=="" set GEMINI_MODEL=gemini-3.8-flash

if not exist "target\classpath.txt" (
    echo [*] Generating classpath...
    call mvn.cmd dependency:build-classpath "-Dmdep.outputFile=target/classpath.txt" "-DincludeScope=test"
)
if not exist "target\classes" (
    echo [*] Compiling project...
    call mvn.cmd compile test-compile -DskipTests
)

for /f "delims=" %%i in (target\classpath.txt) do set CP=%%i
echo.
echo [*] App URL : http://localhost:8080/palanimart/
echo [*] H2 DB   : http://localhost:8080/palanimart/h2-console
echo.
start http://localhost:8080/palanimart/
"%JAVA_HOME%\bin\java.exe" -cp "target\classes;target\test-classes;%CP%" com.palani.palanimart.TomcatServer 8080
goto :eof

:run_h2
call "%~dp0h2-console.bat"
goto :eof

:run_stop
call "%~dp0stop-24-7.bat"
goto :eof
