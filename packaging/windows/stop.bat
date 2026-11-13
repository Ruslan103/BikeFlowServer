@echo off
setlocal
cd /d "%~dp0"
docker compose --file "compose.yaml" stop
if errorlevel 1 (
    echo Could not stop BikeFlowServer. Check that Docker Desktop is running.
    pause
    exit /b 1
)
echo BikeFlowServer is stopped. Database and settings have been kept.
pause
