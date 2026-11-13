#!/bin/sh
set -eu

mkdir -p ./config ./logs
settings_file=./config/server-settings.properties

if [ ! -s "$settings_file" ]; then
    : "${BIKE_DB_URL:?BIKE_DB_URL is required on the first container start}"
    : "${BIKE_DB_USERNAME:?BIKE_DB_USERNAME is required on the first container start}"
    : "${BIKE_DB_PASSWORD:?BIKE_DB_PASSWORD is required on the first container start}"

    umask 077
    temporary_file="${settings_file}.tmp"
    {
        printf 'spring.datasource.url=%s\n' "$BIKE_DB_URL"
        printf 'spring.datasource.username=%s\n' "$BIKE_DB_USERNAME"
        printf 'spring.datasource.password=%s\n' "$BIKE_DB_PASSWORD"
    } > "$temporary_file"
    mv "$temporary_file" "$settings_file"
fi

unset BIKE_DB_URL BIKE_DB_USERNAME BIKE_DB_PASSWORD
exec java -jar /app/app.jar "$@"
