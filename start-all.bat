@echo off
setlocal
cd /d "%~dp0"

echo ============================================
echo  Flint and Thread — Start 3 backends
echo ============================================
echo.
echo  User API   -> http://localhost:8080
echo  Seller API -> http://localhost:8083
echo  Admin API  -> http://localhost:8082
echo.

if not exist "config\application-local.properties" (
    echo TIP: copy config\application-local.properties.example to config\application-local.properties
    echo.
)

call mvnw.cmd -q -pl user-service,seller-service,admin-service -am install -DskipTests
if errorlevel 1 (
    echo Build failed.
    pause
    exit /b 1
)

start "Flint-User-8080" cmd /k "cd /d %~dp0user-service && set FLINT_CONFIG_DIR=%~dp0config && ..\mvnw.cmd spring-boot:run -DskipTests"
timeout /t 10 /nobreak >nul

start "Flint-Seller-8083" cmd /k "cd /d %~dp0seller-service && set FLINT_CONFIG_DIR=%~dp0config && ..\mvnw.cmd spring-boot:run -DskipTests"
timeout /t 8 /nobreak >nul

start "Flint-Admin-8082" cmd /k "cd /d %~dp0admin-service && set FLINT_CONFIG_DIR=%~dp0config && ..\mvnw.cmd spring-boot:run -DskipTests"

echo.
echo All 3 backends starting. Run stop-all.bat to stop.
pause
