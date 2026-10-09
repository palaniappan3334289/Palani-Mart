@echo off
title PalaniMart H2 Web Console
echo ======================================================================
echo                     PALANIMART H2 DATABASE CONSOLE
echo ======================================================================
echo.
echo Login Credentials:
echo   - Driver Class : org.h2.Driver
echo   - JDBC URL     : jdbc:h2:file:./data/palanimart;MODE=PostgreSQL
echo   - User Name    : sa
echo   - Password     : (leave blank)
echo.
echo Opening H2 Web Console in your browser...
start http://localhost:8080/palanimart/h2-console
echo.
pause
