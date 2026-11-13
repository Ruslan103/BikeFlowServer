#!/bin/sh
set -eu
cd "$(dirname "$0")"
docker compose --file compose.yaml logs --follow server
