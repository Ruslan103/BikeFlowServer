BikeFlowServer @VERSION@ — Docker image for linux/amd64

Requirements
  - Docker Desktop with Linux containers (Windows/macOS), or Docker Engine and Docker Compose v2 (Linux).
  - An x86-64 Docker host. This release contains a linux/amd64 image; ARM hosts need amd64 emulation.
  - About 3 GB of free disk space for the package and Docker images.
  - Java 21 is included in the image. A host JVM is not required.

First start
  1. Extract the complete ZIP archive to a folder.
  2. On Windows, run start-windows.bat. On Linux/macOS, run: sh ./start-linux.sh
     The first run creates .env from .env.example and stops. Edit .env and set the passwords.
  3. Set unique values for POSTGRES_PASSWORD and BIKE_ADMIN_PASSWORD, then save .env.
     BIKE_ADMIN_PASSWORD must be at least 12 characters.
  4. Run the start script again. It loads the bundled images and starts the server and database.
  5. Open https://localhost:8443/admin and sign in using BIKE_ADMIN_LOGIN and
     BIKE_ADMIN_PASSWORD from .env.

For access from another computer on your network, set its DNS name or IP address
in BIKE_TLS_DNS_NAMES or BIKE_TLS_IP_ADDRESSES before the first start. Names in
these settings are comma-separated. The certificate is created on first start.

Files
  start-windows.bat     Windows launcher: create .env if needed, load images and start the application.
  start-linux.sh        Linux/macOS launcher: create .env if needed, load images and start the application.
  stop-windows.bat     Windows: stop the application without deleting its database or settings.
  stop-linux.sh        Linux/macOS: stop the application without deleting its database or settings.
  logs.bat / logs.sh    Follow the server log. Press Ctrl+C to return to the console.
  .env                 Local passwords and connection settings. Keep this file private.

The first start may take a minute while PostgreSQL initializes its database.
The browser may show a certificate warning for the self-signed local HTTPS
certificate. The server also exposes HTTP on port 8080.

To remove the installation and its saved database/settings, run this command
from this folder:

  docker compose --file compose.yaml down --volumes

This permanently deletes the database, server settings, certificate and logs.


BikeFlowServer @VERSION@ — Docker-образ для linux/amd64

Требования
  - Docker Desktop с поддержкой Linux-контейнеров (Windows/macOS) либо Docker Engine и Docker Compose v2 (Linux).
  - Сервер с процессором x86-64. В релиз включён образ linux/amd64; для ARM требуется эмуляция amd64.
  - Около 3 ГБ свободного места для пакета и Docker-образов.
  - Java 21 уже включена в образ. Устанавливать JVM на хост не нужно.

Первый запуск
  1. Распакуйте ZIP-архив в папку.
  2. В Windows запустите start-windows.bat. В Linux/macOS выполните: sh ./start-linux.sh
     При первом запуске скрипт создаст .env из .env.example и завершится. Отредактируйте .env и задайте пароли.
  3. Задайте уникальные значения для POSTGRES_PASSWORD и BIKE_ADMIN_PASSWORD и сохраните .env.
     Длина BIKE_ADMIN_PASSWORD должна быть не менее 12 символов.
  4. Запустите скрипт ещё раз. Он загрузит Docker-образы из архива и запустит сервер с базой данных.
  5. Откройте https://localhost:8443/admin и войдите, используя BIKE_ADMIN_LOGIN и
     BIKE_ADMIN_PASSWORD из файла .env.

Для доступа с другого компьютера в локальной сети укажите его DNS-имя или IP-адрес
в BIKE_TLS_DNS_NAMES или BIKE_TLS_IP_ADDRESSES до первого запуска. Если адресов
несколько, разделите их запятыми. Сертификат будет создан при первом запуске.

Файлы
  start-windows.bat     Запуск в Windows: создаёт .env при необходимости, загружает образы и запускает приложение.
  start-linux.sh        Запуск в Linux/macOS: создаёт .env при необходимости, загружает образы и запускает приложение.
  stop-windows.bat     Windows: останавливает приложение, не удаляя базу данных и настройки.
  stop-linux.sh        Linux/macOS: останавливает приложение, не удаляя базу данных и настройки.
  logs.bat / logs.sh    Показывают журнал сервера. Нажмите Ctrl+C, чтобы вернуться в консоль.
  .env                 Локальные пароли и настройки подключения. Храните файл в безопасном месте.

При первом запуске PostgreSQL может понадобиться около минуты для инициализации базы.
Браузер может показать предупреждение о самоподписанном HTTPS-сертификате.
Сервер также использует HTTP-порт 8080.

Чтобы удалить установку вместе с сохранённой базой данных и настройками, выполните
в этой папке команду:

  docker compose --file compose.yaml down --volumes

Эта команда безвозвратно удалит базу данных, настройки сервера, сертификат и журналы.
