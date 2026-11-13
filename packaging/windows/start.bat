@echo off
setlocal
cd /d "%~dp0"

if not exist ".env" (
    copy /Y ".env.example" ".env" >nul
    echo Created .env from .env.example.
    echo Set POSTGRES_PASSWORD and BIKE_ADMIN_PASSWORD in .env, save it, then run start.bat again.
    start "" notepad.exe "%~dp0.env"
    pause
    exit /b 0
)

findstr /x /c:"POSTGRES_PASSWORD=replace_with_a_long_hex_password" ".env" >nul
if not errorlevel 1 goto missing_password
findstr /x /c:"BIKE_ADMIN_PASSWORD=replace_with_a_long_unique_password" ".env" >nul
if not errorlevel 1 goto missing_password

docker info >nul 2>&1
if errorlevel 1 (
    echo Docker Desktop is not running. Start Docker Desktop and run this file again.
    goto failed
)

docker compose version >nul 2>&1
if errorlevel 1 (
    echo Docker Compose is unavailable. Install or update Docker Desktop.
    goto failed
)

if not exist "BikeFlowServer-@VERSION@-images.tar" (
    echo The image archive BikeFlowServer-@VERSION@-images.tar is missing.
    goto failed
)

docker image inspect bikeflowserver:@VERSION@ >nul 2>&1
if errorlevel 1 goto load_images
docker image inspect postgres:16-alpine >nul 2>&1
if errorlevel 1 goto load_images
goto start_stack

:load_images
    echo Loading the BikeFlowServer image and PostgreSQL image...
    docker load --input "BikeFlowServer-@VERSION@-images.tar"
    if errorlevel 1 goto failed

:start_stack
echo Starting BikeFlowServer and PostgreSQL...
docker compose --file "compose.yaml" up --detach
if errorlevel 1 goto failed

echo.
echo BikeFlowServer is starting.
echo Admin page: https://localhost:8443/admin
echo Login page: https://localhost:8443/login
echo Use logs.bat to view startup progress and logs.
pause
exit /b 0

:missing_password
echo Replace the example passwords in .env before starting the server.
notepad.exe "%~dp0.env"
goto failed

:failed
echo.
pause
exit /b 1
