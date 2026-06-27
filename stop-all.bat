@echo off
echo Stopping Flint and Thread backends on ports 8080, 8082, 8083...

for %%P in (8080 8082 8083) do (
    for /f "tokens=5" %%A in ('netstat -ano ^| findstr /R /C:":%%P .*LISTENING"') do (
        echo Killing PID %%A on port %%P
        taskkill /F /PID %%A >nul 2>&1
    )
)

echo Done.
pause
