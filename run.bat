@echo off
REM Start Flint & Thread backend services (separate terminals recommended)

echo Flint ^& Thread Platform — choose a service to start:
echo   1. User API      (port 8080)  — customer / shopper app
echo   2. Seller API    (port 8083)  — seller app (8083 avoids clash with user)
echo   3. Admin API     (port 8082)  — admin panel
echo   4. Start ALL 3   (user 8080 + seller 8083 + admin 8082)
echo   5. Build all modules
echo.
set /p choice=Enter 1-5: 

if "%choice%"=="1" goto user
if "%choice%"=="2" goto seller
if "%choice%"=="3" goto admin
if "%choice%"=="4" goto all
if "%choice%"=="5" goto build
echo Invalid choice
exit /b 1

:user
cd /d "%~dp0user-service"
call ..\mvnw.cmd spring-boot:run
goto end

:seller
cd /d "%~dp0seller-service"
call ..\mvnw.cmd spring-boot:run
goto end

:admin
cd /d "%~dp0admin-service"
call ..\mvnw.cmd spring-boot:run
goto end

:all
call "%~dp0start-all.bat"
goto end

:build
cd /d "%~dp0"
call mvnw.cmd clean install -DskipTests
goto end

:end
