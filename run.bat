@echo off
setlocal enabledelayedexpansion
title PalaniMart Localhost Server
echo ======================================================================
echo                   PALANIMART MARKETPLACE LAUNCHER
echo ======================================================================

set "JAVA_HOME=C:\Java\jdk-17.0.20.1+1"
set "PATH=C:\apache-maven-3.9.16\bin;%JAVA_HOME%\bin;%PATH%"

echo [*] Checking Java 17 environment...
java -version >nul 2>&1
if %ERRORLEVEL% NEQ 0 (
    echo [ERROR] Java 17 not found. Please check Java installation.
    pause
    exit /b 1
)

echo [*] Checking compiled classes and dependencies...
if not exist "target\classes" (
    call .\mvn.cmd compile test-compile -DskipTests
)
if not exist "target\classpath.txt" (
    call .\mvn.cmd dependency:build-classpath "-Dmdep.outputFile=target/classpath.txt" "-DincludeScope=test"
)

echo [*] Starting Apache Tomcat on http://localhost:8080/palanimart ...
echo.
echo ======================================================================
echo   Access the application in your browser at:
echo   http://localhost:8080/palanimart/
echo.
echo   Demo Login Accounts:
echo   - Admin:  admin@palanimart.com  / admin123
echo   - Seller: seller1@palanimart.com / seller123
echo   - Buyer:  buyer1@palanimart.com  / buyer123
echo ======================================================================
echo.

set /p CP=<target\classpath.txt
start http://localhost:8080/palanimart/
java -cp "target\classes;target\test-classes;!CP!" com.palani.palanimart.TomcatServer 8080

pause
