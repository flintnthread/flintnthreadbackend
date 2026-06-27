@echo off
setlocal
cd /d "%~dp0"

echo ============================================
echo  Flint and Thread — Start ALL 3 backends
echo ============================================
echo.
echo  User API   -> http://localhost:8080
echo  Seller API -> http://localhost:8083  (auto — no port conflict)
echo  Admin API  -> http://localhost:8082
echo.

call mvnw.cmd -q -pl platform-config,user-service,seller-service,admin-service install -DskipTests
if errorlevel 1 (
    echo Build failed. Fix errors and try again.
    pause
    exit /b 1
)

start "Flint-User-8080" cmd /k "cd /d %~dp0user-service && ..\mvnw.cmd spring-boot:run -DskipTests"
timeout /t 8 /nobreak >nul

start "Flint-Seller-8083" cmd /k "cd /d %~dp0seller-service && ..\mvnw.cmd spring-boot:run -DskipTests"
timeout /t 5 /nobreak >nul

start "Flint-Admin-8082" cmd /k "cd /d %~dp0admin-service && ..\mvnw.cmd spring-boot:run -DskipTests"

echo.
echo All 3 services starting in separate windows.
echo Close each window to stop that service, or run stop-all.bat
echo.
pause
