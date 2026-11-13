@echo off
setlocal
cd /d "%~dp0"
docker compose --file "compose.yaml" logs --follow server
if errorlevel 1 (
    echo Could not read logs. Check that Docker Desktop is running and the server has started.
    pause
    exit /b 1
)
