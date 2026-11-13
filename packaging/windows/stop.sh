#!/bin/sh
set -eu
cd "$(dirname "$0")"
docker compose --file compose.yaml stop
echo "BikeFlowServer is stopped. Database and settings have been kept."
