BikeFlowServer @VERSION@ — Windows x64

Requirements
  - Windows 10/11 x64
  - Docker Desktop with Linux containers enabled
  - About 3 GB of free disk space for the package and Docker images

First start
  1. Extract the complete ZIP archive to a folder.
  2. Run start.bat. It creates .env from .env.example and opens it in Notepad.
  3. Set unique values for POSTGRES_PASSWORD and BIKE_ADMIN_PASSWORD, then save .env.
     BIKE_ADMIN_PASSWORD must be at least 12 characters.
  4. Run start.bat again. It loads the bundled images and starts the server and database.
  5. Open https://localhost:8443/admin and sign in using BIKE_ADMIN_LOGIN and
     BIKE_ADMIN_PASSWORD from .env.

For access from another computer on your network, set its DNS name or IP address
in BIKE_TLS_DNS_NAMES or BIKE_TLS_IP_ADDRESSES before the first start. Names in
these settings are comma-separated. The certificate is created on first start.

Files
  start.bat  Creates .env if needed, loads images and starts the application.
  stop.bat   Stops the application without deleting its database or settings.
  logs.bat   Follows the server log. Press Ctrl+C to return to the console.
  .env       Local passwords and connection settings. Keep this file private.

The first start may take a minute while PostgreSQL initializes its database.
The browser may show a certificate warning for the self-signed local HTTPS
certificate. The server also exposes HTTP on port 8080.

To remove the installation and its saved database/settings, run this command
from this folder:

  docker compose --file compose.yaml down --volumes

This permanently deletes the database, server settings, certificate and logs.
