#!/bin/sh
set -eu
umask 077

cd "$(dirname "$0")"

if [ ! -f .env ]; then
    cp .env.example .env
    echo "Created .env from .env.example. Set the passwords, save the file, then run this script again."
    exit 0
fi

if grep -Fxq 'POSTGRES_PASSWORD=replace_with_a_long_hex_password' .env \
    || grep -Fxq 'BIKE_ADMIN_PASSWORD=replace_with_a_long_unique_password' .env; then
    echo "Replace the example passwords in .env before starting the server."
    exit 1
fi

if ! command -v docker >/dev/null 2>&1; then
    echo "Docker is not installed or is not on PATH."
    exit 1
fi

if ! docker info >/dev/null 2>&1; then
    echo "Docker Engine is not running. Start Docker Desktop or the Docker service."
    exit 1
fi

if ! docker image inspect bikeflowserver:@VERSION@ >/dev/null 2>&1 \
    || ! docker image inspect postgres:16-alpine >/dev/null 2>&1; then
    if [ ! -f "BikeFlowServer-@VERSION@-images.tar" ]; then
        echo "The bundled Docker image archive is missing."
        exit 1
    fi
    echo "Loading the BikeFlowServer and PostgreSQL images..."
    docker load --input "BikeFlowServer-@VERSION@-images.tar"
fi

echo "Starting BikeFlowServer and PostgreSQL..."
docker compose --file compose.yaml up --detach
echo "Admin page: https://localhost:8443/admin"
echo "Use 'sh ./logs.sh' to follow server startup and logs."
