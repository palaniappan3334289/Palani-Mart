@echo off
title PalaniMart H2 Interactive SQL Shell
cd /d "%~dp0"
echo ======================================================================
echo             PALANIMART H2 INTERACTIVE SQL COMMAND LINE
echo ======================================================================
echo.
echo Connecting to H2 database at ./data/palanimart ...
echo (Type 'help' for commands, 'exit' to quit, e.g. SELECT * FROM products;)
echo.
set JAVA_HOME=C:\Java\jdk-17.0.20.1+1
for /f "delims=" %%i in (target\classpath.txt) do set CP=%%i
"%JAVA_HOME%\bin\java.exe" -cp "target\classes;target\test-classes;%CP%" org.h2.tools.Shell -url "jdbc:h2:file:./data/palanimart;MODE=PostgreSQL;AUTO_SERVER=TRUE" -user sa
pause
